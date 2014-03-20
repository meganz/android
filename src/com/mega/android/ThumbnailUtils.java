package com.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.mega.android.MegaBrowserGridAdapter.ViewHolderBrowserGrid;
import com.mega.android.MegaBrowserListAdapter.ViewHolderBrowserList;
import com.mega.android.MegaExplorerAdapter.ViewHolderExplorer;
import com.mega.android.MegaFullScreenImageAdapter.ViewHolderFullImage;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaListenerInterface;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.MegaUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

/*
 * Service to create thumbnails
 */
public class ThumbnailUtils {
	public static File thumbDir;
	private static int THUMBNAIL_SIZE = 120;
	public static ThumbnailCache thumbnailCache = new ThumbnailCache();
	public static ArrayList<Long> pendingThumbnails = new ArrayList<Long>();
	
	static HashMap<Long, ThumbnailDownloadListenerList> listenersList = new HashMap<Long, ThumbnailDownloadListenerList>();
	static HashMap<Long, ThumbnailDownloadListenerGrid> listenersGrid = new HashMap<Long, ThumbnailDownloadListenerGrid>();
	static HashMap<Long, ThumbnailDownloadListenerExplorer> listenersExplorer = new HashMap<Long, ThumbnailDownloadListenerExplorer>();
	static HashMap<Long, ThumbnailDownloadListenerFull> listenersFull = new HashMap<Long, ThumbnailDownloadListenerFull>();

	static class ThumbnailDownloadListenerList implements MegaRequestListenerInterface{
		Context context;
		ViewHolderBrowserList holder;
		MegaBrowserListAdapter adapter;
		
