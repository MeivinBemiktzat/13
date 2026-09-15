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
            "טור 4", "רשת 2×4", "פס עליון + 3", "5 מרכזי", "רשת 3×4", "רצועה תחתונה",
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
            case 14: for (int i = 0; i < 4; i++) frame(out, m, m + i * (H + 3 * g) / 4, W, (H - 3 * g) / 4); break;
            case 15: grid(out, m, m, W, H, g, 2, 4); break;
            case 16: frame(out, m, m, W, (H - g) * 0.5f);
                     for (int i = 0; i < 3; i++) frame(out, m + i * (W + 2 * g) / 3, m + (H - g) * 0.5f + g, (W - 2 * g) / 3, (H - g) * 0.5f); break;
            case 17: frame(out, m + W * 0.28f, m + H * 0.28f, W * 0.44f, H * 0.44f);
                     frame(out, m, m, W * 0.24f, H * 0.24f); frame(out, m + W * 0.76f, m, W * 0.24f, H * 0.24f);
                     frame(out, m, m + H * 0.76f, W * 0.24f, H * 0.24f); frame(out, m + W * 0.76f, m + H * 0.76f, W * 0.24f, H * 0.24f); break;
            case 18: grid(out, m, m, W, H, g, 3, 4); break;
            case 19: frame(out, m, m, W, H * 0.7f);
                     for (int i = 0; i < 4; i++) frame(out, m + i * (W + 3 * g) / 4, m + H * 0.7f + g, (W - 3 * g) / 4, H * 0.3f - g); break;
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
            "זר פרחים", "באנר מסיבה", "פספורט הרפתקאות", "שמי כוכבים",
            "בוהו טבעי", "מלכותי", "חורף קסום", "אלבום קלאסי",
            // 22+ image-sticker templates
            "מסגרת פרחונית", "זר ורדים", "אלגנט זהב", "תינוק מתוק", "יום הולדת",
            "חתונה מזל טוב", "פרחי בר", "וינטג' זכרונות", "חיות מחמד", "קיץ בים",
            "חורף לבן", "יום בבית ספר", "אלוף הספורט", "טיול בטבע", "מסיבת מוזיקה",
            "פסטל חלומי", "גיאומטרי מודרני", "באנר קלאסי", "פינות זהב", "אהבה פורחת",
            "משפחה שלנו", "גן פורח", "טעים במטבח", "רכות פסטל", "חגיגת פרחים",
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
            case 14: // floral wreath
                pg.bgType = Model.BG_PATTERN; pg.bgColor = 0xFFFFF8F3; pg.bgColor2 = 0xFFF3D9D2; pg.patternId = 4;
                clip(pg, 35, pw * 0.20f, ph * 0.10f, pw * 0.6f, 0xFFEC7CA5, 0xFFF6B26B);
                photo(pg, pw * 0.30f, ph * 0.30f, pw * 0.4f, ph * 0.28f, pw * 0.5f);
                title(pg, "רגעים של אושר", pw, ph * 0.72f, 0xFFAD1457, 5, false);
                clip(pg, 37, pw * 0.30f, ph * 0.86f, pw * 0.4f, 0xFFAD1457, 0xFFAD1457);
                break;
            case 15: // party banner
                grad(pg, 0xFF6A11CB, 0xFF2575FC, 60);
                clip(pg, 33, pw * 0.08f, ph * 0.04f, pw * 0.84f, 0xFFFFD54F, 0xFFEC407A);
                photo(pg, m, ph * 0.22f, pw - 2 * m, ph * 0.5f, pw * 0.04f);
                clip(pg, 27, 0, ph * 0.7f, pw, 0xFFFFD54F, 0xFFEC407A);
                title(pg, "חוגגים!", pw, ph * 0.78f, 0xFFFFFFFF, 2, true);
                clip(pg, 24, pw * 0.05f, ph * 0.08f, pw * 0.14f, 0xFFEF5350, 0xFFFFFFFF);
                clip(pg, 24, pw * 0.80f, ph * 0.10f, pw * 0.14f, 0xFF42A5F5, 0xFFFFFFFF);
                break;
            case 16: // adventure passport
                grad(pg, 0xFFFDFCFB, 0xFFE2D1C3, 90);
                clip(pg, 41, pw * 0.60f, ph * 0.05f, pw * 0.34f, 0xFF5D4037, 0xFFEF5350);
                clip(pg, 43, pw * 0.06f, ph * 0.06f, pw * 0.18f, 0xFF00695C, 0xFFEF5350);
                title(pg, "ADVENTURE", pw, ph * 0.16f, 0xFF3E2723, 1, true);
                photo(pg, m, ph * 0.26f, (pw - 2 * m) * 0.55f, ph * 0.4f, pw * 0.02f);
                photo(pg, m + (pw - 2 * m) * 0.6f, ph * 0.26f, (pw - 2 * m) * 0.4f, ph * 0.4f, pw * 0.02f);
                clip(pg, 14, pw * 0.1f, ph * 0.72f, pw * 0.8f, 0xFF8D6E63, 0xFFFFFFFF);
                clip(pg, 42, pw * 0.44f, ph * 0.68f, pw * 0.12f, 0xFFE53935, 0xFFFFFFFF);
                break;
            case 17: // starry sky
                grad(pg, 0xFF0F2027, 0xFF2C5364, 90); pg.overlay = 1;
                for (int i = 0; i < 10; i++) clip(pg, 22,
                        pw * (float) Math.random() * 0.92f, ph * (float) Math.random() * 0.5f, pw * 0.06f, 0xFFFFF59D, 0xFFFFFFFF);
                clip(pg, 16, pw * 0.72f, ph * 0.06f, pw * 0.2f, 0xFFFFF176, 0xFFFFF176);
                photo(pg, m, ph * 0.34f, pw - 2 * m, ph * 0.44f, pw * 0.05f);
                title(pg, "תחת הכוכבים", pw, ph * 0.82f, 0xFFFFF59D, 5, true);
                break;
            case 18: // boho natural
                solid(pg, 0xFFF3EEE7);
                clip(pg, 5, pw * 0.02f, ph * 0.10f, pw * 0.2f, 0xFF8D9B6A, 0xFF8D9B6A);
                clip(pg, 5, pw * 0.78f, ph * 0.60f, pw * 0.2f, 0xFFB08968, 0xFFB08968);
                photo(pg, pw * 0.16f, ph * 0.14f, pw * 0.68f, ph * 0.54f, pw * 0.02f);
                title(pg, "טבעי ורגוע", pw, ph * 0.74f, 0xFF7A6C53, 3, false);
                clip(pg, 36, pw * 0.36f, ph * 0.84f, pw * 0.28f, 0xFFB08968, 0xFFB08968);
                break;
            case 19: // royal
                solid(pg, 0xFF10131A); pg.overlay = 1;
                clip(pg, 28, pw * 0.40f, ph * 0.04f, pw * 0.2f, 0xFFD4AF37, 0xFFFFF176);
                photo(pg, pw * 0.14f, ph * 0.20f, pw * 0.72f, ph * 0.56f, pw * 0.02f);
                shape(pg, Model.SHAPE_LINE, pw * 0.2f, ph * 0.80f, pw * 0.6f, 4, 0xFFD4AF37);
                title(pg, "ROYAL", pw, ph * 0.84f, 0xFFD4AF37, 3, true);
                clip(pg, 34, pw * 0.30f, ph * 0.16f, pw * 0.4f, 0xFFD4AF37, 0xFFD4AF37);
                break;
            case 20: // winter
                grad(pg, 0xFFE3F2FD, 0xFFBBDEFB, 90);
                for (int i = 0; i < 6; i++) clip(pg, 19,
                        pw * (0.05f + i * 0.16f), ph * (0.04f + (i % 2) * 0.06f), pw * 0.12f, 0xFF90CAF9, 0xFF90CAF9);
                photo(pg, m, ph * 0.2f, pw - 2 * m, ph * 0.52f, pw * 0.06f);
                title(pg, "חורף קסום", pw, ph * 0.78f, 0xFF1565C0, 6, true);
                clip(pg, 19, pw * 0.06f, ph * 0.82f, pw * 0.16f, 0xFF64B5F6, 0xFF64B5F6);
                break;
            case 21: // classic album
                solid(pg, 0xFFFBF9F4);
                shape(pg, Model.SHAPE_ROUND, m * 0.7f, m * 0.7f, pw - 1.4f * m, ph - 1.4f * m, 0x00000000);
                pg.els.get(pg.els.size() - 1).fillColor = 0x00000000;
                pg.els.get(pg.els.size() - 1).strokeColor = 0xFFB59F6B;
                pg.els.get(pg.els.size() - 1).strokeW = 4;
                clip(pg, 36, m, m, pw * 0.16f, 0xFFB59F6B, 0xFFB59F6B);
                title(pg, "זכרונות יקרים", pw, ph * 0.1f, 0xFF6B5B3E, 3, false);
                photo(pg, pw * 0.14f, ph * 0.22f, pw * 0.72f, ph * 0.58f, pw * 0.01f);
                clip(pg, 37, pw * 0.3f, ph * 0.86f, pw * 0.4f, 0xFFB59F6B, 0xFFB59F6B);
                break;
            case 22: // floral frame
                grad(pg, 0xFFFFF6F0, 0xFFFCE7DE, 90);
                photo(pg, pw * 0.16f, ph * 0.14f, pw * 0.68f, ph * 0.56f, pw * 0.02f);
                img(pg, "frames/05.png", pw * 0.10f, ph * 0.08f, pw * 0.80f, ph * 0.68f);
                img(pg, "flowers/01.png", pw * 0.00f, ph * 0.66f, pw * 0.26f, pw * 0.26f);
                img(pg, "flowers/06.png", pw * 0.74f, ph * 0.66f, pw * 0.26f, pw * 0.26f);
                title(pg, "רגעים יפים", pw, ph * 0.80f, 0xFFC2185B, 5, false);
                break;
            case 23: // rose bouquet
                pg.bgType = Model.BG_PATTERN; pg.bgColor = 0xFFFFFBF7; pg.bgColor2 = 0xFFF3DDD0; pg.patternId = 0;
                img(pg, "banners/01.png", pw * 0.2f, ph * 0.04f, pw * 0.6f, ph * 0.12f);
                photo(pg, pw * 0.14f, ph * 0.2f, pw * 0.72f, ph * 0.5f, pw * 0.03f);
                img(pg, "flowers/02.png", pw * 0.00f, ph * 0.60f, pw * 0.3f, pw * 0.3f);
                img(pg, "flowers/09.png", pw * 0.70f, ph * 0.62f, pw * 0.3f, pw * 0.3f);
                img(pg, "ornaments/06.png", pw * 0.25f, ph * 0.74f, pw * 0.5f, pw * 0.14f);
                title(pg, "באהבה", pw, ph * 0.82f, 0xFFAD1457, 5, true);
                break;
            case 24: // elegant gold
                solid(pg, 0xFF14110F); pg.overlay = 1;
                img(pg, "ornaments/01.png", pw * 0.02f, ph * 0.03f, pw * 0.26f, pw * 0.26f);
                img(pg, "ornaments/01.png", pw * 0.72f, ph * 0.03f, pw * 0.26f, pw * 0.26f, 90);
                img(pg, "ornaments/01.png", pw * 0.72f, ph * 0.72f, pw * 0.26f, pw * 0.26f, 180);
                img(pg, "ornaments/01.png", pw * 0.02f, ph * 0.72f, pw * 0.26f, pw * 0.26f, 270);
                photo(pg, pw * 0.18f, ph * 0.2f, pw * 0.64f, ph * 0.56f, pw * 0.02f);
                title(pg, "ELEGANCE", pw, ph * 0.82f, 0xFFD4AF37, 3, true);
                break;
            case 25: // sweet baby
                grad(pg, 0xFFE8F4FF, 0xFFD6EAFB, 90);
                img(pg, "baby/07.png", pw * 0.00f, ph * 0.66f, pw * 0.28f, pw * 0.28f);
                img(pg, "baby/01.png", pw * 0.72f, ph * 0.66f, pw * 0.28f, pw * 0.28f);
                img(pg, "baby/06.png", pw * 0.78f, ph * 0.02f, pw * 0.2f, pw * 0.2f);
                photo(pg, pw * 0.16f, ph * 0.14f, pw * 0.68f, ph * 0.52f, pw * 0.08f);
                title(pg, "ברוך הבא", pw, ph * 0.74f, 0xFF1E88E5, 6, true);
                break;
            case 26: // birthday
                grad(pg, 0xFFFFF3E0, 0xFFFFE0B2, 90);
                img(pg, "party/03.png", pw * 0.02f, ph * 0.02f, pw * 0.2f, pw * 0.2f);
                img(pg, "party/05.png", pw * 0.78f, ph * 0.02f, pw * 0.2f, pw * 0.2f);
                img(pg, "party/04.png", pw * 0.40f, ph * 0.72f, pw * 0.24f, pw * 0.24f);
                img(pg, "banners/03.png", pw * 0.18f, ph * 0.04f, pw * 0.64f, ph * 0.14f);
                photo(pg, pw * 0.14f, ph * 0.22f, pw * 0.72f, ph * 0.48f, pw * 0.04f);
                title(pg, "יום הולדת שמח", pw, ph * 0.72f, 0xFFE65100, 2, true);
                break;
            case 27: // wedding
                solid(pg, 0xFFFFFDFB);
                photo(pg, pw * 0.18f, ph * 0.16f, pw * 0.64f, ph * 0.52f, pw * 0.02f);
                img(pg, "frames/03.png", pw * 0.12f, ph * 0.10f, pw * 0.76f, ph * 0.64f);
                img(pg, "ornaments/07.png", pw * 0.25f, ph * 0.76f, pw * 0.5f, pw * 0.14f);
                title(pg, "מזל טוב", pw, ph * 0.83f, 0xFFB8860B, 3, true);
                break;
            case 28: // wildflowers
                solid(pg, 0xFFF7FAF3);
                for (int i = 0; i < 4; i++) img(pg, "flowers/" + String.format("%02d", 3 + i) + ".png",
                        pw * (0.02f + i * 0.24f), ph * (i % 2 == 0 ? 0.02f : 0.80f), pw * 0.18f, pw * 0.18f);
                photo(pg, pw * 0.16f, ph * 0.2f, pw * 0.68f, ph * 0.56f, pw * 0.03f);
                title(pg, "פריחה", pw, ph * 0.10f, 0xFF558B2F, 5, false);
                break;
            case 29: // vintage memories
                grad(pg, 0xFFFBF6EA, 0xFFEFE3CC, 90);
                img(pg, "banners/09.png", pw * 0.2f, ph * 0.04f, pw * 0.6f, ph * 0.13f);
                photo(pg, pw * 0.14f, ph * 0.2f, pw * 0.72f, ph * 0.5f, pw * 0.01f);
                img(pg, "vintage/08.png", pw * 0.02f, ph * 0.72f, pw * 0.2f, pw * 0.2f);
                img(pg, "vintage/12.png", pw * 0.78f, ph * 0.72f, pw * 0.2f, pw * 0.2f);
                title(pg, "זכרונות", pw, ph * 0.82f, 0xFF6D4C41, 3, false);
                break;
            case 30: // pets
                grad(pg, 0xFFEFF7FF, 0xFFDCEEFF, 90);
                img(pg, "pets/01.png", pw * 0.02f, ph * 0.74f, pw * 0.2f, pw * 0.2f);
                img(pg, "pets/02.png", pw * 0.78f, ph * 0.02f, pw * 0.2f, pw * 0.2f);
                photo(pg, pw * 0.16f, ph * 0.16f, pw * 0.68f, ph * 0.54f, pw * 0.06f);
                title(pg, "החבר הכי טוב", pw, ph * 0.76f, 0xFF00838F, 5, true);
                break;
            case 31: // summer beach
                grad(pg, 0xFF80D0F0, 0xFFCDEFFF, 90);
                img(pg, "seasons/04.png", pw * 0.78f, ph * 0.02f, pw * 0.2f, pw * 0.2f);
                img(pg, "seasons/13.png", pw * 0.02f, ph * 0.74f, pw * 0.22f, pw * 0.22f);
                photo(pg, m, ph * 0.18f, pw - 2 * m, ph * 0.52f, pw * 0.04f);
                title(pg, "קיץ שלנו", pw, ph * 0.78f, 0xFF0277BD, 2, true);
                break;
            case 32: // winter white
                grad(pg, 0xFFF0F7FF, 0xFFD8E9F5, 90);
                for (int i = 0; i < 5; i++) img(pg, "seasons/01.png",
                        pw * (0.04f + i * 0.2f), ph * (0.03f + (i % 2) * 0.05f), pw * 0.1f, pw * 0.1f);
                photo(pg, m, ph * 0.2f, pw - 2 * m, ph * 0.5f, pw * 0.06f);
                title(pg, "חורף לבן", pw, ph * 0.76f, 0xFF1565C0, 6, true);
                break;
            case 33: // school day
                solid(pg, 0xFFFFFDF5);
                img(pg, "school/01.png", pw * 0.02f, ph * 0.02f, pw * 0.16f, pw * 0.16f);
                img(pg, "school/05.png", pw * 0.80f, ph * 0.02f, pw * 0.16f, pw * 0.16f);
                img(pg, "banners/05.png", pw * 0.2f, ph * 0.72f, pw * 0.6f, ph * 0.14f);
                photo(pg, pw * 0.14f, ph * 0.2f, pw * 0.72f, ph * 0.48f, pw * 0.03f);
                title(pg, "שנה טובה", pw, ph * 0.86f, 0xFFD84315, 2, true);
                break;
            case 34: // sports
                grad(pg, 0xFF37474F, 0xFF546E7A, 90);
                img(pg, "sports/03.png", pw * 0.02f, ph * 0.74f, pw * 0.2f, pw * 0.2f);
                img(pg, "sports/05.png", pw * 0.78f, ph * 0.74f, pw * 0.2f, pw * 0.2f);
                photo(pg, m, ph * 0.16f, pw - 2 * m, ph * 0.52f, pw * 0.03f);
                title(pg, "אלוף!", pw, ph * 0.76f, 0xFFFFEB3B, 2, true);
                break;
            case 35: // nature trip
                grad(pg, 0xFFDCEFD3, 0xFFB6D7A8, 90);
                img(pg, "nature/01.png", pw * 0.02f, ph * 0.02f, pw * 0.18f, pw * 0.18f);
                img(pg, "nature/12.png", pw * 0.80f, ph * 0.76f, pw * 0.18f, pw * 0.18f);
                photo(pg, m, ph * 0.18f, pw - 2 * m, ph * 0.54f, pw * 0.04f);
                title(pg, "בטבע", pw, ph * 0.80f, 0xFF2E7D32, 5, true);
                break;
            case 36: // music party
                grad(pg, 0xFF6A11CB, 0xFFEC407A, 60);
                img(pg, "music/01.png", pw * 0.02f, ph * 0.72f, pw * 0.2f, pw * 0.2f);
                img(pg, "music/07.png", pw * 0.78f, ph * 0.04f, pw * 0.2f, pw * 0.2f);
                photo(pg, m, ph * 0.2f, pw - 2 * m, ph * 0.5f, pw * 0.04f);
                title(pg, "המסיבה שלנו", pw, ph * 0.76f, 0xFFFFFFFF, 2, true);
                break;
            case 37: // dreamy pastel
                grad(pg, 0xFFFDE7F3, 0xFFE1F0FF, 60);
                img(pg, "shapes/06.png", pw * 0.70f, ph * 0.04f, pw * 0.26f, pw * 0.16f);
                img(pg, "shapes/01.png", pw * 0.02f, ph * 0.78f, pw * 0.2f, pw * 0.14f);
                photo(pg, pw * 0.16f, ph * 0.16f, pw * 0.68f, ph * 0.54f, pw * 0.08f);
                title(pg, "חלומי", pw, ph * 0.76f, 0xFF8E24AA, 6, false);
                break;
            case 38: // modern geometric
                solid(pg, 0xFFFAFAFA);
                img(pg, "shapes/02.png", pw * 0.02f, ph * 0.02f, pw * 0.22f, pw * 0.22f);
                img(pg, "shapes/08.png", pw * 0.76f, ph * 0.76f, pw * 0.22f, pw * 0.22f);
                photo(pg, pw * 0.14f, ph * 0.16f, pw * 0.72f, ph * 0.56f, pw * 0.0f);
                title(pg, "MODERN", pw, ph * 0.78f, 0xFF212121, 1, true);
                break;
            case 39: // classic banner
                grad(pg, 0xFFF6F1E7, 0xFFEDE3CC, 90);
                img(pg, "banners/02.png", pw * 0.14f, ph * 0.05f, pw * 0.72f, ph * 0.16f);
                img(pg, "ornaments/13.png", pw * 0.3f, ph * 0.74f, pw * 0.4f, pw * 0.1f);
                photo(pg, pw * 0.14f, ph * 0.24f, pw * 0.72f, ph * 0.46f, pw * 0.02f);
                title(pg, "הסיפור שלנו", pw, ph * 0.84f, 0xFF795548, 3, false);
                break;
            case 40: // gold corners
                solid(pg, 0xFFFFFFFF);
                img(pg, "ornaments/02.png", pw * 0.01f, ph * 0.02f, pw * 0.24f, pw * 0.24f);
                img(pg, "ornaments/02.png", pw * 0.75f, ph * 0.02f, pw * 0.24f, pw * 0.24f, 90);
                img(pg, "ornaments/02.png", pw * 0.75f, ph * 0.74f, pw * 0.24f, pw * 0.24f, 180);
                img(pg, "ornaments/02.png", pw * 0.01f, ph * 0.74f, pw * 0.24f, pw * 0.24f, 270);
                photo(pg, pw * 0.2f, ph * 0.22f, pw * 0.6f, ph * 0.54f, pw * 0.02f);
                title(pg, "יוקרה", pw, ph * 0.10f, 0xFFB8860B, 3, true);
                break;
            case 41: // blooming love
                grad(pg, 0xFFFFE3EC, 0xFFFFF0F5, 90);
                img(pg, "flowers/07.png", pw * 0.00f, ph * 0.00f, pw * 0.24f, pw * 0.24f);
                img(pg, "flowers/10.png", pw * 0.76f, ph * 0.76f, pw * 0.24f, pw * 0.24f);
                photo(pg, pw * 0.18f, ph * 0.18f, pw * 0.64f, ph * 0.5f, pw * 0.5f);
                title(pg, "אהבה", pw, ph * 0.74f, 0xFFD81B60, 5, true);
                break;
            case 42: // our family
                grad(pg, 0xFFFFF8E1, 0xFFFFE0B2, 90);
                img(pg, "banners/04.png", pw * 0.18f, ph * 0.03f, pw * 0.64f, ph * 0.13f);
                photo(pg, m, ph * 0.18f, (pw - 3 * m) / 2, ph * 0.34f, pw * 0.03f);
                photo(pg, m * 2 + (pw - 3 * m) / 2, ph * 0.18f, (pw - 3 * m) / 2, ph * 0.34f, pw * 0.03f);
                photo(pg, m, ph * 0.55f, pw - 2 * m, ph * 0.34f, pw * 0.03f);
                img(pg, "flowers/12.png", pw * 0.8f, ph * 0.5f, pw * 0.18f, pw * 0.18f);
                break;
            case 43: // blooming garden
                grad(pg, 0xFFEAF6E1, 0xFFCDE8BE, 90);
                img(pg, "garden/06.png", pw * 0.02f, ph * 0.74f, pw * 0.2f, pw * 0.2f);
                img(pg, "garden/11.png", pw * 0.78f, ph * 0.74f, pw * 0.2f, pw * 0.2f);
                img(pg, "flowers/05.png", pw * 0.78f, ph * 0.02f, pw * 0.18f, pw * 0.18f);
                photo(pg, pw * 0.14f, ph * 0.16f, pw * 0.66f, ph * 0.54f, pw * 0.04f);
                title(pg, "הגינה שלי", pw, ph * 0.80f, 0xFF33691E, 5, false);
                break;
            case 44: // tasty kitchen
                grad(pg, 0xFFFFF0E6, 0xFFFFE0C7, 90);
                img(pg, "food/02.png", pw * 0.02f, ph * 0.02f, pw * 0.18f, pw * 0.18f);
                img(pg, "food/08.png", pw * 0.80f, ph * 0.76f, pw * 0.18f, pw * 0.18f);
                photo(pg, pw * 0.14f, ph * 0.18f, pw * 0.72f, ph * 0.52f, pw * 0.04f);
                title(pg, "טעים!", pw, ph * 0.78f, 0xFFBF360C, 2, true);
                break;
            case 45: // soft pastel
                grad(pg, 0xFFF3E5F5, 0xFFE1F5FE, 45);
                img(pg, "flowers/11.png", pw * 0.00f, ph * 0.70f, pw * 0.26f, pw * 0.26f);
                img(pg, "flowers/14.png", pw * 0.74f, ph * 0.00f, pw * 0.26f, pw * 0.26f);
                photo(pg, pw * 0.16f, ph * 0.16f, pw * 0.68f, ph * 0.54f, pw * 0.06f);
                img(pg, "frames/07.png", pw * 0.10f, ph * 0.10f, pw * 0.80f, ph * 0.66f);
                title(pg, "עדין ורך", pw, ph * 0.78f, 0xFF7B1FA2, 6, false);
                break;
            case 46: // flower celebration
                solid(pg, 0xFFFFFBF8);
                img(pg, "flowers/01.png", pw * 0.00f, ph * 0.00f, pw * 0.2f, pw * 0.2f);
                img(pg, "flowers/06.png", pw * 0.80f, ph * 0.00f, pw * 0.2f, pw * 0.2f);
                img(pg, "flowers/09.png", pw * 0.00f, ph * 0.78f, pw * 0.2f, pw * 0.2f);
                img(pg, "flowers/13.png", pw * 0.80f, ph * 0.78f, pw * 0.2f, pw * 0.2f);
                photo(pg, pw * 0.2f, ph * 0.2f, pw * 0.6f, ph * 0.52f, pw * 0.04f);
                title(pg, "חוגגים", pw, ph * 0.78f, 0xFFC2185B, 5, true);
                break;
            default:
                solid(pg, 0xFFFFFFFF);
                photo(pg, m, m, pw - 2 * m, ph - 2 * m, pw * 0.03f);
        }
        return pg;
    }

    private static void img(Model.Page pg, String path, float x, float y, float w, float h) {
        img(pg, path, x, y, w, h, 0f);
    }

    private static void img(Model.Page pg, String path, float x, float y, float w, float h, float rot) {
        Model.El e = new Model.El();
        e.kind = Model.KIND_IMAGE; e.uri = "asset:///stickers/" + path;
        e.x = x; e.y = y; e.w = w; e.h = h; e.rotation = rot;
        pg.els.add(e);
    }

    private static void clip(Model.Page pg, int id, float x, float y, float size, int c1, int c2) {
        Model.El e = new Model.El();
        e.kind = Model.KIND_CLIP; e.clipId = id; e.fillColor = c1; e.clipColor2 = c2;
        e.x = x; e.y = y; e.w = size; e.h = size;
        pg.els.add(e);
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
