package com.xivevisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Информационный HUD. Показывает только то, что клиент и так знает; ничего не делает за игрока. */
public final class XiveHud {
    private static final ArrayDeque<Long> LMB = new ArrayDeque<>();
    private static final ArrayDeque<Long> RMB = new ArrayDeque<>();
    private static boolean lastL, lastR;
    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private record Line(String text, int color) {}

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden || !XiveConfig.HUD.on) return;
        pollClicks(mc);
        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();

        if (XiveConfig.CPS.on) ctx.drawText(tr, "CPS: " + LMB.size() + " | " + RMB.size(), 6, 6, 0xFFFFFFFF, true);
        if (XiveConfig.EFFECTS.on) drawEffects(ctx, tr, mc);
        if (XiveConfig.KEYS.on) drawKeys(ctx, tr, mc, 6, sh / 2 - 30);
        if (XiveConfig.ARMOR.on) drawArmor(ctx, tr, mc, sw - 64, sh / 2 - 45);
        if (XiveConfig.TARGET.on) drawTarget(ctx, tr, mc, sw / 2, sh / 2 + 30);

        int y = 6;
        for (Line l : info(mc)) {
            ctx.drawText(tr, l.text, sw - 6 - tr.getWidth(l.text), y, l.color, true);
            y += 11;
        }
        drawWarnings(ctx, tr, mc, sw / 2, sh / 2 - 50);
    }

    // ------------------------------------------------------------ информация справа сверху

    private static List<Line> info(MinecraftClient mc) {
        List<Line> out = new ArrayList<>();
        var p = mc.player;
        int white = 0xFFFFFFFF;

        if (XiveConfig.FPS.on) out.add(new Line(mc.getCurrentFps() + " FPS", white));
        if (XiveConfig.PING.on) {
            var h = mc.getNetworkHandler();
            PlayerListEntry e = h == null ? null : h.getPlayerListEntry(p.getUuid());
            if (e != null) {
                int ms = e.getLatency();
                out.add(new Line(ms + " мс", ms < 80 ? 0xFF55FF55 : ms < 160 ? 0xFFFFFF55 : 0xFFFF5555));
            }
        }
        if (XiveConfig.COORDS.on) out.add(new Line("XYZ: " + p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ(), white));
        if (XiveConfig.DIRECTION.on) {
            String d = switch (p.getHorizontalFacing()) {
                case NORTH -> "С"; case SOUTH -> "Ю"; case WEST -> "З"; case EAST -> "В"; default -> "?";
            };
            out.add(new Line(d + " (" + Math.round(MathHelper.wrapDegrees(p.getYaw())) + "°)", white));
        }
        if (XiveConfig.SPEED.on) out.add(new Line(String.format("%.1f бл/с", XiveFeatures.speed), white));
        if (XiveConfig.CLOCK.on) out.add(new Line(LocalTime.now().format(HMS), white));
        if (XiveConfig.BIOME.on) {
            String b = mc.world.getBiome(p.getBlockPos()).getKey()
                .map(k -> k.getValue())
                .map(i -> Text.translatable("biome." + i.getNamespace() + "." + i.getPath()).getString())
                .orElse("?");
            out.add(new Line("Биом: " + b, white));
        }
        if (XiveConfig.DAYTIME.on) {
            long t = mc.world.getTimeOfDay();
            long day = t / 24000L + 1;
            long tod = t % 24000L;
            int hh = (int) ((tod / 1000 + 6) % 24);
            int mm = (int) ((tod % 1000) * 60 / 1000);
            out.add(new Line(String.format("День %d, %02d:%02d", day, hh, mm), white));
        }
        if (XiveConfig.MEMORY.on) {
            Runtime rt = Runtime.getRuntime();
            long used = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
            out.add(new Line("ОЗУ: " + used + "/" + (rt.maxMemory() / 1048576L) + " МБ", white));
        }
        if (XiveConfig.SLOTS.on) {
            int free = 0;
            for (int i = 0; i < 36; i++) if (p.getInventory().getStack(i).isEmpty()) free++;
            out.add(new Line("Свободно слотов: " + free, free < 4 ? 0xFFFF5555 : white));
        }
        if (XiveConfig.SESSION.on) {
            long s = (System.currentTimeMillis() - XiveFeatures.SESSION_START) / 1000;
            out.add(new Line(String.format("Сессия %d:%02d, смертей: %d", s / 60, s % 60, XiveFeatures.deaths), white));
        }
        if (XiveConfig.ITEMS.on) {
            int totem = 0, pearl = 0, apple = 0, pot = 0;
            for (int i = 0; i < p.getInventory().size(); i++) {
                ItemStack st = p.getInventory().getStack(i);
                if (st.isEmpty()) continue;
                if (st.isOf(Items.TOTEM_OF_UNDYING)) totem += st.getCount();
                else if (st.isOf(Items.ENDER_PEARL)) pearl += st.getCount();
                else if (st.isOf(Items.GOLDEN_APPLE) || st.isOf(Items.ENCHANTED_GOLDEN_APPLE)) apple += st.getCount();
                else if (st.isOf(Items.SPLASH_POTION) || st.isOf(Items.POTION)) pot += st.getCount();
            }
            out.add(new Line("Тотемы " + totem + "  Жемчуг " + pearl + "  Яблоки " + apple + "  Зелья " + pot, white));
        }
        if (XiveConfig.COMBO.on && XiveFeatures.combo > 0) out.add(new Line("Комбо: " + XiveFeatures.combo, 0xFFFFAA00));
        if (XiveConfig.HITS.on) out.add(new Line("Ударов: " + XiveFeatures.hits, white));
        if (XiveConfig.DISTANCE.on) {
            Entity t = mc.targetedEntity;
            if (t != null) out.add(new Line(String.format("Дистанция: %.2f", p.distanceTo(t)), white));
        }
        if (XiveConfig.COOLDOWN.on) {
            int pc = Math.round(p.getAttackCooldownProgress(0.5f) * 100);
            out.add(new Line("Перезарядка: " + pc + "%", pc >= 100 ? 0xFF55FF55 : 0xFFFFFF55));
        }
        if (XiveConfig.WAYPOINTS.on && !XiveConfig.WAYPOINTS_MAP.isEmpty()) {
            String best = null; double bd = Double.MAX_VALUE;
            for (var e : XiveConfig.WAYPOINTS_MAP.entrySet()) {
                int[] c = e.getValue();
                double d = Math.sqrt(p.squaredDistanceTo(c[0] + 0.5, c[1], c[2] + 0.5));
                if (d < bd) { bd = d; best = e.getKey(); }
            }
            out.add(new Line("Метка " + best + ": " + Math.round(bd) + " м", 0xFF55FFFF));
        }
        return out;
    }

    // ------------------------------------------------------------ предупреждения

    private static void drawWarnings(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int cx, int y) {
        boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
        var p = mc.player;
        if (XiveConfig.LOWHP.on && p.getHealth() > 0 && p.getHealth() + p.getAbsorptionAmount() <= 6f && blink) {
            centered(ctx, tr, "⚠ МАЛО HP", cx, y, 0xFFFF3333);
            y += 12;
        }
        if (XiveConfig.ARMORWARN.on && blink) {
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack st = p.getEquippedStack(s);
                if (!st.isEmpty() && st.isDamageable() && (float) (st.getMaxDamage() - st.getDamage()) / st.getMaxDamage() < 0.10f) {
                    centered(ctx, tr, "⚠ Сломается: " + st.getName().getString(), cx, y, 0xFFFFAA00);
                    y += 12;
                }
            }
        }
    }

    private static void centered(DrawContext ctx, TextRenderer tr, String s, int cx, int y, int col) {
        ctx.drawText(tr, s, cx - tr.getWidth(s) / 2, y, col, true);
    }

    // ------------------------------------------------------------ CPS

    private static void pollClicks(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        long w = mc.getWindow().getHandle();
        boolean l = mc.currentScreen == null && GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = mc.currentScreen == null && GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (l && !lastL) LMB.addLast(now);
        if (r && !lastR) RMB.addLast(now);
        lastL = l; lastR = r;
        while (!LMB.isEmpty() && now - LMB.peekFirst() > 1000) LMB.removeFirst();
        while (!RMB.isEmpty() && now - RMB.peekFirst() > 1000) RMB.removeFirst();
    }

    // ------------------------------------------------------------ клавиши

    private static void drawKeys(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int x, int y) {
        var o = mc.options;
        key(ctx, tr, "W", x + 22, y, 20, o.forwardKey.isPressed());
        key(ctx, tr, "A", x, y + 22, 20, o.leftKey.isPressed());
        key(ctx, tr, "S", x + 22, y + 22, 20, o.backKey.isPressed());
        key(ctx, tr, "D", x + 44, y + 22, 20, o.rightKey.isPressed());
        key(ctx, tr, "ЛКМ", x, y + 44, 31, lastL);
        key(ctx, tr, "ПКМ", x + 33, y + 44, 31, lastR);
    }

    private static void key(DrawContext ctx, TextRenderer tr, String label, int x, int y, int w, boolean down) {
        ctx.fill(x, y, x + w, y + 20, down ? 0xB0FFFFFF : 0x80000000);
        ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + 6, down ? 0xFF000000 : 0xFFFFFFFF, false);
    }

    // ------------------------------------------------------------ броня

    private static void drawArmor(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int x, int y) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                                 EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
        int row = 0;
        for (EquipmentSlot s : slots) {
            ItemStack st = mc.player.getEquippedStack(s);
            if (st.isEmpty()) continue;
            int yy = y + row * 18;
            ctx.drawItem(st, x, yy);
            if (st.isDamageable()) {
                int max = st.getMaxDamage();
                int left = max - st.getDamage();
                float f = (float) left / max;
                int col = f > 0.5f ? 0xFF55FF55 : f > 0.25f ? 0xFFFFFF55 : 0xFFFF5555;
                ctx.drawText(tr, left + "/" + max, x + 20, yy + 5, col, true);
            } else if (st.getCount() > 1) {
                ctx.drawText(tr, "x" + st.getCount(), x + 20, yy + 5, 0xFFFFFFFF, true);
            }
            row++;
        }
    }

    // ------------------------------------------------------------ эффекты

    private static void drawEffects(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        int y = 20;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            Text name = e.getEffectType().value().getName();
            String time;
            int col = 0xFFFFFFFF;
            if (e.isInfinite()) {
                time = "∞";
            } else {
                int sec = e.getDuration() / 20;
                time = (sec / 60) + ":" + String.format("%02d", sec % 60);
                if (sec < 10) col = 0xFFFF5555;
            }
            ctx.drawText(tr, name.copy().append(" " + (e.getAmplifier() + 1) + "  " + time), 6, y, col, true);
            y += 11;
        }
    }

    // ------------------------------------------------------------ цель

    private static void drawTarget(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int cx, int y) {
        Entity t = mc.targetedEntity;
        if (!(t instanceof PlayerEntity p)) return;
        float hp = p.getHealth() + p.getAbsorptionAmount();
        float max = Math.max(1f, p.getMaxHealth());
        float f = Math.min(1f, hp / max);
        int w = 120, x = cx - w / 2;
        ctx.fill(x - 4, y - 4, x + w + 4, y + 26, 0x90000000);
        ctx.drawText(tr, p.getName(), x, y, 0xFFFFFFFF, true);
        ctx.fill(x, y + 13, x + w, y + 19, 0xFF333333);
        int col = f > 0.5f ? 0xFF55FF55 : f > 0.25f ? 0xFFFFFF55 : 0xFFFF5555;
        ctx.fill(x, y + 13, x + (int) (w * f), y + 19, col);
        String s = String.format("%.1f", hp / 2f) + " ♥";
        ctx.drawText(tr, s, x + w - tr.getWidth(s), y, col, true);
    }

    private XiveHud() {}
}
