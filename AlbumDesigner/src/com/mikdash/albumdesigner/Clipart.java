package com.mikdash.albumdesigner;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * A library of vector illustrations drawn with Canvas paths — crisp at any
 * size, fully offline. Every item is drawn into a normalized 0..1 box mapped
 * onto the element rect, using a primary colour (c1) and an accent (c2).
 */
public final class Clipart {

    public static final String[] NAMES = {
            "לב", "לב כפול", "לב חץ", "טבעת יהלום",                 // 0-3 love
            "עלה", "ענף עלים", "ורד", "צבעוני", "חיננית", "פרח",    // 4-9 flowers
            "תלתן", "קקטוס", "עץ", "דקל", "הר", "שמש",              // 10-15 nature
            "ירח", "ענן", "קשת", "פתית שלג", "גל", "כוכב", "ניצוץ", // 16-22 sky
            "פרץ כוכב", "בלון", "מתנה", "עוגה", "קונפטי", "כתר",     // 23-28 celebrate
            "מדליה", "שמפניה", "נר",                                  // 29-31
            "סרט באנר", "דגלון", "זר דפנה", "זר פרחים",              // 32-35 decor
            "פינה מעוטרת", "מפריד", "חץ", "מרכאות",                  // 36-39
            "מצלמה", "מטוס", "סיכת מיקום", "מצפן", "מזוודה",         // 40-44 travel
            "כוס קפה", "תו מוזיקה", "פרפר", "יונה", "כף רגל תינוק",  // 45-49
    };

    // Category ranges for the tabbed picker: {label, startInclusive, endExclusive}
    public static final Object[][] CATEGORIES = {
            {"אהבה", 0, 4}, {"פרחים", 4, 10}, {"טבע", 10, 16}, {"שמיים", 16, 23},
            {"חגיגה", 23, 32}, {"קישוט", 32, 40}, {"אובייקטים", 40, 50},
    };

    public static void draw(Canvas g, int id, float x, float y, float w, float h,
                            Paint p, int c1, int c2) {
        g.save();
        g.translate(x, y);
        g.scale(w, h); // now draw in a 0..1 box
        // use a fresh paint scaled: stroke widths are in unit space
        p.setStyle(Paint.Style.FILL);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        switch (id) {
            case 0: heart(g, p, c1); break;
            case 1: doubleHeart(g, p, c1, c2); break;
            case 2: arrowHeart(g, p, c1, c2); break;
            case 3: ring(g, p, c1, c2); break;
            case 4: leaf(g, p, c1, c2); break;
            case 5: branch(g, p, c1); break;
            case 6: rose(g, p, c1, c2); break;
            case 7: tulip(g, p, c1, c2); break;
            case 8: daisy(g, p, c1, c2); break;
            case 9: flower(g, p, c1, c2); break;
            case 10: clover(g, p, c1); break;
            case 11: cactus(g, p, c1, c2); break;
            case 12: tree(g, p, c1, c2); break;
            case 13: palm(g, p, c1, c2); break;
            case 14: mountain(g, p, c1, c2); break;
            case 15: sun(g, p, c1); break;
            case 16: moon(g, p, c1); break;
            case 17: cloud(g, p, c1); break;
            case 18: rainbow(g, p); break;
            case 19: snowflake(g, p, c1); break;
            case 20: wave(g, p, c1); break;
            case 21: star(g, p, c1, 5, 0.5f); break;
            case 22: sparkle(g, p, c1); break;
            case 23: burst(g, p, c1); break;
            case 24: balloon(g, p, c1, c2); break;
            case 25: gift(g, p, c1, c2); break;
            case 26: cake(g, p, c1, c2); break;
            case 27: confetti(g, p, c1, c2); break;
            case 28: crown(g, p, c1, c2); break;
            case 29: medal(g, p, c1, c2); break;
            case 30: champagne(g, p, c1, c2); break;
            case 31: candle(g, p, c1, c2); break;
            case 32: banner(g, p, c1, c2); break;
            case 33: pennant(g, p, c1, c2); break;
            case 34: laurel(g, p, c1); break;
            case 35: floralWreath(g, p, c1, c2); break;
            case 36: flourish(g, p, c1); break;
            case 37: divider(g, p, c1); break;
            case 38: arrow(g, p, c1); break;
            case 39: quote(g, p, c1); break;
            case 40: camera(g, p, c1, c2); break;
            case 41: plane(g, p, c1, c2); break;
            case 42: pin(g, p, c1, c2); break;
            case 43: compass(g, p, c1, c2); break;
            case 44: suitcase(g, p, c1, c2); break;
            case 45: coffee(g, p, c1, c2); break;
            case 46: note(g, p, c1); break;
            case 47: butterfly(g, p, c1, c2); break;
            case 48: dove(g, p, c1, c2); break;
            case 49: footprint(g, p, c1); break;
            default: heart(g, p, c1);
        }
        g.restore();
    }

