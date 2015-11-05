package nz.mega.android.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import nz.mega.android.MegaBrowserGridAdapter;
import nz.mega.android.MegaBrowserListAdapter;
import nz.mega.android.MegaBrowserNewGridAdapter;
import nz.mega.android.MegaExplorerAdapter;
import nz.mega.android.MegaFullScreenImageAdapter;
import nz.mega.android.MegaPhotoSyncGridAdapter;
import nz.mega.android.MegaPhotoSyncListAdapter;
import nz.mega.android.MimeTypeList;
import nz.mega.android.R;
import nz.mega.android.ThumbnailCache;
import nz.mega.android.MegaBrowserGridAdapter.ViewHolderBrowserGrid;
import nz.mega.android.MegaBrowserNewGridAdapter.ViewHolderBrowserNewGrid;
import nz.mega.android.MegaExplorerAdapter.ViewHolderExplorer;
import nz.mega.android.MegaFullScreenImageAdapter.ViewHolderFullImage;
import nz.mega.android.NavigationDrawerAdapter.ViewHolderNavigationDrawer;
import nz.mega.android.lollipop.MegaBrowserLollipopAdapter.ViewHolderBrowser;
import nz.mega.android.lollipop.MegaBrowserLollipopAdapter;
import nz.mega.android.lollipop.MegaExplorerLollipopAdapter;
import nz.mega.android.lollipop.MegaPhotoSyncGridAdapterLollipop;
import nz.mega.android.lollipop.MegaPhotoSyncListAdapterLollipop;
import nz.mega.android.lollipop.MegaExplorerLollipopAdapter.ViewHolderExplorerLollipop;
import nz.mega.android.lollipop.MegaPhotoSyncGridAdapterLollipop.ViewHolderPhotoSyncGrid;
import nz.mega.android.lollipop.MegaPhotoSyncListAdapterLollipop.ViewHolderPhotoSyncList;
import nz.mega.android.lollipop.MegaTransfersLollipopAdapter;
import nz.mega.android.lollipop.MegaTransfersLollipopAdapter.ViewHolderTransfer;
import nz.mega.android.lollipop.providers.MegaProviderLollipopAdapter;
import nz.mega.android.lollipop.providers.MegaProviderLollipopAdapter.ViewHolderLollipopProvider;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUtilsAndroid;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;


/*
 * Service to create thumbnails
 */
public class ThumbnailUtilsLollipop {
	public static File thumbDir;
	private static int THUMBNAIL_SIZE = 120;
	public static ThumbnailCache thumbnailCache = new ThumbnailCache();
	public static ThumbnailCache thumbnailCachePath = new ThumbnailCache(1);
//	public static ArrayList<Long> pendingThumbnails = new ArrayList<Long>();
	
	static HashMap<Long, ThumbnailDownloadListenerListBrowser> listenersList = new HashMap<Long, ThumbnailDownloadListenerListBrowser>();
	static HashMap<Long, ThumbnailDownloadListenerGrid> listenersGrid = new HashMap<Long, ThumbnailDownloadListenerGrid>();
	static HashMap<Long, ThumbnailDownloadListenerNewGrid> listenersNewGrid = new HashMap<Long, ThumbnailDownloadListenerNewGrid>();
	static HashMap<Long, ThumbnailDownloadListenerExplorerLollipop> listenersExplorerLollipop = new HashMap<Long, ThumbnailDownloadListenerExplorerLollipop>();
	static HashMap<Long, ThumbnailDownloadListenerProvider> listenersProvider = new HashMap<Long, ThumbnailDownloadListenerProvider>();
	static HashMap<Long, ThumbnailDownloadListenerFull> listenersFull = new HashMap<Long, ThumbnailDownloadListenerFull>();
	static HashMap<Long, ThumbnailDownloadListenerTransfer> listenersTransfer = new HashMap<Long, ThumbnailDownloadListenerTransfer>();
	static HashMap<Long, ThumbnailDownloadListenerPhotoSyncList> listenersPhotoSyncList = new HashMap<Long, ThumbnailDownloadListenerPhotoSyncList>();
	static HashMap<Long, ThumbnailDownloadListenerPhotoSyncGrid> listenersPhotoSyncGrid = new HashMap<Long, ThumbnailDownloadListenerPhotoSyncGrid>();

	
	static class ThumbnailDownloadListenerPhotoSyncList implements MegaRequestListenerInterface{
		Context context;
		ViewHolderPhotoSyncList holder;
		MegaPhotoSyncListAdapterLollipop adapter;
		
