package mega.privacy.android.app.utils;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import nz.mega.sdk.AndroidGfxProcessor;
import nz.mega.sdk.MegaUtilsAndroid;

public class MegaUtilsAndroidApp extends MegaUtilsAndroid {
    private static int THUMBNAIL_SIZE = 200;

    public static boolean createThumbnail(File image, Context context, File thumbnail) {
        if (!image.exists())
            return false;

        if (thumbnail.exists())
            thumbnail.delete();

        String path = image.getAbsolutePath();
        int orientation = AndroidGfxProcessor.getExifOrientation(path);

        Rect rect = nz.mega.sdk.AndroidGfxProcessor.getImageDimensions(path, orientation);
        if (rect.isEmpty())
            return false;

        int w = rect.right;
        int h = rect.bottom;

        if ((w == 0) || (h == 0))
            return false;
        if (w < h) {
            h = h * THUMBNAIL_SIZE / w;
            w = THUMBNAIL_SIZE;
        } else {
            w = w * THUMBNAIL_SIZE / h;
            h = THUMBNAIL_SIZE;
        }
        if ((w == 0) || (h == 0))
            return false;

        int px = (w - THUMBNAIL_SIZE) / 2;
        int py = (h - THUMBNAIL_SIZE) / 3;

        Bitmap bitmap = AndroidAppGfxProcessor.getBitmap(path, context, rect, orientation, w, h);
        bitmap = nz.mega.sdk.AndroidGfxProcessor.extractRect(bitmap, px, py, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        if (bitmap == null)
            return false;

        return AndroidGfxProcessor.saveBitmap(bitmap, thumbnail);
    }
}
