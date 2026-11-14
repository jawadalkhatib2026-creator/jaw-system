package dev.sultan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

final class Images {
    /** Reads an image from a content/file Uri, shrinks it to about maxW pixels wide and saves it into dest. */
    static boolean save(Context c, Uri src, File dest, int maxW, boolean png) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = c.getContentResolver().openInputStream(src)) {
                BitmapFactory.decodeStream(in, null, o);
            }
            int s = 1;
            while (o.outWidth / (s * 2) >= maxW) s *= 2;
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = s;
            Bitmap bmp;
            try (InputStream in = c.getContentResolver().openInputStream(src)) {
                bmp = BitmapFactory.decodeStream(in, null, d);
            }
            if (bmp == null) return false;
            try (FileOutputStream out = new FileOutputStream(dest)) {
                bmp.compress(png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 90, out);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
