package com.mikdash.albumdesigner;

import java.util.ArrayList;
import java.util.List;

/** Ready-made photo layouts and fully-designed themed pages. */
public final class Templates {

    /* ------------------------- Photo layouts --------------------------- */

    public static final String[] LAYOUT_NAMES = {
            "תמונה 1 מלאה", "2 לרוחב", "2 לאורך", "3 טור", "3 שורה",
            "4 רשת", "קולאז' 1+2", "קולאז' 2+1", "6 רשת", "9 רשת",
            "מגזין", "פסיפס", "לב באמצע", "פוליארויד ×3",
    };

    /** Returns photo-frame elements arranged for the given layout, in page coords. */
    public static List<Model.El> layout(int idx, int pw, int ph) {
        List<Model.El> out = new ArrayList<>();
        float g = Math.min(pw, ph) * 0.03f;       // gap
        float m = Math.min(pw, ph) * 0.05f;       // margin
        float W = pw - 2 * m, H = ph - 2 * m;
        switch (idx) {
            case 0: frame(out, m, m, W, H); break;
            case 1: frame(out, m, m, (W - g) / 2, H); frame(out, m + (W + g) / 2, m, (W - g) / 2, H); break;
            case 2: frame(out, m, m, W, (H - g) / 2); frame(out, m, m + (H + g) / 2, W, (H - g) / 2); break;
            case 3: for (int i = 0; i < 3; i++) frame(out, m, m + i * (H + 2 * g) / 3, W, (H - 2 * g) / 3); break;
            case 4: for (int i = 0; i < 3; i++) frame(out, m + i * (W + 2 * g) / 3, m, (W - 2 * g) / 3, H); break;
            case 5: grid(out, m, m, W, H, g, 2, 2); break;
            case 6: frame(out, m, m, W, (H - g) * 0.55f);
                    frame(out, m, m + (H - g) * 0.55f + g, (W - g) / 2, (H - g) * 0.45f);
                    frame(out, m + (W + g) / 2, m + (H - g) * 0.55f + g, (W - g) / 2, (H - g) * 0.45f); break;
            case 7: frame(out, m, m, (W - g) / 2, (H - g) * 0.45f);
                    frame(out, m + (W + g) / 2, m, (W - g) / 2, (H - g) * 0.45f);
                    frame(out, m, m + (H - g) * 0.45f + g, W, (H - g) * 0.55f); break;
            case 8: grid(out, m, m, W, H, g, 2, 3); break;
            case 9: grid(out, m, m, W, H, g, 3, 3); break;
            case 10: frame(out, m, m, W * 0.62f, H);
                     frame(out, m + W * 0.62f + g, m, W * 0.38f - g, (H - g) / 2);
                     frame(out, m + W * 0.62f + g, m + (H + g) / 2, W * 0.38f - g, (H - g) / 2); break;
            case 11: mosaic(out, m, m, W, H, g); break;
            case 12: {
                Model.El e = frameEl(m + W * 0.2f, m + H * 0.2f, W * 0.6f, H * 0.6f);
                e.corner = Math.min(e.w, e.h) * 0.5f; out.add(e); break;
            }
            case 13: polaroids(out, pw, ph); break;
            default: frame(out, m, m, W, H);
        }
        return out;
    }

    private static void grid(List<Model.El> out, float x, float y, float W, float H, float g, int cols, int rows) {
        float cw = (W - (cols - 1) * g) / cols, ch = (H - (rows - 1) * g) / rows;
        for (int rI = 0; rI < rows; rI++)
            for (int cI = 0; cI < cols; cI++)
                frame(out, x + cI * (cw + g), y + rI * (ch + g), cw, ch);
    }

    private static void mosaic(List<Model.El> out, float x, float y, float W, float H, float g) {
        float bw = W * 0.6f;
        frame(out, x, y, bw, (H - g) * 0.6f);
        frame(out, x + bw + g, y, W - bw - g, (H - g) * 0.6f);
        float y2 = y + (H - g) * 0.6f + g, h2 = (H - g) * 0.4f;
        frame(out, x, y2, (W - 2 * g) / 3, h2);
        frame(out, x + (W - 2 * g) / 3 + g, y2, (W - 2 * g) / 3, h2);
        frame(out, x + 2 * ((W - 2 * g) / 3 + g), y2, (W - 2 * g) / 3, h2);
    }

