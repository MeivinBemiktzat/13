package com.mikdash.albumdesigner;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

/**
 * Draws a page and its elements in canonical page coordinates. Callers scale
 * the canvas beforehand so the same routine serves the on-screen editor and
 * the full-resolution exporter, keeping WYSIWYG output.
 */
public final class Renderer {

    public interface ImageProvider {
        Bitmap get(String uri);
    }

    public static final String[] FILTER_NAMES = {
            "מקורי", "שחור-לבן", "ספיה", "חמים", "קריר", "בהיר", "ניגודיות", "וינטג'", "דהוי"
    };

    private static android.graphics.ColorMatrixColorFilter filterFor(int f) {
        android.graphics.ColorMatrix m = new android.graphics.ColorMatrix();
        switch (f) {
            case 1: m.setSaturation(0f); break;
            case 2: m.setSaturation(0f);
                android.graphics.ColorMatrix s = new android.graphics.ColorMatrix(new float[]{
                        1.07f,0,0,0,20, 0,0.94f,0,0,10, 0,0,0.62f,0,-10, 0,0,0,1,0}); m.postConcat(s); break;
            case 3: m.postConcat(new android.graphics.ColorMatrix(new float[]{
                    1.12f,0,0,0,12, 0,1.02f,0,0,4, 0,0,0.9f,0,0, 0,0,0,1,0})); break;
            case 4: m.postConcat(new android.graphics.ColorMatrix(new float[]{
                    0.9f,0,0,0,0, 0,1.0f,0,0,4, 0,0,1.15f,0,12, 0,0,0,1,0})); break;
            case 5: m.postConcat(new android.graphics.ColorMatrix(new float[]{
                    1.15f,0,0,0,25, 0,1.15f,0,0,25, 0,0,1.15f,0,25, 0,0,0,1,0})); break;
            case 6: m.postConcat(new android.graphics.ColorMatrix(new float[]{
                    1.35f,0,0,0,-40, 0,1.35f,0,0,-40, 0,0,1.35f,0,-40, 0,0,0,1,0})); break;
            case 7: m.setSaturation(0.7f);
                m.postConcat(new android.graphics.ColorMatrix(new float[]{
                        1.0f,0,0,0,10, 0,0.95f,0,0,5, 0,0,0.8f,0,10, 0,0,0,1,0})); break;
            case 8: m.setSaturation(0.55f);
                m.postConcat(new android.graphics.ColorMatrix(new float[]{
                        1.0f,0,0,0,30, 0,1.0f,0,0,30, 0,0,1.0f,0,30, 0,0,0,0.92f,0})); break;
            default: return null;
        }
        return new android.graphics.ColorMatrixColorFilter(m);
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint bmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF r = new RectF();

    public void drawPage(Canvas c, Model.Page page, int pw, int ph, ImageProvider img) {
        // background
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        if (page.bgType == Model.BG_GRADIENT) {
            double a = Math.toRadians(page.gradientAngle);
            float ex = (float) Math.cos(a) * pw, ey = (float) Math.sin(a) * ph;
            p.setShader(new LinearGradient(0, 0, ex, ey, page.bgColor, page.bgColor2, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, pw, ph, p);
            p.setShader(null);
        } else if (page.bgType == Model.BG_PHOTO && page.bgUri != null && img != null) {
            Bitmap b = img.get(page.bgUri);
            p.setColor(0xFFEEEEEE);
            c.drawRect(0, 0, pw, ph, p);
            if (b != null) drawCoverBitmap(c, b, 0, 0, pw, ph);
        } else if (page.bgType == Model.BG_RADIAL) {
            android.graphics.RadialGradient rg = new android.graphics.RadialGradient(
                    pw / 2f, ph / 2f, Math.max(pw, ph) / 1.4f, page.bgColor, page.bgColor2, Shader.TileMode.CLAMP);
            p.setShader(rg); c.drawRect(0, 0, pw, ph, p); p.setShader(null);
        } else if (page.bgType == Model.BG_PATTERN) {
            p.setColor(page.bgColor); c.drawRect(0, 0, pw, ph, p);
            Patterns.draw(c, page.patternId, pw, ph, page.bgColor2);
        } else {
            p.setColor(page.bgColor);
            c.drawRect(0, 0, pw, ph, p);
        }

        for (Model.El e : page.els) drawEl(c, e, img);

        if (page.overlay == 1) {
            android.graphics.RadialGradient v = new android.graphics.RadialGradient(
                    pw / 2f, ph / 2f, Math.max(pw, ph) * 0.72f,
                    new int[]{0x00000000, 0x00000000, 0x55000000}, new float[]{0f, 0.65f, 1f}, Shader.TileMode.CLAMP);
            p.setShader(v); c.drawRect(0, 0, pw, ph, p); p.setShader(null);
        } else if (page.overlay == 2) {
            p.setShader(new LinearGradient(0, 0, 0, ph, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, pw, ph, p); p.setShader(null);
        }
    }

    public void drawEl(Canvas c, Model.El e, ImageProvider img) {
        c.save();
        c.rotate(e.rotation, e.x + e.w / 2, e.y + e.h / 2);
        int alpha = Math.max(0, Math.min(255, e.alpha));
        switch (e.kind) {
            case Model.KIND_PHOTO: drawPhoto(c, e, img, alpha); break;
            case Model.KIND_TEXT: drawText(c, e, alpha); break;
            case Model.KIND_SHAPE: drawShape(c, e, alpha); break;
            case Model.KIND_STICKER: drawSticker(c, e, alpha); break;
            case Model.KIND_CLIP: drawClip(c, e, alpha); break;
        }
        c.restore();
    }

    private final Paint clipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private void drawClip(Canvas c, Model.El e, int alpha) {
        clipPaint.reset();
        clipPaint.setAntiAlias(true);
        clipPaint.setAlpha(alpha);
        Clipart.draw(c, e.clipId, e.x, e.y, e.w, e.h, clipPaint, e.fillColor, e.clipColor2);
    }

    private void drawPhoto(Canvas c, Model.El e, ImageProvider img, int alpha) {
        float mi = Frames.matInset(e.frameStyle) * Math.min(e.w, e.h);
        r.set(e.x + mi, e.y + mi, e.x + e.w - mi, e.y + e.h - mi);
        Path clip = new Path();
        clip.addRoundRect(r, e.corner, e.corner, Path.Direction.CW);
        final float px = r.left, py = r.top, pw = r.width(), ph = r.height();
        c.save();
        c.clipPath(clip);
        Bitmap b = img != null && e.uri != null ? img.get(e.uri) : null;
        if (b != null) {
            bmpPaint.setAlpha(alpha);
            bmpPaint.setColorFilter(filterFor(e.filter));
            drawCoverBitmap(c, b, px, py, pw, ph, e.photoScale, e.photoDx, e.photoDy);
            bmpPaint.setColorFilter(null);
        } else {
            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFE0E0E0);
            c.drawRect(px, py, px + pw, py + ph, p);
            p.setColor(0xFF9E9E9E);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(Math.min(pw, ph) * 0.16f);
            c.drawText("＋ תמונה", px + pw / 2, py + ph / 2, p);
            p.setTextAlign(Paint.Align.LEFT);
        }
        c.restore();
        if (e.borderW > 0) {
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(e.borderW);
            p.setColor(e.borderColor);
            r.set(px + e.borderW / 2, py + e.borderW / 2, px + pw - e.borderW / 2, py + ph - e.borderW / 2);
            c.drawRoundRect(r, e.corner, e.corner, p);
            p.setStyle(Paint.Style.FILL);
        }
        if (e.frameStyle > 0) {
            r.set(px, py, px + pw, py + ph);
            Frames.draw(c, e.frameStyle, r, e.corner, e.borderColor, framePaint);
        }
    }

    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private void drawCoverBitmap(Canvas c, Bitmap b, float x, float y, float w, float h) {
        drawCoverBitmap(c, b, x, y, w, h, 1f, 0f, 0f);
    }

    private void drawCoverBitmap(Canvas c, Bitmap b, float x, float y, float w, float h,
                                 float scale, float dx, float dy) {
        float bw = b.getWidth(), bh = b.getHeight();
        float s = Math.max(w / bw, h / bh) * scale;
        float dw = bw * s, dh = bh * s;
        float left = x + (w - dw) / 2 + dx * w;
        float top = y + (h - dh) / 2 + dy * h;
        r.set(left, top, left + dw, top + dh);
        c.drawBitmap(b, null, r, bmpPaint);
    }

    private void drawText(Canvas c, Model.El e, int alpha) {
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(e.textColor);
        tp.setAlpha(alpha);
        tp.setTextSize(e.textSize);
        tp.setTypeface(Palette.font(e.font, e.bold, e.italic));
        tp.setUnderlineText(e.underline);
        if (e.letterSpacing != 0) try { tp.setLetterSpacing(e.letterSpacing); } catch (Throwable ignored) {}
        if (e.shadow == 1) tp.setShadowLayer(e.textSize * 0.08f, 0, e.textSize * 0.05f, 0x88000000);
        Layout.Alignment al = e.align == Model.ALIGN_LEFT ? Layout.Alignment.ALIGN_OPPOSITE
                : e.align == Model.ALIGN_RIGHT ? Layout.Alignment.ALIGN_NORMAL
                : Layout.Alignment.ALIGN_CENTER;
        int width = Math.max(1, (int) e.w);
        StaticLayout sl = new StaticLayout(e.text == null ? "" : e.text, tp, width,
                al, 1.0f, 0.0f, false);
        float th = sl.getHeight();
        c.save();
        if (e.bgBox != 0) {
            p.setShader(null); p.setStyle(Paint.Style.FILL);
            p.setColor(e.bgBox); p.setAlpha(alpha);
            float pad = e.textSize * 0.3f;
            r.set(e.x - pad, e.y + (e.h - th) / 2 - pad, e.x + e.w + pad, e.y + (e.h - th) / 2 + th + pad);
            c.drawRoundRect(r, pad, pad, p);
            p.setAlpha(255);
        }
        c.translate(e.x, e.y + (e.h - th) / 2);
        sl.draw(c);
        c.restore();
    }

    private void drawSticker(Canvas c, Model.El e, int alpha) {
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF000000);
        p.setAlpha(alpha);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.DEFAULT);
        float size = Math.min(e.w, e.h);
        p.setTextSize(size * 0.86f);
        Paint.FontMetrics fm = p.getFontMetrics();
        float baseline = e.y + e.h / 2 - (fm.ascent + fm.descent) / 2;
        c.drawText(e.emoji == null ? "★" : e.emoji, e.x + e.w / 2, baseline, p);
        p.setTextAlign(Paint.Align.LEFT);
        p.setAlpha(255);
    }

