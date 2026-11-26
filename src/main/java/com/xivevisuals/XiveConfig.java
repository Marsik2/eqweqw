package com.xivevisuals;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class XiveConfig {
    public record Palette(String name, int horizon, int zenith, int c1, int c2) {}

    public static final class Module {
        public final String id, title, desc;
        public boolean on;
        Module(String id, String title, String desc, boolean on) {
            this.id = id; this.title = title; this.desc = desc; this.on = on;
        }
    }

    private static final List<Module> LIST = new ArrayList<>();
    private static Module m(String id, String title, String desc, boolean on) {
        Module mod = new Module(id, title, desc, on);
        LIST.add(mod);
        return mod;
    }

    public static final Palette[] PALETTES = {
        new Palette("Туманность", 0x1A1030, 0x02020A, 0x1F8FA0, 0xF2C14E),
        new Palette("Аврора",     0x081A22, 0x020A12, 0x14D6A8, 0x7A5CFF),
        new Palette("Космос",     0x0A1830, 0x01040C, 0x1AA6B7, 0x7FEAFF),
        new Palette("Закат",      0x2A0F22, 0x050208, 0x9A1840, 0xFF8A3D)
    };
    public static final int[] PARTICLE_LEVELS = {0, 1, 2, 4, 7};

    // ---- основные
    public static final Module HUD = m("hud", "Общий HUD", "Главный выключатель всех надписей на экране (клавиша H).", true);
    public static final Module SPRINT = m("sprint", "Авто-спринт", "Держите W — персонаж сам бежит, Ctrl нажимать не нужно. Не работает при приседании, использовании предмета и голоде (6 и меньше). Это то же самое, что «переключаемый спринт» в настройках игры.", true);
    public static final Module ZOOM = m("zoom", "Приближение", "Пока зажата клавиша C, обзор сужается (приближение). Отпустили — всё возвращается.", true);
    // ---- прежние
    public static final Module CPS = m("cps", "CPS", "Клики в секунду для ЛКМ и ПКМ.", true);
    public static final Module KEYS = m("keys", "Клавиши", "Нажатия WASD, ЛКМ и ПКМ на экране.", true);
    public static final Module ARMOR = m("armor", "Броня", "Прочность брони и предмета в руке цифрами и цветом.", true);
    public static final Module EFFECTS = m("effects", "Эффекты", "Эффекты зелий с таймером; последние 10 секунд красным.", true);
    public static final Module TARGET = m("target", "Цель", "Ник и здоровье игрока в прицеле (в пределах обычной дальности удара).", true);
    // ---- информация справа сверху
    public static final Module FPS = m("fps", "FPS", "Показывает количество кадров в секунду.", true);
    public static final Module PING = m("ping", "Пинг", "Показывает задержку до сервера в миллисекундах.", true);
    public static final Module COORDS = m("coords", "Координаты", "Ваши координаты X Y Z.", true);
    public static final Module DIRECTION = m("direction", "Направление", "Сторона света (С/Ю/З/В), куда вы смотрите, и угол поворота.", true);
    public static final Module SPEED = m("speed", "Скорость", "Ваша скорость в блоках в секунду.", false);
    public static final Module CLOCK = m("clock", "Часы", "Реальное время на вашем устройстве.", false);
    public static final Module BIOME = m("biome", "Биом", "Название биома, в котором вы находитесь.", false);
    public static final Module DAYTIME = m("daytime", "Время мира", "Номер игрового дня и время суток в мире.", false);
    public static final Module MEMORY = m("memory", "Память", "Сколько оперативной памяти использует игра.", false);
    public static final Module SLOTS = m("slots", "Слоты", "Сколько свободных слотов осталось в инвентаре.", false);
    public static final Module SESSION = m("session", "Сессия", "Сколько вы играете и сколько раз умерли за сессию.", false);
    public static final Module ITEMS = m("items", "Предметы", "Сколько у вас тотемов, жемчуга Края, золотых яблок и зелий в инвентаре.", true);
    public static final Module COMBO = m("combo", "Комбо", "Счётчик ваших ударов подряд; сбрасывается, когда вас ударили.", true);
    public static final Module HITS = m("hits", "Удары", "Общее число ваших ударов по существам за сессию.", false);
    public static final Module DISTANCE = m("distance", "Дистанция", "Расстояние до существа в прицеле.", true);
    public static final Module COOLDOWN = m("cooldown", "Перезарядка", "Заряд оружия в процентах — 100% значит удар будет полной силы.", false);
    public static final Module WAYPOINTS = m("waypoints", "Метки", "Показывает расстояние до ближайшей вашей метки. Метки: /xive wp add <имя>, /xive wp del <имя>, /xive wp list.", true);
    // ---- предупреждения и чат
    public static final Module LOWHP = m("lowhp", "Мало HP", "Мигающее предупреждение, когда осталось 3 сердца или меньше.", true);
    public static final Module ARMORWARN = m("armorwarn", "Поломка брони", "Предупреждение, когда прочность части брони ниже 10%.", true);
    public static final Module DEATHPOS = m("deathpos", "Место смерти", "После смерти пишет в чат координаты, где вы погибли.", true);
    public static final Module CHATTIME = m("chattime", "Время в чате", "Добавляет время [ЧЧ:ММ] перед сообщениями в чате.", false);
    public static final Module MENTION = m("mention", "Звук ника", "Короткий звук, когда в чате упомянули ваш ник (может сработать и на ваши собственные сообщения).", true);
    public static final Module HITSOUND = m("hitsound", "Звук удара", "Тихий звук при каждом вашем ударе по существу.", false);

    public static final Module[] MODULES = LIST.toArray(new Module[0]);

    public static final Map<String, int[]> WAYPOINTS_MAP = new LinkedHashMap<>();
    public static int palette = 0;
    public static int particleLevel = 2;

    public static Palette palette() { return PALETTES[palette]; }
    public static int particlesPerTick() { return PARTICLE_LEVELS[particleLevel]; }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("xivevisuals.properties"); }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (var in = Files.newInputStream(f)) { p.load(in); }
            for (Module mod : MODULES) {
                String v = p.getProperty(mod.id);
                if (v != null) mod.on = Boolean.parseBoolean(v);
            }
            palette = Math.floorMod(Integer.parseInt(p.getProperty("palette", "0")), PALETTES.length);
            particleLevel = Math.floorMod(Integer.parseInt(p.getProperty("particles", "2")), PARTICLE_LEVELS.length);
            for (String key : p.stringPropertyNames()) {
                if (key.startsWith("wp.")) {
                    String[] s = p.getProperty(key).split(",");
                    WAYPOINTS_MAP.put(key.substring(3), new int[]{Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2])});
                }
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Properties p = new Properties();
            for (Module mod : MODULES) p.setProperty(mod.id, Boolean.toString(mod.on));
            p.setProperty("palette", Integer.toString(palette));
            p.setProperty("particles", Integer.toString(particleLevel));
            for (var e : WAYPOINTS_MAP.entrySet()) {
                int[] c = e.getValue();
                p.setProperty("wp." + e.getKey(), c[0] + "," + c[1] + "," + c[2]);
            }
            try (var out = Files.newOutputStream(file())) { p.store(out, "XiveVisuals"); }
        } catch (Exception ignored) {}
    }

    private XiveConfig() {}
}
