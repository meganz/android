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

import androidx.annotation.Nullable;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;

import java.io.File;
import java.util.concurrent.Executors;

import nz.mega.sdk.AndroidGfxProcessor;

public class ImageProcessor {

    private static final int PREVIEW_SIZE = 1000;

    private static final int THUMBNAIL_SIZE = 200;

    /**
     * Create a thumbnail file based on a local image file and store it to target location.
     * The process is load the file as Bitmap in memory then resize and crop it, finally store it.
     * The thumbnail size should be {@link #THUMBNAIL_SIZE} * {@link #THUMBNAIL_SIZE}.
     *
     * @param origin The local image file.
     * @param thumbnail Target location where the created thumbnail will be stored.
     */
    public static void createThumbnail(File origin, File thumbnail) {
        if (!origin.exists()) return;
        if (thumbnail.exists()) thumbnail.delete();

        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        ImageRequest imageRequest = ImageRequest.fromFile(origin);

        DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, null);

        dataSource.subscribe(new BaseBitmapDataSubscriber() {

            @Override
            public void onNewResultImpl(@Nullable Bitmap bitmap) {
                if(bitmap != null) {
                    String path = origin.getAbsolutePath();

                    int orientation = AndroidGfxProcessor.getExifOrientation(path);
                    Rect rect = AndroidGfxProcessor.getImageDimensions(path, orientation);
                    if (rect.isEmpty()) return;

                    int w = rect.right;
                    int h = rect.bottom;

                    if ((w == 0) || (h == 0)) return;

                    if (w < h) {
                        h = h * THUMBNAIL_SIZE / w;
                        w = THUMBNAIL_SIZE;
                    } else {
                        w = w * THUMBNAIL_SIZE / h;
                        h = THUMBNAIL_SIZE;
                    }

                    if ((w == 0) || (h == 0)) return;

                    int px = (w - THUMBNAIL_SIZE) / 2;
                    int py = (h - THUMBNAIL_SIZE) / 3;

                    bitmap =  Bitmap.createScaledBitmap(bitmap, w, h, true);
                    bitmap = AndroidGfxProcessor.extractRect(bitmap, px, py, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
                    AndroidGfxProcessor.saveBitmap(bitmap, thumbnail);
                }
            }

            @Override
            public void onFailureImpl(DataSource dataSource) {
                // No cleanup required here.
            }
        }, CallerThreadExecutor.getInstance());

    }

    /**
     * Create a preview file based on a local image file and store it to target location.
     * The process is load the file as Bitmap in memory then resize it, finally store it.
     * The preview's longest side size should be {@link #PREVIEW_SIZE}.
     *
     * @param origin The local image file.
     * @param preview Target location where the created preview will be stored.
     */
    public static void createImagePreview(File origin, File preview) {
        int[] wh = calculatePreviewWidthAndHeight(origin);
        int w = wh[0];
        int h = wh[1];

        if (w == 0 || h == 0) return;

        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        ImageRequest imageRequest = ImageRequest.fromFile(origin);

        DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, null);

        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            public void onNewResultImpl(@Nullable Bitmap bitmap) {
                if(bitmap != null) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
                    AndroidGfxProcessor.saveBitmap(bitmap, preview);
                }
            }

            @Override
            public void onFailureImpl(DataSource dataSource) {
                // No cleanup required here.
            }
        }, CallerThreadExecutor.getInstance());
    }

    /**
     * Calculate the preview's width and height, base on its orientation.
     * The longest side should be {@link #PREVIEW_SIZE}.
     *
     * @param origin The local image file.
     * @return Preview's width and height, stored in a int array.
     *         The first element is witdth, the second one is height.
     */
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
                return Bitmap.createScaledBitmap(bmThumbnail, w, h, true);
            }
        } catch (Exception e) {
        }
        return null;
    }
}