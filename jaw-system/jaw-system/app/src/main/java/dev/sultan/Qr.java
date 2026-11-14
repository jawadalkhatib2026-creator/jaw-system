package dev.sultan;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.HashMap;
import java.util.Map;

final class Qr {
    static Bitmap make(String text, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 2);
        BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
        int w = m.getWidth(), h = m.getHeight();
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                px[y * w + x] = m.get(x, y) ? Color.BLACK : Color.WHITE;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        b.setPixels(px, 0, w, 0, 0, w, h);
        return b;
    }

    /** Shows a QR code for url in a dialog; the server is stopped when the dialog closes. */
    static AlertDialog show(Activity a, String title, String url, LocalServer srv) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(Ui.dp(a, 24), Ui.dp(a, 16), Ui.dp(a, 24), Ui.dp(a, 8));
        ImageView iv = new ImageView(a);
        try { iv.setImageBitmap(make(url, Ui.dp(a, 280))); } catch (Exception e) { }
        box.addView(iv, new LinearLayout.LayoutParams(Ui.dp(a, 280), Ui.dp(a, 280)));
        TextView hint = new TextView(a);
        hint.setText(R.string.scan_hint);
        hint.setGravity(Gravity.CENTER);
        box.addView(hint);
        TextView u = new TextView(a);
        u.setText(url);
        u.setTextSize(12);
        u.setAlpha(0.6f);
        u.setGravity(Gravity.CENTER);
        box.addView(u);
        AlertDialog dlg = new AlertDialog.Builder(a).setTitle(title).setView(box)
            .setNegativeButton(R.string.close, null).create();
        dlg.setOnDismissListener(d -> srv.stop());
        dlg.show();
        return dlg;
    }
}
