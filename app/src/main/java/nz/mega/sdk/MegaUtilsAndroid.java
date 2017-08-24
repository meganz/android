package nz.mega.sdk;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class MegaUtilsAndroid {
    private static int THUMBNAIL_SIZE = 120;
    private static int AVATAR_SIZE = 250;
    private static int PREVIEW_SIZE = 1000;

    public static boolean createAvatar(File image, File avatar) {
        if (!image.exists())
            return false;

        if (avatar.exists())
            avatar.delete();

        String path = image.getAbsolutePath();
        int orientation = AndroidGfxProcessor.getExifOrientation(path);

        Rect rect = AndroidGfxProcessor.getImageDimensions(path, orientation);
        if (rect.isEmpty())
            return false;

        int w = rect.right;
        int h = rect.bottom;

        if ((w == 0) || (h == 0))
            return false;
        if (w < h) {
            h = h * AVATAR_SIZE / w;
            w = AVATAR_SIZE;
        } else {
            w = w * AVATAR_SIZE / h;
            h = AVATAR_SIZE;
        }
        if ((w == 0) || (h == 0))
            return false;

        int px = (w - AVATAR_SIZE) / 2;
        int py = (h - AVATAR_SIZE) / 3;

        Bitmap bitmap = AndroidGfxProcessor.getBitmap(path, rect, orientation, w, h);
        bitmap = AndroidGfxProcessor.extractRect(bitmap, px, py, AVATAR_SIZE, AVATAR_SIZE);
        if (bitmap == null)
            return false;

        return AndroidGfxProcessor.saveBitmap(bitmap, avatar);
    }

    public static boolean createThumbnail(File image, File thumbnail) {
        if (!image.exists())
            return false;

        if (thumbnail.exists())
            thumbnail.delete();

        String path = image.getAbsolutePath();
        int orientation = AndroidGfxProcessor.getExifOrientation(path);

        Rect rect = AndroidGfxProcessor.getImageDimensions(path, orientation);
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

        Bitmap bitmap = AndroidGfxProcessor.getBitmap(path, rect, orientation, w, h);
        bitmap = AndroidGfxProcessor.extractRect(bitmap, px, py, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        if (bitmap == null)
            return false;

        return AndroidGfxProcessor.saveBitmap(bitmap, thumbnail);
    }

    public static boolean createPreview(File image, File preview) {
        if (!image.exists())
            return false;

        if (preview.exists())
            preview.delete();

        String path = image.getAbsolutePath();
        int orientation = AndroidGfxProcessor.getExifOrientation(path);
        Rect rect = AndroidGfxProcessor.getImageDimensions(path, orientation);

        int w = rect.right;
        int h = rect.bottom;

        if ((w == 0) || (h == 0))
            return false;
        
        if (w >= PREVIEW_SIZE || h >= PREVIEW_SIZE) {
            if (h > w) {
                w = w * PREVIEW_SIZE / h;
                h = PREVIEW_SIZE;
            } else {
                h = h * PREVIEW_SIZE / w;
                w = PREVIEW_SIZE;
            }
        }
        
        if ((w == 0) || (h == 0))
            return false;

        Bitmap bitmap = AndroidGfxProcessor.getBitmap(path, rect, orientation, w, h);
        return AndroidGfxProcessor.saveBitmap(bitmap, preview);
    }
}