    private static void polaroids(List<Model.El> out, int pw, int ph) {
        float w = pw * 0.34f, h = w * 1.15f;
        float[] rot = {-9, 6, -4};
        for (int i = 0; i < 3; i++) {
            Model.El e = frameEl(pw * (0.10f + i * 0.24f), ph * (0.28f + (i % 2) * 0.12f), w, h);
            e.borderW = w * 0.06f; e.borderColor = 0xFFFFFFFF; e.rotation = rot[i];
            out.add(e);
        }
    }

    private static void frame(List<Model.El> out, float x, float y, float w, float h) {
        out.add(frameEl(x, y, w, h));
    }

    private static Model.El frameEl(float x, float y, float w, float h) {
        Model.El e = new Model.El();
        e.kind = Model.KIND_PHOTO;
        e.x = x; e.y = y; e.w = w; e.h = h;
        e.corner = Math.min(w, h) * 0.04f;
        return e;
    }

    /* ------------------------ Themed full pages ------------------------ */

    public static final String[] THEME_NAMES = {
            "רומנטי ❤", "חתונה 💍", "תינוק 👶", "יום הולדת 🎂", "טיולים ✈",
            "מינימלי", "וינטג'", "חגיגי ✨", "טבע 🌿", "ים וקיץ 🌊",
            "פסטל", "שחור זהב", "משפחה", "פרחוני 🌸",
    };

