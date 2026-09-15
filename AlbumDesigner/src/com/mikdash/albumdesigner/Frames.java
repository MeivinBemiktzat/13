package com.mikdash.albumdesigner;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Decorative frames drawn around a photo element (page coordinates). */
public final class Frames {

    public static final String[] NAMES = {
            "ללא", "קו דק", "קו עבה", "קו כפול", "מקווקו", "פינות",
            "פולארויד", "סרט צילום", "גלי", "צל פנימי", "נייר דבק", "זהב מעוטר",
    };

    /** Draws frame `style` around rect. `c` is the frame colour. */
    public static void draw(Canvas g, int style, RectF r, float corner, int c, Paint p) {
        p.setShader(null);
        float t = Math.min(r.width(), r.height());
        switch (style) {
            case 1: line(g, r, corner, c, t * 0.012f, p); break;
            case 2: line(g, r, corner, c, t * 0.05f, p); break;
            case 3:
                line(g, r, corner, c, t * 0.02f, p);
                line(g, inset(r, t * 0.05f), Math.max(0, corner - t * 0.05f), c, t * 0.01f, p);
                break;
            case 4: dashed(g, r, corner, c, t, p); break;
            case 5: corners(g, r, c, t, p); break;
            case 6: polaroid(g, r, p); break;
            case 7: film(g, r, p); break;
            case 8: scallop(g, r, c, t, p); break;
            case 9: innerShadow(g, r, corner, p); break;
            case 10: tape(g, r, p); break;
            case 11: gold(g, r, corner, t, p); break;
            default: break;
        }
    }

    /** Some frames (polaroid, film) draw a mat OUTSIDE the photo, so the photo
     *  should be inset. Returns the inset fraction of the shorter side. */
    public static float matInset(int style) {
        if (style == 6) return 0.06f;   // polaroid
        if (style == 7) return 0.10f;   // film strip
        return 0f;
    }

    private static RectF inset(RectF r, float d) { return new RectF(r.left + d, r.top + d, r.right - d, r.bottom - d); }

