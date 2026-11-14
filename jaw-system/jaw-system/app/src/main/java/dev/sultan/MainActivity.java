package dev.sultan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int[][] PALETTE = {
        {0xFFE8590C, 0xFFC92A2A}, {0xFF1C7ED6, 0xFF1864AB}, {0xFF2F9E44, 0xFF1B6B30},
        {0xFF7048E8, 0xFF5F3DC4}, {0xFFD6336C, 0xFFA61E4D}, {0xFF0CA678, 0xFF087F5B},
    };

    private FrameLayout root;
    private ImageView bg;
    private View dim;
    private LinearLayout row;
    private TextClock clock, date;
    private Bitmap wallBitmap;
    private long wallStamp = -1;

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        root = new FrameLayout(this);
        root.setClipChildren(false);

        bg = new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(bg, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dim = new View(this);
        dim.setBackgroundColor(0x66000000);
        root.addView(dim, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = text(getString(R.string.app_name), 26, 0xCCFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.START);
        tp.setMarginStart(dp(56));
        tp.topMargin = dp(44);
        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_logo);
        brand.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.setMarginStart(dp(14));
        brand.addView(title, tl);
        root.addView(brand, tp);

        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.END);
        clock = new TextClock(this);
        clock.setTextSize(64);
        clock.setTextColor(Color.WHITE);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        date = new TextClock(this);
        date.setTextSize(20);
        date.setTextColor(0xB3FFFFFF);
        date.setFormat12Hour("EEEE, d MMMM");
        date.setFormat24Hour("EEEE, d MMMM");
        clockBox.addView(clock);
        clockBox.addView(date);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.END);
        cp.setMarginEnd(dp(56));
        cp.topMargin = dp(28);
        root.addView(clockBox, cp);

        HorizontalScrollView sv = new HorizontalScrollView(this);
        sv.setHorizontalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setClipChildren(false);
        sv.setPadding(dp(56), dp(40), dp(56), dp(40));
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        sv.addView(row);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM);
        sp.bottomMargin = dp(90);
        root.addView(sv, sp);

        TextView hint = text(getString(R.string.hint), 14, 0x80FFFFFF);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hp.bottomMargin = dp(36);
        root.addView(hint, hp);

        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        root.setBackground(Theme.background(Store.theme(this)));
        applyWallpaper();
        String f = Store.h24(this) ? "HH:mm" : "h:mm a";
        clock.setFormat12Hour(f);
        clock.setFormat24Hour(f);
        populate();
    }

    @Override public void onBackPressed() { /* a launcher has nowhere to go back to */ }

    private void applyWallpaper() {
        File w = Store.wallpaperFile(this);
        if (w.exists()) {
            if (wallBitmap == null || wallStamp != w.lastModified()) {
                wallBitmap = BitmapFactory.decodeFile(w.getPath());
                wallStamp = w.lastModified();
            }
            bg.setImageBitmap(wallBitmap);
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                bg.setRenderEffect(Store.glass(this)
                    ? android.graphics.RenderEffect.createBlurEffect(18f, 18f, android.graphics.Shader.TileMode.CLAMP)
                    : null);
            }
            bg.setVisibility(View.VISIBLE);
            dim.setVisibility(View.VISIBLE);
        } else {
            wallBitmap = null;
            wallStamp = -1;
            bg.setImageDrawable(null);
            bg.setVisibility(View.GONE);
            dim.setVisibility(View.GONE);
        }
    }

    private void populate() {
        row.removeAllViews();
        PackageManager pm = getPackageManager();
        for (String pkg : Store.apps(this)) {
            Intent li = Store.launch(pm, pkg);
            if (li == null) continue;
            addTile(appTile(pm, pkg, li));
        }
        JSONArray chans = Store.channels(this);
        for (int i = 0; i < chans.length(); i++) {
            JSONObject o = chans.optJSONObject(i);
            if (o != null) addTile(channelTile(o));
        }
        int[] grey = {0xFF3A3A3C, 0xFF2C2C2E};
        int[] jaw = {0xFF4C6EF5, 0xFF3B5BDB};
        addTile(textTile("↓", getString(R.string.jaw_store), jaw,
            v -> startActivity(new Intent(this, JawStoreActivity.class))));
        addTile(textTile("☺", getString(R.string.jaw_accounts), jaw,
            v -> startActivity(new Intent(this, AccountsActivity.class))));
        addTile(textTile("⇄", getString(R.string.jaw_share), jaw,
            v -> startActivity(new Intent(this, ShareActivity.class))));
        addTile(textTile("⚙", getString(R.string.original_settings), grey, v -> openOriginal()));
        addTile(textTile("✦", getString(R.string.sultan_settings),
            new int[]{0xFF4C6EF5, 0xFF3B5BDB},
            v -> startActivity(new Intent(this, SultanSettingsActivity.class))));
        row.getChildAt(0).requestFocus();
    }

    private void addTile(View t) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(28));
        row.addView(t, lp);
    }

    private View appTile(PackageManager pm, String pkg, Intent li) {
        CharSequence label = pkg;
        try { label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)); } catch (Exception e) { }
        ImageView iv = new ImageView(this);
        Drawable d = null;
        boolean banner = false;
        ActivityInfo ai = li.resolveActivityInfo(pm, 0);
        if (ai != null) {
            d = ai.loadBanner(pm);
            if (d != null) banner = true; else d = ai.loadIcon(pm);
        }
        if (banner) {
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setPadding(dp(30), dp(20), dp(30), dp(20));
        }
        iv.setImageDrawable(d);
        LinearLayout t = tile(label.toString(), iv, new int[]{0xFF2C2C2E, 0xFF1C1C1E}, v -> startActivity(li));
        t.setOnLongClickListener(v -> {
            List<String> l = Store.apps(this);
            l.remove(pkg);
            Store.saveApps(this, l);
            Toast.makeText(this, R.string.removed, Toast.LENGTH_SHORT).show();
            populate();
            return true;
        });
        return t;
    }

    private View channelTile(JSONObject o) {
        final String id = o.optString("id");
        final String name = o.optString("name");
        final String pkg = o.optString("pkg");
        final String url = o.optString("url");
        View content;
        File logo = Store.logoFile(this, id);
        Bitmap bmp = logo.exists() ? BitmapFactory.decodeFile(logo.getPath()) : null;
        if (bmp != null) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(bmp);
            content = iv;
        } else {
            String initials = name.length() >= 2 ? name.substring(0, 2) : name;
            TextView g = text(initials.toUpperCase(), 44, Color.WHITE);
            g.setGravity(Gravity.CENTER);
            g.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            content = g;
        }
        int[] colors = PALETTE[Math.abs(name.hashCode()) % PALETTE.length];
        LinearLayout t = tile(name, content, colors, v -> openChannel(pkg, url));
        t.setOnLongClickListener(v -> {
            Store.removeChannel(this, id);
            Toast.makeText(this, R.string.removed, Toast.LENGTH_SHORT).show();
            populate();
            return true;
        });
        return t;
    }

    private void openChannel(String pkg, String url) {
        if (url != null && !url.isEmpty()) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (pkg != null && !pkg.isEmpty()) i.setPackage(pkg);
                startActivity(i);
                return;
            } catch (Exception e) { /* fall back to just opening the app */ }
        }
        Intent li = pkg == null || pkg.isEmpty() ? null : Store.launch(getPackageManager(), pkg);
        if (li != null) startActivity(li);
        else Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show();
    }

    private View textTile(String glyph, String label, int[] colors, View.OnClickListener click) {
        TextView g = text(glyph, 52, Color.WHITE);
        g.setGravity(Gravity.CENTER);
        return tile(label, g, colors, click);
    }

    private LinearLayout tile(String label, View content, int[] colors, View.OnClickListener click) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setFocusable(true);
        t.setClickable(true);
        t.setClipChildren(false);

        FrameLayout card = new FrameLayout(this);
        if (Store.glass(this)) {
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x66FFFFFF, 0x22FFFFFF});
            gd.setCornerRadius(dp(18));
            gd.setStroke(dp(1), 0x99FFFFFF);
            card.setBackground(gd);
        } else {
            card.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors));
        }
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(18));
            }
        });
        card.setClipToOutline(true);
        card.addView(content, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        t.addView(card, new LinearLayout.LayoutParams(dp(240), dp(135)));

        TextView lab = text(label, 16, Color.WHITE);
        lab.setSingleLine();
        lab.setEllipsize(TextUtils.TruncateAt.END);
        lab.setGravity(Gravity.CENTER);
        lab.setAlpha(0.7f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(240), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        t.addView(lab, lp);

        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(dp(18));
        ring.setColor(Color.TRANSPARENT);
        ring.setStroke(dp(4), Color.WHITE);

        t.setOnFocusChangeListener((v, focused) -> {
            float s = focused ? 1.12f : 1f;
            v.animate().scaleX(s).scaleY(s).setDuration(160).start();
            card.setForeground(focused ? ring : null);
            lab.setAlpha(focused ? 1f : 0.7f);
        });
        t.setOnClickListener(click);
        return t;
    }

    private void openOriginal() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show();
        }
    }
}