    public static Model.Page theme(int idx, int pw, int ph) {
        Model.Page pg = new Model.Page();
        float m = Math.min(pw, ph) * 0.06f;
        switch (idx) {
            case 0:
                grad(pg, 0xFFFF9A9E, 0xFFFAD0C4, 60);
                sticker(pg, "❤", pw * 0.06f, ph * 0.05f, pw * 0.12f);
                sticker(pg, "❤", pw * 0.80f, ph * 0.86f, pw * 0.14f);
                photo(pg, m, ph * 0.14f, pw - 2 * m, ph * 0.6f, pw * 0.03f);
                title(pg, "אהבה", pw, ph * 0.80f, 0xFFB71C1C, 2, true);
                break;
            case 1:
                grad(pg, 0xFFFDFCFB, 0xFFE2D1C3, 90);
                shape(pg, Model.SHAPE_LINE, m, ph * 0.12f, pw - 2 * m, 6, 0xFFB8860B);
                title(pg, "היום בו התחלנו", pw, ph * 0.04f, 0xFF8D6E63, 3, false);
                photo(pg, pw * 0.14f, ph * 0.18f, pw * 0.72f, ph * 0.58f, pw * 0.36f);
                sticker(pg, "💍", pw * 0.44f, ph * 0.80f, pw * 0.12f);
                title(pg, "בכל הלב", pw, ph * 0.88f, 0xFFB8860B, 3, true);
                break;
            case 2:
                grad(pg, 0xFFA1C4FD, 0xFFC2E9FB, 90);
                sticker(pg, "🐣", pw * 0.08f, ph * 0.82f, pw * 0.14f);
                sticker(pg, "⭐", pw * 0.82f, ph * 0.06f, pw * 0.1f);
                photo(pg, m, ph * 0.16f, pw - 2 * m, ph * 0.56f, pw * 0.08f);
                title(pg, "ברוך הבא לעולם", pw, ph * 0.78f, 0xFF1E88E5, 5, true);
                break;
            case 3:
                solid(pg, 0xFF4A148C);
                for (int i = 0; i < 6; i++) sticker(pg, i % 2 == 0 ? "🎈" : "🎉",
                        pw * (0.05f + i * 0.15f), ph * (0.04f + (i % 2) * 0.06f), pw * 0.1f);
                photo(pg, m, ph * 0.2f, pw - 2 * m, ph * 0.52f, pw * 0.04f);
                title(pg, "יום הולדת שמח!", pw, ph * 0.78f, 0xFFFFD54F, 2, true);
                sticker(pg, "🎂", pw * 0.44f, ph * 0.88f, pw * 0.14f);
                break;
            case 4:
                grad(pg, 0xFF84FAB0, 0xFF8FD3F4, 45);
                title(pg, "הרפתקאות", pw, ph * 0.05f, 0xFF00695C, 4, true);
                photo(pg, m, ph * 0.16f, (pw - 2 * m) * 0.62f, ph * 0.5f, pw * 0.03f);
                photo(pg, m + (pw - 2 * m) * 0.66f, ph * 0.16f, (pw - 2 * m) * 0.34f, ph * 0.24f, pw * 0.03f);
                photo(pg, m + (pw - 2 * m) * 0.66f, ph * 0.42f, (pw - 2 * m) * 0.34f, ph * 0.24f, pw * 0.03f);
                sticker(pg, "✈", pw * 0.8f, ph * 0.82f, pw * 0.14f);
                break;
            case 5:
                solid(pg, 0xFFFFFFFF);
                shape(pg, Model.SHAPE_LINE, m, ph * 0.1f, pw * 0.3f, 4, 0xFF212121);
                title(pg, "MEMORIES", pw, ph * 0.03f, 0xFF212121, 1, false);
                photo(pg, m, ph * 0.16f, pw - 2 * m, ph * 0.7f, 0);
                break;
            case 6:
                solid(pg, 0xFFFDF6E3);
                shape(pg, Model.SHAPE_ROUND, m * 0.5f, m * 0.5f, pw - m, ph - m, 0x00000000);
                pg.els.get(pg.els.size() - 1).fillColor = 0x00000000;
                pg.els.get(pg.els.size() - 1).strokeColor = 0xFF8D6E63;
                pg.els.get(pg.els.size() - 1).strokeW = 6;
                title(pg, "רגעים יקרים", pw, ph * 0.08f, 0xFF6D4C41, 3, false);
                photo(pg, pw * 0.16f, ph * 0.2f, pw * 0.68f, ph * 0.56f, pw * 0.02f);
                sticker(pg, "🕊", pw * 0.44f, ph * 0.82f, pw * 0.12f);
                break;
            case 7:
                grad(pg, 0xFFCC208E, 0xFF6713D2, 45);
                for (int i = 0; i < 8; i++) sticker(pg, "✨",
                        pw * (float) Math.random() * 0.9f, ph * (float) Math.random() * 0.95f, pw * 0.06f);
                photo(pg, m, ph * 0.18f, pw - 2 * m, ph * 0.56f, pw * 0.05f);
                title(pg, "לילה קסום", pw, ph * 0.8f, 0xFFFFFFFF, 2, true);
                break;
            case 8:
                grad(pg, 0xFFDCE35B, 0xFF45B649, 90);
                sticker(pg, "🌿", pw * 0.04f, ph * 0.04f, pw * 0.14f);
                sticker(pg, "🍀", pw * 0.82f, ph * 0.84f, pw * 0.14f);
                photo(pg, m, ph * 0.16f, pw - 2 * m, ph * 0.6f, pw * 0.06f);
                title(pg, "בחיק הטבע", pw, ph * 0.8f, 0xFF1B5E20, 5, true);
                break;
            case 9:
                grad(pg, 0xFF2193B0, 0xFF6DD5ED, 90);
                sticker(pg, "🌊", pw * 0.06f, ph * 0.85f, pw * 0.14f);
                sticker(pg, "☀", pw * 0.82f, ph * 0.05f, pw * 0.13f);
                photo(pg, m, ph * 0.18f, pw - 2 * m, ph * 0.56f, pw * 0.04f);
                title(pg, "קיץ בלתי נשכח", pw, ph * 0.8f, 0xFFFFFFFF, 2, true);
                break;
            case 10:
                grad(pg, 0xFFFBC2EB, 0xFFA6C1EE, 60);
                photo(pg, pw * 0.1f, ph * 0.12f, pw * 0.8f, ph * 0.5f, pw * 0.06f);
                title(pg, "מתוק ורך", pw, ph * 0.68f, 0xFF7B1FA2, 6, false);
                sticker(pg, "🎀", pw * 0.44f, ph * 0.82f, pw * 0.12f);
                break;
            case 11:
                solid(pg, 0xFF111111);
                shape(pg, Model.SHAPE_ROUND, m * 0.6f, m * 0.6f, pw - 1.2f * m, ph - 1.2f * m, 0x00000000);
                pg.els.get(pg.els.size() - 1).fillColor = 0x00000000;
                pg.els.get(pg.els.size() - 1).strokeColor = 0xFFD4AF37;
                pg.els.get(pg.els.size() - 1).strokeW = 5;
                title(pg, "GOLD", pw, ph * 0.08f, 0xFFD4AF37, 3, true);
                photo(pg, pw * 0.14f, ph * 0.2f, pw * 0.72f, ph * 0.58f, pw * 0.02f);
                break;
            case 12:
                grad(pg, 0xFFFFF3E0, 0xFFFFE0B2, 90);
                title(pg, "המשפחה שלנו", pw, ph * 0.05f, 0xFFE65100, 3, true);
                photo(pg, m, ph * 0.16f, (pw - 3 * m) / 2, ph * 0.34f, pw * 0.03f);
                photo(pg, m * 2 + (pw - 3 * m) / 2, ph * 0.16f, (pw - 3 * m) / 2, ph * 0.34f, pw * 0.03f);
                photo(pg, m, ph * 0.54f, pw - 2 * m, ph * 0.36f, pw * 0.03f);
                break;
            case 13:
                grad(pg, 0xFFFFDEE9, 0xFFB5FFFC, 60);
                sticker(pg, "🌸", pw * 0.03f, ph * 0.02f, pw * 0.16f);
                sticker(pg, "🌷", pw * 0.82f, ph * 0.82f, pw * 0.16f);
                sticker(pg, "🌺", pw * 0.02f, ph * 0.82f, pw * 0.14f);
                photo(pg, pw * 0.16f, ph * 0.18f, pw * 0.68f, ph * 0.56f, pw * 0.5f);
                title(pg, "פריחה", pw, ph * 0.8f, 0xFFAD1457, 6, true);
                break;
            default:
                solid(pg, 0xFFFFFFFF);
                photo(pg, m, m, pw - 2 * m, ph - 2 * m, pw * 0.03f);
        }
        return pg;
    }

