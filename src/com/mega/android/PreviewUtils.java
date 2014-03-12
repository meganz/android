package com.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.mega.android.MegaFullScreenImageAdapter.ViewHolderFullImage;
import com.mega.android.ThumbnailUtils.ResizerParams;
import com.mega.android.ThumbnailUtils.ThumbnailDownloadListenerList;
import com.mega.components.TouchImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaUtils;

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
//									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
//									holder.imgDisplay.startAnimation(fadeInAnimation);
									holder.progressBar.setVisibility(View.GONE);
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
	
	public static void createPreview (Context context, MegaNode document, ViewHolderFullImage holder, MegaApiAndroid megaApi, MegaFullScreenImageAdapter adapter){
		
		if (!MimeType.typeForName(document.getName()).isImage()) {
			log("no image");
			return;
		}
		
		String localPath = Util.getLocalFile(context, document.getName(), document.getSize(), null); //if file already exists returns != null
		if(localPath != null) //Si la tengo en el sistema de ficheros
		{
			log("localPath no es nulo: " + localPath);
			ResizerParams params = new ResizerParams();
			params.document = document;
			params.file = new File(localPath);
			new AttachPreviewTask(context, megaApi, holder, adapter).execute(params);
		}
		else{	//Si no, me toca descargármela
			
		}
	}
	
	public static class ResizerParams {
		File file;
		MegaNode document;
	}
	
	static class AttachPreviewTask extends AsyncTask<ResizerParams, Void, Boolean>{
		
		Context context;
		MegaApiAndroid megaApi;
		File previewFile;
		ResizerParams param;
		ViewHolderFullImage holder;
		MegaFullScreenImageAdapter adapter;
		
		AttachPreviewTask(Context context, MegaApiAndroid megaApi, ViewHolderFullImage holder, MegaFullScreenImageAdapter adapter)
		{
			this.context = context;
			this.megaApi = megaApi;
			this.holder = holder;
			this.adapter = adapter;
			this.previewFile = null;
			this.param = null;
		}
		
		@Override
		protected Boolean doInBackground(ResizerParams... params) {
			log("AttachPreviewStart");
			param = params[0];
			
			File previewDir = getPreviewFolder(context);
			previewFile = new File(previewDir, param.document.getBase64Handle());
			boolean previewCreated = MegaUtils.createPreview(param.file, previewFile);
			
			return previewCreated;
		}
		
		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onPreviewGenerated(megaApi, previewFile, param.document, holder, adapter);
			}
		}
	}
	
	private static void onPreviewGenerated(MegaApiAndroid megaApi, File previewFile, MegaNode document, ViewHolderFullImage holder, MegaFullScreenImageAdapter adapter){
		log("onPreviewGenerated");
		//Tengo que mostrarla
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(previewFile.getAbsolutePath(), options);
		holder.imgDisplay.setImageBitmap(bitmap);
		previewCache.put(document.getHandle(), bitmap);
		holder.progressBar.setVisibility(View.GONE);
		adapter.notifyDataSetChanged();
		
		//Y ahora subirla
		megaApi.setPreview(document, previewFile.getAbsolutePath());		
	}
	
	private static void log(String log) {
		Util.log("PreviewUtils", log);
	}	
}
