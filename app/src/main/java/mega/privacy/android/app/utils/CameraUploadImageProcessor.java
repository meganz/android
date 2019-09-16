package mega.privacy.android.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import java.io.File;

import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaUtilsAndroid;

import static android.provider.BaseColumns._ID;

public class CameraUploadImageProcessor {

    private static int PREVIEW_SIZE = 1000;

    public static void createImageThumbnail(Context context,MegaApiJava api,String localPath,File dst) {
        LogUtil.logDebug("Create image thumbnail for: " + localPath);
        boolean result = MegaUtilsAndroid.createThumbnail(new File(localPath),dst);
        if (!result) {
            Bitmap thumbnail = getImageThumbnailFromDB(context,localPath);
            if (thumbnail == null) {
                LogUtil.logDebug("Create thumbnail using api");
                api.createThumbnail(localPath,dst.getAbsolutePath());
            } else {
                LogUtil.logDebug("Get from DB is not null");
                nz.mega.sdk.AndroidGfxProcessor.saveBitmap(thumbnail,dst);
            }
        }
    }

    public static void createVideoThumbnail(Context context,MegaApiJava api,String localPath,File dst) {
        LogUtil.logDebug("Create video thumbnail for: " + localPath);
        boolean result = MegaUtilsAndroid.createThumbnail(new File(localPath),dst);
        if (!result) {
            Bitmap thumbnail = getVideoThumbnailFromDB(context,localPath);
            if (thumbnail == null) {
                LogUtil.logDebug("Create thumbnail using api");
                api.createThumbnail(localPath,dst.getAbsolutePath());
            } else {
                LogUtil.logDebug("Get from DB is not null");
                nz.mega.sdk.AndroidGfxProcessor.saveBitmap(thumbnail,dst);
            }
        }
    }

    public static boolean createImagePreview(File img,File preview) {
        return MegaUtilsAndroid.createPreview(img,preview);
    }

    public static boolean createVideoPreview(Context context,File video,File preview) {
        if (!video.exists())
            return false;

        if (preview.exists())
            preview.delete();

        String path = video.getAbsolutePath();
        int orientation = nz.mega.sdk.AndroidGfxProcessor.getExifOrientation(path);
        Rect rect = nz.mega.sdk.AndroidGfxProcessor.getImageDimensions(path,orientation);

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

        Bitmap bitmap = getVideoBitmap(context,path,w,h);
        return nz.mega.sdk.AndroidGfxProcessor.saveBitmap(bitmap,preview);
    }

    public static Bitmap getVideoBitmap(Context context,String path,int w,int h) {
        Bitmap bmThumbnail = null;
        try {
            bmThumbnail = ThumbnailUtils.createVideoThumbnail(path,MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
            if (context != null && bmThumbnail == null) {

                String SELECTION = MediaStore.MediaColumns.DATA + "=?";
                String[] PROJECTION = {BaseColumns._ID};

                Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                String[] selectionArgs = {path};
                ContentResolver cr = context.getContentResolver();
                Cursor cursor = cr.query(uri,PROJECTION,SELECTION,selectionArgs,null);
                if (cursor.moveToFirst()) {
                    long videoId = cursor.getLong(0);
                    bmThumbnail = MediaStore.Video.Thumbnails.getThumbnail(cr,videoId,MediaStore.Video.Thumbnails.FULL_SCREEN_KIND,null);
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
                bmThumbnail = ThumbnailUtils.createVideoThumbnail(path,MediaStore.Video.Thumbnails.MINI_KIND);
                if (context != null && bmThumbnail == null) {

                    String SELECTION = MediaStore.MediaColumns.DATA + "=?";
                    String[] PROJECTION = {BaseColumns._ID};

                    Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    String[] selectionArgs = {path};
                    ContentResolver cr = context.getContentResolver();
                    Cursor cursor = cr.query(uri,PROJECTION,SELECTION,selectionArgs,null);
                    if (cursor.moveToFirst()) {
                        long videoId = cursor.getLong(0);
                        bmThumbnail = MediaStore.Video.Thumbnails.getThumbnail(cr,videoId,MediaStore.Video.Thumbnails.MINI_KIND,null);
                    }
                    cursor.close();
                }
            } catch (Exception e2) {
            }
        }

        try {
            if (bmThumbnail != null) {
                return Bitmap.createScaledBitmap(bmThumbnail,w,h,true);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static Bitmap getVideoThumbnailFromDB(Context context,String localPath) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[] {_ID},
                MediaStore.Video.Media.DATA + "=?",
                new String[] {localPath},
                null);
        long id = 0;
        if (cursor != null && cursor.moveToFirst()) {
            LogUtil.logDebug(localPath + "'s ID is " + id);
            id = cursor.getLong(cursor.getColumnIndex(_ID));
            cursor.close();
            return MediaStore.Video.Thumbnails.getThumbnail(resolver,id,MediaStore.Video.Thumbnails.MICRO_KIND,null);
        }
        return null;
    }

    private static Bitmap getImageThumbnailFromDB(Context context,String localPath) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] {_ID},
                MediaStore.Images.Media.DATA + "=?",
                new String[] {localPath},
                null);
        long id = 0;
        if (cursor != null && cursor.moveToFirst()) {
            LogUtil.logDebug(localPath + "'s ID is " + id);
            id = cursor.getLong(cursor.getColumnIndex(_ID));
            cursor.close();
            return MediaStore.Images.Thumbnails.getThumbnail(resolver,id,MediaStore.Images.Thumbnails.MICRO_KIND,null);
        }
        return null;
    }
}