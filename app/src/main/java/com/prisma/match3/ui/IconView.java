package com.prisma.match3.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * Lightweight vector icon view. All glyphs are drawn as paths on a canvas so
 * the UI uses real icons (never emoji) with no image assets. Sized by its
 * layout bounds; color and glyph set via constructor / setters.
 */
public class IconView extends View {
    public enum Glyph { PLAY, HOME, TROPHY, GEAR, SOUND_ON, SOUND_OFF, STAR, STAR_OUTLINE,
        LOCK, BACK, PAUSE, RESTART, BOLT, HAMMER, GEM, NEXT, CLOSE, SHUFFLE, COIN }

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Glyph glyph;
    private int color;

    public IconView(Context c, Glyph g, int color) {
        super(c);
        this.glyph = g;
        this.color = color;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setGlyph(Glyph g) { this.glyph = g; invalidate(); }
    public void setColor(int c) { this.color = c; invalidate(); }

    @Override protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float s = Math.min(w, h);
        float cx = w / 2f, cy = h / 2f;
        float sw = s * 0.10f;
        fill.setColor(color);
        stroke.setColor(color);
        stroke.setStrokeWidth(sw);
        float u = s * 0.30f; // unit radius

        switch (glyph) {
            case PLAY: {
                Path p = new Path();
                p.moveTo(cx - u * 0.7f, cy - u);
                p.lineTo(cx + u, cy);
                p.lineTo(cx - u * 0.7f, cy + u);
                p.close();
                cv.drawPath(p, fill);
                break;
            }
            case PAUSE:
                cv.drawRoundRect(cx - u * 0.7f, cy - u, cx - u * 0.15f, cy + u, sw, sw, fill);
                cv.drawRoundRect(cx + u * 0.15f, cy - u, cx + u * 0.7f, cy + u, sw, sw, fill);
                break;
            case HOME: {
                Path p = new Path();
                p.moveTo(cx, cy - u);
                p.lineTo(cx + u, cy);
                p.lineTo(cx + u * 0.7f, cy);
                p.lineTo(cx + u * 0.7f, cy + u * 0.8f);
                p.lineTo(cx - u * 0.7f, cy + u * 0.8f);
                p.lineTo(cx - u * 0.7f, cy);
                p.lineTo(cx - u, cy);
                p.close();
                cv.drawPath(p, fill);
                break;
            }
            case TROPHY: {
                cv.drawRoundRect(cx - u * 0.7f, cy - u, cx + u * 0.7f, cy + u * 0.1f, sw, sw, fill);
                cv.drawArc(cx - u * 1.15f, cy - u * 0.9f, cx - u * 0.4f, cy - u * 0.1f, 90, 180, false, stroke);
                cv.drawArc(cx + u * 0.4f, cy - u * 0.9f, cx + u * 1.15f, cy - u * 0.1f, 270, 180, false, stroke);
                cv.drawRect(cx - u * 0.18f, cy + u * 0.1f, cx + u * 0.18f, cy + u * 0.7f, fill);
                cv.drawRoundRect(cx - u * 0.6f, cy + u * 0.7f, cx + u * 0.6f, cy + u, sw, sw, fill);
                break;
            }
            case GEAR: {
                cv.drawCircle(cx, cy, u * 0.9f, stroke);
                cv.drawCircle(cx, cy, u * 0.35f, fill);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    float x0 = cx + (float) Math.cos(a) * u * 0.9f;
                    float y0 = cy + (float) Math.sin(a) * u * 0.9f;
                    float x1 = cx + (float) Math.cos(a) * u * 1.25f;
                    float y1 = cy + (float) Math.sin(a) * u * 1.25f;
                    cv.drawLine(x0, y0, x1, y1, stroke);
                }
                break;
            }
            case SOUND_ON:
            case SOUND_OFF: {
                Path p = new Path();
                p.moveTo(cx - u, cy - u * 0.4f);
                p.lineTo(cx - u * 0.4f, cy - u * 0.4f);
                p.lineTo(cx + u * 0.1f, cy - u);
                p.lineTo(cx + u * 0.1f, cy + u);
                p.lineTo(cx - u * 0.4f, cy + u * 0.4f);
                p.lineTo(cx - u, cy + u * 0.4f);
                p.close();
                cv.drawPath(p, fill);
                if (glyph == Glyph.SOUND_ON) {
                    cv.drawArc(cx - u * 0.1f, cy - u * 0.7f, cx + u * 0.9f, cy + u * 0.7f, -50, 100, false, stroke);
                    cv.drawArc(cx + u * 0.1f, cy - u * 1.1f, cx + u * 1.4f, cy + u * 1.1f, -50, 100, false, stroke);
                } else {
                    cv.drawLine(cx + u * 0.5f, cy - u * 0.5f, cx + u * 1.1f, cy + u * 0.5f, stroke);
                    cv.drawLine(cx + u * 1.1f, cy - u * 0.5f, cx + u * 0.5f, cy + u * 0.5f, stroke);
                }
                break;
            }
            case STAR:
            case STAR_OUTLINE: {
                Path p = starPath(cx, cy, u * 1.1f, u * 0.45f);
                if (glyph == Glyph.STAR) cv.drawPath(p, fill);
                else { stroke.setStrokeWidth(sw * 0.8f); cv.drawPath(p, stroke); }
                break;
            }
            case LOCK:
                cv.drawRoundRect(cx - u * 0.75f, cy - u * 0.15f, cx + u * 0.75f, cy + u, sw, sw, fill);
                cv.drawArc(cx - u * 0.5f, cy - u, cx + u * 0.5f, cy + u * 0.1f, 180, 180, false, stroke);
                break;
            case BACK: {
                Path p = new Path();
                p.moveTo(cx + u * 0.5f, cy - u);
                p.lineTo(cx - u * 0.6f, cy);
                p.lineTo(cx + u * 0.5f, cy + u);
                cv.drawPath(p, stroke);
                break;
            }
            case NEXT: {
                Path p = new Path();
                p.moveTo(cx - u * 0.5f, cy - u);
                p.lineTo(cx + u * 0.6f, cy);
                p.lineTo(cx - u * 0.5f, cy + u);
                cv.drawPath(p, stroke);
                break;
            }
            case RESTART:
                cv.drawArc(cx - u, cy - u, cx + u, cy + u, -40, 300, false, stroke);
                Path arrow = new Path();
                arrow.moveTo(cx + u * 0.95f, cy - u * 0.75f);
                arrow.lineTo(cx + u * 0.95f, cy - u * 0.1f);
                arrow.lineTo(cx + u * 0.35f, cy - u * 0.45f);
                arrow.close();
                cv.drawPath(arrow, fill);
                break;
            case BOLT: {
                Path p = new Path();
                p.moveTo(cx + u * 0.3f, cy - u);
                p.lineTo(cx - u * 0.6f, cy + u * 0.15f);
                p.lineTo(cx, cy + u * 0.15f);
                p.lineTo(cx - u * 0.3f, cy + u);
                p.lineTo(cx + u * 0.6f, cy - u * 0.15f);
                p.lineTo(cx, cy - u * 0.15f);
                p.close();
                cv.drawPath(p, fill);
                break;
            }
            case HAMMER:
                cv.drawRoundRect(cx - u, cy - u, cx + u * 0.1f, cy - u * 0.4f, sw, sw, fill);
                stroke.setStrokeWidth(sw * 1.3f);
                cv.drawLine(cx - u * 0.4f, cy - u * 0.4f, cx + u * 0.8f, cy + u, stroke);
                break;
            case SHUFFLE:
                cv.drawLine(cx - u, cy - u * 0.6f, cx + u, cy + u * 0.6f, stroke);
                cv.drawLine(cx - u, cy + u * 0.6f, cx + u, cy - u * 0.6f, stroke);
                break;
            case CLOSE:
                cv.drawLine(cx - u * 0.8f, cy - u * 0.8f, cx + u * 0.8f, cy + u * 0.8f, stroke);
                cv.drawLine(cx - u * 0.8f, cy + u * 0.8f, cx + u * 0.8f, cy - u * 0.8f, stroke);
                break;
            case COIN:
                cv.drawCircle(cx, cy, u, fill);
                stroke.setColor(Ui.darken(color, 0.6f));
                cv.drawCircle(cx, cy, u * 0.6f, stroke);
                break;
            case GEM:
            default: {
                Path p = new Path();
                p.moveTo(cx, cy - u);
                p.lineTo(cx + u, cy - u * 0.2f);
                p.lineTo(cx, cy + u);
                p.lineTo(cx - u, cy - u * 0.2f);
                p.close();
                cv.drawPath(p, fill);
                break;
            }
        }
    }

    public static Path starPath(float cx, float cy, float rOut, float rIn) {
        Path p = new Path();
        for (int i = 0; i < 10; i++) {
            double a = Math.PI / 2 + i * Math.PI / 5;
            float r = (i % 2 == 0) ? rOut : rIn;
            float x = cx + (float) Math.cos(a) * r;
            float y = cy - (float) Math.sin(a) * r;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
        return p;
    }
}
