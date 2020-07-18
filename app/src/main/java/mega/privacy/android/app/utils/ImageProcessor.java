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

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.concurrent.ExecutionException;

import nz.mega.sdk.AndroidGfxProcessor;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaUtilsAndroid;

import static mega.privacy.android.app.utils.LogUtil.*;

public class ImageProcessor {

    private static final int PREVIEW_SIZE = 1000;

    private static final int THUMBNAIL_SIZE = 200;

    public static void createImageThumbnail(MegaApiJava api,String localPath,File dst) {
        logDebug("Create image thumbnail for: " + localPath);
        boolean result = MegaUtilsAndroid.createThumbnail(new File(localPath),dst);
        if (!result) {
            processThumbnail(null, api, localPath, dst);
        }
    }

    public static void createVideoThumbnail(MegaApiJava api,String localPath,File dst) {
        logDebug("Create video thumbnail for: " + localPath);
        boolean result = MegaUtilsAndroid.createThumbnail(new File(localPath),dst);
        if (!result) {
            processThumbnail(null, api, localPath, dst);
        }
    }

    private static void processThumbnail(Bitmap thumbnail,MegaApiJava api,String localPath,File dst) {
        if (thumbnail == null) {
            logDebug("create thumbnail use api");
            api.createThumbnail(localPath,dst.getAbsolutePath());
        } else {
            logDebug("get from db is not null");
            nz.mega.sdk.AndroidGfxProcessor.saveBitmap(thumbnail,dst);
        }
    }

    public static boolean createImageThumbnail(Context context, File origin ,File thumbnail) {
        if (!origin.exists()) return false;
        if (thumbnail.exists()) thumbnail.delete();

        try {
            Bitmap bitmap = Glide.with(context).asBitmap().load(origin).centerCrop().submit(THUMBNAIL_SIZE, THUMBNAIL_SIZE).get();
            return AndroidGfxProcessor.saveBitmap(bitmap, thumbnail);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            logError("Create image thumbnail failed.", e);
        }
        return false;
    }

    public static boolean createImagePreview(Context context, File img,File preview) {
        int [] wh = calculatePreviewWidthAndHeight(img);
        int w = wh[0];
        int h = wh[1];

        if ((w == 0) || (h == 0)) return false;
        try {
            Bitmap bitmap = Glide.with(context).asBitmap().load(img).submit(w, h).get();
            return AndroidGfxProcessor.saveBitmap(bitmap, preview);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            logError("Create image prview failed.", e);
        }
        return false;
    }

    private static int[] calculatePreviewWidthAndHeight(File origin) {
        int[] wh = new int[2];
        String path = origin.getAbsolutePath();
        int orientation = AndroidGfxProcessor.getExifOrientation(path);
        Rect rect = AndroidGfxProcessor.getImageDimensions(path, orientation);

        int w = rect.right;
        int h = rect.bottom;

        if (w >= PREVIEW_SIZE || h >= PREVIEW_SIZE) {
            if (h > w) {
                w = w * PREVIEW_SIZE / h;
                h = PREVIEW_SIZE;
            } else {
                h = h * PREVIEW_SIZE / w;
                w = PREVIEW_SIZE;
            }
        }
        wh[0] = w;
        wh[1] = h;
        return wh;
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

    private static Bitmap getVideoBitmap(Context context,String path,int w,int h) {
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
}