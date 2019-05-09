package mega.privacy.android.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import java.io.FileInputStream;

import nz.mega.sdk.AndroidGfxProcessor;

public class AndroidAppGfxProcessor extends AndroidGfxProcessor {
    static public Bitmap getBitmap(String path, Context context, Rect rect, int orientation, int w, int h) {
        int width;
        int height;

        if (isVideoFile(path)) {
            Bitmap bmThumbnail = null;
            try {
                bmThumbnail = android.media.ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
                if (context != null && bmThumbnail == null) {

                    String SELECTION = MediaStore.MediaColumns.DATA + "=?";
                    String[] PROJECTION = {BaseColumns._ID};

                    Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    String[] selectionArgs = {path};
                    ContentResolver cr = context.getContentResolver();
                    Cursor cursor = cr.query(uri, PROJECTION, SELECTION, selectionArgs, null);
                    if (cursor.moveToFirst()) {
                        long videoId = cursor.getLong(0);
                        bmThumbnail = MediaStore.Video.Thumbnails.getThumbnail(cr, videoId, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND, null);
                    }
                    cursor.close();
                }
            } catch (Exception e) {
            }

            if (bmThumbnail == null) {

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(path);
                    bmThumbnail = retriever.getFrameAtTime();
                } catch (Exception e1) {
                } finally {
                    try {
                        retriever.release();
                    } catch (Exception ex) {
                    }
                }
            }

            if (bmThumbnail == null) {
                try {
                    bmThumbnail = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND);
                    if (context != null && bmThumbnail == null) {

                        String SELECTION = MediaStore.MediaColumns.DATA + "=?";
                        String[] PROJECTION = {BaseColumns._ID};

                        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        String[] selectionArgs = {path};
                        ContentResolver cr = context.getContentResolver();
                        Cursor cursor = cr.query(uri, PROJECTION, SELECTION, selectionArgs, null);
                        if (cursor.moveToFirst()) {
                            long videoId = cursor.getLong(0);
                            bmThumbnail = MediaStore.Video.Thumbnails.getThumbnail(cr, videoId, MediaStore.Video.Thumbnails.MINI_KIND, null);
                        }
                        cursor.close();
                    }
                } catch (Exception e2) {
                }
            }

            try {
                if (bmThumbnail != null) {
                    return Bitmap.createScaledBitmap(bmThumbnail, w, h, true);
                }
            } catch (Exception e) {
            }
        } else {
            if ((orientation < 5) || (orientation > 8)) {
                width = rect.right;
                height = rect.bottom;
            } else {
                width = rect.bottom;
                height = rect.right;
            }

            try {
                int scale = 1;
                while (width / scale / 2 >= w && height / scale / 2 >= h) {
                    scale *= 2;
                }

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                options.inSampleSize = scale;
                Bitmap tmp = BitmapFactory.decodeStream(new FileInputStream(path), null, options);
                tmp = fixExifOrientation(tmp, orientation);
                return Bitmap.createScaledBitmap(tmp, w, h, true);
            } catch (Exception e) {
            }
        }

        return null;
    }
}
