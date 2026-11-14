package dev.sultan;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import java.io.File;

public class ShareActivity extends Activity {
    private LocalServer server;
    private LinearLayout waiting;
    private ImageView photo;
    private VideoView video;
    private File shown;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout root = new FrameLayout(this);
        root.setBackground(Theme.background(Store.theme(this)));

        photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photo.setVisibility(View.GONE);
        root.addView(photo, new FrameLayout.LayoutParams(-1, -1));

        video = new VideoView(this);
        video.setVisibility(View.GONE);
        FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(-1, -1);
        root.addView(video, vp);

        waiting = new LinearLayout(this);
        waiting.setOrientation(LinearLayout.VERTICAL);
        waiting.setGravity(Gravity.CENTER);
        TextView title = Ui.text(this, getString(R.string.share_title), 28, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        waiting.addView(title);
        root.addView(waiting, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        server = new LocalServer(this, (route, type, f) -> {
            File dest = new File(getCacheDir(), "share_" + System.currentTimeMillis());
            if (!f.renameTo(dest)) return "Failed / فشل";
            runOnUiThread(() -> show(type, dest));
            return null;
        });
        String url = server.start() ? server.url("share") : null;
        if (url == null) {
            waiting.addView(Ui.text(this, getString(R.string.conn_none), 18, 0xCCFFFFFF));
            return;
        }
        ImageView qr = new ImageView(this);
        try { qr.setImageBitmap(Qr.make(url, Ui.dp(this, 300))); } catch (Exception e) { }
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(Ui.dp(this, 300), Ui.dp(this, 300));
        qp.topMargin = Ui.dp(this, 24);
        waiting.addView(qr, qp);
        TextView hint = Ui.text(this, getString(R.string.scan_hint), 16, 0xCCFFFFFF);
        hint.setGravity(Gravity.CENTER);
        waiting.addView(hint);
        TextView wait = Ui.text(this, getString(R.string.share_wait), 14, 0x80FFFFFF);
        wait.setGravity(Gravity.CENTER);
        waiting.addView(wait);
    }

    private void show(String type, File f) {
        waiting.setVisibility(View.GONE);
        if (shown != null) shown.delete();
        shown = f;
        if ("video".equals(type)) {
            photo.setVisibility(View.GONE);
            video.setVisibility(View.VISIBLE);
            video.setVideoPath(f.getPath());
            video.setOnPreparedListener(mp -> mp.setLooping(true));
            video.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show();
                return true;
            });
            video.start();
        } else {
            video.stopPlayback();
            video.setVisibility(View.GONE);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            int s = 1;
            while (o.outWidth / (s * 2) >= 1920) s *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = s;
            Bitmap bm = BitmapFactory.decodeFile(f.getPath(), o);
            if (bm == null) { Toast.makeText(this, R.string.cant_open, Toast.LENGTH_SHORT).show(); return; }
            photo.setImageBitmap(bm);
            photo.setVisibility(View.VISIBLE);
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
        video.stopPlayback();
        if (shown != null) shown.delete();
    }
}
