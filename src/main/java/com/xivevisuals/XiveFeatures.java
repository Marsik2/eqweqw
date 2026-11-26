package com.xivevisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Логика функций: спринт, приближение, счётчики, чат. */
public final class XiveFeatures {
    public static KeyBinding zoomKey;

    public static double speed;
    public static int combo, hits, deaths;
    public static final long SESSION_START = System.currentTimeMillis();

    private static double lastX, lastZ;
    private static boolean wasDead, forcedSprint, zoomed;
    private static int savedFov, prevHurt;
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    public static void tick(MinecraftClient mc) {
        updateZoom(mc);
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) return;

        // скорость
        double now = Math.hypot(p.getX() - lastX, p.getZ() - lastZ) * 20;
        speed = speed * 0.6 + now * 0.4;
        lastX = p.getX(); lastZ = p.getZ();

        // комбо сбрасывается при получении урона
        if (p.hurtTime > 0 && prevHurt == 0) combo = 0;
        prevHurt = p.hurtTime;

        // смерть
        boolean dead = p.getHealth() <= 0;
        if (dead && !wasDead) {
            deaths++;
            combo = 0;
            if (XiveConfig.DEATHPOS.on) {
                String dim = mc.world.getRegistryKey().getValue().getPath();
                p.sendMessage(Text.literal("§c☠ Вы погибли: §fX " + p.getBlockX() + " Y " + p.getBlockY()
                    + " Z " + p.getBlockZ() + " §7(" + dim + ")"), false);
            }
        }
        wasDead = dead;

        // авто-спринт (то же, что «переключаемый спринт» в настройках)
        boolean want = XiveConfig.SPRINT.on && mc.currentScreen == null
            && mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.isUsingItem()
            && p.getHungerManager().getFoodLevel() > 6;
        if (want) {
            mc.options.sprintKey.setPressed(true);
            forcedSprint = true;
        } else if (forcedSprint) {
            mc.options.sprintKey.setPressed(false);
            forcedSprint = false;
        }
    }

    private static void updateZoom(MinecraftClient mc) {
        boolean held = XiveConfig.ZOOM.on && zoomKey != null && zoomKey.isPressed() && mc.currentScreen == null;
        if (held && !zoomed) {
            savedFov = mc.options.getFov().getValue();
            mc.options.getFov().setValue(30);
            zoomed = true;
        } else if (!held && zoomed) {
            mc.options.getFov().setValue(savedFov);
            zoomed = false;
        }
    }

    /** Вызывается, когда игрок бьёт существо. */
    public static void onHit() {
        hits++;
        combo++;
        if (XiveConfig.HITSOUND.on) {
            MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 0.5f));
        }
    }

    /** Обработка сообщений чата: время и звук при упоминании ника. */
    public static Text onChat(Text msg, boolean overlay) {
        if (overlay) return msg;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (XiveConfig.MENTION.on && mc.player != null) {
            String me = mc.player.getName().getString();
            String s = msg.getString();
            if (s.contains(me) && !s.startsWith("<" + me + ">")) {
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f, 1f));
            }
        }
        if (XiveConfig.CHATTIME.on) {
            return Text.literal("§7[" + LocalTime.now().format(HM) + "] §r").append(msg);
        }
        return msg;
    }

    private XiveFeatures() {}
}