    /* ------------------------- primitives ------------------------------ */

    private static Path P() { return new Path(); }
    private static void fill(Canvas g, Path pa, Paint p, int c) { p.setStyle(Paint.Style.FILL); p.setColor(c); g.drawPath(pa, p); }
    private static void stroke(Canvas g, Path pa, Paint p, int c, float w) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(w); p.setColor(c); g.drawPath(pa, p); p.setStyle(Paint.Style.FILL);
    }
    private static void circle(Canvas g, Paint p, float cx, float cy, float r, int c) { p.setStyle(Paint.Style.FILL); p.setColor(c); g.drawCircle(cx, cy, r, p); }

    private static void heartPath(Path pa, float cx, float cy, float w, float h) {
        pa.moveTo(cx, cy + h * 0.30f);
        pa.cubicTo(cx - w * 0.55f, cy - h * 0.30f, cx - w * 0.52f, cy + h * 0.10f, cx, cy + h * 0.55f);
        pa.cubicTo(cx + w * 0.52f, cy + h * 0.10f, cx + w * 0.55f, cy - h * 0.30f, cx, cy + h * 0.30f);
        pa.close();
    }

    /* ------------------------- illustrations --------------------------- */

    private static void heart(Canvas g, Paint p, int c1) {
        Path pa = P(); heartPath(pa, 0.5f, 0.28f, 0.9f, 0.9f); fill(g, pa, p, c1);
    }
    private static void doubleHeart(Canvas g, Paint p, int c1, int c2) {
        Path a = P(); heartPath(a, 0.38f, 0.34f, 0.6f, 0.6f); fill(g, a, p, c2);
        Path b = P(); heartPath(b, 0.62f, 0.24f, 0.68f, 0.68f); fill(g, b, p, c1);
    }
    private static void arrowHeart(Canvas g, Paint p, int c1, int c2) {
        Path pa = P(); heartPath(pa, 0.5f, 0.30f, 0.72f, 0.72f); fill(g, pa, p, c1);
        Path ln = P(); ln.moveTo(0.05f, 0.75f); ln.lineTo(0.95f, 0.28f); stroke(g, ln, p, c2, 0.05f);
        Path tip = P(); tip.moveTo(0.95f, 0.28f); tip.lineTo(0.80f, 0.28f); tip.lineTo(0.90f, 0.40f); tip.close(); fill(g, tip, p, c2);
        Path fl = P(); fl.moveTo(0.05f, 0.75f); fl.lineTo(0.18f, 0.66f); fl.lineTo(0.16f, 0.80f); fl.close(); fill(g, fl, p, c2);
    }
    private static void ring(Canvas g, Paint p, int c1, int c2) {
        Path band = P(); band.addCircle(0.5f, 0.62f, 0.30f, Path.Direction.CW); stroke(g, band, p, c1, 0.10f);
        Path dia = P(); dia.moveTo(0.5f, 0.05f); dia.lineTo(0.66f, 0.24f); dia.lineTo(0.5f, 0.40f); dia.lineTo(0.34f, 0.24f); dia.close();
        fill(g, dia, p, c2);
        Path fac = P(); fac.moveTo(0.34f, 0.24f); fac.lineTo(0.66f, 0.24f); fac.lineTo(0.5f, 0.40f); fac.close(); fill(g, fac, p, lighten(c2));
    }
    private static void leaf(Canvas g, Paint p, int c1, int c2) {
        Path pa = P(); pa.moveTo(0.5f, 0.05f);
        pa.cubicTo(0.95f, 0.35f, 0.75f, 0.9f, 0.5f, 0.95f);
        pa.cubicTo(0.25f, 0.9f, 0.05f, 0.35f, 0.5f, 0.05f); pa.close(); fill(g, pa, p, c1);
        Path v = P(); v.moveTo(0.5f, 0.12f); v.lineTo(0.5f, 0.9f); stroke(g, v, p, c2, 0.03f);
    }
    private static void branch(Canvas g, Paint p, int c1) {
        Path stem = P(); stem.moveTo(0.5f, 0.98f); stem.lineTo(0.5f, 0.05f); stroke(g, stem, p, c1, 0.03f);
        for (int i = 0; i < 5; i++) {
            float ty = 0.15f + i * 0.16f;
            leaflet(g, p, c1, ty, true); leaflet(g, p, c1, ty + 0.04f, false);
        }
    }
    private static void leaflet(Canvas g, Paint p, int c1, float ty, boolean left) {
        Path pa = P(); float dir = left ? -1 : 1;
        pa.moveTo(0.5f, ty);
        pa.quadTo(0.5f + dir * 0.30f, ty - 0.05f, 0.5f + dir * 0.34f, ty + 0.06f);
        pa.quadTo(0.5f + dir * 0.20f, ty + 0.10f, 0.5f, ty); pa.close(); fill(g, pa, p, c1);
    }
    private static void rose(Canvas g, Paint p, int c1, int c2) {
        circle(g, p, 0.5f, 0.42f, 0.36f, c1);
        Path sp = P(); sp.addArc(new RectF(0.28f, 0.20f, 0.72f, 0.64f), -90, 300); stroke(g, sp, p, c2, 0.045f);
        Path sp2 = P(); sp2.addArc(new RectF(0.38f, 0.30f, 0.62f, 0.54f), -40, 300); stroke(g, sp2, p, c2, 0.04f);
        Path stem = P(); stem.moveTo(0.5f, 0.75f); stem.lineTo(0.5f, 0.98f); stroke(g, stem, p, 0xFF4CAF50, 0.04f);
    }
    private static void tulip(Canvas g, Paint p, int c1, int c2) {
        Path cup = P(); cup.moveTo(0.28f, 0.30f); cup.quadTo(0.5f, 0.62f, 0.72f, 0.30f);
        cup.quadTo(0.66f, 0.12f, 0.5f, 0.24f); cup.quadTo(0.34f, 0.12f, 0.28f, 0.30f); cup.close(); fill(g, cup, p, c1);
        Path mid = P(); mid.moveTo(0.5f, 0.24f); mid.lineTo(0.5f, 0.55f); stroke(g, mid, p, c2, 0.03f);
        Path stem = P(); stem.moveTo(0.5f, 0.55f); stem.lineTo(0.5f, 0.98f); stroke(g, stem, p, 0xFF4CAF50, 0.045f);
        Path lf = P(); lf.moveTo(0.5f, 0.78f); lf.quadTo(0.85f, 0.66f, 0.78f, 0.95f); lf.quadTo(0.6f, 0.9f, 0.5f, 0.78f); fill(g, lf, p, 0xFF4CAF50);
    }
    private static void daisy(Canvas g, Paint p, int c1, int c2) {
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2 * i / 10;
            float px = 0.5f + (float) Math.cos(a) * 0.30f, py = 0.45f + (float) Math.sin(a) * 0.30f;
            circle(g, p, px, py, 0.13f, c1);
        }
        circle(g, p, 0.5f, 0.45f, 0.16f, c2);
    }
    private static void flower(Canvas g, Paint p, int c1, int c2) {
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5 - Math.PI / 2;
            float px = 0.5f + (float) Math.cos(a) * 0.28f, py = 0.42f + (float) Math.sin(a) * 0.28f;
            circle(g, p, px, py, 0.20f, c1);
        }
        circle(g, p, 0.5f, 0.42f, 0.15f, c2);
        Path stem = P(); stem.moveTo(0.5f, 0.55f); stem.lineTo(0.5f, 0.98f); stroke(g, stem, p, 0xFF4CAF50, 0.04f);
    }
    private static void clover(Canvas g, Paint p, int c1) {
        for (int i = 0; i < 4; i++) {
            double a = Math.PI * 2 * i / 4 - Math.PI / 2;
            float px = 0.5f + (float) Math.cos(a) * 0.24f, py = 0.42f + (float) Math.sin(a) * 0.24f;
            Path hp = P(); heartPath(hp, px, py - 0.12f, 0.4f, 0.4f);
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.postRotate((float) Math.toDegrees(a) + 90, px, py); hp.transform(m); fill(g, hp, p, c1);
        }
        Path stem = P(); stem.moveTo(0.5f, 0.55f); stem.quadTo(0.4f, 0.8f, 0.5f, 0.98f); stroke(g, stem, p, c1, 0.03f);
    }
    private static void cactus(Canvas g, Paint p, int c1, int c2) {
        round(g, p, 0.40f, 0.30f, 0.20f, 0.68f, 0.1f, c1);
        round(g, p, 0.15f, 0.45f, 0.14f, 0.16f, 0.07f, c1);
        round(g, p, 0.20f, 0.35f, 0.10f, 0.28f, 0.05f, c1);
        round(g, p, 0.66f, 0.40f, 0.14f, 0.14f, 0.07f, c1);
        round(g, p, 0.70f, 0.30f, 0.10f, 0.26f, 0.05f, c1);
        round(g, p, 0.34f, 0.88f, 0.32f, 0.10f, 0.03f, c2);
    }
    private static void tree(Canvas g, Paint p, int c1, int c2) {
        round(g, p, 0.44f, 0.55f, 0.12f, 0.45f, 0.02f, c2);
        circle(g, p, 0.5f, 0.36f, 0.34f, c1); circle(g, p, 0.3f, 0.45f, 0.22f, c1); circle(g, p, 0.7f, 0.45f, 0.22f, c1);
    }
    private static void palm(Canvas g, Paint p, int c1, int c2) {
        Path tr = P(); tr.moveTo(0.46f, 0.98f); tr.quadTo(0.40f, 0.5f, 0.5f, 0.28f); tr.quadTo(0.58f, 0.5f, 0.54f, 0.98f); tr.close(); fill(g, tr, p, c2);
        for (int i = -2; i <= 2; i++) {
            Path fr = P(); fr.moveTo(0.5f, 0.28f);
            fr.quadTo(0.5f + i * 0.18f, 0.10f + Math.abs(i) * 0.02f, 0.5f + i * 0.36f, 0.20f + Math.abs(i) * 0.05f);
            stroke(g, fr, p, c1, 0.06f);
        }
    }
    private static void mountain(Canvas g, Paint p, int c1, int c2) {
        Path m = P(); m.moveTo(0.02f, 0.9f); m.lineTo(0.4f, 0.25f); m.lineTo(0.62f, 0.6f); m.lineTo(0.78f, 0.4f); m.lineTo(0.98f, 0.9f); m.close(); fill(g, m, p, c1);
        Path snow = P(); snow.moveTo(0.4f, 0.25f); snow.lineTo(0.3f, 0.42f); snow.quadTo(0.36f, 0.36f, 0.42f, 0.42f); snow.quadTo(0.46f, 0.36f, 0.5f, 0.44f); snow.lineTo(0.4f, 0.25f); fill(g, snow, p, c2);
    }
    private static void sun(Canvas g, Paint p, int c1) {
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            Path r = P(); r.moveTo(0.5f + (float) Math.cos(a) * 0.34f, 0.5f + (float) Math.sin(a) * 0.34f);
            r.lineTo(0.5f + (float) Math.cos(a) * 0.48f, 0.5f + (float) Math.sin(a) * 0.48f); stroke(g, r, p, c1, 0.05f);
        }
        circle(g, p, 0.5f, 0.5f, 0.28f, c1);
    }
    private static void moon(Canvas g, Paint p, int c1) {
        Path pa = P(); pa.addCircle(0.5f, 0.5f, 0.42f, Path.Direction.CW);
        Path cut = P(); cut.addCircle(0.66f, 0.40f, 0.36f, Path.Direction.CW);
        pa.op(cut, Path.Op.DIFFERENCE); fill(g, pa, p, c1);
    }
    private static void cloud(Canvas g, Paint p, int c1) {
        circle(g, p, 0.34f, 0.58f, 0.22f, c1); circle(g, p, 0.54f, 0.48f, 0.28f, c1); circle(g, p, 0.72f, 0.58f, 0.20f, c1);
        round(g, p, 0.30f, 0.60f, 0.46f, 0.22f, 0.11f, c1);
    }
    private static void rainbow(Canvas g, Paint p) {
        int[] cs = {0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA};
        for (int i = 0; i < cs.length; i++) {
            Path a = P(); a.addArc(new RectF(0.08f + i * 0.05f, 0.30f + i * 0.05f, 0.92f - i * 0.05f, 1.4f - i * 0.05f), 180, 180);
            stroke(g, a, p, cs[i], 0.055f);
        }
    }
    private static void snowflake(Canvas g, Paint p, int c1) {
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * i / 3;
            float ex = 0.5f + (float) Math.cos(a) * 0.45f, ey = 0.5f + (float) Math.sin(a) * 0.45f;
            Path r = P(); r.moveTo(0.5f, 0.5f); r.lineTo(ex, ey); stroke(g, r, p, c1, 0.035f);
            float mx = 0.5f + (float) Math.cos(a) * 0.28f, my = 0.5f + (float) Math.sin(a) * 0.28f;
            double a2 = a + 0.5, a3 = a - 0.5;
            Path b1 = P(); b1.moveTo(mx, my); b1.lineTo(mx + (float) Math.cos(a2) * 0.14f, my + (float) Math.sin(a2) * 0.14f); stroke(g, b1, p, c1, 0.03f);
            Path b2 = P(); b2.moveTo(mx, my); b2.lineTo(mx + (float) Math.cos(a3) * 0.14f, my + (float) Math.sin(a3) * 0.14f); stroke(g, b2, p, c1, 0.03f);
        }
    }
    private static void wave(Canvas g, Paint p, int c1) {
        for (int r = 0; r < 3; r++) {
            Path w = P(); float yy = 0.35f + r * 0.20f; w.moveTo(0.02f, yy);
            for (float xx = 0.02f; xx <= 0.98f; xx += 0.24f) w.rQuadTo(0.06f, -0.10f, 0.12f, 0f);
            stroke(g, w, p, c1, 0.04f);
        }
    }
    private static void star(Canvas g, Paint p, int c1, int pts, float inner) {
        Path pa = P(); double a = -Math.PI / 2; float out = 0.46f, in = out * inner;
        pa.moveTo(0.5f + (float) Math.cos(a) * out, 0.5f + (float) Math.sin(a) * out);
        for (int i = 1; i < pts * 2; i++) { a += Math.PI / pts; float rad = (i % 2 == 0) ? out : in;
            pa.lineTo(0.5f + (float) Math.cos(a) * rad, 0.5f + (float) Math.sin(a) * rad); }
        pa.close(); fill(g, pa, p, c1);
    }
    private static void sparkle(Canvas g, Paint p, int c1) {
        Path pa = P();
        pa.moveTo(0.5f, 0.05f); pa.quadTo(0.55f, 0.45f, 0.95f, 0.5f); pa.quadTo(0.55f, 0.55f, 0.5f, 0.95f);
        pa.quadTo(0.45f, 0.55f, 0.05f, 0.5f); pa.quadTo(0.45f, 0.45f, 0.5f, 0.05f); pa.close(); fill(g, pa, p, c1);
    }
    private static void burst(Canvas g, Paint p, int c1) { star(g, p, c1, 12, 0.72f); }
    private static void balloon(Canvas g, Paint p, int c1, int c2) {
        Path b = P(); b.addOval(new RectF(0.28f, 0.05f, 0.72f, 0.62f), Path.Direction.CW); fill(g, b, p, c1);
        Path tie = P(); tie.moveTo(0.46f, 0.60f); tie.lineTo(0.54f, 0.60f); tie.lineTo(0.5f, 0.68f); tie.close(); fill(g, tie, p, c1);
        Path str = P(); str.moveTo(0.5f, 0.68f); str.quadTo(0.6f, 0.85f, 0.5f, 0.98f); stroke(g, str, p, c2, 0.02f);
    }
    private static void gift(Canvas g, Paint p, int c1, int c2) {
        round(g, p, 0.18f, 0.40f, 0.64f, 0.52f, 0.03f, c1);
        round(g, p, 0.44f, 0.40f, 0.12f, 0.52f, 0.0f, c2);
        round(g, p, 0.14f, 0.28f, 0.72f, 0.16f, 0.03f, lighten(c1));
        Path bowL = P(); bowL.addOval(new RectF(0.30f, 0.10f, 0.50f, 0.30f), Path.Direction.CW); stroke(g, bowL, p, c2, 0.04f);
        Path bowR = P(); bowR.addOval(new RectF(0.50f, 0.10f, 0.70f, 0.30f), Path.Direction.CW); stroke(g, bowR, p, c2, 0.04f);
    }
    private static void cake(Canvas g, Paint p, int c1, int c2) {
        round(g, p, 0.18f, 0.55f, 0.64f, 0.38f, 0.04f, c1);
        Path icing = P(); icing.moveTo(0.18f, 0.60f);
        for (float xx = 0.18f; xx < 0.82f; xx += 0.16f) icing.rQuadTo(0.08f, 0.10f, 0.16f, 0f);
        icing.lineTo(0.82f, 0.55f); icing.lineTo(0.18f, 0.55f); icing.close(); fill(g, icing, p, c2);
        round(g, p, 0.48f, 0.30f, 0.04f, 0.20f, 0f, c2);
        circle(g, p, 0.5f, 0.26f, 0.05f, 0xFFFFA000);
    }
    private static void confetti(Canvas g, Paint p, int c1, int c2) {
        int[] cs = {c1, c2, 0xFF42A5F5, 0xFFFFCA28, 0xFF66BB6A, 0xFFEF5350};
        long seed = 7;
        for (int i = 0; i < 16; i++) {
            seed = seed * 1103515245 + 12345; float px = ((seed >> 16) & 0x7fff) / 32767f;
            seed = seed * 1103515245 + 12345; float py = ((seed >> 16) & 0x7fff) / 32767f;
            round(g, p, px * 0.9f, py * 0.9f, 0.08f, 0.05f, 0.01f, cs[i % cs.length]);
        }
    }
    private static void crown(Canvas g, Paint p, int c1, int c2) {
        Path cr = P(); cr.moveTo(0.12f, 0.75f); cr.lineTo(0.20f, 0.30f); cr.lineTo(0.35f, 0.55f);
        cr.lineTo(0.5f, 0.22f); cr.lineTo(0.65f, 0.55f); cr.lineTo(0.80f, 0.30f); cr.lineTo(0.88f, 0.75f); cr.close(); fill(g, cr, p, c1);
        round(g, p, 0.12f, 0.74f, 0.76f, 0.12f, 0.02f, c1);
        circle(g, p, 0.20f, 0.30f, 0.05f, c2); circle(g, p, 0.5f, 0.22f, 0.05f, c2); circle(g, p, 0.80f, 0.30f, 0.05f, c2);
    }
    private static void medal(Canvas g, Paint p, int c1, int c2) {
        Path r1 = P(); r1.moveTo(0.35f, 0.05f); r1.lineTo(0.45f, 0.45f); r1.lineTo(0.30f, 0.45f); r1.close(); fill(g, r1, p, c2);
        Path r2 = P(); r2.moveTo(0.65f, 0.05f); r2.lineTo(0.70f, 0.45f); r2.lineTo(0.55f, 0.45f); r2.close(); fill(g, r2, p, lighten(c2));
        circle(g, p, 0.5f, 0.62f, 0.30f, c1); circle(g, p, 0.5f, 0.62f, 0.22f, lighten(c1));
        star(g, p, c2, 5, 0.5f); // small star drawn full box — reposition
    }
    private static void champagne(Canvas g, Paint p, int c1, int c2) {
        Path glass = P(); glass.moveTo(0.30f, 0.10f); glass.lineTo(0.70f, 0.10f); glass.lineTo(0.56f, 0.48f); glass.lineTo(0.44f, 0.48f); glass.close(); fill(g, glass, p, c2);
        round(g, p, 0.485f, 0.48f, 0.03f, 0.34f, 0f, c1);
        round(g, p, 0.36f, 0.82f, 0.28f, 0.05f, 0.02f, c1);
        circle(g, p, 0.62f, 0.06f, 0.03f, c2); circle(g, p, 0.74f, 0.14f, 0.025f, c2);
    }
    private static void candle(Canvas g, Paint p, int c1, int c2) {
        round(g, p, 0.40f, 0.28f, 0.20f, 0.66f, 0.03f, c1);
        Path fl = P(); fl.moveTo(0.5f, 0.06f); fl.quadTo(0.60f, 0.18f, 0.5f, 0.26f); fl.quadTo(0.40f, 0.18f, 0.5f, 0.06f); fill(g, fl, p, c2);
    }
    private static void banner(Canvas g, Paint p, int c1, int c2) {
        Path b = P(); b.moveTo(0.05f, 0.35f); b.lineTo(0.95f, 0.35f); b.lineTo(0.95f, 0.65f); b.lineTo(0.05f, 0.65f); b.close(); fill(g, b, p, c1);
        Path lt = P(); lt.moveTo(0.05f, 0.35f); lt.lineTo(0.05f, 0.65f); lt.lineTo(-0.05f, 0.70f); lt.lineTo(0.0f, 0.50f); lt.lineTo(-0.05f, 0.30f); lt.close(); fill(g, lt, p, c2);
        Path rt = P(); rt.moveTo(0.95f, 0.35f); rt.lineTo(0.95f, 0.65f); rt.lineTo(1.05f, 0.70f); rt.lineTo(1.0f, 0.50f); rt.lineTo(1.05f, 0.30f); rt.close(); fill(g, rt, p, c2);
    }
    private static void pennant(Canvas g, Paint p, int c1, int c2) {
        Path line = P(); line.moveTo(0.0f, 0.15f); line.quadTo(0.5f, 0.30f, 1.0f, 0.15f); stroke(g, line, p, 0xFF888888, 0.015f);
        int[] cs = {c1, c2, 0xFF42A5F5, 0xFFFFCA28, c1, c2};
        for (int i = 0; i < 6; i++) {
            float fx = 0.08f + i * 0.16f, fy = 0.20f + (float) Math.sin(i) * 0.02f;
            Path fl = P(); fl.moveTo(fx, fy); fl.lineTo(fx + 0.12f, fy); fl.lineTo(fx + 0.06f, fy + 0.28f); fl.close(); fill(g, fl, p, cs[i]);
        }
    }
    private static void laurel(Canvas g, Paint p, int c1) {
        Path l = P(); l.addArc(new RectF(0.10f, 0.05f, 0.90f, 0.95f), 110, 140); stroke(g, l, p, c1, 0.03f);
        Path r = P(); r.addArc(new RectF(0.10f, 0.05f, 0.90f, 0.95f), -70, -140); stroke(g, r, p, c1, 0.03f);
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(120 + i * 22);
            float lx = 0.5f + (float) Math.cos(a) * 0.40f, ly = 0.5f + (float) Math.sin(a) * 0.40f;
            leafAt(g, p, c1, lx, ly, (float) Math.toDegrees(a));
            leafAt(g, p, c1, 0.5f - (float) Math.cos(a) * 0.40f, ly, 180 - (float) Math.toDegrees(a));
        }
    }
    private static void leafAt(Canvas g, Paint p, int c1, float x, float y, float rot) {
        Path pa = P(); pa.addOval(new RectF(x - 0.06f, y - 0.03f, x + 0.06f, y + 0.03f), Path.Direction.CW);
        android.graphics.Matrix m = new android.graphics.Matrix(); m.postRotate(rot, x, y); pa.transform(m); fill(g, pa, p, c1);
    }
    private static void floralWreath(Canvas g, Paint p, int c1, int c2) {
        Path ring = P(); ring.addCircle(0.5f, 0.5f, 0.38f, Path.Direction.CW); stroke(g, ring, p, 0xFF8BC34A, 0.03f);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            float px = 0.5f + (float) Math.cos(a) * 0.38f, py = 0.5f + (float) Math.sin(a) * 0.38f;
            circle(g, p, px, py, 0.09f, i % 2 == 0 ? c1 : c2);
            circle(g, p, px, py, 0.035f, 0xFFFFF176);
        }
    }
    private static void flourish(Canvas g, Paint p, int c1) {
        Path pa = P(); pa.moveTo(0.05f, 0.5f); pa.cubicTo(0.35f, 0.1f, 0.6f, 0.4f, 0.5f, 0.55f);
        pa.cubicTo(0.42f, 0.66f, 0.6f, 0.7f, 0.95f, 0.45f); stroke(g, pa, p, c1, 0.025f);
        circle(g, p, 0.5f, 0.55f, 0.02f, c1);
    }
    private static void divider(Canvas g, Paint p, int c1) {
        Path l1 = P(); l1.moveTo(0.02f, 0.5f); l1.lineTo(0.40f, 0.5f); stroke(g, l1, p, c1, 0.02f);
        Path l2 = P(); l2.moveTo(0.60f, 0.5f); l2.lineTo(0.98f, 0.5f); stroke(g, l2, p, c1, 0.02f);
        Path d = P(); d.moveTo(0.5f, 0.35f); d.lineTo(0.58f, 0.5f); d.lineTo(0.5f, 0.65f); d.lineTo(0.42f, 0.5f); d.close(); fill(g, d, p, c1);
    }
    private static void arrow(Canvas g, Paint p, int c1) {
        Path pa = P(); pa.moveTo(0.05f, 0.5f); pa.lineTo(0.70f, 0.5f); stroke(g, pa, p, c1, 0.06f);
        Path t = P(); t.moveTo(0.65f, 0.28f); t.lineTo(0.95f, 0.5f); t.lineTo(0.65f, 0.72f); t.close(); fill(g, t, p, c1);
    }
    private static void quote(Canvas g, Paint p, int c1) {
        round(g, p, 0.14f, 0.20f, 0.28f, 0.32f, 0.06f, c1);
        round(g, p, 0.20f, 0.44f, 0.10f, 0.34f, 0.04f, c1);
        round(g, p, 0.58f, 0.20f, 0.28f, 0.32f, 0.06f, c1);
        round(g, p, 0.64f, 0.44f, 0.10f, 0.34f, 0.04f, c1);
    }
    private static void camera(Canvas g, Paint p, int c1, int c2) {
        round(g, p, 0.10f, 0.28f, 0.80f, 0.56f, 0.06f, c1);
        round(g, p, 0.34f, 0.18f, 0.32f, 0.14f, 0.03f, c1);
        circle(g, p, 0.5f, 0.56f, 0.18f, c2); circle(g, p, 0.5f, 0.56f, 0.10f, lighten(c2));
        circle(g, p, 0.76f, 0.40f, 0.04f, 0xFFFFF176);
    }
    private static void plane(Canvas g, Paint p, int c1, int c2) {
        Path b = P(); b.moveTo(0.08f, 0.52f); b.lineTo(0.72f, 0.40f); b.quadTo(0.95f, 0.38f, 0.92f, 0.50f);
        b.quadTo(0.95f, 0.60f, 0.72f, 0.60f); b.lineTo(0.30f, 0.66f); b.close(); fill(g, b, p, c1);
        Path w = P(); w.moveTo(0.40f, 0.48f); w.lineTo(0.30f, 0.20f); w.lineTo(0.55f, 0.44f); w.close(); fill(g, w, p, c2);
        Path tail = P(); tail.moveTo(0.10f, 0.52f); tail.lineTo(0.02f, 0.36f); tail.lineTo(0.18f, 0.48f); tail.close(); fill(g, tail, p, c2);
    }
    private static void pin(Canvas g, Paint p, int c1, int c2) {
        Path pa = P(); pa.moveTo(0.5f, 0.95f); pa.cubicTo(0.05f, 0.55f, 0.18f, 0.05f, 0.5f, 0.05f);
        pa.cubicTo(0.82f, 0.05f, 0.95f, 0.55f, 0.5f, 0.95f); pa.close(); fill(g, pa, p, c1);
        circle(g, p, 0.5f, 0.36f, 0.14f, c2);
    }
    private static void compass(Canvas g, Paint p, int c1, int c2) {
        circle(g, p, 0.5f, 0.5f, 0.44f, c1); circle(g, p, 0.5f, 0.5f, 0.36f, lighten(c1));
        Path n = P(); n.moveTo(0.5f, 0.18f); n.lineTo(0.60f, 0.5f); n.lineTo(0.5f, 0.44f); n.lineTo(0.40f, 0.5f); n.close(); fill(g, n, p, c2);
        Path s = P(); s.moveTo(0.5f, 0.82f); s.lineTo(0.60f, 0.5f); s.lineTo(0.5f, 0.56f); s.lineTo(0.40f, 0.5f); s.close(); fill(g, s, p, 0xFFECEFF1);
    }
    private static void suitcase(Canvas g, Paint p, int c1, int c2) {
        round(g, p, 0.14f, 0.32f, 0.72f, 0.58f, 0.05f, c1);
        round(g, p, 0.38f, 0.18f, 0.24f, 0.16f, 0.03f, c2);
        round(g, p, 0.42f, 0.14f, 0.16f, 0.08f, 0.02f, c1);
        round(g, p, 0.30f, 0.32f, 0.06f, 0.58f, 0f, lighten(c1));
        round(g, p, 0.64f, 0.32f, 0.06f, 0.58f, 0f, lighten(c1));
    }
    private static void coffee(Canvas g, Paint p, int c1, int c2) {
        round(g, p, 0.20f, 0.35f, 0.48f, 0.50f, 0.05f, c1);
        Path handle = P(); handle.addArc(new RectF(0.60f, 0.40f, 0.90f, 0.72f), -80, 180); stroke(g, handle, p, c1, 0.06f);
        round(g, p, 0.16f, 0.82f, 0.56f, 0.08f, 0.03f, c1);
        for (int i = 0; i < 3; i++) { Path s = P(); float sx = 0.30f + i * 0.12f; s.moveTo(sx, 0.28f); s.rQuadTo(0.05f, -0.08f, 0f, -0.16f); stroke(g, s, p, c2, 0.02f); }
    }
    private static void note(Canvas g, Paint p, int c1) {
        round(g, p, 0.60f, 0.10f, 0.10f, 0.62f, 0.03f, c1);
        round(g, p, 0.25f, 0.30f, 0.10f, 0.52f, 0.03f, c1);
        Path beam = P(); beam.moveTo(0.30f, 0.30f); beam.lineTo(0.65f, 0.16f); beam.lineTo(0.65f, 0.30f); beam.lineTo(0.30f, 0.44f); beam.close(); fill(g, beam, p, c1);
        circle(g, p, 0.28f, 0.80f, 0.13f, c1); circle(g, p, 0.63f, 0.70f, 0.13f, c1);
    }
    private static void butterfly(Canvas g, Paint p, int c1, int c2) {
        round(g, p, 0.48f, 0.20f, 0.04f, 0.60f, 0.02f, 0xFF5D4037);
        wing(g, p, c1, 0.32f, 0.32f, true); wing(g, p, c2, 0.34f, 0.62f, true);
        wing(g, p, c1, 0.68f, 0.32f, false); wing(g, p, c2, 0.66f, 0.62f, false);
    }
    private static void wing(Canvas g, Paint p, int c, float cx, float cy, boolean left) {
        Path pa = P(); pa.addOval(new RectF(cx - 0.20f, cy - 0.16f, cx + 0.20f, cy + 0.16f), Path.Direction.CW); fill(g, pa, p, c);
    }
    private static void dove(Canvas g, Paint p, int c1, int c2) {
        Path body = P(); body.moveTo(0.15f, 0.55f); body.quadTo(0.45f, 0.35f, 0.80f, 0.45f);
        body.quadTo(0.95f, 0.48f, 0.85f, 0.58f); body.quadTo(0.55f, 0.72f, 0.30f, 0.70f); body.close(); fill(g, body, p, c1);
        Path wing = P(); wing.moveTo(0.40f, 0.50f); wing.quadTo(0.55f, 0.15f, 0.72f, 0.48f); wing.quadTo(0.55f, 0.42f, 0.40f, 0.50f); fill(g, wing, p, c2);
        Path beak = P(); beak.moveTo(0.80f, 0.46f); beak.lineTo(0.92f, 0.44f); beak.lineTo(0.82f, 0.52f); beak.close(); fill(g, beak, p, 0xFFFFB300);
    }
    private static void footprint(Canvas g, Paint p, int c1) {
        round(g, p, 0.30f, 0.45f, 0.40f, 0.44f, 0.20f, c1);
        circle(g, p, 0.30f, 0.28f, 0.07f, c1); circle(g, p, 0.44f, 0.20f, 0.07f, c1);
        circle(g, p, 0.58f, 0.20f, 0.07f, c1); circle(g, p, 0.70f, 0.30f, 0.06f, c1);
    }

    /* ------------------------- helpers --------------------------------- */

    private static void round(Canvas g, Paint p, float x, float y, float w, float h, float r, int c) {
        p.setStyle(Paint.Style.FILL); p.setColor(c);
        g.drawRoundRect(new RectF(x, y, x + w, y + h), r, r, p);
    }

    static int lighten(int c) {
        int a = (c >> 24) & 0xff, r = (c >> 16) & 0xff, gg = (c >> 8) & 0xff, b = c & 0xff;
        r = Math.min(255, r + 45); gg = Math.min(255, gg + 45); b = Math.min(255, b + 45);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    private Clipart() {}
}
