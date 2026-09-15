package com.mikdash.albumdesigner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Interactive WYSIWYG page editor: select, drag, pinch-scale, rotate, resize. */
public class EditorView extends View {

    public interface Listener {
        void onSelectionChanged(Model.El sel);
        void onEdited();
        void onBeginChange();
    }

    public void beginChange() { if (listener != null) listener.onBeginChange(); }

    private Model.Project project;
    private Model.Page page;
    private Images images;
    private final Renderer renderer = new Renderer();
    private Listener listener;

    private Model.El selected;
    private float scale = 1f, offX = 0f, offY = 0f;

    private final Paint chrome = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmp = new RectF();

    // gesture state
    private static final int MODE_NONE = 0, MODE_DRAG = 1, MODE_RESIZE = 2, MODE_ROTATE = 3, MODE_PINCH = 4;
    private int mode = MODE_NONE;
    private float lastX, lastY;
    private float startDist, startAngle, startW, startH, startRot;
    private float handlePx;

    public EditorView(Context c) {
        super(c);
        images = new Images(c, 1400);
        shadow.setColor(0x33000000);
        setLayerType(LAYER_TYPE_SOFTWARE, null); // reliable clipPath + shadow on all API levels
    }

    public void setListener(Listener l) { this.listener = l; }

    public void bind(Model.Project p, Model.Page pg) {
        this.project = p; this.page = pg; this.selected = null;
        if (listener != null) listener.onSelectionChanged(null);
        invalidate();
    }

    public Model.Page getPage() { return page; }
    public Model.El getSelected() { return selected; }
    public Images getImages() { return images; }

    public void select(Model.El e) {
        selected = e;
        if (listener != null) listener.onSelectionChanged(e);
        invalidate();
    }

    public void edited() {
        if (listener != null) listener.onEdited();
        invalidate();
    }

    public void addElement(Model.El e) {
        page.els.add(e);
        select(e);
        edited();
    }

    public void deleteSelected() {
        if (selected != null) { page.els.remove(selected); select(null); edited(); }
    }

    public void bringToFront() {
        if (selected != null && page.els.remove(selected)) { page.els.add(selected); edited(); }
    }

    public void sendToBack() {
        if (selected != null && page.els.remove(selected)) { page.els.add(0, selected); edited(); }
    }

    public void centerH() {
        if (selected != null) { beginChange(); selected.x = (project.pw() - selected.w) / 2f; edited(); }
    }

    public void centerV() {
        if (selected != null) { beginChange(); selected.y = (project.ph() - selected.h) / 2f; edited(); }
    }

    public void duplicateSelected() {
        if (selected != null) { Model.El c = selected.copy(); page.els.add(c); select(c); edited(); }
    }

    private void computeTransform() {
        int pw = project.pw(), ph = project.ph();
        float pad = Math.min(getWidth(), getHeight()) * 0.04f;
        float availW = getWidth() - 2 * pad, availH = getHeight() - 2 * pad;
        scale = Math.min(availW / pw, availH / ph);
        offX = (getWidth() - pw * scale) / 2f;
        offY = (getHeight() - ph * scale) / 2f;
        handlePx = Math.max(28, Math.min(getWidth(), getHeight()) * 0.045f);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (project == null || page == null) return;
        computeTransform();
        int pw = project.pw(), ph = project.ph();

        c.drawRoundRect(offX + 6, offY + 8, offX + pw * scale + 6, offY + ph * scale + 10, 8, 8, shadow);
        c.save();
        c.translate(offX, offY);
        c.scale(scale, scale);
        c.clipRect(0, 0, pw, ph);
        renderer.drawPage(c, page, pw, ph, images);
        c.restore();

        if (selected != null) drawSelection(c);
    }

    private void drawSelection(Canvas c) {
        Model.El e = selected;
        float cx = offX + (e.x + e.w / 2) * scale, cy = offY + (e.y + e.h / 2) * scale;
        c.save();
        c.rotate(e.rotation, cx, cy);
        float l = offX + e.x * scale, t = offY + e.y * scale, rr = l + e.w * scale, b = t + e.h * scale;
        chrome.setStyle(Paint.Style.STROKE);
        chrome.setStrokeWidth(3);
        chrome.setColor(0xFF6A1B9A);
        chrome.setPathEffect(new android.graphics.DashPathEffect(new float[]{14, 10}, 0));
        c.drawRect(l, t, rr, b, chrome);
        chrome.setPathEffect(null);
        chrome.setStyle(Paint.Style.FILL);
        // delete (top-left)
        handle(c, l, t, 0xFFE53935, "✕");
        // rotate (top-right)
        handle(c, rr, t, 0xFF1E88E5, "↻");
        // resize (bottom-right)
        handle(c, rr, b, 0xFF43A047, "⤡");
        c.restore();
    }

    private void handle(Canvas c, float x, float y, int color, String glyph) {
        chrome.setColor(0xFFFFFFFF);
        c.drawCircle(x, y, handlePx * 0.6f, chrome);
        chrome.setColor(color);
        c.drawCircle(x, y, handlePx * 0.52f, chrome);
        chrome.setColor(0xFFFFFFFF);
        chrome.setTextAlign(Paint.Align.CENTER);
        chrome.setTextSize(handlePx * 0.6f);
        Paint.FontMetrics fm = chrome.getFontMetrics();
        c.drawText(glyph, x, y - (fm.ascent + fm.descent) / 2, chrome);
        chrome.setTextAlign(Paint.Align.LEFT);
    }

