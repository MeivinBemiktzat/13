package com.prisma.match3.render;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.prisma.match3.engine.Board;

/**
 * GLSurfaceView hosting the 3D board. Translates a tap-and-swipe into a swap of
 * two adjacent cells and forwards it to the renderer on the GL thread.
 */
public class GameView extends GLSurfaceView {
    private final GameRenderer renderer;
    private Board board;

    public interface TapListener { void onTap(int r, int c); }
    private TapListener tapListener;
    private volatile boolean tapArmed;

    private float downX, downY;
    private int downR = -1, downC = -1;
    private boolean swiped;
    private static final float THRESH_DP;
    static { THRESH_DP = 18f; }

    public GameView(Context ctx, GameRenderer renderer) {
        super(ctx);
        this.renderer = renderer;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    public void setBoard(Board b) { this.board = b; }

    public void setTapListener(TapListener l) { this.tapListener = l; }

    /** Arm single-tap targeting (used by power-ups); consumes the next tap. */
    public void armTap() { this.tapArmed = true; }
    public boolean isTapArmed() { return tapArmed; }

    @Override public boolean onTouchEvent(MotionEvent e) {
        Board b = board;
        if (b == null) return true;
        float px = e.getX(), py = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = px; downY = py; swiped = false;
                int[] cell = cellAt(px, py);
                downR = cell[0]; downC = cell[1];
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!swiped && downR >= 0) {
                    float dx = px - downX, dy = py - downY;
                    float thresh = THRESH_DP * getResources().getDisplayMetrics().density;
                    if (Math.abs(dx) > thresh || Math.abs(dy) > thresh) {
                        swiped = true;
                        int tr = downR, tc = downC;
                        if (Math.abs(dx) > Math.abs(dy)) tc += dx > 0 ? 1 : -1;
                        else tr += dy > 0 ? 1 : -1;
                        fireSwap(downR, downC, tr, tc);
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!swiped && downR >= 0 && tapArmed && tapListener != null) {
                    tapArmed = false;
                    final int fr = downR, fc = downC;
                    post(new Runnable() { @Override public void run() { tapListener.onTap(fr, fc); } });
                }
                downR = downC = -1;
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void fireSwap(final int r1, final int c1, final int r2, final int c2) {
        queueEvent(new Runnable() {
            @Override public void run() { renderer.requestSwap(r1, c1, r2, c2); }
        });
    }

    private int[] cellAt(float px, float py) {
        int w = getWidth(), h = getHeight();
        float wx = (px / w * 2f - 1f) * renderer.halfW;
        float wy = -(py / h * 2f - 1f) * renderer.halfH;
        int cols = board.cols, rows = board.rows;
        int c = Math.round(wx + (cols - 1) / 2f);
        int r = Math.round((rows - 1) / 2f - wy);
        if (c < 0) c = 0; if (c >= cols) c = cols - 1;
        if (r < 0) r = 0; if (r >= rows) r = rows - 1;
        return new int[]{r, c};
    }
}
