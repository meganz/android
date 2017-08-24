package nz.mega.sdk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;

public class AndroidGfxProcessor extends MegaGfxProcessor {
    Rect size;
    int orientation;
    String srcPath;
    Bitmap bitmap;
    byte[] bitmapData;

    protected AndroidGfxProcessor() {
    }

    public static Rect getImageDimensions(String path, int orientation) {
        Rect rect = new Rect();
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(path), null, options);

            if ((options.outWidth > 0) && (options.outHeight > 0)) {
                if ((orientation < 5) || (orientation > 8)) {
                    rect.right = options.outWidth;
                    rect.bottom = options.outHeight;
                } else {
                    rect.bottom = options.outWidth;
                    rect.right = options.outHeight;
                }
            }
        } catch (Exception e) {
        }
        return rect;
    }

    public boolean readBitmap(String path) {
        srcPath = path;
        orientation = getExifOrientation(path);
        size = getImageDimensions(srcPath, orientation);
        return (size.right != 0) && (size.bottom != 0);
    }

    public int getWidth() {
        return size.right;
    }

    public int getHeight() {
        return size.bottom;
    }

    static public Bitmap getBitmap(String path, Rect rect, int orientation, int w, int h) {
        int width;
        int height;

        if ((orientation < 5) || (orientation > 8)) {
            width = rect.right;
            height = rect.bottom;
        } else {
            width = rect.bottom;
            height = rect.right;
        }

        try {
            int scale = 1;
            while (width / scale / 2 >= w && height / scale / 2 >= h)
                scale *= 2;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;
            Bitmap tmp = BitmapFactory.decodeStream(new FileInputStream(path), null, options);
            tmp = fixExifOrientation(tmp, orientation);
            return Bitmap.createScaledBitmap(tmp, w, h, true);
        } catch (Exception e) {
        }
        return null;
    }

    public static int getExifOrientation(String srcPath) {
        int orientation = ExifInterface.ORIENTATION_UNDEFINED;

        int i = 0;
        while ((i < 5) && (orientation == ExifInterface.ORIENTATION_UNDEFINED)) {
            try {
                ExifInterface exif = new ExifInterface(srcPath);
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, orientation);
            } catch (IOException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {}
            }

            i++;
        }
        return orientation;
    }

    /*
     * Change image orientation based on EXIF image data
     */
    public static Bitmap fixExifOrientation(Bitmap bitmap, int orientation) {
        if (bitmap == null)
            return null;

        if ((orientation < 2) || (orientation > 8)) {
            // No changes required or invalid orientation
            return bitmap;
        }

        Matrix matrix = new Matrix();
        switch (orientation) {
        case ExifInterface.ORIENTATION_TRANSPOSE:
        case ExifInterface.ORIENTATION_ROTATE_90:
            matrix.postRotate(90);
            break;
        case ExifInterface.ORIENTATION_ROTATE_180:
        case ExifInterface.ORIENTATION_FLIP_VERTICAL:
            matrix.postRotate(180);
            break;
        case ExifInterface.ORIENTATION_TRANSVERSE:
        case ExifInterface.ORIENTATION_ROTATE_270:
            matrix.postRotate(270);
            break;
        default:
            break;
        }

        if ((orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL)
                || (orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL))
            matrix.preScale(-1, 1);
        else if ((orientation == ExifInterface.ORIENTATION_TRANSPOSE)
                || (orientation == ExifInterface.ORIENTATION_TRANSVERSE))
            matrix.preScale(1, -1);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap extractRect(Bitmap bitmap, int px, int py, int rw, int rh) {
        if (bitmap == null)
            return null;

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if ((px != 0) || (py != 0) || (rw != w) || (rh != h)) {
            Bitmap.Config conf = Bitmap.Config.ARGB_8888;
            Bitmap scaled = Bitmap.createBitmap(rw, rh, conf);
            Canvas canvas = new Canvas(scaled);
            canvas.drawBitmap(bitmap, new Rect(px, py, px + rw, py + rh), new Rect(0, 0, rw, rh), null);
            bitmap = scaled;
        }
        return bitmap;
    }

    public static boolean saveBitmap(Bitmap bitmap, File file) {
        if (bitmap == null)
            return false;

        FileOutputStream stream;
        try {
            stream = new FileOutputStream(file);
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream))
                return false;

            stream.close();
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public int getBitmapDataSize(int w, int h, int px, int py, int rw, int rh) {
        if (bitmap == null)
            bitmap = getBitmap(srcPath, size, orientation, w, h);
        else
            bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);

        bitmap = extractRect(bitmap, px, py, rw, rh);
        if (bitmap == null)
            return 0;

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream))
                return 0;

            bitmapData = stream.toByteArray();
            return bitmapData.length;
        } catch (Exception e) {
        }
        return 0;
    }

    public boolean getBitmapData(byte[] buffer) {
        try {
            System.arraycopy(bitmapData, 0, buffer, 0, bitmapData.length);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public void freeBitmap() {
        bitmap = null;
        bitmapData = null;
        size = null;
        srcPath = null;
        orientation = 0;
    }
}