		ThumbnailDownloadListenerList(Context context, ViewHolderBrowserList holder, MegaBrowserListAdapter adapter){
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
			
			pendingThumbnails.remove(handle);
			
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
									holder.imageView.setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imageView.startAnimation(fadeInAnimation);
									adapter.notifyDataSetChanged();
									log("Thumbnail update");
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
	
	static class ThumbnailDownloadListenerGrid implements MegaRequestListenerInterface{
		Context context;
		ViewHolderBrowserGrid holder;
		MegaBrowserGridAdapter adapter;
		int numView;
		
		ThumbnailDownloadListenerGrid(Context context, ViewHolderBrowserGrid holder, MegaBrowserGridAdapter adapter, int numView){
			this.context = context;
			this.holder = holder;
			this.adapter = adapter;
			this.numView = numView;
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
			
			pendingThumbnails.remove(handle);
			
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
								if (numView == 1){
									if ((holder.document1 == handle)){
										holder.imageView1.setImageBitmap(bitmap);
										Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
										holder.imageView1.startAnimation(fadeInAnimation);
										adapter.notifyDataSetChanged();
										log("Thumbnail update");
									}
								}
								else if (numView == 2){
									if ((holder.document2 == handle)){
										holder.imageView2.setImageBitmap(bitmap);
										Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
										holder.imageView2.startAnimation(fadeInAnimation);
										adapter.notifyDataSetChanged();
										log("Thumbnail update");
									}
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
			
			log("Downloading thumbnail finished");
			final long handle = request.getNodeHandle();
			MegaNode node = api.getNodeByHandle(handle);
			
			pendingThumbnails.remove(handle);
			
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
									holder.imageView.setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imageView.startAnimation(fadeInAnimation);
									adapter.notifyDataSetChanged();
									log("Thumbnail update");
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
	
	static class ThumbnailDownloadListenerFull implements MegaRequestListenerInterface{
		Context context;
		ViewHolderFullImage holder;
		MegaFullScreenImageAdapter adapter;
		
		ThumbnailDownloadListenerFull(Context context, ViewHolderFullImage holder, MegaFullScreenImageAdapter adapter){
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
			
			pendingThumbnails.remove(handle);
			
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
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
			// TODO Auto-generated method stub
			
		}
	}
	
	/*
	 * Get thumbnail folder
	 */	
	public static File getThumbFolder(Context context) {
		if (thumbDir == null) {
			thumbDir = context.getDir("thumbnailsMEGA", 0);
		}
		return thumbDir;
	}
	
	public static Bitmap getThumbnailFromCache(MegaNode node){
		return thumbnailCache.get(node.getHandle());
	}
	
	public static Bitmap getThumbnailFromFolder(MegaNode node, Context context){
		File thumbDir = getThumbFolder(context);
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
	
	public static Bitmap getThumbnailFromMegaList(MegaNode document, Context context, ViewHolderBrowserList viewHolder, MegaApiAndroid megaApi, MegaBrowserListAdapter adapter){
		
		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
			log("the thumbnail is already downloaded or added to the list");
			return thumbnailCache.get(document.getHandle());
		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerList listener = new ThumbnailDownloadListenerList(context, viewHolder, adapter);
		listenersList.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}
	
	public static Bitmap getThumbnailFromMegaExplorer(MegaNode document, Context context, ViewHolderExplorer viewHolder, MegaApiAndroid megaApi, MegaExplorerAdapter adapter){
		
		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
			log("the thumbnail is already downloaded or added to the list");
			return thumbnailCache.get(document.getHandle());
		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerExplorer listener = new ThumbnailDownloadListenerExplorer(context, viewHolder, adapter);
		listenersExplorer.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}
	
	public static Bitmap getThumbnailFromMegaGrid(MegaNode document, Context context, ViewHolderBrowserGrid viewHolder, MegaApiAndroid megaApi, MegaBrowserGridAdapter adapter, int numView){
		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
			log("the thumbnail is already downloaded or added to the list");
			return thumbnailCache.get(document.getHandle());
		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerGrid listener = new ThumbnailDownloadListenerGrid(context, viewHolder, adapter, numView);
		listenersGrid.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
	}
	
	public static Bitmap getThumbnailFromMegaFull(MegaNode document, Context context, ViewHolderFullImage viewHolder, MegaApiAndroid megaApi, MegaFullScreenImageAdapter adapter){
		
		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
			log("the thumbnail is already downloaded or added to the list");
			return thumbnailCache.get(document.getHandle());
		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerFull listener = new ThumbnailDownloadListenerFull(context, viewHolder, adapter);
		listenersFull.put(document.getHandle(), listener);
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
	
	public static class ResizerParams {
		File file;
		MegaNode document;
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
			log("AttachPreviewStart");
			param = params[0];
			
			File thumbDir = getThumbFolder(context);
			thumbFile = new File(thumbDir, param.document.getBase64Handle()+".jpg");
			boolean thumbCreated = MegaUtils.createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onThumbnailGeneratedExplorer(megaApi, thumbFile, param.document, holder, adapter);
			}
		}
	}
	
	private static void onThumbnailGeneratedExplorer(MegaApiAndroid megaApi, File thumbFile, MegaNode document, ViewHolderExplorer holder, MegaExplorerAdapter adapter){
		log("onPreviewGenerated");
		//Tengo que mostrarla
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
		holder.imageView.setImageBitmap(bitmap);
		thumbnailCache.put(document.getHandle(), bitmap);
		adapter.notifyDataSetChanged();
		
		//Y ahora subirla
		megaApi.setThumbnail(document, thumbFile.getAbsolutePath(), new MegaListenerInterface() {
			
			@Override
			public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
				log("onTransferUpdate");
			}
			
			@Override
			public void onTransferTemporaryError(MegaApiJava api,
					MegaTransfer transfer, MegaError e) {
				log("onTransferTemporaryError");
				
			}
			
			@Override
			public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
				log("onTransferStart");				
			}
			
			@Override
			public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
					MegaError e) {
				log("onTransferFinish");
			}
			
			@Override
			public void onUsersUpdate(MegaApiJava api) {
				log("onUsersUpdate");				
			}
			
			@Override
			public void onReloadNeeded(MegaApiJava api) {
				log("onReloadNeeded");				
			}
			
			@Override
			public void onNodesUpdate(MegaApiJava api) {
				log("onNodesUpdate");
			}
			
			@Override
			public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
					MegaError e) {
				log("onRequestTemporaryError");
			}
			
			@Override
			public void onRequestStart(MegaApiJava api, MegaRequest request) {
				log("onRequestStart");
			}
			
			@Override
			public void onRequestFinish(MegaApiJava api, MegaRequest request,
					MegaError e) {
				log("onRequestFinish");				
			}
		});
		log("AttachThumbnailTask end");		
	}
	
	static class AttachThumbnailTaskGrid extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderBrowserGrid holder;
		MegaBrowserGridAdapter adapter;
		int numView;
		
		AttachThumbnailTaskGrid(Context context, MegaApiAndroid megaApi, ViewHolderBrowserGrid holder, MegaBrowserGridAdapter adapter, int numView)
		{
			this.context = context;
			this.megaApi = megaApi;
			this.holder = holder;
			this.adapter = adapter;
			this.thumbFile = null;
			this.param = null;
			this.numView = numView;
		}
		