    /* --------------------------- builders ------------------------------ */

    private static void solid(Model.Page pg, int c) { pg.bgType = Model.BG_SOLID; pg.bgColor = c; }

    private static void grad(Model.Page pg, int a, int b, int ang) {
        pg.bgType = Model.BG_GRADIENT; pg.bgColor = a; pg.bgColor2 = b; pg.gradientAngle = ang;
    }

    private static void photo(Model.Page pg, float x, float y, float w, float h, float corner) {
        Model.El e = new Model.El();
        e.kind = Model.KIND_PHOTO; e.x = x; e.y = y; e.w = w; e.h = h; e.corner = corner;
        pg.els.add(e);
    }

    private static void title(Model.Page pg, String t, int pw, float y, int color, int font, boolean bold) {
        Model.El e = new Model.El();
        e.kind = Model.KIND_TEXT; e.text = t; e.textColor = color; e.font = font; e.bold = bold;
        e.x = pw * 0.05f; e.y = y; e.w = pw * 0.9f; e.h = pw * 0.14f;
        e.textSize = pw * 0.09f; e.align = Model.ALIGN_CENTER; e.shadow = 1;
        pg.els.add(e);
    }

    private static void sticker(Model.Page pg, String em, float x, float y, float size) {
        Model.El e = new Model.El();
        e.kind = Model.KIND_STICKER; e.emoji = em; e.x = x; e.y = y; e.w = size; e.h = size;
        pg.els.add(e);
    }

    private static void shape(Model.Page pg, int type, float x, float y, float w, float h, int color) {
        Model.El e = new Model.El();
        e.kind = Model.KIND_SHAPE; e.shapeType = type; e.x = x; e.y = y; e.w = w; e.h = h; e.fillColor = color;
        pg.els.add(e);
    }

    private Templates() {}
}