    /* --------------------------- touch --------------------------------- */

    private float[] rotatePoint(float px, float py, float cx, float cy, float deg) {
        double a = Math.toRadians(deg);
        float dx = px - cx, dy = py - cy;
        float rx = (float) (dx * Math.cos(a) - dy * Math.sin(a));
        float ry = (float) (dx * Math.sin(a) + dy * Math.cos(a));
        return new float[]{cx + rx, cy + ry};
    }

    private boolean near(float x, float y, float hx, float hy) {
        return Math.hypot(x - hx, y - hy) <= handlePx * 0.8f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (project == null) return false;
        float x = ev.getX(), y = ev.getY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (selected != null) {
                    beginChange();
                    if (hitHandles(x, y)) return true;
                }
                Model.El hit = hitTest(x, y);
                boolean was = selected != null;
                select(hit);
                if (hit != null) { if (!was) beginChange(); mode = MODE_DRAG; lastX = x; lastY = y; }
                else mode = MODE_NONE;
                return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (selected != null && ev.getPointerCount() == 2) {
                    beginChange();
                    mode = MODE_PINCH;
                    startDist = spacing(ev);
                    startAngle = angle(ev);
                    startW = selected.w; startH = selected.h; startRot = selected.rotation;
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (selected == null) return true;
                if (mode == MODE_PINCH && ev.getPointerCount() >= 2) {
                    float d = spacing(ev), ang = angle(ev);
                    float f = Math.max(0.15f, d / Math.max(1f, startDist));
                    float ncx = selected.x + selected.w / 2, ncy = selected.y + selected.h / 2;
                    selected.w = Math.max(20, startW * f);
                    selected.h = Math.max(20, startH * f);
                    selected.x = ncx - selected.w / 2; selected.y = ncy - selected.h / 2;
                    selected.rotation = startRot + (ang - startAngle);
                    edited();
                } else if (mode == MODE_DRAG) {
                    float dx = (x - lastX) / scale, dy = (y - lastY) / scale;
                    selected.x += dx; selected.y += dy;
                    lastX = x; lastY = y;
                    edited();
                } else if (mode == MODE_RESIZE) {
                    float cx = selected.x + selected.w / 2, cy = selected.y + selected.h / 2;
                    float px = (x - offX) / scale, py = (y - offY) / scale;
                    float[] loc = rotatePoint(px, py, cx, cy, -selected.rotation);
                    float hw = Math.max(15, Math.abs(loc[0] - cx)), hh = Math.max(15, Math.abs(loc[1] - cy));
                    selected.w = hw * 2; selected.h = hh * 2;
                    selected.x = cx - hw; selected.y = cy - hh;
                    edited();
                } else if (mode == MODE_ROTATE) {
                    float cx = offX + (selected.x + selected.w / 2) * scale;
                    float cy = offY + (selected.y + selected.h / 2) * scale;
                    float deg = (float) Math.toDegrees(Math.atan2(y - cy, x - cx)) + 135f;
                    selected.rotation = Math.round(deg / 1f);
                    if (Math.abs(selected.rotation % 90) < 4) selected.rotation = Math.round(selected.rotation / 90) * 90;
                    edited();
                }
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP:
                if (ev.getPointerCount() <= 2) mode = MODE_DRAG;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mode = MODE_NONE;
                return true;
        }
        return true;
    }

    private boolean hitHandles(float x, float y) {
        Model.El e = selected;
        float cx = offX + (e.x + e.w / 2) * scale, cy = offY + (e.y + e.h / 2) * scale;
        float l = offX + e.x * scale, t = offY + e.y * scale, rr = l + e.w * scale, b = t + e.h * scale;
        float[] del = rotatePoint(l, t, cx, cy, e.rotation);
        float[] rot = rotatePoint(rr, t, cx, cy, e.rotation);
        float[] res = rotatePoint(rr, b, cx, cy, e.rotation);
        if (near(x, y, del[0], del[1])) { deleteSelected(); return true; }
        if (near(x, y, rot[0], rot[1])) { mode = MODE_ROTATE; return true; }
        if (near(x, y, res[0], res[1])) { mode = MODE_RESIZE; return true; }
        return false;
    }

    private Model.El hitTest(float x, float y) {
        float px = (x - offX) / scale, py = (y - offY) / scale;
        for (int i = page.els.size() - 1; i >= 0; i--) {
            Model.El e = page.els.get(i);
            float cx = e.x + e.w / 2, cy = e.y + e.h / 2;
            float[] loc = rotatePoint(px, py, cx, cy, -e.rotation);
            if (loc[0] >= e.x && loc[0] <= e.x + e.w && loc[1] >= e.y && loc[1] <= e.y + e.h) return e;
        }
        return null;
    }

    private float spacing(MotionEvent ev) {
        float dx = ev.getX(0) - ev.getX(1), dy = ev.getY(0) - ev.getY(1);
        return (float) Math.hypot(dx, dy);
    }

    private float angle(MotionEvent ev) {
        return (float) Math.toDegrees(Math.atan2(ev.getY(1) - ev.getY(0), ev.getX(1) - ev.getX(0)));
    }
}
