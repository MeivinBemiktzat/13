package com.mikdash.albumdesigner;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Seamless decorative background patterns drawn in page coordinates. */
public final class Patterns {

    public static final String[] NAMES = {
            "נקודות", "פסים", "רשת", "שברון", "לבבות", "כוכבים",
            "משולשים", "קונפטי", "פלוס", "גלים",
    };

    public static void draw(Canvas g, int id, int pw, int ph, int c) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(c);
        float u = Math.min(pw, ph) / 12f; // tile unit
        switch (id) {
            case 0:
                p.setStyle(Paint.Style.FILL);
                for (float y = u; y < ph; y += u) for (float x = u; x < pw; x += u) g.drawCircle(x, y, u * 0.12f, p);
                break;
            case 1:
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(u * 0.18f);
                for (float x = -ph; x < pw; x += u) g.drawLine(x, 0, x + ph, ph, p);
                break;
            case 2:
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(u * 0.05f);
                for (float x = u; x < pw; x += u) g.drawLine(x, 0, x, ph, p);
                for (float y = u; y < ph; y += u) g.drawLine(0, y, pw, y, p);
                break;
            case 3:
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(u * 0.12f); p.setStrokeJoin(Paint.Join.ROUND);
                for (float y = 0; y < ph + u; y += u) {
                    Path path = new Path();
                    for (float x = 0; x < pw + u; x += u) { path.moveTo(x, y); path.lineTo(x + u / 2, y - u * 0.4f); path.lineTo(x + u, y); }
                    g.drawPath(path, p);
                }
                break;
            case 4:
                for (float y = u; y < ph; y += u * 1.4f)
                    for (float x = u; x < pw; x += u * 1.4f) { boolean off = ((int) (y / (u * 1.4f))) % 2 == 0; heart(g, p, (off ? x : x + u * 0.7f), y, u * 0.4f); }
                break;
            case 5:
                for (float y = u; y < ph; y += u * 1.3f)
                    for (float x = u; x < pw; x += u * 1.3f) star(g, p, x, y, u * 0.32f);
                break;
            case 6:
                p.setStyle(Paint.Style.FILL);
                for (float y = u; y < ph; y += u) for (float x = u; x < pw; x += u) {
                    Path t = new Path(); t.moveTo(x, y - u * 0.22f); t.lineTo(x + u * 0.22f, y + u * 0.18f); t.lineTo(x - u * 0.22f, y + u * 0.18f); t.close(); g.drawPath(t, p);
                }
                break;
            case 7: {
                p.setStyle(Paint.Style.FILL);
                int[] cs = {c, Clipart.lighten(c), 0x66FFFFFF};
                long s = 99;
                for (int i = 0; i < 90; i++) {
                    s = s * 1103515245 + 12345; float x = ((s >> 16) & 0x7fff) / 32767f * pw;
                    s = s * 1103515245 + 12345; float y = ((s >> 16) & 0x7fff) / 32767f * ph;
                    p.setColor(cs[i % cs.length]);
                    g.save(); g.rotate((i * 40) % 360, x, y); g.drawRect(x, y, x + u * 0.28f, y + u * 0.14f, p); g.restore();
                }
                break;
            }
            case 8:
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(u * 0.08f); p.setStrokeCap(Paint.Cap.ROUND);
                for (float y = u; y < ph; y += u) for (float x = u; x < pw; x += u) {
                    g.drawLine(x - u * 0.18f, y, x + u * 0.18f, y, p); g.drawLine(x, y - u * 0.18f, x, y + u * 0.18f, p);
                }
                break;
            case 9:
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(u * 0.08f);
                for (float y = u; y < ph + u; y += u * 0.7f) {
                    Path path = new Path(); path.moveTo(0, y);
                    for (float x = 0; x < pw; x += u) path.rQuadTo(u / 2, -u * 0.3f, u, 0);
                    g.drawPath(path, p);
                }
                break;
        }
    }

    private static void heart(Canvas g, Paint p, float cx, float cy, float s) {
        p.setStyle(Paint.Style.FILL);
        Path pa = new Path();
        pa.moveTo(cx, cy + s * 0.3f);
        pa.cubicTo(cx - s, cy - s * 0.6f, cx - s, cy + s * 0.3f, cx, cy + s);
        pa.cubicTo(cx + s, cy + s * 0.3f, cx + s, cy - s * 0.6f, cx, cy + s * 0.3f);
        pa.close(); g.drawPath(pa, p);
    }

    private static void star(Canvas g, Paint p, float cx, float cy, float r) {
        p.setStyle(Paint.Style.FILL);
        Path pa = new Path(); double a = -Math.PI / 2;
        pa.moveTo(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r);
        for (int i = 1; i < 10; i++) { a += Math.PI / 5; float rad = (i % 2 == 0) ? r : r * 0.45f;
            pa.lineTo(cx + (float) Math.cos(a) * rad, cy + (float) Math.sin(a) * rad); }
        pa.close(); g.drawPath(pa, p);
    }

    private Patterns() {}
}
