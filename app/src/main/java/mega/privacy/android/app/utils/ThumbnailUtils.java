package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.Constants.THUMB_CORNER_RADIUS_DP;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.Util.dp2px;
import static nz.mega.sdk.MegaUtilsAndroid.createThumbnail;

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
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ThumbnailCache;
import mega.privacy.android.app.main.adapters.MegaNodeAdapter;
import mega.privacy.android.app.main.adapters.VersionsFileAdapter;
import mega.privacy.android.app.main.megachat.chatAdapters.NodeAttachmentHistoryAdapter;
import mega.privacy.android.app.main.providers.MegaProviderAdapter;
import mega.privacy.android.app.main.providers.MegaProviderAdapter.ViewHolderProvider;
import mega.privacy.android.app.presentation.recentactions.recentactionbucket.RecentActionBucketAdapter;
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
    public static ThumbnailCache thumbnailCachePath = new ThumbnailCache(1);
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

    static class ThumbnailDownloadListenerListBrowser implements MegaRequestListenerInterface {
        Context context;
        RecyclerView.ViewHolder holder;
        RecyclerView.Adapter adapter;

        ThumbnailDownloadListenerListBrowser(Context context, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter) {
            this.context = context;
            this.holder = holder;
            this.adapter = adapter;
        }

        @Override
        public void onRequestStart(MegaApiJava api, MegaRequest request) {


        }

        @Override
        public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

            Timber.d("Downloading thumbnail finished");
            final long handle = request.getNodeHandle();
            String base64 = MegaApiJava.handleToBase64(handle);

            if (e.getErrorCode() == MegaError.API_OK) {
                Timber.d("Downloading thumbnail OK: %s", handle);
                thumbnailCache.remove(handle);

                if (holder == null) return;

                File thumbDir = getThumbFolder(context);
                File thumb = new File(thumbDir, base64 + ".jpg");

                if (!thumb.exists() || thumb.length() <= 0) return;

                final Bitmap bitmap = getBitmapForCache(thumb, context);
                if (bitmap == null) return;

                thumbnailCache.put(handle, bitmap);

                Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);

                if (holder instanceof MegaNodeAdapter.ViewHolderBrowserList) {
                    if ((((MegaNodeAdapter.ViewHolderBrowserList) holder).document == handle)) {
                        ((MegaNodeAdapter.ViewHolderBrowserList) holder).imageView.setImageBitmap(
                                getRoundedBitmap(context, bitmap, dp2px(THUMB_CORNER_RADIUS_DP)));
                        ((MegaNodeAdapter.ViewHolderBrowserList) holder).imageView.startAnimation(fadeInAnimation);
                    }
                } else if (holder instanceof VersionsFileAdapter.ViewHolderVersion) {
                    if ((((VersionsFileAdapter.ViewHolderVersion) holder).document == handle)) {
                        ((VersionsFileAdapter.ViewHolderVersion) holder).imageView.setImageBitmap(
                                getRoundedBitmap(context, bitmap, dp2px(THUMB_CORNER_RADIUS_DP)));
                        ((VersionsFileAdapter.ViewHolderVersion) holder).imageView.startAnimation(fadeInAnimation);
                    }
                } else if (holder instanceof NodeAttachmentHistoryAdapter.ViewHolderBrowserList) {
                    if ((((NodeAttachmentHistoryAdapter.ViewHolderBrowserList) holder).document == handle)) {
                        ((NodeAttachmentHistoryAdapter.ViewHolderBrowserList) holder).imageView.setImageBitmap(
                                getRoundedBitmap(context, bitmap, dp2px(THUMB_CORNER_RADIUS_DP)));
                        ((NodeAttachmentHistoryAdapter.ViewHolderBrowserList) holder).imageView.startAnimation(fadeInAnimation);
                    }
                } else if (holder instanceof RecentActionBucketAdapter.ViewHolderMultipleBucket) {
                    RecentActionBucketAdapter.ViewHolderMultipleBucket viewHolderMultipleBucket = (RecentActionBucketAdapter.ViewHolderMultipleBucket) holder;
                    if (viewHolderMultipleBucket.getDocument() == handle) {
                        viewHolderMultipleBucket.setImageThumbnail(bitmap);
                        if (((RecentActionBucketAdapter) adapter).isMedia()) {
                            viewHolderMultipleBucket.getThumbnailMedia().startAnimation(fadeInAnimation);
                        } else {
                            viewHolderMultipleBucket.getThumbnailList().startAnimation(fadeInAnimation);
                        }
                    }
                }

                adapter.notifyItemChanged(holder.getAbsoluteAdapterPosition());
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

    static class ThumbnailDownloadListenerGridBrowser implements MegaRequestListenerInterface {
        Context context;
        RecyclerView.ViewHolder holder;
        RecyclerView.Adapter adapter;

        ThumbnailDownloadListenerGridBrowser(Context context, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter) {
            this.context = context;
            this.holder = holder;
            this.adapter = adapter;
        }

        @Override
        public void onRequestStart(MegaApiJava api, MegaRequest request) {


        }

        @Override
        public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

            Timber.d("Downloading thumbnail finished");
            final long handle = request.getNodeHandle();
            if (e.getErrorCode() == MegaError.API_OK) {
                Timber.d("Downloading thumbnail OK: %s", handle);
                thumbnailCache.remove(handle);

                if (holder != null) {
                    File thumbDir = getThumbFolder(context);
                    File thumb = new File(thumbDir, MegaApiJava.handleToBase64(handle) + ".jpg");
                    if (thumb.exists()) {
                        if (thumb.length() > 0) {
                            final Bitmap bitmap = getBitmapForCache(thumb, context);
                            if (bitmap != null) {
                                thumbnailCache.put(handle, bitmap);
                                if (holder instanceof MegaNodeAdapter.ViewHolderBrowserGrid) {
                                    if ((((MegaNodeAdapter.ViewHolderBrowserGrid) holder).document == handle)) {
                                        ((MegaNodeAdapter.ViewHolderBrowserGrid) holder).imageViewThumb.setVisibility(View.VISIBLE);
                                        ((MegaNodeAdapter.ViewHolderBrowserGrid) holder).imageViewIcon.setVisibility(View.GONE);
                                        ((MegaNodeAdapter.ViewHolderBrowserGrid) holder).imageViewThumb.setImageBitmap(bitmap);
                                        ((MegaNodeAdapter.ViewHolderBrowserGrid) holder).thumbLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));
                                        Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                                        ((MegaNodeAdapter.ViewHolderBrowserGrid) holder).imageViewThumb.startAnimation(fadeInAnimation);
                                        adapter.notifyItemChanged(holder.getAbsoluteAdapterPosition());
                                        Timber.d("Thumbnail update");
                                    }
                                } else if (holder instanceof NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) {
                                    if ((((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) holder).document == handle)) {
                                        ((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) holder).imageViewThumb.setVisibility(View.VISIBLE);
                                        ((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) holder).imageViewIcon.setVisibility(View.GONE);
                                        ((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) holder).imageViewThumb.setImageBitmap(bitmap);
                                        ((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) holder).thumbLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));
                                        Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                                        ((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) holder).imageViewThumb.startAnimation(fadeInAnimation);
                                        adapter.notifyItemChanged(holder.getAbsoluteAdapterPosition());
                                        Timber.d("Thumbnail update");
                                    }
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

    public static Bitmap getThumbnailFromCache(long handle) {
        return thumbnailCache.get(handle);
    }

    public static Bitmap getThumbnailFromCache(String path) {
        return thumbnailCachePath.get(path);
    }

    public static void setThumbnailCache(long handle, Bitmap bitmap) {
        thumbnailCache.put(handle, bitmap);
    }

    public static void setThumbnailCache(String path, Bitmap bitmap) {
        thumbnailCachePath.put(path, bitmap);
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

    public static Bitmap getThumbnailFromMegaList(MegaNode document, Context context, RecyclerView.ViewHolder viewHolder, MegaApiAndroid megaApi, RecyclerView.Adapter adapter) {

        if (!Util.isOnline(context)) {
            return thumbnailCache.get(document.getHandle());
        }
        ThumbnailDownloadListenerListBrowser listener = new ThumbnailDownloadListenerListBrowser(context, viewHolder, adapter);
        File thumbFile = new File(getThumbFolder(context), document.getBase64Handle() + ".jpg");

        megaApi.getThumbnail(document, thumbFile.getAbsolutePath(), listener);

        return thumbnailCache.get(document.getHandle());

    }

    public static Bitmap getThumbnailFromMegaGrid(MegaNode document, Context context, RecyclerView.ViewHolder viewHolder, MegaApiAndroid megaApi, RecyclerView.Adapter adapter) {
        if (!Util.isOnline(context)) {
            return thumbnailCache.get(document.getHandle());
        }

        ThumbnailDownloadListenerGridBrowser listener = new ThumbnailDownloadListenerGridBrowser(context, viewHolder, adapter);
        File thumbFile = new File(getThumbFolder(context), document.getBase64Handle() + ".jpg");
        Timber.d("Will download here: %s", thumbFile.getAbsolutePath());
        megaApi.getThumbnail(document, thumbFile.getAbsolutePath(), listener);

        return thumbnailCache.get(document.getHandle());

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

    public static class ResizerParams {
        File file;
        MegaNode document;
    }

    static class AttachThumbnailTaskList extends AsyncTask<ResizerParams, Void, Boolean> {
        Context context;
        MegaApiAndroid megaApi;
        File thumbFile;
        ResizerParams param;
        RecyclerView.ViewHolder holder;
        RecyclerView.Adapter adapter;

        AttachThumbnailTaskList(Context context, MegaApiAndroid megaApi, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter) {
            this.context = context;
            this.megaApi = megaApi;
            this.holder = holder;
            this.adapter = adapter;
            this.thumbFile = null;
            this.param = null;
        }


        @Override
        protected Boolean doInBackground(ResizerParams... params) {
            Timber.d("AttachThumbnailTaskList");
            param = params[0];

            File thumbDir = getThumbFolder(context);
            thumbFile = new File(thumbDir, param.document.getBase64Handle() + ".jpg");

            return createThumbnail(param.file, thumbFile);
        }

        @Override
        protected void onPostExecute(Boolean shouldContinueObject) {

            if (!shouldContinueObject) return;

            if (holder instanceof MegaNodeAdapter.ViewHolderBrowserList) {
                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) ((MegaNodeAdapter.ViewHolderBrowserList) holder).imageView.getLayoutParams();
                params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.setMargins(18, 0, 12, 0);
                ((MegaNodeAdapter.ViewHolderBrowserList) holder).imageView.setLayoutParams(params1);

                onThumbnailGeneratedList(context, megaApi, thumbFile, param.document, holder, adapter);
            } else if (holder instanceof VersionsFileAdapter.ViewHolderVersion) {
                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) ((VersionsFileAdapter.ViewHolderVersion) holder).imageView.getLayoutParams();
                params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.setMargins(18, 0, 12, 0);
                ((VersionsFileAdapter.ViewHolderVersion) holder).imageView.setLayoutParams(params1);

                onThumbnailGeneratedList(context, megaApi, thumbFile, param.document, holder, adapter);
            } else if (holder instanceof RecentActionBucketAdapter.ViewHolderMultipleBucket) {
                onThumbnailGeneratedList(context, megaApi, thumbFile, param.document, holder, adapter);
            }
        }
    }

    static class AttachThumbnailTaskGrid extends AsyncTask<ResizerParams, Void, Boolean> {
        Context context;
        MegaApiAndroid megaApi;
        File thumbFile;
        ResizerParams param;
        RecyclerView.ViewHolder holder;
        RecyclerView.Adapter adapter;

        AttachThumbnailTaskGrid(Context context, MegaApiAndroid megaApi, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter) {
            this.context = context;
            this.megaApi = megaApi;
            this.holder = holder;
            this.adapter = adapter;
            this.thumbFile = null;
            this.param = null;
        }

        @Override
        protected Boolean doInBackground(ResizerParams... params) {
            Timber.d("AttachThumbnailTaskGrid");
            param = params[0];

            File thumbDir = getThumbFolder(context);
            thumbFile = new File(thumbDir, param.document.getBase64Handle() + ".jpg");

            return createThumbnail(param.file, thumbFile);
        }

        @Override
        protected void onPostExecute(Boolean shouldContinueObject) {
            if (shouldContinueObject) {

                onThumbnailGeneratedGrid(context, thumbFile, param.document, holder, adapter);
            }
        }
    }

    private static void onThumbnailGeneratedList(Context context, MegaApiAndroid megaApi, File thumbFile, MegaNode document, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter) {
        Timber.d("onThumbnailGeneratedList");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);

        if (holder instanceof MegaNodeAdapter.ViewHolderBrowserList) {
            ((MegaNodeAdapter.ViewHolderBrowserList) holder).imageView.setImageBitmap(bitmap);
        } else if (holder instanceof VersionsFileAdapter.ViewHolderVersion) {
            ((VersionsFileAdapter.ViewHolderVersion) holder).imageView.setImageBitmap(bitmap);
        } else if (holder instanceof RecentActionBucketAdapter.ViewHolderMultipleBucket) {
            ((RecentActionBucketAdapter.ViewHolderMultipleBucket) holder).setImageThumbnail(bitmap);
        }

        thumbnailCache.put(document.getHandle(), bitmap);

        adapter.notifyItemChanged(holder.getAbsoluteAdapterPosition());
        Timber.d("AttachThumbnailTask end");
    }

    private static void onThumbnailGeneratedGrid(Context context, File thumbFile, MegaNode document, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter) {
        Timber.d("onThumbnailGeneratedGrid");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);

        if (holder instanceof MegaNodeAdapter.ViewHolderBrowserGrid) {
            ((MegaNodeAdapter.ViewHolderBrowserGrid) holder).imageViewThumb.setVisibility(View.VISIBLE);
            ((MegaNodeAdapter.ViewHolderBrowserGrid) holder).imageViewIcon.setVisibility(View.GONE);
            ((MegaNodeAdapter.ViewHolderBrowserGrid) holder).imageViewThumb.setImageBitmap(bitmap);
            ((MegaNodeAdapter.ViewHolderBrowserGrid) holder).thumbLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));
        } else if (holder instanceof NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) {
            ((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) holder).imageViewThumb.setVisibility(View.VISIBLE);
            ((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) holder).imageViewIcon.setVisibility(View.GONE);
            ((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) holder).imageViewThumb.setImageBitmap(bitmap);
            ((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid) holder).thumbLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));
        }

        thumbnailCache.put(document.getHandle(), bitmap);
        adapter.notifyItemChanged(holder.getAdapterPosition());
        Timber.d("AttachThumbnailTask end");
    }

    public static void createThumbnailList(Context context, MegaNode document, RecyclerView.ViewHolder holder, MegaApiAndroid megaApi, RecyclerView.Adapter adapter) {

        if (!MimeTypeList.typeForName(document.getName()).isImage()) {
            Timber.w("No image");
            return;
        }

        String localPath = getLocalFile(document); //if file already exists returns != null
        if (localPath != null) {
            ResizerParams params = new ResizerParams();
            params.document = document;
            params.file = new File(localPath);
            new AttachThumbnailTaskList(context, megaApi, holder, adapter).execute(params);
        } //Si no, no hago nada

    }

    public static void createThumbnailGrid(Context context, MegaNode document, RecyclerView.ViewHolder holder, MegaApiAndroid megaApi, RecyclerView.Adapter adapter) {

        if (!MimeTypeList.typeForName(document.getName()).isImage()) {
            Timber.w("No image");
            return;
        }

        String localPath = getLocalFile(document); //if file already exists returns != null
        if (localPath != null) {
            Timber.d("localPath is not null: %s", localPath);
            ResizerParams params = new ResizerParams();
            params.document = document;
            params.file = new File(localPath);
            new AttachThumbnailTaskGrid(context, megaApi, holder, adapter).execute(params);
        } //Si no, no hago nada

    }

    private static void setThumbLayoutParamsForList(Context context, ImageView imageView) {
        RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
        params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
        params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
        int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
        params1.setMargins(left, 0, 0, 0);

        imageView.setLayoutParams(params1);
    }

    public static void getThumbAndSetViewForList(Context context, MegaNode node, RecyclerView.ViewHolder holder,
                                                 MegaApiAndroid megaApi, RecyclerView.Adapter adapter, ImageView imageView) {
        Bitmap thumb;
        setThumbLayoutParamsForList(context, imageView);

        if ((thumb = ThumbnailUtils.getThumbnailFromCache(node)) == null &&
                ((thumb = ThumbnailUtils.getThumbnailFromFolder(node, context)) == null)) {
            try {
                thumb = ThumbnailUtils.getThumbnailFromMegaList(node, context, holder, megaApi, adapter);
            } catch (Exception e) {
                Timber.w(e);
            }// Too many AsyncTasks
        }

        if (thumb != null) {
            imageView.setImageBitmap(ThumbnailUtils.getRoundedBitmap(context, thumb, dp2px(THUMB_CORNER_RADIUS_DP)));
        }
    }

    public static void getThumbAndSetViewOrCreateForList(Context context, MegaNode node, RecyclerView.ViewHolder holder,
                                                         MegaApiAndroid megaApi, RecyclerView.Adapter adapter, ImageView imageView) {
        Bitmap thumb;
        if ((thumb = ThumbnailUtils.getThumbnailFromCache(node)) != null ||
                (thumb = ThumbnailUtils.getThumbnailFromFolder(node, context)) != null) {
            setThumbLayoutParamsForList(context, imageView);
            imageView.setImageBitmap(ThumbnailUtils.getRoundedBitmap(context, thumb, dp2px(THUMB_CORNER_RADIUS_DP)));
        } else {
            Timber.d("NOT thumbnail");
            imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
            try {
                ThumbnailUtils.createThumbnailList(context, node, holder, megaApi, adapter);
            } catch (Exception e) {
                Timber.w(e);
            } // Too many AsyncTasks
        }
    }
}
