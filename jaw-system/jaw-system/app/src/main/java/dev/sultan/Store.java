package dev.sultan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

final class Store {
    private static final String[] DEFAULTS = {
        "com.google.android.youtube.tv", "com.netflix.ninja",
        "com.amazon.amazonvideo.webapp", "com.disney.disneyplus",
        "com.spotify.tv.android", "com.android.vending"
    };

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("sultan", Context.MODE_PRIVATE);
    }

    static List<String> apps(Context c) {
        List<String> out = new ArrayList<>();
        String s = sp(c).getString("apps", null);
        if (s == null) {
            PackageManager pm = c.getPackageManager();
            for (String p : DEFAULTS) if (launch(pm, p) != null) out.add(p);
        } else if (!s.isEmpty()) {
            for (String p : s.split(",")) if (!p.isEmpty()) out.add(p);
        }
        return out;
    }

    static void saveApps(Context c, List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String p : l) { if (sb.length() > 0) sb.append(','); sb.append(p); }
        sp(c).edit().putString("apps", sb.toString()).apply();
    }

    static void resetApps(Context c) { sp(c).edit().remove("apps").apply(); }

    static int theme(Context c) { return sp(c).getInt("theme", 0); }
    static void setTheme(Context c, int t) { sp(c).edit().putInt("theme", t).apply(); }
    static boolean h24(Context c) { return sp(c).getBoolean("h24", false); }
    static void setH24(Context c, boolean v) { sp(c).edit().putBoolean("h24", v).apply(); }

    static File wallpaperFile(Context c) { return new File(c.getFilesDir(), "wallpaper.jpg"); }
    static File logoFile(Context c, String id) { return new File(c.getFilesDir(), "ch_" + id + ".png"); }

    static JSONArray channels(Context c) {
        try { return new JSONArray(sp(c).getString("channels", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    static void addChannel(Context c, String id, String name, String pkg, String url) {
        try {
            JSONArray a = channels(c);
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("name", name);
            o.put("pkg", pkg);
            o.put("url", url);
            a.put(o);
            sp(c).edit().putString("channels", a.toString()).apply();
        } catch (Exception e) { }
    }

    static void removeChannel(Context c, String id) {
        try {
            JSONArray a = channels(c);
            JSONArray out = new JSONArray();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (!id.equals(o.optString("id"))) out.put(o);
            }
            sp(c).edit().putString("channels", out.toString()).apply();
            logoFile(c, id).delete();
        } catch (Exception e) { }
    }

    static boolean glass(Context c) { return sp(c).getBoolean("glass", true); }
    static void setGlass(Context c, boolean v) { sp(c).edit().putBoolean("glass", v).apply(); }
    static String accountMode(Context c) { return sp(c).getString("acct_mode", "both"); }
    static void setAccountMode(Context c, String m) { sp(c).edit().putString("acct_mode", m).apply(); }
    static String nextMode(String m) { return "jaw".equals(m) ? "google" : "google".equals(m) ? "both" : "jaw"; }
    static int modeLabel(String m) {
        return "jaw".equals(m) ? R.string.acct_jaw : "google".equals(m) ? R.string.acct_google : R.string.acct_both;
    }
    static String user(Context c) { return sp(c).getString("user", null); }
    static void setUser(Context c, String u) {
        SharedPreferences.Editor e = sp(c).edit();
        if (u == null) e.remove("user"); else e.putString("user", u);
        e.apply();
    }

    static Intent launch(PackageManager pm, String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i != null) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }
}
