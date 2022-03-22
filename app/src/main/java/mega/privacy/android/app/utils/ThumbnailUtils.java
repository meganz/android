package mega.privacy.android.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video.Thumbnails;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ThumbnailCache;
import mega.privacy.android.app.FileDocument;
import mega.privacy.android.app.main.adapters.FileStorageAdapter;
import mega.privacy.android.app.main.adapters.FileStorageAdapter.ViewHolderFileStorage;
import mega.privacy.android.app.main.adapters.MegaExplorerAdapter;
import mega.privacy.android.app.main.adapters.MegaExplorerAdapter.ViewHolderExplorer;
import mega.privacy.android.app.main.adapters.MegaNodeAdapter;
import mega.privacy.android.app.main.adapters.MegaTransfersAdapter;
import mega.privacy.android.app.main.adapters.MegaTransfersAdapter.ViewHolderTransfer;
import mega.privacy.android.app.main.adapters.MultipleBucketAdapter;
import mega.privacy.android.app.main.adapters.RecentsAdapter;
import mega.privacy.android.app.main.adapters.VersionsFileAdapter;
import mega.privacy.android.app.main.megachat.chatAdapters.NodeAttachmentHistoryAdapter;
import mega.privacy.android.app.main.providers.MegaProviderAdapter;
import mega.privacy.android.app.main.providers.MegaProviderAdapter.ViewHolderProvider;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.THUMB_CORNER_RADIUS_DP;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.dp2px;
import static nz.mega.sdk.MegaUtilsAndroid.createThumbnail;

/*
 * Service to create thumbnails
 */
public class ThumbnailUtils {
	public static File thumbDir;
	public static ThumbnailCache thumbnailCache = new ThumbnailCache();
	public static ThumbnailCache thumbnailCachePath = new ThumbnailCache(1);
	public static Boolean isDeviceMemoryLow = false;

	static HashMap<Long, ThumbnailDownloadListenerListBrowser> listenersList = new HashMap<Long, ThumbnailDownloadListenerListBrowser>();
	static HashMap<Long, ThumbnailDownloadListenerGridBrowser> listenersGrid = new HashMap<Long, ThumbnailDownloadListenerGridBrowser>();
	static HashMap<Long, ThumbnailDownloadListenerExplorer> listenersExplorer = new HashMap<Long, ThumbnailDownloadListenerExplorer>();
	static HashMap<Long, ThumbnailDownloadListenerProvider> listenersProvider = new HashMap<Long, ThumbnailDownloadListenerProvider>();
	static HashMap<Long, ThumbnailDownloadListenerTransfer> listenersTransfer = new HashMap<Long, ThumbnailDownloadListenerTransfer>();

	static HashMap<Long, ThumbnailDownloadListenerThumbnailInterface> listenersThumbnailInterface = new HashMap<Long, ThumbnailDownloadListenerThumbnailInterface>();

	public static Bitmap getRoundedRectBitmap(Context context, final Bitmap bitmap,final int pixels)
	{
		logDebug("getRoundedRectBitmap");
		final Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(result);
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);

