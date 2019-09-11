package mega.privacy.android.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

import mega.privacy.android.app.R;
import mega.privacy.android.app.ThumbnailCache;
import mega.privacy.android.app.lollipop.adapters.MegaFullScreenImageAdapterLollipop;
import nz.mega.sdk.AndroidGfxProcessor;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static nz.mega.sdk.AndroidGfxProcessor.fixExifOrientation;
import static nz.mega.sdk.AndroidGfxProcessor.isVideoFile;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.isFileAvailable;


/*
 * Service to create thumbnails
 */
public class ThumbnailUtils {
	public static File thumbDir;
	public static ThumbnailCache thumbnailCache = new ThumbnailCache();
	public static ThumbnailCache thumbnailCachePath = new ThumbnailCache(1);
	private static int THUMBNAIL_SIZE = 200;
	
	static HashMap<Long, ThumbnailDownloadListenerExplorer> listenersExplorer = new HashMap<Long, ThumbnailDownloadListenerExplorer>();
	static HashMap<Long, ThumbnailDownloadListenerFull> listenersFull = new HashMap<Long, ThumbnailDownloadListenerFull>();
	static HashMap<Long, ThumbnailDownloadListenerPhotoSyncList> listenersPhotoSyncList = new HashMap<Long, ThumbnailDownloadListenerPhotoSyncList>();

	static class ThumbnailDownloadListenerPhotoSyncList{
		Context context;
//		ViewHolderPhotoSyncList holder;
//		MegaPhotoSyncListAdapter adapter;
//
//		ThumbnailDownloadListenerPhotoSyncList(Context context, ViewHolderPhotoSyncList holder, MegaPhotoSyncListAdapter adapter){
//			this.context = context;
//			this.holder = holder;
//			this.adapter = adapter;
//		}
//
//		@Override
//		public void onRequestStart(MegaApiJava api, MegaRequest request) {
//			// TODO Auto-generated method stub
//
//		}
//
//		@Override
//		public void onRequestFinish(MegaApiJava api, MegaRequest request,MegaError e) {
//
//			log("Downloading thumbnail finished");
//			final long handle = request.getNodeHandle();
//			MegaNode node = api.getNodeByHandle(handle);
//
////			pendingThumbnails.remove(handle);
//
//			if (e.getErrorCode() == MegaError.API_OK){
//				log("Downloading thumbnail OK: " + handle);
//				thumbnailCache.remove(handle);
//
//				if (holder != null){
//					File thumbDir = getThumbFolder(context);
//					File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
//					if (thumb.exists()) {
//						if (thumb.length() > 0) {
//							final Bitmap bitmap = getBitmapForCache(thumb, context);
//							if (bitmap != null) {
//								thumbnailCache.put(handle, bitmap);
//								if ((holder.document == handle)){
//									holder.imageView.setImageBitmap(bitmap);
//									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
//									holder.imageView.startAnimation(fadeInAnimation);
//									adapter.notifyDataSetChanged();
//									log("Thumbnail update");
//								}
//							}
//						}
//					}
//				}
//			}
//			else{
//				log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
//			}
//		}

	}

	static class ThumbnailDownloadListenerExplorer {
		Context context;

	}

	static class ThumbnailDownloadListenerFull implements MegaRequestListenerInterface{
		Context context;
		MegaFullScreenImageAdapterLollipop.ViewHolderFullImage holder;
		MegaFullScreenImageAdapterLollipop adapter;
		
		ThumbnailDownloadListenerFull(Context context, MegaFullScreenImageAdapterLollipop.ViewHolderFullImage holder, MegaFullScreenImageAdapterLollipop adapter){
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
			
			log("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
			MegaNode node = api.getNodeByHandle(handle);
			
//			pendingThumbnails.remove(handle);
			
			if (e.getErrorCode() == MegaError.API_OK){
				log("Downloading thumbnail OK: " + handle);
				thumbnailCache.remove(handle);
				
				if (holder != null){
					File thumbDir = getThumbFolder(context);
					File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
					if (thumb.exists()) {
						if (thumb.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(thumb, context);
							if (bitmap != null) {
								thumbnailCache.put(handle, bitmap);
								if ((holder.document == handle)){
									holder.imgDisplay.setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imgDisplay.startAnimation(fadeInAnimation);
									adapter.notifyDataSetChanged();
									log("Thumbnail update");
								}
							}
						}
					}
				}
			}
			else{
				log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
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
		log("getThumbFolder(): thumbDir= " + thumbDir);
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
		if(node!=null){
			File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
			Bitmap bitmap = null;
			if (thumb.exists()){
				if (thumb.length() > 0){
					bitmap = getBitmapForCache(thumb, context);
					if (bitmap == null) {
						thumb.delete();
					}
					else{
						thumbnailCache.put(node.getHandle(), bitmap);
					}
				}
			}
			return thumbnailCache.get(node.getHandle());
		}
		return null;
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
	
	public static class ResizerParams {
		File file;
		MegaNode document;
	}

	public static Bitmap getBitmap(String path, Context context, Rect rect, int orientation, int w, int h) {
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
					bmThumbnail = android.media.ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND);
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

		Bitmap bitmap = getBitmap(path, context, rect, orientation, w, h);
		bitmap = nz.mega.sdk.AndroidGfxProcessor.extractRect(bitmap, px, py, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
		if (bitmap == null)
			return false;

		return AndroidGfxProcessor.saveBitmap(bitmap, thumbnail);
	}

	private static void log(String log) {
		Util.log("ThumbnailUtils", log);
	}	
}