    private void drawShape(Canvas c, Model.El e, int alpha) {
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(e.fillColor);
        p.setAlpha(Color.alpha(e.fillColor) * alpha / 255);
        Path path = shapePath(e);
        if (e.shapeType == Model.SHAPE_LINE) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(Math.max(4, e.h));
            p.setColor(e.fillColor); p.setAlpha(alpha);
            c.drawLine(e.x, e.y + e.h / 2, e.x + e.w, e.y + e.h / 2, p);
        } else {
            c.drawPath(path, p);
        }
        if (e.strokeW > 0 && e.shapeType != Model.SHAPE_LINE) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(e.strokeW);
            p.setColor(e.strokeColor);
            p.setAlpha(Color.alpha(e.strokeColor) * alpha / 255);
            c.drawPath(path, p);
        }
        p.setAlpha(255);
        p.setStyle(Paint.Style.FILL);
    }

    private Path shapePath(Model.El e) {
        Path path = new Path();
        float x = e.x, y = e.y, w = e.w, h = e.h;
        r.set(x, y, x + w, y + h);
        switch (e.shapeType) {
            case Model.SHAPE_ROUND:
                path.addRoundRect(r, Math.min(w, h) * 0.18f, Math.min(w, h) * 0.18f, Path.Direction.CW);
                break;
            case Model.SHAPE_CIRCLE:
                path.addOval(r, Path.Direction.CW);
                break;
            case Model.SHAPE_TRIANGLE:
                path.moveTo(x + w / 2, y);
                path.lineTo(x + w, y + h);
                path.lineTo(x, y + h);
                path.close();
                break;
            case Model.SHAPE_HEART:
                heart(path, x, y, w, h);
                break;
            case Model.SHAPE_STAR:
                star(path, x + w / 2, y + h / 2, Math.min(w, h) / 2, Math.min(w, h) / 2 * 0.45f, 5);
                break;
            default:
                path.addRect(r, Path.Direction.CW);
        }
        return path;
    }

    private void heart(Path path, float x, float y, float w, float h) {
        path.moveTo(x + w / 2, y + h * 0.28f);
        path.cubicTo(x + w * 0.15f, y - h * 0.10f, x - w * 0.25f, y + h * 0.45f, x + w / 2, y + h);
        path.cubicTo(x + w * 1.25f, y + h * 0.45f, x + w * 0.85f, y - h * 0.10f, x + w / 2, y + h * 0.28f);
        path.close();
    }

    private void star(Path path, float cx, float cy, float outer, float inner, int points) {
        double step = Math.PI / points;
        double a = -Math.PI / 2;
        path.moveTo(cx + (float) Math.cos(a) * outer, cy + (float) Math.sin(a) * outer);
        for (int i = 1; i < points * 2; i++) {
            a += step;
            float rad = (i % 2 == 0) ? outer : inner;
            path.lineTo(cx + (float) Math.cos(a) * rad, cy + (float) Math.sin(a) * rad);
        }
        path.close();
    }
}
