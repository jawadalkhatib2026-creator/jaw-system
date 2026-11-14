package dev.sultan;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.List;

public class SultanSettingsActivity extends Activity {
    private static final int REQ_WALL = 1;
    private static final int REQ_LOGO = 2;

    private ScrollView scroll;
    private LinearLayout page;
    private TextView themeValue, clockValue, glassValue, acctValue;
    private LocalServer server;
    private String pendingChannelId;

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private GradientDrawable pill(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(16));
        g.setColor(color);
        return g;
    }

    private LinearLayout row(String title, TextView value, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(28), dp(20), dp(28), dp(20));
        r.setFocusable(true);
        r.setClickable(true);
        r.setBackground(pill(0x14FFFFFF));
        TextView t = text(title, 20, Color.WHITE);
        r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (value != null) r.addView(value);
        r.setOnFocusChangeListener((v, f) -> r.setBackground(pill(f ? 0x40FFFFFF : 0x14FFFFFF)));
        r.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        page.addView(r, lp);
        return r;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(120), dp(48), dp(120), dp(40));
        scroll.addView(page, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text(getString(R.string.sultan_settings), 30, Color.WHITE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.bottomMargin = dp(24);
        page.addView(title, tp);

        themeValue = text("", 18, 0xCCFFFFFF);
        clockValue = text("", 18, 0xCCFFFFFF);

        LinearLayout first = row(getString(R.string.theme), themeValue, v -> {
            Store.setTheme(this, (Store.theme(this) + 1) % Theme.GRADIENTS.length);
            refresh();
        });
        row(getString(R.string.clock_format), clockValue, v -> {
            Store.setH24(this, !Store.h24(this));
            refresh();
        });
        glassValue = text("", 18, 0xCCFFFFFF);
        acctValue = text("", 18, 0xCCFFFFFF);
        row(getString(R.string.liquid_glass), glassValue, v -> {
            Store.setGlass(this, !Store.glass(this));
            refresh();
        });
        row(getString(R.string.account_type), acctValue, v -> {
            Store.setAccountMode(this, Store.nextMode(Store.accountMode(this)));
            refresh();
        });
        row(getString(R.string.jaw_accounts), null, v -> startActivity(new Intent(this, AccountsActivity.class)));
        row(getString(R.string.cast_settings), null, v -> {
            try { startActivity(new Intent(Settings.ACTION_CAST_SETTINGS)); }
            catch (Exception e) { Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show(); }
        });
        row(getString(R.string.add_apps), null, v -> showAppPicker());
        row(getString(R.string.wallpaper), null, v -> wallpaperChooser());
        row(getString(R.string.wallpaper_default), null, v -> {
            Store.wallpaperFile(this).delete();
            Toast.makeText(this, R.string.wallpaper_set, Toast.LENGTH_SHORT).show();
        });
        row(getString(R.string.add_channel), null, v -> addChannelStep1());
        row(getString(R.string.free_memory), null, v -> freeMemory());
        row(getString(R.string.net_check), null, v -> connectionCheck());
        row(getString(R.string.speed_test), null, v -> speedTest());
        row(getString(R.string.wifi_settings), null, v -> {
            try { startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); }
            catch (Exception e) { Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show(); }
        });
        row(getString(R.string.clear_cache), null, v -> {
            try { startActivity(new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)); }
            catch (Exception e) {
                try { startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS)); }
                catch (Exception e2) { Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show(); }
            }
        });
        row(getString(R.string.reset_apps), null, v -> {
            Store.resetApps(this);
            Toast.makeText(this, R.string.reset_done, Toast.LENGTH_SHORT).show();
        });
        row(getString(R.string.open_original), null, v -> {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show();
            }
        });
        page.addView(text(getString(R.string.about), 14, 0x80FFFFFF));

        setContentView(scroll);
        refresh();
        first.requestFocus();
    }

    private void refresh() {
        int t = Store.theme(this);
        scroll.setBackground(Theme.background(t));
        themeValue.setText(Theme.NAMES[t]);
        clockValue.setText(Store.h24(this) ? R.string.h24 : R.string.h12);
        glassValue.setText(Store.glass(this) ? R.string.on : R.string.off);
        acctValue.setText(Store.modeLabel(Store.accountMode(this)));
    }

    // ---- home screen apps ----

    private void showAppPicker() {
        List<String[]> apps = Apps.list(this);
        int n = apps.size();
        String[] pkgs = new String[n];
        String[] names = new String[n];
        boolean[] checked = new boolean[n];
        List<String> current = Store.apps(this);
        for (int k = 0; k < n; k++) {
            pkgs[k] = apps.get(k)[0];
            names[k] = apps.get(k)[1];
            checked[k] = current.contains(pkgs[k]);
        }
        new AlertDialog.Builder(this)
            .setTitle(R.string.choose_apps)
            .setMultiChoiceItems(names, checked, (dlg, which, isChecked) -> checked[which] = isChecked)
            .setPositiveButton(R.string.ok, (dlg, w) -> {
                List<String> out = new java.util.ArrayList<>();
                for (String p : current) {
                    for (int k = 0; k < n; k++) if (pkgs[k].equals(p) && checked[k]) out.add(p);
                }
                for (int k = 0; k < n; k++) if (checked[k] && !out.contains(pkgs[k])) out.add(pkgs[k]);
                Store.saveApps(this, out);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    // ---- wallpaper chooser ----

    @Override protected void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
    }

    private void wallpaperChooser() {
        String[] items = {getString(R.string.wp_phone), getString(R.string.wp_tv_files), getString(R.string.wp_presets)};
        new AlertDialog.Builder(this).setTitle(R.string.wp_choose)
            .setItems(items, (d, which) -> {
                if (which == 0) wallpaperFromPhone();
                else if (which == 1) pickImage(REQ_WALL);
                else presetPicker();
            })
            .setNegativeButton(R.string.cancel, null).show();
    }

    private void presetPicker() {
        String[] names = new String[Presets.NAMES.length];
        for (int i = 0; i < names.length; i++) names[i] = getString(Presets.NAMES[i]);
        new AlertDialog.Builder(this).setTitle(R.string.wp_presets)
            .setItems(names, (d, which) -> {
                android.graphics.Bitmap bm = Presets.render(which);
                try (java.io.FileOutputStream o = new java.io.FileOutputStream(Store.wallpaperFile(this))) {
                    bm.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, o);
                    Toast.makeText(this, R.string.wallpaper_set, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null).show();
    }

    private void wallpaperFromPhone() {
        if (server != null) server.stop();
        final AlertDialog[] dlg = new AlertDialog[1];
        server = new LocalServer(this, (route, type, f) -> {
            boolean ok = Images.save(this, Uri.fromFile(f), Store.wallpaperFile(this), 1920, false);
            f.delete();
            if (ok) runOnUiThread(() -> {
                Toast.makeText(this, R.string.wallpaper_set, Toast.LENGTH_SHORT).show();
                if (dlg[0] != null) dlg[0].dismiss();
            });
            return ok ? null : "Failed / \u0641\u0634\u0644";
        });
        String url = server.start() ? server.url("wall") : null;
        if (url == null) {
            Toast.makeText(this, R.string.conn_none, Toast.LENGTH_LONG).show();
            server.stop();
            return;
        }
        dlg[0] = Qr.show(this, getString(R.string.wallpaper), url, server);
    }

    // ---- pictures ----

    private void pickImage(int req) {
        String[] actions = {Intent.ACTION_OPEN_DOCUMENT, Intent.ACTION_GET_CONTENT};
        for (String a : actions) {
            try {
                Intent i = new Intent(a).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
                startActivityForResult(i, req);
                return;
            } catch (Exception e) { /* try the next picker */ }
        }
        if (req == REQ_WALL) {
            File dir = getExternalFilesDir(null);
            if (dir != null) {
                File ext = new File(dir, "wallpaper.jpg");
                if (ext.exists() && Images.save(this, Uri.fromFile(ext), Store.wallpaperFile(this), 1920, false)) {
                    Toast.makeText(this, R.string.wallpaper_set, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        Toast.makeText(this, R.string.no_picker, Toast.LENGTH_LONG).show();
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri u = data.getData();
        if (req == REQ_WALL) {
            boolean ok = Images.save(this, u, Store.wallpaperFile(this), 1920, false);
            Toast.makeText(this, ok ? R.string.wallpaper_set : R.string.cant_open, Toast.LENGTH_SHORT).show();
        } else if (req == REQ_LOGO && pendingChannelId != null) {
            Images.save(this, u, Store.logoFile(this, pendingChannelId), 512, true);
            Toast.makeText(this, R.string.channel_added, Toast.LENGTH_SHORT).show();
        }
    }

    // ---- TV channels ----

    private void addChannelStep1() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(16), dp(24), 0);
        EditText name = new EditText(this);
        name.setHint(R.string.channel_name);
        name.setSingleLine();
        EditText link = new EditText(this);
        link.setHint(R.string.channel_link);
        link.setSingleLine();
        link.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        box.addView(name);
        box.addView(link);
        new AlertDialog.Builder(this)
            .setTitle(R.string.add_channel)
            .setView(box)
            .setPositiveButton(R.string.next, (d, w) -> {
                String n = name.getText().toString().trim();
                if (n.isEmpty()) return;
                addChannelStep2(n, link.getText().toString().trim());
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void addChannelStep2(String name, String url) {
        List<String[]> apps = Apps.list(this);
        int n = apps.size();
        String[] pkgs = new String[n];
        String[] names = new String[n];
        for (int k = 0; k < n; k++) {
            pkgs[k] = apps.get(k)[0];
            names[k] = apps.get(k)[1];
        }
        new AlertDialog.Builder(this)
            .setTitle(R.string.choose_app)
            .setItems(names, (d, which) -> finishChannel(name, url, pkgs[which]))
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void finishChannel(String name, String url, String pkg) {
        String id = String.valueOf(System.currentTimeMillis());
        Store.addChannel(this, id, name, pkg, url);
        pendingChannelId = id;
        new AlertDialog.Builder(this)
            .setMessage(R.string.add_logo)
            .setPositiveButton(R.string.pick, (d, w) -> pickImage(REQ_LOGO))
            .setNegativeButton(R.string.skip, (d, w) ->
                Toast.makeText(this, R.string.channel_added, Toast.LENGTH_SHORT).show())
            .show();
    }

    // ---- network ----

    private void connectionCheck() {
        StringBuilder sb = new StringBuilder();
        android.net.ConnectivityManager cm =
            (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        android.net.Network n = cm.getActiveNetwork();
        android.net.NetworkCapabilities caps = n == null ? null : cm.getNetworkCapabilities(n);
        if (caps == null) {
            sb.append(getString(R.string.conn_none));
        } else if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)) {
            sb.append(getString(R.string.conn_ethernet));
        } else if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
            android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager)
                getApplicationContext().getSystemService(WIFI_SERVICE);
            android.net.wifi.WifiInfo wi = wm.getConnectionInfo();
            int pct = Math.max(0, Math.min(100, 2 * (wi.getRssi() + 100)));
            boolean five = wi.getFrequency() > 4900;
            sb.append(getString(R.string.conn_wifi,
                getString(five ? R.string.band5 : R.string.band24), pct));
        } else {
            sb.append(getString(R.string.conn_none));
        }
        long freeMb = new android.os.StatFs(getFilesDir().getPath()).getAvailableBytes() / (1024L * 1024L);
        sb.append("\n\n").append(getString(R.string.storage_free, freeMb));
        if (freeMb < 1024) sb.append("\n").append(getString(R.string.storage_low));
        new AlertDialog.Builder(this).setMessage(sb.toString())
            .setPositiveButton(R.string.close, null).show();
    }

    private void speedTest() {
        Toast.makeText(this, R.string.testing, Toast.LENGTH_LONG).show();
        new Thread(() -> {
            double mbps = -1;
            try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection)
                    new java.net.URL("https://speed.cloudflare.com/__down?bytes=50000000").openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                long start = System.nanoTime();
                long bytes = 0;
                byte[] buf = new byte[65536];
                try (java.io.InputStream in = c.getInputStream()) {
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        bytes += r;
                        if (System.nanoTime() - start > 8_000_000_000L) break;
                    }
                }
                double secs = (System.nanoTime() - start) / 1e9;
                if (secs > 0 && bytes > 0) mbps = bytes * 8 / secs / 1e6;
                c.disconnect();
            } catch (Exception e) { /* mbps stays -1 */ }
            final double result = mbps;
            runOnUiThread(() -> {
                String msg;
                if (result < 0) msg = getString(R.string.speed_fail);
                else msg = getString(R.string.speed_result, result) + "\n"
                    + getString(result >= 8 ? R.string.speed_good : R.string.speed_low);
                new AlertDialog.Builder(this).setMessage(msg)
                    .setPositiveButton(R.string.close, null).show();
            });
        }).start();
    }

    // ---- memory ----

    private void freeMemory() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo before = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(before);
        Toast.makeText(this, R.string.freeing, Toast.LENGTH_SHORT).show();
        for (String[] a : Apps.list(this)) am.killBackgroundProcesses(a[0]);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ActivityManager.MemoryInfo after = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(after);
            long mb = Math.max(0L, (after.availMem - before.availMem) / (1024L * 1024L));
            Toast.makeText(this, getString(R.string.freed, mb), Toast.LENGTH_LONG).show();
        }, 1500);
    }
}
