package com.prisma.match3.engine;

/**
 * The colored gem families used across the board. Each family carries a base
 * RGB color that the 3D renderer feeds into its lighting model, plus a lighter
 * "sheen" color used for facet highlights so every gem reads as a distinct,
 * faceted crystal rather than a flat sprite.
 */
public enum GemType {
    RUBY   (0xE23B4E, 0xFF8FA0, "Ruby"),
    AMBER  (0xF39422, 0xFFD08A, "Amber"),
    CITRINE(0xF7D33B, 0xFFF29A, "Citrine"),
    EMERALD(0x3FBF6B, 0x9CF0B8, "Emerald"),
    SAPPHIRE(0x2E8CF0, 0x8FC6FF, "Sapphire"),
    AMETHYST(0x9B54E0, 0xD3A8FF, "Amethyst"),
    QUARTZ (0xE8ECF5, 0xFFFFFF, "Quartz");

    public final float r, g, b;         // base color 0..1
    public final float sr, sg, sb;      // sheen color 0..1
    public final String display;

    GemType(int base, int sheen, String display) {
        this.r = ((base >> 16) & 0xFF) / 255f;
        this.g = ((base >> 8) & 0xFF) / 255f;
        this.b = (base & 0xFF) / 255f;
        this.sr = ((sheen >> 16) & 0xFF) / 255f;
        this.sg = ((sheen >> 8) & 0xFF) / 255f;
        this.sb = (sheen & 0xFF) / 255f;
        this.display = display;
    }

    public int argb() {
        return 0xFF000000
                | ((int) (r * 255) << 16)
                | ((int) (g * 255) << 8)
                | ((int) (b * 255));
    }

    private static final GemType[] VALUES = values();
    public static GemType byIndex(int i) { return VALUES[i]; }
    public static int count() { return VALUES.length; }
}
