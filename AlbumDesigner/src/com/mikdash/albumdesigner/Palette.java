package com.mikdash.albumdesigner;

import android.graphics.Typeface;

/** Central catalog of colors, gradients, stickers and fonts used across the app. */
public final class Palette {

    // A broad swatch palette for pickers.
    public static final int[] COLORS = {
            0xFF000000, 0xFF424242, 0xFF757575, 0xFFBDBDBD, 0xFFFFFFFF,
            0xFFB71C1C, 0xFFE53935, 0xFFEF5350, 0xFFFF7043, 0xFFFF9800,
            0xFFFFB300, 0xFFFFD54F, 0xFFFFF176, 0xFFCDDC39, 0xFF9CCC65,
            0xFF66BB6A, 0xFF26A69A, 0xFF00ACC1, 0xFF29B6F6, 0xFF2196F3,
            0xFF1E88E5, 0xFF3949AB, 0xFF5E35B1, 0xFF7B1FA2, 0xFF9C27B0,
            0xFFAB47BC, 0xFFEC407A, 0xFFF06292, 0xFFF8BBD0, 0xFF8D6E63,
            0xFF6D4C41, 0xFF4E342E, 0xFFFCE4EC, 0xFFEDE7F6, 0xFFE8F5E9,
    };

    // Gradient presets: {colorStart, colorEnd, angleDeg}
    public static final int[][] GRADIENTS = {
            {0xFFFDEB71, 0xFFF8D800, 45}, {0xFFF6D365, 0xFFFDA085, 45},
            {0xFFFF9A9E, 0xFFFAD0C4, 45}, {0xFFA18CD1, 0xFFFBC2EB, 45},
            {0xFF84FAB0, 0xFF8FD3F4, 45}, {0xFFA1C4FD, 0xFFC2E9FB, 45},
            {0xFFFBC2EB, 0xFFA6C1EE, 45}, {0xFFFDCBF1, 0xFFE6DEE9, 45},
            {0xFF667EEA, 0xFF764BA2, 45}, {0xFF6A11CB, 0xFF2575FC, 45},
            {0xFFFF6A88, 0xFFFF99AC, 45}, {0xFF11998E, 0xFF38EF7D, 45},
            {0xFF243949, 0xFF517FA4, 45}, {0xFFE8198B, 0xFFC7EAFD, 45},
            {0xFFFAD961, 0xFFF76B1C, 45}, {0xFFCC208E, 0xFF6713D2, 45},
    };

    // Solid pastel/background palette for pages.
    public static final int[] PAGE_BG = {
            0xFFFFFFFF, 0xFFFDF6E3, 0xFFF5F0FA, 0xFFEDE7F6, 0xFFE3F2FD,
            0xFFE8F5E9, 0xFFFFF3E0, 0xFFFCE4EC, 0xFFF1F8E9, 0xFF212121,
            0xFF37474F, 0xFF4A148C, 0xFF1A237E, 0xFF004D40, 0xFF3E2723,
    };

    public static final String[] STICKERS = {
            "★","☆","❤","💛","💚","💙","💜","🧡","✨","🌟","💫","⭐",
            "🌸","🌺","🌻","🌹","🌷","🌈","☀","🌙","⛅","❄","🍀","🌿",
            "🎀","🎉","🎊","🎈","🎂","🍰","🥳","🎁","👑","💎","🔥","💧",
            "📷","🖼","💐","🕊","🦋","🐣","🐥","🍓","🍒","🍑","🥂","🍾",
            "😊","😍","🥰","😘","👶","👨‍👩‍👧","💍","💒","🤍","❣","➰","♥",
    };

    public static final String[] FONT_NAMES = {
            "רגיל", "Sans", "מודגש", "Serif", "כתב מכונה", "עגול",
    };

    public static Typeface font(int i, boolean bold, boolean italic) {
        int style = Typeface.NORMAL;
        if (bold && italic) style = Typeface.BOLD_ITALIC;
        else if (bold) style = Typeface.BOLD;
        else if (italic) style = Typeface.ITALIC;
        Typeface base;
        switch (i) {
            case 1: base = Typeface.SANS_SERIF; break;
            case 2: base = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
                    return Typeface.create(base, italic ? Typeface.BOLD_ITALIC : Typeface.BOLD);
            case 3: base = Typeface.SERIF; break;
            case 4: base = Typeface.MONOSPACE; break;
            case 5: base = Typeface.create("cursive", Typeface.NORMAL); break;
            default: base = Typeface.DEFAULT; break;
        }
        return Typeface.create(base, style);
    }

    private Palette() {}
}