		@Override
		protected Boolean doInBackground(ResizerParams... params) {
			log("AttachPreviewStart");
			param = params[0];
			
			File thumbDir = getThumbFolder(context);
			thumbFile = new File(thumbDir, param.document.getBase64Handle()+".jpg");
			boolean thumbCreated = MegaUtils.createThumbnail(param.file, thumbFile);
			
			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onThumbnailGeneratedGrid(megaApi, thumbFile, param.document, holder, adapter, numView);
			}
		}
	}
	
	private static void onThumbnailGeneratedGrid(MegaApiAndroid megaApi, File thumbFile, MegaNode document, ViewHolderBrowserGrid holder, MegaBrowserGridAdapter adapter, int numView){
		log("onPreviewGenerated");
		//Tengo que mostrarla
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
		if (numView == 1){
			holder.imageView1.setImageBitmap(bitmap);
		}
		else if (numView == 2){
			holder.imageView2.setImageBitmap(bitmap);
		}
		thumbnailCache.put(document.getHandle(), bitmap);
		adapter.notifyDataSetChanged();
		
		//Y ahora subirla
		megaApi.setThumbnail(document, thumbFile.getAbsolutePath(), new MegaListenerInterface() {
			
			@Override
			public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
				log("onTransferUpdate");
			}
			
			@Override
			public void onTransferTemporaryError(MegaApiJava api,
					MegaTransfer transfer, MegaError e) {
				log("onTransferTemporaryError");
				
			}
			
			@Override
			public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
				log("onTransferStart");				
			}
			
			@Override
			public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
					MegaError e) {
				log("onTransferFinish");
			}
			
			@Override
			public void onUsersUpdate(MegaApiJava api) {
				log("onUsersUpdate");				
			}
			
			@Override
			public void onReloadNeeded(MegaApiJava api) {
				log("onReloadNeeded");				
			}
			
			@Override
			public void onNodesUpdate(MegaApiJava api) {
				log("onNodesUpdate");
			}
			
			@Override
			public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
					MegaError e) {
				log("onRequestTemporaryError");
			}
			
			@Override
			public void onRequestStart(MegaApiJava api, MegaRequest request) {
				log("onRequestStart");
			}
			
			@Override
			public void onRequestFinish(MegaApiJava api, MegaRequest request,
					MegaError e) {
				log("onRequestFinish");				
			}
		});
		log("AttachThumbnailTask end");		
	}
	
	static class AttachThumbnailTaskList extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderBrowserList holder;
		MegaBrowserListAdapter adapter;
		
		AttachThumbnailTaskList(Context context, MegaApiAndroid megaApi, ViewHolderBrowserList holder, MegaBrowserListAdapter adapter)
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
			log("AttachPreviewStart");
			param = params[0];
			
			File thumbDir = getThumbFolder(context);
			thumbFile = new File(thumbDir, param.document.getBase64Handle()+".jpg");
			boolean thumbCreated = MegaUtils.createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onThumbnailGeneratedList(megaApi, thumbFile, param.document, holder, adapter);
			}
		}
	}
	
	private static void onThumbnailGeneratedList(MegaApiAndroid megaApi, File thumbFile, MegaNode document, ViewHolderBrowserList holder, MegaBrowserListAdapter adapter){
		log("onPreviewGenerated");
		//Tengo que mostrarla
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
		holder.imageView.setImageBitmap(bitmap);
		thumbnailCache.put(document.getHandle(), bitmap);
		adapter.notifyDataSetChanged();
		
		//Y ahora subirla
		megaApi.setThumbnail(document, thumbFile.getAbsolutePath(), new MegaListenerInterface() {
			
			@Override
			public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
				log("onTransferUpdate");
			}
			
			@Override
			public void onTransferTemporaryError(MegaApiJava api,
					MegaTransfer transfer, MegaError e) {
				log("onTransferTemporaryError");
				
			}
			
			@Override
			public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
				log("onTransferStart");				
			}
			
			@Override
			public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
					MegaError e) {
				log("onTransferFinish");
			}
			
			@Override
			public void onUsersUpdate(MegaApiJava api) {
				log("onUsersUpdate");				
			}
			
			@Override
			public void onReloadNeeded(MegaApiJava api) {
				log("onReloadNeeded");				
			}
			
			@Override
			public void onNodesUpdate(MegaApiJava api) {
				log("onNodesUpdate");
			}
			
			@Override
			public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
					MegaError e) {
				log("onRequestTemporaryError");
			}
			
			@Override
			public void onRequestStart(MegaApiJava api, MegaRequest request) {
				log("onRequestStart");
			}
			
			@Override
			public void onRequestFinish(MegaApiJava api, MegaRequest request,
					MegaError e) {
				log("onRequestFinish");				
			}
		});
		log("AttachThumbnailTask end");		
	}
	
	
	public static void createThumbnailList(Context context, MegaNode document, ViewHolderBrowserList holder, MegaApiAndroid megaApi, MegaBrowserListAdapter adapter){
		
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
			new AttachThumbnailTaskList(context, megaApi, holder, adapter).execute(params);
		} //Si no, no hago nada
		
	}

	public static void createThumbnailGrid(Context context, MegaNode document, ViewHolderBrowserGrid holder, MegaApiAndroid megaApi, MegaBrowserGridAdapter adapter, int numView){
		
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
			new AttachThumbnailTaskGrid(context, megaApi, holder, adapter, numView).execute(params);
		} //Si no, no hago nada
		
	}
	
	public static void createThumbnailExplorer(Context context, MegaNode document, ViewHolderExplorer holder, MegaApiAndroid megaApi, MegaExplorerAdapter adapter){
		
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
			new AttachThumbnailTaskExplorer(context, megaApi, holder, adapter).execute(params);
		} //Si no, no hago nada
		
	}
	
	private static void log(String log) {
		Util.log("ThumbnailUtils", log);
	}	
}
