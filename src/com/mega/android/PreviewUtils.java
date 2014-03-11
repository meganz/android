package com.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.mega.android.MegaFullScreenImageAdapter.ViewHolderFullImage;
import com.mega.android.ThumbnailUtils.ThumbnailDownloadListenerList;
import com.mega.components.TouchImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;

public class PreviewUtils {
	
	public static File previewDir;
	public static PreviewCache previewCache = new PreviewCache();
	public static ArrayList<Long> pendingPreviews = new ArrayList<Long>();
	
	static HashMap<Long, PreviewDownloadListener> listeners = new HashMap<Long, PreviewDownloadListener>();

	
	static class PreviewDownloadListener implements MegaRequestListenerInterface{
		Context context;
		ViewHolderFullImage holder;
		MegaFullScreenImageAdapter adapter;
	
		PreviewDownloadListener(Context context, ViewHolderFullImage holder, MegaFullScreenImageAdapter adapter){
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
			
			log("Downloading preview finished");
			final long handle = request.getNodeHandle();
			MegaNode node = api.getNodeByHandle(handle);
			
			pendingPreviews.remove(handle);
			
			if (e.getErrorCode() == MegaError.API_OK){
				log("Downloading preview OK: " + handle);
				previewCache.remove(handle);
				
				if (holder != null){
					File previewDir = getPreviewFolder(context);
					File preview = new File(previewDir, node.getBase64Handle());
					if (preview.exists()) {
						if (preview.length() > 0) {
							final Bitmap bitmap = getBitmapForCache(preview, context);
							if (bitmap != null) {
								previewCache.put(handle, bitmap);
								if ((holder.document == handle)){
									holder.imgDisplay.setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imgDisplay.startAnimation(fadeInAnimation);
									adapter.notifyDataSetChanged();
									log("Preview update");
								}
							}
						}
					}
				}
			}

		}
		
		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub
			
		}
		
	}

	
	/*
	 * Get preview folder
	 */	
	public static File getPreviewFolder(Context context) {
		if (previewDir == null) {
			previewDir = context.getDir("previewsMEGA", 0);
		}
		return previewDir;
	}

	public static Bitmap getPreviewFromCache(MegaNode node){
		return previewCache.get(node.getHandle());
	}
	
	public static Bitmap getPreviewFromFolder(MegaNode node, Context context){
		File previewDir = getPreviewFolder(context);
		File preview = new File(previewDir, node.getBase64Handle());
		Bitmap bitmap = null;
		if (preview.exists()){
			if (preview.length() > 0){
				bitmap = getBitmapForCache(preview, context);
				if (bitmap == null) {
					preview.delete();
				}
				else{
					previewCache.put(node.getHandle(), bitmap);
				}
			}
		}
		return previewCache.get(node.getHandle());
	}
	
	public static Bitmap getPreviewFromMega(MegaNode document, Context context, ViewHolderFullImage holder, MegaApiAndroid megaApi, MegaFullScreenImageAdapter adapter){
		
		if (pendingPreviews.contains(document.getHandle()) || !document.hasPreview()){
			log("the preview is already downloaded or added to the list");
			return previewCache.get(document.getHandle());
		}
		
		if (!Util.isOnline(context)){
			return previewCache.get(document.getHandle());
		}
		
		pendingPreviews.add(document.getHandle());
		PreviewDownloadListener listener = new PreviewDownloadListener(context, holder, adapter);
		listeners.put(document.getHandle(), listener);
		File previewFile = new File(getPreviewFolder(context), document.getBase64Handle());
		megaApi.getPreview(document,  previewFile.getAbsolutePath(), listener);

		return previewCache.get(document.getHandle());
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
	
	private static void log(String log) {
		Util.log("PreviewUtils", log);
	}	
}