		final float densityMultiplier = context.getResources().getDisplayMetrics().density;
		final float roundPx = pixels*densityMultiplier;
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(ContextCompat.getColor(context, R.color.white));
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		canvas.drawRect(0, bitmap.getHeight()/2, bitmap.getWidth()/2, bitmap.getHeight(), paint);
		canvas.drawRect(bitmap.getWidth()/2, bitmap.getHeight()/2, bitmap.getWidth(), bitmap.getHeight(), paint);

		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return result;
	}

	public static Bitmap getRoundedBitmap(Context context, final Bitmap bitmap,final int pixels){
		logDebug("getRoundedRectBitmap");
		final Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(result);
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);

		final float densityMultiplier = context.getResources().getDisplayMetrics().density;
		final float roundPx = pixels*densityMultiplier;
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(ContextCompat.getColor(context, R.color.white));
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, 0, 0, paint);
		return result;
	}

	public static Path getRoundedRect(float left, float top, float right, float bottom, float rx, float ry,
								   boolean tl, boolean tr, boolean br, boolean bl){
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
		else{
			path.rLineTo(0, -ry);
			path.rLineTo(-rx,0);
		}
		path.rLineTo(-widthMinusCorners, 0);
		if (tl)
			path.rQuadTo(-rx, 0, -rx, ry); //top-left corner
		else{
			path.rLineTo(-rx, 0);
			path.rLineTo(0,ry);
		}
		path.rLineTo(0, heightMinusCorners);

		if (bl)
			path.rQuadTo(0, ry, rx, ry);//bottom-left corner
		else{
			path.rLineTo(0, ry);
			path.rLineTo(rx,0);
		}

		path.rLineTo(widthMinusCorners, 0);
		if (br)
			path.rQuadTo(rx, 0, rx, -ry); //bottom-right corner
		else{
			path.rLineTo(rx,0);
			path.rLineTo(0, -ry);
		}

		path.rLineTo(0, -heightMinusCorners);

		path.close();//Given close, last lineto can be removed.

		return path;
	}

	static class VideoThumbGeneratorListener implements MegaRequestListenerInterface{

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("OK thumb de video");
			}
			else{
				logError("ERROR thumb de video: " + e.getErrorString());
			}

		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub

		}

	}

	static class ThumbnailDownloadListenerListBrowser implements MegaRequestListenerInterface{
		Context context;
		RecyclerView.ViewHolder holder;
		RecyclerView.Adapter adapter;

		ThumbnailDownloadListenerListBrowser(Context context, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {

			logDebug("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
			String base64 = MegaApiJava.handleToBase64(handle);

			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);

				if (holder == null) return;

				File thumbDir = getThumbFolder(context);
				File thumb = new File(thumbDir, base64+".jpg");

				if (!thumb.exists() || thumb.length() <= 0) return;

				final Bitmap bitmap = getBitmapForCache(thumb, context);
				if (bitmap == null) return;

				thumbnailCache.put(handle, bitmap);

				Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);

				if(holder instanceof MegaNodeAdapter.ViewHolderBrowserList){
					if ((((MegaNodeAdapter.ViewHolderBrowserList)holder).document == handle)){
						((MegaNodeAdapter.ViewHolderBrowserList)holder).imageView.setImageBitmap(
								getRoundedBitmap(context, bitmap, dp2px(THUMB_CORNER_RADIUS_DP)));
						((MegaNodeAdapter.ViewHolderBrowserList)holder).imageView.startAnimation(fadeInAnimation);
					}
				}
				else if(holder instanceof VersionsFileAdapter.ViewHolderVersion){
					if ((((VersionsFileAdapter.ViewHolderVersion)holder).document == handle)){
						((VersionsFileAdapter.ViewHolderVersion)holder).imageView.setImageBitmap(
								getRoundedBitmap(context, bitmap, dp2px(THUMB_CORNER_RADIUS_DP)));
						((VersionsFileAdapter.ViewHolderVersion)holder).imageView.startAnimation(fadeInAnimation);
					}
				}
				else if(holder instanceof NodeAttachmentHistoryAdapter.ViewHolderBrowserList){
					if ((((NodeAttachmentHistoryAdapter.ViewHolderBrowserList)holder).document == handle)){
						((NodeAttachmentHistoryAdapter.ViewHolderBrowserList)holder).imageView.setImageBitmap(
								getRoundedBitmap(context, bitmap, dp2px(THUMB_CORNER_RADIUS_DP)));
						((NodeAttachmentHistoryAdapter.ViewHolderBrowserList)holder).imageView.startAnimation(fadeInAnimation);
					}
				}
				else if (holder instanceof MultipleBucketAdapter.ViewHolderMultipleBucket) {
					MultipleBucketAdapter.ViewHolderMultipleBucket viewHolderMultipleBucket = (MultipleBucketAdapter.ViewHolderMultipleBucket) holder;
					if (viewHolderMultipleBucket.getDocument() == handle) {
						viewHolderMultipleBucket.setImageThumbnail(bitmap);
						if (((MultipleBucketAdapter) adapter).isMedia()) {
							viewHolderMultipleBucket.getThumbnailMedia().startAnimation(fadeInAnimation);
						} else {
							viewHolderMultipleBucket.getThumbnailList().startAnimation(fadeInAnimation);
						}
					}
				}

				adapter.notifyItemChanged(holder.getAdapterPosition());
			}
			else{
				logError("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub

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
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

			logDebug("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
			if (e.getErrorCode() == MegaError.API_OK) {
				logDebug("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);

				if (holder != null) {
					File thumbDir = getThumbFolder(context);
					File thumb = new File(thumbDir, MegaApiJava.handleToBase64(handle) + ".jpg");
					if (thumb.exists()) {
						if (thumb.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(thumb, context);
							if (bitmap != null) {
								thumbnailCache.put(handle, bitmap);
								if(holder instanceof MegaNodeAdapter.ViewHolderBrowserGrid){
									if ((((MegaNodeAdapter.ViewHolderBrowserGrid)holder).document == handle)) {
										((MegaNodeAdapter.ViewHolderBrowserGrid)holder).imageViewThumb.setVisibility(View.VISIBLE);
										((MegaNodeAdapter.ViewHolderBrowserGrid)holder).imageViewIcon.setVisibility(View.GONE);
										((MegaNodeAdapter.ViewHolderBrowserGrid)holder).imageViewThumb.setImageBitmap(bitmap);
										((MegaNodeAdapter.ViewHolderBrowserGrid)holder).thumbLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));
										Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
										((MegaNodeAdapter.ViewHolderBrowserGrid)holder).imageViewThumb.startAnimation(fadeInAnimation);
										adapter.notifyItemChanged(holder.getAdapterPosition());
										logDebug("Thumbnail update");
									}
								}
								else if(holder instanceof NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid){
									if ((((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid)holder).document == handle)) {
										((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid)holder).imageViewThumb.setVisibility(View.VISIBLE);
										((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid)holder).imageViewIcon.setVisibility(View.GONE);
										((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid)holder).imageViewThumb.setImageBitmap(bitmap);
										((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid)holder).thumbLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));
										Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
										((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid)holder).imageViewThumb.startAnimation(fadeInAnimation);
										adapter.notifyItemChanged(holder.getAdapterPosition());
										logDebug("Thumbnail update");
									}
								}
							}
						}
					}
				}
			} else {
				logError("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError (MegaApiJava api, MegaRequest request, MegaError e){
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestUpdate (MegaApiJava api, MegaRequest request){
			// TODO Auto-generated method stub

		}
	}

	static class ThumbnailDownloadListenerExplorer implements MegaRequestListenerInterface{
		Context context;
		ViewHolderExplorer holder;
		MegaExplorerAdapter adapter;

		ThumbnailDownloadListenerExplorer(Context context, ViewHolderExplorer holder, MegaExplorerAdapter adapter){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {

			logDebug("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
            String handleBase64 = MegaApiJava.handleToBase64(handle);
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);

				if (holder != null){
					File thumbDir = getThumbFolder(context);
                    File thumb = new File(thumbDir, handleBase64 + ".jpg");
					if (thumb.exists()) {
						if (thumb.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(thumb, context);
							if (bitmap != null) {
								thumbnailCache.put(handle, bitmap);
								if ((holder.document == handle)){
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									if (holder instanceof MegaExplorerAdapter.ViewHolderListExplorer) {
										MegaExplorerAdapter.ViewHolderListExplorer holderList = (MegaExplorerAdapter.ViewHolderListExplorer) holder;
										holderList.imageView.setImageBitmap(getRoundedBitmap(context, bitmap, dp2px(THUMB_CORNER_RADIUS_DP)));
										holderList.imageView.startAnimation(fadeInAnimation);
										adapter.notifyItemChanged(holderList.getAdapterPosition());
									}
									else if (holder instanceof MegaExplorerAdapter.ViewHolderGridExplorer) {
										MegaExplorerAdapter.ViewHolderGridExplorer holderGrid = (MegaExplorerAdapter.ViewHolderGridExplorer) holder;
										holderGrid.fileThumbnail.setImageBitmap(ThumbnailUtils.getRoundedRectBitmap(context,bitmap,2));
										holderGrid.fileThumbnail.setVisibility(View.VISIBLE);
										holderGrid.fileIcon.setVisibility(View.GONE);
										holderGrid.fileThumbnail.startAnimation(fadeInAnimation);
										adapter.notifyItemChanged(holderGrid.getAdapterPosition());
									}
									logDebug("Thumbnail update");
								}
							}
						}
					}
				}
			}
			else{
				logError("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub

		}
	}

	static class ThumbnailDownloadListenerProvider implements MegaRequestListenerInterface{
		Context context;
		ViewHolderProvider holder;
		MegaProviderAdapter adapter;

		ThumbnailDownloadListenerProvider(Context context, ViewHolderProvider holder, MegaProviderAdapter adapter){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {

			final long handle = request.getNodeHandle();
            logDebug("Downloading thumbnail finished");
            String handleBase64 = MegaApiJava.handleToBase64(handle);
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);

				if (holder != null){
					File thumbDir = getThumbFolder(context);
                    File thumb = new File(thumbDir, handleBase64 + ".jpg");
					if (thumb.exists()) {
						if (thumb.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(thumb, context);
							if (bitmap != null) {
								thumbnailCache.put(handle, bitmap);
								if ((holder.document == handle)){
									holder.imageView.setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imageView.startAnimation(fadeInAnimation);
									adapter.notifyItemChanged(holder.getAdapterPosition());
									logDebug("Thumbnail update");
								}
							}
						}
					}
				}
			}
			else{
				logError("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub

		}
	}

	static class ThumbnailDownloadListenerTransfer implements MegaRequestListenerInterface{
		Context context;
		ViewHolderTransfer holder;
		MegaTransfersAdapter adapter;

		ThumbnailDownloadListenerTransfer(Context context, ViewHolderTransfer holder, MegaTransfersAdapter adapter){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {

			logDebug("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
            String handleBase64 = MegaApiJava.handleToBase64(handle);

			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);

				if (holder != null){
					File thumbDir = getThumbFolder(context);
                    File thumb = new File(thumbDir, handleBase64 + ".jpg");
					if (thumb.exists()) {
						if (thumb.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(thumb, context);
							if (bitmap != null) {
								thumbnailCache.put(handle, bitmap);
								if ((holder.document == handle)){
									holder.imageView.setImageBitmap(getRoundedBitmap(context, bitmap, dp2px(THUMB_CORNER_RADIUS_DP)));
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imageView.startAnimation(fadeInAnimation);
									adapter.notifyItemChanged(holder.getAdapterPosition());
									logDebug("Thumbnail update");
								}
							}
						}
					}
				}
			}
			else{
				logError("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub

		}
	}

	/*
	 * Get thumbnail folder
	 */
	public static File getThumbFolder(Context context) {
        if(!isFileAvailable(thumbDir)) {
            thumbDir = getCacheFolder(context, THUMBNAIL_FOLDER);
        }
		logDebug("getThumbFolder(): thumbDir= " + thumbDir);
        return thumbDir;
	}

	public static Bitmap getThumbnailFromCache(MegaNode node){
		return thumbnailCache.get(node.getHandle());
	}

	public static Bitmap getThumbnailFromCache(long handle){
		return thumbnailCache.get(handle);
	}

	public static Bitmap getThumbnailFromCache(String path){
		return thumbnailCachePath.get(path);
	}

	public static void setThumbnailCache(long handle, Bitmap bitmap){
		thumbnailCache.put(handle, bitmap);
	}

	public static void setThumbnailCache(String path, Bitmap bitmap){
		thumbnailCachePath.put(path, bitmap);
	}

	public static Bitmap getThumbnailFromFolder(MegaNode node, Context context){
		File thumbDir = getThumbFolder(context);
		if (node != null) {
			File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
			Bitmap bitmap = null;
			if (thumb.exists() && thumb.length() > 0){
				bitmap = getBitmapForCache(thumb, context);
				if (bitmap == null) {
					thumb.delete();
				}
				else {
					thumbnailCache.put(node.getHandle(), bitmap);
				}
			}
			return thumbnailCache.get(node.getHandle());
		}
		return null;
	}

	public static Bitmap getThumbnailFromMegaList(MegaNode document, Context context, RecyclerView.ViewHolder viewHolder, MegaApiAndroid megaApi, RecyclerView.Adapter adapter){

		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		ThumbnailDownloadListenerListBrowser listener = new ThumbnailDownloadListenerListBrowser(context, viewHolder, adapter);
		listenersList.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");

		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);

		return thumbnailCache.get(document.getHandle());

	}

	public static Bitmap getThumbnailFromMegaGrid(MegaNode document, Context context, RecyclerView.ViewHolder viewHolder, MegaApiAndroid megaApi, RecyclerView.Adapter adapter){
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}

		ThumbnailDownloadListenerGridBrowser listener = new ThumbnailDownloadListenerGridBrowser(context, viewHolder, adapter);
		listenersGrid.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		logDebug("Will download here: " + thumbFile.getAbsolutePath());
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);

		return thumbnailCache.get(document.getHandle());

	}

	public static Bitmap getThumbnailFromMegaTransfer(MegaNode document, Context context, ViewHolderTransfer viewHolder, MegaApiAndroid megaApi, MegaTransfersAdapter adapter){
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}

		ThumbnailDownloadListenerTransfer listener = new ThumbnailDownloadListenerTransfer(context, viewHolder, adapter);
		listenersTransfer.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		logDebug("Will download here: " + thumbFile.getAbsolutePath());
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);

		return thumbnailCache.get(document.getHandle());

	}

	public static Bitmap getThumbnailFromMegaExplorer(MegaNode document, Context context, ViewHolderExplorer viewHolder, MegaApiAndroid megaApi, MegaExplorerAdapter adapter){
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}

		ThumbnailDownloadListenerExplorer listener = new ThumbnailDownloadListenerExplorer(context, viewHolder, adapter);
		listenersExplorer.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);

		return thumbnailCache.get(document.getHandle());

	}

	public static Bitmap getThumbnailFromMegaProvider(MegaNode document, Context context, ViewHolderProvider viewHolder, MegaApiAndroid megaApi, MegaProviderAdapter adapter){
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}

		ThumbnailDownloadListenerProvider listener = new ThumbnailDownloadListenerProvider(context, viewHolder, adapter);
		listenersProvider.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);

		return thumbnailCache.get(document.getHandle());

	}

	/*
	 * Load Bitmap for cache
	 */
	private static Bitmap getBitmapForCache(File bmpFile, Context context) {
		BitmapFactory.Options bOpts = new BitmapFactory.Options();
		bOpts.inPurgeable = true;
		bOpts.inInputShareable = true;
		Bitmap bmp = BitmapFactory.decodeFile(bmpFile.getAbsolutePath(), bOpts);
		return bmp;
	}

    private static Bitmap getBitmapForCacheForList(File bmpFile, Context context) {
        if(isDeviceMemoryLow){
            return null;
        }
        BitmapFactory.Options bOpts = new BitmapFactory.Options();
        Bitmap bmp = BitmapFactory.decodeFile(bmpFile.getAbsolutePath(), bOpts);
        return bmp;
    }

	public static class ResizerParams {
		File file;
		MegaNode document;
	}


	/*
	* This async task is to patch thumbnail picture to video or image files
	* in device folder when select device file to upload
	*/
	static class AttachThumbnailToFileStorageExplorerTask extends AsyncTask<FileDocument, Void, Boolean> {

		Context context;
		File thumbFile;
		File originalFile;
		FileStorageAdapter adapter;
		MegaApiAndroid megaApi;
		int position;

		AttachThumbnailToFileStorageExplorerTask(Context context, MegaApiAndroid megaApi, FileStorageAdapter adapter, int position) {
			this.context = context;
			this.adapter = adapter;
			this.thumbFile = null;
			this.megaApi = megaApi;
			this.position = position;
		}

		@Override
		protected Boolean doInBackground(FileDocument... params) {
			logDebug("Attach Thumbnails to file storage explorer Start");
			File thumbDir = getThumbFolder(context);
			this.originalFile = params[0].getFile();
			thumbFile = new File(thumbDir, megaApi.getFingerprint(this.originalFile.getAbsolutePath()) + ".jpg" );
			boolean thumbCreated = createThumbnail(this.originalFile, thumbFile);
			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onThumbnailGeneratedExplorer(megaApi, this.thumbFile, this.originalFile, adapter, position);
			}
		}
	}

	static class AttachThumbnailTaskExplorer extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderExplorer holder;
		MegaExplorerAdapter adapter;

		AttachThumbnailTaskExplorer(Context context, MegaApiAndroid megaApi, ViewHolderExplorer holder, MegaExplorerAdapter adapter)
		{
			this.context = context;
			this.megaApi = megaApi;
			this.holder = holder;
			this.adapter = adapter;
			this.thumbFile = null;
			this.param = null;
		}

		@Override
		protected Boolean doInBackground(ResizerParams... params) {
			logDebug("AttachPreviewStart");
			param = params[0];

			File thumbDir = getThumbFolder(context);
			thumbFile = new File(thumbDir, param.document.getBase64Handle()+".jpg");
			boolean thumbCreated = createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onThumbnailGeneratedExplorer(context, thumbFile, param.document, holder, adapter);
			}
		}
	}

	static class AttachThumbnailTaskProvider extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderProvider holder;
		MegaProviderAdapter adapter;

		AttachThumbnailTaskProvider(Context context, MegaApiAndroid megaApi, ViewHolderProvider holder, MegaProviderAdapter adapter)
		{
			this.context = context;
			this.megaApi = megaApi;
			this.holder = holder;
			this.adapter = adapter;
			this.thumbFile = null;
			this.param = null;
		}

		@Override
		protected Boolean doInBackground(ResizerParams... params) {
			logDebug("AttachPreviewStart");
			param = params[0];

			File thumbDir = getThumbFolder(context);
			thumbFile = new File(thumbDir, param.document.getBase64Handle()+".jpg");
			boolean thumbCreated = createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {}
	}

	private static void onThumbnailGeneratedExplorer(Context context, File thumbFile, MegaNode document, ViewHolderExplorer holder, MegaExplorerAdapter adapter){
		logDebug("onPreviewGenerated");
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
		if (holder instanceof MegaExplorerAdapter.ViewHolderListExplorer) {
			MegaExplorerAdapter.ViewHolderListExplorer holderList = (MegaExplorerAdapter.ViewHolderListExplorer) holder;
			holderList.imageView.setImageBitmap(getRoundedBitmap(context, bitmap, dp2px(THUMB_CORNER_RADIUS_DP)));
			adapter.notifyItemChanged(holderList.getAdapterPosition());
		}
		else if (holder instanceof MegaExplorerAdapter.ViewHolderGridExplorer) {
			MegaExplorerAdapter.ViewHolderGridExplorer holderGrid = (MegaExplorerAdapter.ViewHolderGridExplorer) holder;
			holderGrid.fileThumbnail.setImageBitmap(ThumbnailUtils.getRoundedRectBitmap(context,bitmap,2));
			holderGrid.fileThumbnail.setVisibility(View.VISIBLE);
			holderGrid.fileIcon.setVisibility(View.GONE);
			adapter.notifyItemChanged(holderGrid.getAdapterPosition());
		}
		thumbnailCache.put(document.getHandle(), bitmap);
		logDebug("AttachThumbnailTask end");
	}

	private static void onThumbnailGeneratedExplorer(MegaApiAndroid megaApi, File thumbFile, File originalFile, FileStorageAdapter adapter, int position){
		logDebug("onPreviewGenerated");
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
		String key = megaApi.getFingerprint(originalFile.getAbsolutePath());
		//put thumbnail picture into cache
		if (key != null && bitmap != null) {
			thumbnailCache.put(megaApi.getFingerprint(originalFile.getAbsolutePath()), bitmap);
		}
		//refresh the position only in required
		adapter.notifyItemChanged(position);
		logDebug("AttachThumbnailTask end");
	}

	static class AttachThumbnailTaskList extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		RecyclerView.ViewHolder holder;
		RecyclerView.Adapter adapter;

		AttachThumbnailTaskList(Context context, MegaApiAndroid megaApi, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter)
		{
			this.context = context;
			this.megaApi = megaApi;
			this.holder = holder;
			this.adapter = adapter;
			this.thumbFile = null;
			this.param = null;
		}


		@Override
		protected Boolean doInBackground(ResizerParams... params) {
			logDebug("AttachThumbnailTaskList");
			param = params[0];

			File thumbDir = getThumbFolder(context);
			thumbFile = new File(thumbDir, param.document.getBase64Handle()+".jpg");
			boolean thumbCreated = createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {

			if (!shouldContinueObject) return;

			if(holder instanceof MegaNodeAdapter.ViewHolderBrowserList){
				RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) ((MegaNodeAdapter.ViewHolderBrowserList)holder).imageView.getLayoutParams();
				params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
				params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
				params1.setMargins(18, 0, 12, 0);
				((MegaNodeAdapter.ViewHolderBrowserList)holder).imageView.setLayoutParams(params1);

				onThumbnailGeneratedList(context, megaApi, thumbFile, param.document, holder, adapter);
			}
			else if(holder instanceof VersionsFileAdapter.ViewHolderVersion){
				RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) ((VersionsFileAdapter.ViewHolderVersion)holder).imageView.getLayoutParams();
				params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
				params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
				params1.setMargins(18, 0, 12, 0);
				((VersionsFileAdapter.ViewHolderVersion)holder).imageView.setLayoutParams(params1);

				onThumbnailGeneratedList(context, megaApi, thumbFile, param.document, holder, adapter);
			} else if (holder instanceof RecentsAdapter.ViewHolderBucket
					|| holder instanceof MultipleBucketAdapter.ViewHolderMultipleBucket) {
				onThumbnailGeneratedList(context, megaApi, thumbFile, param.document, holder, adapter);
			}
		}
	}

	static class AttachThumbnailTaskGrid extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		RecyclerView.ViewHolder holder;
		RecyclerView.Adapter adapter;

		AttachThumbnailTaskGrid(Context context, MegaApiAndroid megaApi, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter)
		{
			this.context = context;
			this.megaApi = megaApi;
			this.holder = holder;
			this.adapter = adapter;
			this.thumbFile = null;
			this.param = null;
		}

		@Override
		protected Boolean doInBackground(ResizerParams... params) {
			logDebug("AttachThumbnailTaskGrid");
			param = params[0];

			File thumbDir = getThumbFolder(context);
			thumbFile = new File(thumbDir, param.document.getBase64Handle()+".jpg");
			boolean thumbCreated = createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){

				onThumbnailGeneratedGrid(context, thumbFile, param.document, holder, adapter);
			}
		}
	}

	private static void onThumbnailGeneratedList(Context context, MegaApiAndroid megaApi, File thumbFile, MegaNode document, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter){
		logDebug("onThumbnailGeneratedList");

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);

		if(holder instanceof MegaNodeAdapter.ViewHolderBrowserList){
			((MegaNodeAdapter.ViewHolderBrowserList)holder).imageView.setImageBitmap(bitmap);
		}
		else if(holder instanceof VersionsFileAdapter.ViewHolderVersion){
			((VersionsFileAdapter.ViewHolderVersion)holder).imageView.setImageBitmap(bitmap);
		}
		else if (holder instanceof MultipleBucketAdapter.ViewHolderMultipleBucket) {
			((MultipleBucketAdapter.ViewHolderMultipleBucket) holder).setImageThumbnail(bitmap);
		}

		thumbnailCache.put(document.getHandle(), bitmap);

		adapter.notifyItemChanged(holder.getAdapterPosition());
		logDebug("AttachThumbnailTask end");
	}

	private static void onThumbnailGeneratedGrid(Context context, File thumbFile, MegaNode document, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter){
		logDebug("onThumbnailGeneratedGrid");

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);

		if(holder instanceof MegaNodeAdapter.ViewHolderBrowserGrid){
			((MegaNodeAdapter.ViewHolderBrowserGrid)holder).imageViewThumb.setVisibility(View.VISIBLE);
			((MegaNodeAdapter.ViewHolderBrowserGrid)holder).imageViewIcon.setVisibility(View.GONE);
			((MegaNodeAdapter.ViewHolderBrowserGrid)holder).imageViewThumb.setImageBitmap(bitmap);
			((MegaNodeAdapter.ViewHolderBrowserGrid)holder).thumbLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));
		}
		else if(holder instanceof NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid){
			((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid)holder).imageViewThumb.setVisibility(View.VISIBLE);
			((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid)holder).imageViewIcon.setVisibility(View.GONE);
			((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid)holder).imageViewThumb.setImageBitmap(bitmap);
			((NodeAttachmentHistoryAdapter.ViewHolderBrowserGrid)holder).thumbLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_010));
		}

		thumbnailCache.put(document.getHandle(), bitmap);
		adapter.notifyItemChanged(holder.getAdapterPosition());
		logDebug("AttachThumbnailTask end");
	}

	public static void createThumbnailList(Context context, MegaNode document, RecyclerView.ViewHolder holder, MegaApiAndroid megaApi, RecyclerView.Adapter adapter){

		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
			logWarning("No image");
			return;
		}

		String localPath = getLocalFile(document); //if file already exists returns != null
		if(localPath != null)
		{
			ResizerParams params = new ResizerParams();
			params.document = document;
			params.file = new File(localPath);
			new AttachThumbnailTaskList(context, megaApi, holder, adapter).execute(params);
		} //Si no, no hago nada

	}

	public static void createThumbnailGrid(Context context, MegaNode document, RecyclerView.ViewHolder holder, MegaApiAndroid megaApi, RecyclerView.Adapter adapter){

		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
			logWarning("No image");
			return;
		}

		String localPath = getLocalFile(document); //if file already exists returns != null
		if(localPath != null)
		{
			logDebug("localPath is not null: " + localPath);
			ResizerParams params = new ResizerParams();
			params.document = document;
			params.file = new File(localPath);
			new AttachThumbnailTaskGrid(context, megaApi, holder, adapter).execute(params);
		} //Si no, no hago nada

	}


	public static void createThumbnailExplorer(Context context, MegaNode document, ViewHolderExplorer holder, MegaApiAndroid megaApi, MegaExplorerAdapter adapter){

		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
			logWarning("No image");
			return;
		}

		String localPath = getLocalFile(document); //if file already exists returns != null
		if(localPath != null)
		{
			logDebug("localPath is not null: " + localPath);
			ResizerParams params = new ResizerParams();
			params.document = document;
			params.file = new File(localPath);
			new AttachThumbnailTaskExplorer(context, megaApi, holder, adapter).execute(params);
		} //Si no, no hago nada

	}

	public static void createThumbnailExplorer(Context context, FileDocument document, ViewHolderFileStorage holder, MegaApiAndroid megaApi, FileStorageAdapter adapter, int position) {
		if (!MimeTypeList.typeForName(document.getName()).isImage() &&
				!MimeTypeList.typeForName(document.getName()).isVideo()) {
			logDebug("no image or video");
			return;
		}

		// if the document is gone or deleted
		String key = megaApi.getFingerprint(document.getFile().getAbsolutePath());
		if (key == null) {
			logDebug("no key");
			return;
		}

		// if the thumbnail bitmap is cached in memory cache
		Bitmap bitmap = getThumbnailFromCache(key);
		if (bitmap == null) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			File directoryCachedFile = new File(getThumbFolder(context), key + ".jpg");
			if (directoryCachedFile.exists()) {
				bitmap = BitmapFactory.decodeFile(directoryCachedFile.getAbsolutePath(), options);
			}
		}

		if (bitmap != null) {
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
			params.height = params.width = dp2px(Constants.THUMBNAIL_SIZE_DP);
			int margin = dp2px(Constants.THUMBNAIL_MARGIN_DP);
			params.setMargins(margin, 0, margin, 0);
			holder.imageView.setImageBitmap(getRoundedBitmap(context, bitmap, dp2px(THUMB_CORNER_RADIUS_DP)));
			return;
		}

		// There is no cache before, we have to start an async task to have the thumbnail bitmap
		new AttachThumbnailToFileStorageExplorerTask(context, megaApi, adapter, position).execute(document);

	}

	public static void createThumbnailPdf(Context context, String localPath, MegaApiAndroid megaApi, long handle){
		logDebug("createThumbnailPdf: " + localPath + " : " + handle);

		MegaNode pdfNode = megaApi.getNodeByHandle(handle);

		if (pdfNode == null){
			logWarning("Pdf is NULL");
			return;
		}

		int pageNumber = 0;
		PdfiumCore pdfiumCore = new PdfiumCore(context);
		FileOutputStream out = null;

		File thumbDir = getThumbFolder(context);
		File thumb = new File(thumbDir, pdfNode.getBase64Handle()+".jpg");
		File file = new File(localPath);
		try {
			PdfDocument pdfDocument = pdfiumCore.newDocument(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
			pdfiumCore.openPage(pdfDocument, pageNumber);
			int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
			int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);
			Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
			Bitmap resizedBitmap = Bitmap.createScaledBitmap(bmp, 200, 200, false);
			out = new FileOutputStream(thumb);
			boolean result = resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
			if(result){
				logDebug("Compress OK!");
				megaApi.setThumbnail(pdfNode, thumb.getAbsolutePath());
			}
			else{
				logDebug("Not Compress");
			}
			pdfiumCore.closeDocument(pdfDocument);
		} catch(Exception e) {
			//todo with exception
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (Exception e) {
				//todo with exception
			}
		}
	}

	public static void createThumbnailVideo(Context context, String localPath, MegaApiAndroid megaApi, long handle){
		logDebug("createThumbnailVideo: " + localPath+ " : " + handle);
		
		//mp4 and 3gp OK, other formats check from Android DB with loadVideoThumbnail
		// mov, mkv, flv not working even not in Android DB

		MegaNode videoNode = megaApi.getNodeByHandle(handle);
		
		if(videoNode==null){
			logWarning("videoNode is NULL");
			return;
		}
		
		Bitmap bmThumbnail;
		// MICRO_KIND, size: 96 x 96 thumbnail 
		bmThumbnail = android.media.ThumbnailUtils.createVideoThumbnail(localPath, Thumbnails.MICRO_KIND);
		if(bmThumbnail==null){
			logDebug("Create video thumb NULL, get with Cursor");
			bmThumbnail= loadVideoThumbnail(localPath, context);
		}	
		else{
			logDebug("Create Video Thumb worked!");
		}
		
		if(bmThumbnail!=null){
			Bitmap resizedBitmap = Bitmap.createScaledBitmap(bmThumbnail, 200, 200, false);

			logDebug("After resize thumb: " + resizedBitmap.getHeight() + " : " + resizedBitmap.getWidth());
			
			try {
				File thumbDir = getThumbFolder(context);
				File thumbVideo = new File(thumbDir, videoNode.getBase64Handle()+".jpg");
				
				thumbVideo.createNewFile();
				
				FileOutputStream out = null;
				try {
				    out = new FileOutputStream(thumbVideo);
				    boolean result = resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
				    if(result){
						logDebug("Compress OK!");
						megaApi.setThumbnail(videoNode, thumbVideo.getAbsolutePath(), new VideoThumbGeneratorListener());
				    }
				    else{
						logDebug("Not Compress");
				    }
				} catch (Exception e) {
					logError("Error with FileOutputStream", e);
				} finally {
				    try {
				        if (out != null) {
				            out.close();
				        }
				    } catch (IOException e) {
						logError("Error", e);
				    }
				}			

			} catch (IOException e1) {
				logError("Error creating new thumb file", e1);
			}			
		}
		else{
			logWarning("Create video thumb NULL");
		}
	}
	
	private static final String SELECTION = MediaColumns.DATA + "=?";
	private static final String[] PROJECTION = { BaseColumns._ID };

	public static Bitmap loadVideoThumbnail(String videoFilePath, Context context) {
		logDebug("loadVideoThumbnail");
		Bitmap result = null;
		Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
		String[] selectionArgs = {videoFilePath};
		ContentResolver cr = context.getContentResolver();
		Cursor cursor = null;
		try {
			cursor = cr.query(uri, PROJECTION, SELECTION, selectionArgs, null);
			if (cursor.moveToFirst()) {
				// it's the only & first thing in projection, so it is 0
				long videoId = cursor.getLong(0);
				result = MediaStore.Video.Thumbnails.getThumbnail(cr, videoId, Thumbnails.MICRO_KIND, null);
			}
			cursor.close();
			return result;
		} catch (Exception ex) {
			logError("Exception is thrown", ex);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return null;
	}

	public interface ThumbnailInterface{
		long getDocument();
		ImageView getImageView();
		int getPositionOnAdapter();
		void postSetImageView();
		void preSetImageView();
		void setBitmap(Bitmap bitmap);
	}

	static class ThumbnailDownloadListenerThumbnailInterface implements MegaRequestListenerInterface{
		Context context;
		ThumbnailInterface holder;
		RecyclerView.Adapter adapter;

		ThumbnailDownloadListenerThumbnailInterface(Context context, ThumbnailInterface holder, RecyclerView.Adapter adapter){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
		}

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {

			logDebug("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
            String handleBase64 = MegaApiJava.handleToBase64(handle);

			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);

				if (holder != null){
					File thumbDir = getThumbFolder(context);
                    File thumb = new File(thumbDir, handleBase64 + ".jpg");
					if (thumb.exists() && thumb.length() > 0) {
						final Bitmap bitmap = getBitmapForCache(thumb, context);
						if (bitmap != null) {
							thumbnailCache.put(handle, bitmap);

							if ((holder.getDocument() == handle)){
								holder.postSetImageView();
								holder.setBitmap(bitmap);
								try {
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.getImageView().startAnimation(fadeInAnimation);
								} catch (NumberFormatException n) {
									logError("Error loading animation", n);
								}
								holder.postSetImageView();
								adapter.notifyItemChanged(holder.getPositionOnAdapter());
								logDebug("Thumbnail update");
							}
						}
					}
				}
			}
			else{
				logError("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			// TODO Auto-generated method stub

		}
	}

	public static Bitmap getThumbnailFromThumbnailInterface(MegaNode document, Context context, ThumbnailInterface viewHolder, MegaApiAndroid megaApi, RecyclerView.Adapter adapter){
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}

		ThumbnailDownloadListenerThumbnailInterface listener = new ThumbnailDownloadListenerThumbnailInterface(context, viewHolder, adapter);
		listenersThumbnailInterface.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);

		return thumbnailCache.get(document.getHandle());
	}

	private static void setThumbLayoutParamsForList(Context context, ImageView imageView) {
		RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)imageView.getLayoutParams();
		params1.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
		params1.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,36,context.getResources().getDisplayMetrics());
		int left = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6,context.getResources().getDisplayMetrics());
		params1.setMargins(left,0,0,0);

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
				logWarning(e.getMessage());
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
			logDebug("NOT thumbnail");
			imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
			try {
				ThumbnailUtils.createThumbnailList(context, node, holder, megaApi, adapter);
			} catch (Exception e) {
				logWarning(e.getMessage());
			} // Too many AsyncTasks
		}
	}
}