		ThumbnailDownloadListenerPhotoSyncList(Context context, ViewHolderPhotoSyncList holder, MegaPhotoSyncListAdapterLollipop adapter){
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
	
	static class ThumbnailDownloadListenerPhotoSyncGrid implements MegaRequestListenerInterface{
		Context context;
		ViewHolderPhotoSyncGrid holder;
		MegaPhotoSyncGridAdapterLollipop adapter;
		int numView;
		
		ThumbnailDownloadListenerPhotoSyncGrid(Context context, ViewHolderPhotoSyncGrid holder, MegaPhotoSyncGridAdapterLollipop adapter, int numView){
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
								
								if ((holder.documents.get(numView) == handle)){
									holder.imageViews.get(numView).setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imageViews.get(numView).startAnimation(fadeInAnimation);
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
	
	static class ThumbnailDownloadListenerListBrowser implements MegaRequestListenerInterface{
		Context context;
		ViewHolderBrowser holder;
		MegaBrowserLollipopAdapter adapter;
		
		ThumbnailDownloadListenerListBrowser(Context context, ViewHolderBrowser holder, MegaBrowserLollipopAdapter adapter){
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
	
	static class ThumbnailDownloadListenerNewGrid implements MegaRequestListenerInterface{
		Context context;
		ViewHolderBrowserNewGrid holder;
		MegaBrowserNewGridAdapter adapter;
		int numView;
		
		ThumbnailDownloadListenerNewGrid(Context context, ViewHolderBrowserNewGrid holder, MegaBrowserNewGridAdapter adapter, int numView){
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
								if (holder.documents.get(numView) == handle){
									holder.imageViews.get(numView).setImageBitmap(bitmap);
									Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
									holder.imageViews.get(numView).startAnimation(fadeInAnimation);
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
	
	static class ThumbnailDownloadListenerExplorerLollipop implements MegaRequestListenerInterface{
		Context context;
		ViewHolderExplorerLollipop holder;
		MegaExplorerLollipopAdapter adapter;
		
		ThumbnailDownloadListenerExplorerLollipop(Context context, ViewHolderExplorerLollipop holder, MegaExplorerLollipopAdapter adapter){
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
	
	static class ThumbnailDownloadListenerProvider implements MegaRequestListenerInterface{
		Context context;
		ViewHolderLollipopProvider holder;
		MegaProviderLollipopAdapter adapter;
		
		ThumbnailDownloadListenerProvider(Context context, ViewHolderLollipopProvider holder, MegaProviderLollipopAdapter adapter){
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
	
	static class ThumbnailDownloadListenerTransfer implements MegaRequestListenerInterface{
		Context context;
		ViewHolderTransfer holder;
		MegaTransfersLollipopAdapter adapter;
		
		ThumbnailDownloadListenerTransfer(Context context, ViewHolderTransfer holder, MegaTransfersLollipopAdapter adapter){
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
		if (thumbDir == null) {
			if (context.getExternalCacheDir() != null){
				thumbDir = new File (context.getExternalCacheDir(), "thumbnailsMEGA");
				thumbDir.mkdirs();
			}
			else{
				thumbDir = context.getDir("thumbnailsMEGA", 0);
			}
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
	
	public static Bitmap getThumbnailFromMegaList(MegaNode document, Context context, ViewHolderBrowser viewHolder, MegaApiAndroid megaApi, MegaBrowserLollipopAdapter adapter){
		
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerListBrowser listener = new ThumbnailDownloadListenerListBrowser(context, viewHolder, adapter);
		listenersList.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		log("Lo descargare aqui: " + thumbFile.getAbsolutePath());
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}
	
	public static Bitmap getThumbnailFromMegaPhotoSyncList(MegaNode document, Context context, ViewHolderPhotoSyncList viewHolder, MegaApiAndroid megaApi, MegaPhotoSyncListAdapterLollipop adapter){
		
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerPhotoSyncList listener = new ThumbnailDownloadListenerPhotoSyncList(context, viewHolder, adapter);
		listenersPhotoSyncList.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		log("Lo descargare aqui: " + thumbFile.getAbsolutePath());
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}
	
	public static Bitmap getThumbnailFromMegaTransfer(MegaNode document, Context context, ViewHolderTransfer viewHolder, MegaApiAndroid megaApi, MegaTransfersLollipopAdapter adapter){
		
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerTransfer listener = new ThumbnailDownloadListenerTransfer(context, viewHolder, adapter);
		listenersTransfer.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		log("Lo descargare aqui: " + thumbFile.getAbsolutePath());
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}
	
	public static Bitmap getThumbnailFromMegaExplorerLollipop(MegaNode document, Context context, ViewHolderExplorerLollipop viewHolder, MegaApiAndroid megaApi, MegaExplorerLollipopAdapter adapter){
		
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerExplorerLollipop listener = new ThumbnailDownloadListenerExplorerLollipop(context, viewHolder, adapter);
		listenersExplorerLollipop.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}
	
	public static Bitmap getThumbnailFromMegaProvider(MegaNode document, Context context, ViewHolderLollipopProvider viewHolder, MegaApiAndroid megaApi, MegaProviderLollipopAdapter adapter){
		
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerProvider listener = new ThumbnailDownloadListenerProvider(context, viewHolder, adapter);
		listenersProvider.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
		
	}
	
	public static Bitmap getThumbnailFromMegaGrid(MegaNode document, Context context, ViewHolderBrowserGrid viewHolder, MegaApiAndroid megaApi, MegaBrowserGridAdapter adapter, int numView){
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerGrid listener = new ThumbnailDownloadListenerGrid(context, viewHolder, adapter, numView);
		listenersGrid.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
	}
	
	public static Bitmap getThumbnailFromMegaNewGrid(MegaNode document, Context context, ViewHolderBrowserNewGrid viewHolder, MegaApiAndroid megaApi, MegaBrowserNewGridAdapter adapter, int numView){
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerNewGrid listener = new ThumbnailDownloadListenerNewGrid(context, viewHolder, adapter, numView);
		listenersNewGrid.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
	}
	
	public static Bitmap getThumbnailFromMegaPhotoSyncGrid(MegaNode document, Context context, ViewHolderPhotoSyncGrid viewHolder, MegaApiAndroid megaApi, MegaPhotoSyncGridAdapterLollipop adapter, int numView){
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
		ThumbnailDownloadListenerPhotoSyncGrid listener = new ThumbnailDownloadListenerPhotoSyncGrid(context, viewHolder, adapter, numView);
		listenersPhotoSyncGrid.put(document.getHandle(), listener);
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle()+".jpg");
		megaApi.getThumbnail(document,  thumbFile.getAbsolutePath(), listener);
		
		return thumbnailCache.get(document.getHandle());
	}
	
	public static Bitmap getThumbnailFromMegaFull(MegaNode document, Context context, ViewHolderFullImage viewHolder, MegaApiAndroid megaApi, MegaFullScreenImageAdapter adapter){
		
//		if (pendingThumbnails.contains(document.getHandle()) || !document.hasThumbnail()){
//			log("the thumbnail is already downloaded or added to the list");
//			return thumbnailCache.get(document.getHandle());
//		}
		
		if (!Util.isOnline(context)){
			return thumbnailCache.get(document.getHandle());
		}
		
//		pendingThumbnails.add(document.getHandle());
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
	
	static class AttachThumbnailTaskExplorerLollipop extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderExplorerLollipop holder;
		MegaExplorerLollipopAdapter adapter;
		
		AttachThumbnailTaskExplorerLollipop(Context context, MegaApiAndroid megaApi, ViewHolderExplorerLollipop holder, MegaExplorerLollipopAdapter adapter)
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
			boolean thumbCreated = MegaUtilsAndroid.createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onThumbnailGeneratedExplorerLollipop(megaApi, thumbFile, param.document, holder, adapter);
			}
		}
	}
	
	static class AttachThumbnailTaskProviderLollipop extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderLollipopProvider holder;
		MegaProviderLollipopAdapter adapter;
		
		AttachThumbnailTaskProviderLollipop(Context context, MegaApiAndroid megaApi, ViewHolderLollipopProvider holder, MegaProviderLollipopAdapter adapter)
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
			boolean thumbCreated = MegaUtilsAndroid.createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
//				onThumbnailGeneratedExplorerLollipop(megaApi, thumbFile, param.document, holder, adapter);
			}
		}
	}	
	
	private static void onThumbnailGeneratedExplorerLollipop(MegaApiAndroid megaApi, File thumbFile, MegaNode document, ViewHolderExplorerLollipop holder, MegaExplorerLollipopAdapter adapter){
		log("onPreviewGenerated");
		//Tengo que mostrarla
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
		holder.imageView.setImageBitmap(bitmap);
		thumbnailCache.put(document.getHandle(), bitmap);
		adapter.notifyDataSetChanged();
		
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
			boolean thumbCreated = MegaUtilsAndroid.createThumbnail(param.file, thumbFile);
			
			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onThumbnailGeneratedGrid(megaApi, thumbFile, param.document, holder, adapter, numView);
			}
		}
	}
	
	static class AttachThumbnailTaskNewGrid extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderBrowserNewGrid holder;
		MegaBrowserNewGridAdapter adapter;
		int numView;
		
		AttachThumbnailTaskNewGrid(Context context, MegaApiAndroid megaApi, ViewHolderBrowserNewGrid holder, MegaBrowserNewGridAdapter adapter, int numView)
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
			boolean thumbCreated = MegaUtilsAndroid.createThumbnail(param.file, thumbFile);
			
			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onThumbnailGeneratedNewGrid(megaApi, thumbFile, param.document, holder, adapter, numView);
			}
		}
	}
	
	private static void onThumbnailGeneratedNewGrid(MegaApiAndroid megaApi, File thumbFile, MegaNode document, ViewHolderBrowserNewGrid holder, MegaBrowserNewGridAdapter adapter, int numView){
		log("onPreviewGenerated");
		//Tengo que mostrarla
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
		holder.imageViews.get(numView).setImageBitmap(bitmap);
		
		thumbnailCache.put(document.getHandle(), bitmap);
		adapter.notifyDataSetChanged();
		
		log("AttachThumbnailTask end");		
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
		
		log("AttachThumbnailTask end");		
	}
	
	static class AttachThumbnailTaskPhotoSyncList extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderPhotoSyncList holder;
		MegaPhotoSyncListAdapterLollipop adapter;
		
		AttachThumbnailTaskPhotoSyncList(Context context, MegaApiAndroid megaApi, ViewHolderPhotoSyncList holder, MegaPhotoSyncListAdapterLollipop adapter)
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
			boolean thumbCreated = MegaUtilsAndroid.createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onThumbnailGeneratedPhotoSyncList(megaApi, thumbFile, param.document, holder, adapter);
			}
		}
	}
	
	private static void onThumbnailGeneratedPhotoSyncList(MegaApiAndroid megaApi, File thumbFile, MegaNode document, ViewHolderPhotoSyncList holder, MegaPhotoSyncListAdapterLollipop adapter){
		log("onPreviewGenerated");
		//Tengo que mostrarla
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
		holder.imageView.setImageBitmap(bitmap);
		thumbnailCache.put(document.getHandle(), bitmap);
		adapter.notifyDataSetChanged();
		
		log("AttachThumbnailTask end");		
	}
	
	static class AttachThumbnailTaskList extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		ViewHolderBrowser holder;
		MegaBrowserLollipopAdapter adapter;
		
		AttachThumbnailTaskList(Context context, MegaApiAndroid megaApi, ViewHolderBrowser holder, MegaBrowserLollipopAdapter adapter)
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
			boolean thumbCreated = MegaUtilsAndroid.createThumbnail(param.file, thumbFile);

			return thumbCreated;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			if (shouldContinueObject){
				onThumbnailGeneratedList(megaApi, thumbFile, param.document, holder, adapter);
			}
		}
	}	
		