    private static void line(Canvas g, RectF r, float corner, int c, float w, Paint p) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(w); p.setColor(c);
        RectF rr = inset(r, w / 2);
        g.drawRoundRect(rr, corner, corner, p);
    }

    private static void dashed(Canvas g, RectF r, float corner, int c, float t, Paint p) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(t * 0.02f); p.setColor(c);
        p.setPathEffect(new DashPathEffect(new float[]{t * 0.06f, t * 0.04f}, 0));
        g.drawRoundRect(inset(r, t * 0.01f), corner, corner, p);
        p.setPathEffect(null);
    }

    private static void corners(Canvas g, RectF r, int c, float t, Paint p) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(t * 0.03f); p.setColor(c); p.setStrokeCap(Paint.Cap.ROUND);
        float L = t * 0.22f, m = t * 0.04f;
        corner(g, r.left + m, r.top + m, L, L, p);
        corner(g, r.right - m, r.top + m, -L, L, p);
        corner(g, r.left + m, r.bottom - m, L, -L, p);
        corner(g, r.right - m, r.bottom - m, -L, -L, p);
    }
    private static void corner(Canvas g, float x, float y, float lx, float ly, Paint p) {
        g.drawLine(x, y, x + lx, y, p); g.drawLine(x, y, x, y + ly, p);
    }

    private static void polaroid(Canvas g, RectF r, Paint p) {
        // white mat around, thicker at bottom (caller already inset the photo)
        p.setStyle(Paint.Style.STROKE); p.setColor(0xFFFFFFFF);
        float t = Math.min(r.width(), r.height());
        // draw white border by stroking with the mat width, then bottom extra
        p.setStrokeWidth(t * 0.001f); // outline crispness
        // full white rounded backing is drawn by the caller frame region; emulate with 4 rects
        p.setStyle(Paint.Style.FILL);
        float m = t * 0.06f;
        g.drawRect(r.left - m, r.top - m, r.right + m, r.top, p);
        g.drawRect(r.left - m, r.bottom, r.right + m, r.bottom + m * 2.4f, p);
        g.drawRect(r.left - m, r.top - m, r.left, r.bottom + m * 2.4f, p);
        g.drawRect(r.right, r.top - m, r.right + m, r.bottom + m * 2.4f, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(t * 0.004f); p.setColor(0x22000000);
        g.drawRect(r.left - m, r.top - m, r.right + m, r.bottom + m * 2.4f, p);
    }

    private static void film(Canvas g, RectF r, Paint p) {
        float t = Math.min(r.width(), r.height());
        float bar = t * 0.10f;
        p.setStyle(Paint.Style.FILL); p.setColor(0xFF1A1A1A);
        g.drawRect(r.left - bar, r.top - bar, r.right + bar, r.top, p);
        g.drawRect(r.left - bar, r.bottom, r.right + bar, r.bottom + bar, p);
        g.drawRect(r.left - bar, r.top - bar, r.left, r.bottom + bar, p);
        g.drawRect(r.right, r.top - bar, r.right + bar, r.bottom + bar, p);
        p.setColor(0xFFFFFFFF);
        float hole = bar * 0.5f, step = bar * 1.1f;
        for (float x = r.left; x < r.right; x += step) {
            g.drawRoundRect(new RectF(x, r.top - bar * 0.75f, x + hole, r.top - bar * 0.25f), hole * 0.2f, hole * 0.2f, p);
            g.drawRoundRect(new RectF(x, r.bottom + bar * 0.25f, x + hole, r.bottom + bar * 0.75f), hole * 0.2f, hole * 0.2f, p);
        }
    }

    private static void scallop(Canvas g, RectF r, int c, float t, Paint p) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(t * 0.015f); p.setColor(c);
        float d = t * 0.09f;
        Path path = new Path();
        for (float x = r.left; x < r.right; x += d) { path.addArc(new RectF(x, r.top - d / 2, x + d, r.top + d / 2), 0, 180); }
        for (float x = r.left; x < r.right; x += d) { path.addArc(new RectF(x, r.bottom - d / 2, x + d, r.bottom + d / 2), 180, 180); }
        for (float y = r.top; y < r.bottom; y += d) { path.addArc(new RectF(r.left - d / 2, y, r.left + d / 2, y + d), 90, 180); }
        for (float y = r.top; y < r.bottom; y += d) { path.addArc(new RectF(r.right - d / 2, y, r.right + d / 2, y + d), -90, 180); }
        g.drawPath(path, p);
    }

    private static void innerShadow(Canvas g, RectF r, float corner, Paint p) {
        p.setStyle(Paint.Style.STROKE);
        float t = Math.min(r.width(), r.height());
        for (int i = 0; i < 6; i++) {
            int alpha = (int) (60 * (1 - i / 6f));
            p.setColor((alpha << 24));
            p.setStrokeWidth(t * 0.02f);
            g.drawRoundRect(inset(r, i * t * 0.015f + 1), corner, corner, p);
        }
    }

    private static void tape(Canvas g, RectF r, Paint p) {
        p.setStyle(Paint.Style.FILL); p.setColor(0x99FFF59D);
        float t = Math.min(r.width(), r.height()) * 0.16f;
        g.save(); g.rotate(-35, r.left, r.top); g.drawRect(r.left - t, r.top - t * 0.4f, r.left + t, r.top + t * 0.4f, p); g.restore();
        g.save(); g.rotate(35, r.right, r.top); g.drawRect(r.right - t, r.top - t * 0.4f, r.right + t, r.top + t * 0.4f, p); g.restore();
    }

    private static void gold(Canvas g, RectF r, float corner, float t, Paint p) {
        int gold = 0xFFC9A227;
        line(g, r, corner, gold, t * 0.02f, p);
        line(g, inset(r, t * 0.045f), Math.max(0, corner - t * 0.045f), gold, t * 0.008f, p);
        p.setStyle(Paint.Style.FILL); p.setColor(gold);
        float m = t * 0.06f, rad = t * 0.02f;
        g.drawCircle(r.left + m, r.top + m, rad, p);
        g.drawCircle(r.right - m, r.top + m, rad, p);
        g.drawCircle(r.left + m, r.bottom - m, rad, p);
        g.drawCircle(r.right - m, r.bottom - m, rad, p);
    }

    private Frames() {}
}
