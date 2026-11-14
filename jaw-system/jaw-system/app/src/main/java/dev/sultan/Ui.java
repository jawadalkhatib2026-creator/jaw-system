package dev.sultan;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class Ui {
    static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    static TextView text(Context c, String s, int sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    static GradientDrawable pill(Context c, int color) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, 16));
        g.setColor(color);
        return g;
    }

    /** A themed scrolling screen with a title. The vertical list is returned in page[0]. */
    static ScrollView screen(Activity a, String title, LinearLayout[] page) {
        ScrollView sv = new ScrollView(a);
        sv.setFillViewport(true);
        sv.setBackground(Theme.background(Store.theme(a)));
        LinearLayout p = new LinearLayout(a);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(a, 120), dp(a, 48), dp(a, 120), dp(a, 40));
        sv.addView(p, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView t = text(a, title, 30, Color.WHITE);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.bottomMargin = dp(a, 24);
        p.addView(t, tp);
        page[0] = p;
        return sv;
    }

    static LinearLayout row(Context c, LinearLayout page, String title, String value, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(c, 28), dp(c, 20), dp(c, 28), dp(c, 20));
        r.setFocusable(true);
        r.setClickable(true);
        r.setBackground(pill(c, 0x14FFFFFF));
        r.addView(text(c, title, 20, Color.WHITE),
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (value != null) r.addView(text(c, value, 18, 0xCCFFFFFF));
        r.setOnFocusChangeListener((v, f) -> r.setBackground(pill(c, f ? 0x40FFFFFF : 0x14FFFFFF)));
        r.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 12);
        page.addView(r, lp);
        return r;
    }
}
