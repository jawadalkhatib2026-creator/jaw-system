package dev.sultan;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import java.util.List;

public class JawStoreActivity extends Activity {
    // Editable list: {name, package id}. Apps that aren't installed open their Google Play page.
    private static final String[][] CATALOG = {
        {"YouTube", "com.google.android.youtube.tv"},
        {"Netflix", "com.netflix.ninja"},
        {"Disney+", "com.disney.disneyplus"},
        {"Prime Video", "com.amazon.amazonvideo.webapp"},
        {"Shahid", "net.mbc.shahid"},
        {"Discovery+", "com.discovery.discoveryplus.androidtv"},
        {"Spotify", "com.spotify.tv.android"},
        {"Twitch", "tv.twitch.android.app"},
        {"VLC", "org.videolan.vlc"},
        {"Kodi", "org.xbmc.kodi"},
        {"Google Play Store", "com.android.vending"},
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        LinearLayout[] pg = new LinearLayout[1];
        ScrollView sv = Ui.screen(this, getString(R.string.jaw_store), pg);
        PackageManager pm = getPackageManager();
        View first = null;
        for (String[] app : CATALOG) {
            final String name = app[0], pkg = app[1];
            boolean installed = Store.launch(pm, pkg) != null;
            LinearLayout r = Ui.row(this, pg[0], name,
                getString(installed ? R.string.open : R.string.get), v -> openOrGet(pkg));
            r.setOnLongClickListener(v -> {
                List<String> l = Store.apps(this);
                if (!l.contains(pkg)) l.add(pkg);
                Store.saveApps(this, l);
                Toast.makeText(this, R.string.added_home, Toast.LENGTH_SHORT).show();
                return true;
            });
            if (first == null) first = r;
        }
        pg[0].addView(Ui.text(this, getString(R.string.store_hint), 14, 0x80FFFFFF));
        setContentView(sv);
        if (first != null) first.requestFocus();
    }

    private void openOrGet(String pkg) {
        Intent li = Store.launch(getPackageManager(), pkg);
        if (li != null) { startActivity(li); return; }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg))
                .setPackage("com.android.vending").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e2) {
                Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
