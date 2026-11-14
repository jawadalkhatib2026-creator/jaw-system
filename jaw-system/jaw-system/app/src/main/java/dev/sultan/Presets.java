package dev.sultan;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/** Soft, colourful wallpapers drawn in code (original artwork, not Apple's images). */
final class Presets {
    static final int[] NAMES = {R.string.wp_aurora, R.string.wp_dusk, R.string.wp_ocean, R.string.wp_dune};
    private static final int[][] BASE = {
        {0xFF0B1B3A, 0xFF1B6B6F}, {0xFF2B1055, 0xFFD76D77}, {0xFF021B3B, 0xFF1C7ED6}, {0xFF3A1C0F, 0xFFE8A55A}};
    private static final int[][] BLOB = {
        {0xFF7CFFCB, 0xFF9B5DE5, 0xFF00BBF9}, {0xFFFFB86B, 0xFFFF6B9D, 0xFF7B2FF7},
        {0xFF4DD0E1, 0xFF3D5AFE, 0xFF80DEEA}, {0xFFFFD27F, 0xFFFF7B54, 0xFFC2410C}};

    static Bitmap render(int i) {
        int w = 1920, h = 1080;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, w, h, BASE[i][0], BASE[i][1], Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        float[][] pos = {{0.25f, 0.30f, 0.45f}, {0.75f, 0.65f, 0.40f}, {0.55f, 0.15f, 0.35f}};
        for (int k = 0; k < 3; k++) {
            float cx = pos[k][0] * w, cy = pos[k][1] * h, r = pos[k][2] * w;
            int col = BLOB[i][k];
            p.setShader(new RadialGradient(cx, cy, r, (col & 0x00FFFFFF) | 0x99000000, col & 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, r, p);
        }
        return b;
    }
}