	private static void onThumbnailGeneratedList(MegaApiAndroid megaApi, File thumbFile, MegaNode document, ViewHolderBrowser holder, MegaBrowserLollipopAdapter adapter){
		log("onPreviewGenerated");
		//Tengo que mostrarla
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
		holder.imageView.setImageBitmap(bitmap);
		thumbnailCache.put(document.getHandle(), bitmap);
		adapter.notifyDataSetChanged();
		
		log("AttachThumbnailTask end");		
	}
	
	public static void createThumbnailPhotoSyncList(Context context, MegaNode document, ViewHolderPhotoSyncList holder, MegaApiAndroid megaApi, MegaPhotoSyncListAdapterLollipop adapter){
		
		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
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
			new AttachThumbnailTaskPhotoSyncList(context, megaApi, holder, adapter).execute(params);
		} //Si no, no hago nada
		
	}
	
	public static void createThumbnailList(Context context, MegaNode document, ViewHolderBrowser holder, MegaApiAndroid megaApi, MegaBrowserLollipopAdapter adapter){
		
		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
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
		
		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
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
	
	public static void createThumbnailNewGrid(Context context, MegaNode document, ViewHolderBrowserNewGrid holder, MegaApiAndroid megaApi, MegaBrowserNewGridAdapter adapter, int numView){
		
		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
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
			new AttachThumbnailTaskNewGrid(context, megaApi, holder, adapter, numView).execute(params);
		} //Si no, no hago nada
		
	}
	
	public static void createThumbnailExplorerLollipop(Context context, MegaNode document, ViewHolderExplorerLollipop holder, MegaApiAndroid megaApi, MegaExplorerLollipopAdapter adapter){
		
		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
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
			new AttachThumbnailTaskExplorerLollipop(context, megaApi, holder, adapter).execute(params);
		} //Si no, no hago nada
		
	}
	
	public static void createThumbnailProviderLollipop(Context context, MegaNode document, ViewHolderLollipopProvider holder, MegaApiAndroid megaApi, MegaProviderLollipopAdapter adapter){
		
		if (!MimeTypeList.typeForName(document.getName()).isImage()) {
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
			new AttachThumbnailTaskProviderLollipop(context, megaApi, holder, adapter).execute(params);
		} //Si no, no hago nada
		
	}
	
	private static void log(String log) {
		Util.log("ThumbnailUtils", log);
	}	
}
