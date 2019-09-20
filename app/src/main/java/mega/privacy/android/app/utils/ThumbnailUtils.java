package mega.privacy.android.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.io.File;
import java.util.HashMap;

import mega.privacy.android.app.R;
import mega.privacy.android.app.ThumbnailCache;
import mega.privacy.android.app.lollipop.adapters.MegaFullScreenImageAdapterLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.isFileAvailable;
import static mega.privacy.android.app.utils.LogUtil.*;


/*
 * Service to create thumbnails
 */
public class ThumbnailUtils {
	public static File thumbDir;
	public static ThumbnailCache thumbnailCache = new ThumbnailCache();
	public static ThumbnailCache thumbnailCachePath = new ThumbnailCache(1);
//	public static ArrayList<Long> pendingThumbnails = new ArrayList<Long>();
	
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

			logDebug("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
			MegaNode node = api.getNodeByHandle(handle);
			
//			pendingThumbnails.remove(handle);
			
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("Downloading thumbnail OK: " + handle);
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
		if(node!=null){
			File thumb = new File(thumbDir, node.getBase64Handle()+".jpg");
			Bitmap bitmap = null;
			if (thumb.exists() && thumb.length() > 0){
				bitmap = getBitmapForCache(thumb, context);
				if (bitmap == null) {
					thumb.delete();
				}
				else{
					thumbnailCache.put(node.getHandle(), bitmap);
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
}
