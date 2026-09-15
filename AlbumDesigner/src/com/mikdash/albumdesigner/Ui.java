package com.mikdash.albumdesigner;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers for building the programmatic UI consistently. */
public final class Ui {

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable roundBg(int color, int radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static TextView pillButton(Context c, String label, int bg, int fg, View.OnClickListener l) {
        TextView t = new TextView(c);
        t.setText(label);
        t.setTextColor(fg);
        t.setTextSize(15);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 18), dp(c, 12), dp(c, 18), dp(c, 12));
        t.setBackground(roundBg(bg, 24, c));
        t.setOnClickListener(l);
        return t;
    }

    public static TextView iconButton(Context c, String glyph, String label, View.OnClickListener l) {
        TextView t = new TextView(c);
        t.setText(glyph + "\n" + label);
        t.setTextColor(0xFF333333);
        t.setTextSize(12);
        t.setLineSpacing(dp(c, 2), 1f);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        t.setOnClickListener(l);
        return t;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static View swatch(Context c, int color, int sizeDp, View.OnClickListener l) {
        View v = new View(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp));
        lp.setMargins(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 4));
        v.setLayoutParams(lp);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, 8));
        g.setStroke(dp(c, 1), 0x33000000);
        v.setBackground(g);
        v.setOnClickListener(l);
        return v;
    }

    private Ui() {}
}
