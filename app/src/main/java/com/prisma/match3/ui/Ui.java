package com.prisma.match3.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

/** Styling helpers for a consistent, modern, dark UI built entirely in code. */
public final class Ui {
    private Ui() {}

    public static final int BG0     = 0xFF0E0B22;
    public static final int PANEL   = 0xF21A1533;
    public static final int PANEL2  = 0xFF241C46;
    public static final int ACCENT  = 0xFF6FE7FF;
    public static final int ACCENT2 = 0xFF9B54E0;
    public static final int GOLD     = 0xFFFFD34E;
    public static final int TEXT     = 0xFFF2F0FF;
    public static final int TEXT_DIM = 0xFFB4AEDA;
    public static final int GREEN    = 0xFF3FBF6B;
    public static final int RED      = 0xFFE23B4E;

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    public static float sp(Context c, float v) { return v; }

    public static GradientDrawable roundRect(int color, int radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static GradientDrawable gradient(int c0, int c1, int radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{c0, c1});
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static GradientDrawable stroked(int fill, int stroke, int radiusDp, int strokeDp, Context c) {
        GradientDrawable g = roundRect(fill, radiusDp, c);
        g.setStroke(dp(c, strokeDp), stroke);
        return g;
    }

    /** Primary action button background with a pressed state. */
    public static StateListDrawable buttonBg(int c0, int c1, Context c) {
        StateListDrawable sl = new StateListDrawable();
        GradientDrawable pressed = gradient(darken(c0, 0.8f), darken(c1, 0.8f), 16, c);
        GradientDrawable normal = gradient(c0, c1, 16, c);
        sl.addState(new int[]{android.R.attr.state_pressed}, pressed);
        sl.addState(new int[]{}, normal);
        return sl;
    }

    public static TextView label(Context c, String text, float sizeSp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    public static int darken(int color, float f) {
        int r = (int) (Color.red(color) * f);
        int g = (int) (Color.green(color) * f);
        int b = (int) (Color.blue(color) * f);
        return Color.argb(Color.alpha(color), r, g, b);
    }

    public static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
