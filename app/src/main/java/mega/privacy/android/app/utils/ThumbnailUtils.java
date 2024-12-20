package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.core.content.ContextCompat;

import java.io.File;

import mega.privacy.android.app.R;
import mega.privacy.android.app.ThumbnailCache;
import mega.privacy.android.app.main.providers.MegaProviderAdapter;
import mega.privacy.android.app.main.providers.MegaProviderAdapter.ViewHolderProvider;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import timber.log.Timber;

/*
 * Service to create thumbnails
 */
public class ThumbnailUtils {
    public static File thumbDir;
    public static ThumbnailCache thumbnailCache = new ThumbnailCache();
    public static Boolean isDeviceMemoryLow = false;

    public static Bitmap getRoundedRectBitmap(Context context, final Bitmap bitmap, final int pixels) {
        Timber.d("getRoundedRectBitmap");
        final Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(result);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        final float densityMultiplier = context.getResources().getDisplayMetrics().density;
        final float roundPx = pixels * densityMultiplier;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(ContextCompat.getColor(context, R.color.white));
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        canvas.drawRect(0, bitmap.getHeight() / 2, bitmap.getWidth() / 2, bitmap.getHeight(), paint);
        canvas.drawRect(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth(), bitmap.getHeight(), paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return result;
    }

    public static Bitmap getRoundedBitmap(Context context, final Bitmap bitmap, final int pixels) {
        Timber.d("getRoundedRectBitmap");
        final Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(result);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        final float densityMultiplier = context.getResources().getDisplayMetrics().density;
        final float roundPx = pixels * densityMultiplier;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(ContextCompat.getColor(context, R.color.white));
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return result;
    }

    public static Path getRoundedRect(float left, float top, float right, float bottom, float rx, float ry,
                                      boolean tl, boolean tr, boolean br, boolean bl) {
        Path path = new Path();
        if (rx < 0) rx = 0;
        if (ry < 0) ry = 0;
        float width = right - left;
        float height = bottom - top;
        if (rx > width / 2) rx = width / 2;
        if (ry > height / 2) ry = height / 2;
        float widthMinusCorners = (width - (2 * rx));
        float heightMinusCorners = (height - (2 * ry));

        path.moveTo(right, top + ry);
        if (tr)
            path.rQuadTo(0, -ry, -rx, -ry);//top-right corner
        else {
            path.rLineTo(0, -ry);
            path.rLineTo(-rx, 0);
        }
        path.rLineTo(-widthMinusCorners, 0);
        if (tl)
            path.rQuadTo(-rx, 0, -rx, ry); //top-left corner
        else {
            path.rLineTo(-rx, 0);
            path.rLineTo(0, ry);
        }
        path.rLineTo(0, heightMinusCorners);

        if (bl)
            path.rQuadTo(0, ry, rx, ry);//bottom-left corner
        else {
            path.rLineTo(0, ry);
            path.rLineTo(rx, 0);
        }

        path.rLineTo(widthMinusCorners, 0);
        if (br)
            path.rQuadTo(rx, 0, rx, -ry); //bottom-right corner
        else {
            path.rLineTo(rx, 0);
            path.rLineTo(0, -ry);
        }

        path.rLineTo(0, -heightMinusCorners);

        path.close();//Given close, last lineto can be removed.

        return path;
    }

    static class ThumbnailDownloadListenerProvider implements MegaRequestListenerInterface {
        Context context;
        ViewHolderProvider holder;
        MegaProviderAdapter adapter;

        ThumbnailDownloadListenerProvider(Context context, ViewHolderProvider holder, MegaProviderAdapter adapter) {
            this.context = context;
            this.holder = holder;
            this.adapter = adapter;
        }

        @Override
        public void onRequestStart(MegaApiJava api, MegaRequest request) {


        }

        @Override
        public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

            final long handle = request.getNodeHandle();
            Timber.d("Downloading thumbnail finished");
            String handleBase64 = MegaApiJava.handleToBase64(handle);
            if (e.getErrorCode() == MegaError.API_OK) {
                Timber.d("Downloading thumbnail OK: %s", handle);
                thumbnailCache.remove(handle);

                if (holder != null) {
                    File thumbDir = getThumbFolder(context);
                    File thumb = new File(thumbDir, handleBase64 + ".jpg");
                    if (thumb.exists()) {
                        if (thumb.length() > 0) {
                            final Bitmap bitmap = getBitmapForCache(thumb, context);
                            if (bitmap != null) {
                                thumbnailCache.put(handle, bitmap);
                                if ((holder.document == handle)) {
                                    holder.imageView.setImageBitmap(bitmap);
                                    Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                                    holder.imageView.startAnimation(fadeInAnimation);
                                    adapter.notifyItemChanged(holder.getAbsoluteAdapterPosition());
                                    Timber.d("Thumbnail update");
                                }
                            }
                        }
                    }
                }
            } else {
                Timber.e("ERROR: %d___%s", e.getErrorCode(), e.getErrorString());
            }
        }

        @Override
        public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {


        }

        @Override
        public void onRequestUpdate(MegaApiJava api, MegaRequest request) {


        }
    }

    /*
     * Get thumbnail folder
     */
    public static File getThumbFolder(Context context) {
        if (!isFileAvailable(thumbDir)) {
            thumbDir = CacheFolderManager.getCacheFolder(CacheFolderManager.THUMBNAIL_FOLDER);
        }
        Timber.d("getThumbFolder(): thumbDir= %s", thumbDir);
        return thumbDir;
    }

    public static Bitmap getThumbnailFromCache(MegaNode node) {
        return thumbnailCache.get(node.getHandle());
    }

    public static Bitmap getThumbnailFromFolder(MegaNode node, Context context) {
        File thumbDir = getThumbFolder(context);
        if (node != null) {
            File thumb = new File(thumbDir, node.getBase64Handle() + ".jpg");
            Bitmap bitmap;
            if (thumb.exists() && thumb.length() > 0) {
                bitmap = getBitmapForCache(thumb, context);
                if (bitmap == null) {
                    thumb.delete();
                } else {
                    thumbnailCache.put(node.getHandle(), bitmap);
                }
            }
            return thumbnailCache.get(node.getHandle());
        }
        return null;
    }

    public static Bitmap getThumbnailFromMegaProvider(MegaNode document, Context context, ViewHolderProvider viewHolder, MegaApiAndroid megaApi, MegaProviderAdapter adapter) {
        if (!Util.isOnline(context)) {
            return thumbnailCache.get(document.getHandle());
        }

        ThumbnailDownloadListenerProvider listener = new ThumbnailDownloadListenerProvider(context, viewHolder, adapter);
        File thumbFile = new File(getThumbFolder(context), document.getBase64Handle() + ".jpg");
        megaApi.getThumbnail(document, thumbFile.getAbsolutePath(), listener);

        return thumbnailCache.get(document.getHandle());

    }

    /*
     * Load Bitmap for cache
     */
    private static Bitmap getBitmapForCache(File bmpFile, Context context) {
        BitmapFactory.Options bOpts = new BitmapFactory.Options();
        bOpts.inPurgeable = true;
        bOpts.inInputShareable = true;
        return BitmapFactory.decodeFile(bmpFile.getAbsolutePath(), bOpts);
    }
}
