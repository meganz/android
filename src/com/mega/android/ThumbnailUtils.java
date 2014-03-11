package com.mega.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.mega.android.MegaBrowserGridAdapter.ViewHolderBrowserGrid;
import com.mega.android.MegaBrowserListAdapter.ViewHolderBrowserList;
import com.mega.android.MegaFullScreenImageAdapter.ViewHolderFullImage;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaListener;
import com.mega.sdk.MegaListenerInterface;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.MegaTransfer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

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
					File thumb = new File(thumbDir, node.getBase64Handle());
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
					File thumb = new File(thumbDir, node.getBase64Handle());
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
					File thumb = new File(thumbDir, node.getBase64Handle());
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
		File thumb = new File(thumbDir, node.getBase64Handle());
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
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle());
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
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle());
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
		File thumbFile = new File(getThumbFolder(context), document.getBase64Handle());
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
	
	/*
	 * Check is thumbnail could be created from the document
	 */
	public static boolean isPossibleThumbnail(MegaNode document) {
		if(document == null) return false;
		
		if (document.getSize() > 10l * 1024 * 1024) {
			return false;
		}
		return MimeType.typeForName(document.getName()).isImage();
	}
	
	/*
	 * Add local cached file for later thumbnail creation
	 */
	public static boolean addLocalFile(MegaApiAndroid megaApi, Context context, MegaNode document,	File location) {
		log("addLocalFile");
		
		if (!isPossibleThumbnail(document)) {
			log("no image");
			return false;
		}
		
		if ((location != null) && location.exists()	&& location.length() == document.getSize() && !document.hasThumbnail()) 
		{
			log("Attaching new thumbnail");
			ResizerParams params = new ResizerParams();
			params.document = document;
			params.file = location;
			new AttachThumbnailTask(context, megaApi).execute(params);
		}
		
		
		return true;
		
	}
	
	static File transformThumbnail(File resultFile, Bitmap bitmap, MegaNode document)
	{
		int srcWidth = bitmap.getWidth();
		int srcHeight = bitmap.getHeight();

		if (srcWidth == 0 || srcHeight == 0) {
			log("width or height is null");
			return null;
		}

		double scale = (double) THUMBNAIL_SIZE
				/ Math.min(srcWidth, srcHeight);
		int translateX = 0;
		int translateY = 0;
		if (srcHeight > srcWidth) {
			translateY = (srcHeight - srcWidth) / -3;
		} else {
			translateX = (srcWidth - srcHeight) / -2;
		}

		log(String.format("Translate x: %d y: %d ; Source w: %d h: %d",
				translateX, translateY, srcWidth, srcHeight));
		Bitmap.Config conf = Bitmap.Config.ARGB_8888;
		Bitmap scaled = Bitmap.createBitmap(THUMBNAIL_SIZE, THUMBNAIL_SIZE,
				conf);
		Canvas canvas = new Canvas(scaled);
		canvas.setDensity(bitmap.getDensity());
		Matrix matrix = new Matrix();
		matrix.preTranslate(translateX, translateY);
		matrix.postScale((float) scale, (float) scale);
		canvas.drawBitmap(bitmap, matrix, null);
		FileOutputStream stream;
		try {
			stream = new FileOutputStream(resultFile);
			boolean success = scaled.compress(Bitmap.CompressFormat.JPEG,
					85, stream);
			if (!success) {
				resultFile.delete();
				return null;
			}
			File thumbFile = new File(resultFile.getParentFile(), document.getBase64Handle());
			resultFile.renameTo(thumbFile);
			return thumbFile;
		} catch (FileNotFoundException e) {
			log("file not found?");
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * Change image orientation based on EXIF image data
	 */
	static Bitmap fixExifOrientation(File file, Bitmap bitmap) {
		int orientation = ExifInterface.ORIENTATION_UNDEFINED;
		try {
			ExifInterface exif = new ExifInterface(file.getAbsolutePath());
			orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION, orientation);
		} catch (IOException e) {
			// no exif data for this file
		}
		int rotation = 0;
		if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
			rotation = 90;
		} else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
			rotation = 180;
		} else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
			rotation = 270;
		}
		if (rotation == 0) {
			log("no rotation");
			return bitmap;
		} else {
			log("rotating " + rotation);
			Matrix matrix = new Matrix();
			matrix.postRotate(rotation);
			return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
					bitmap.getHeight(), matrix, true);
		}
	}
	
	/*
	 * Decode/resize Bitmap for require size
	 */
	static Bitmap decodeFile(File f, int REQUIRED_SIZE) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// Find the correct scale value. It should be the power of 2.
			int scale = 1;
			while (o.outWidth / scale / 2 >= REQUIRED_SIZE
					&& o.outHeight / scale / 2 >= REQUIRED_SIZE)
				scale *= 2;

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null,
					o2);
		} catch (FileNotFoundException e) {
			//log("not found :(");
		}
		return null;
	}
	
	public static class ResizerParams {
		File file;
		MegaNode document;
	}
	
	static class AttachThumbnailTask extends AsyncTask<ResizerParams, Void, Boolean>
	{
		Context context;
		MegaApiAndroid megaApi;
		File thumbFile;
		ResizerParams param;
		
		AttachThumbnailTask(Context context, MegaApiAndroid megaApi)
		{
			this.context = context;
			this.megaApi = megaApi;
			this.thumbFile = null;
			this.param = null;
		}
		
		@Override
		protected Boolean doInBackground(ResizerParams... params) {
			log("AttachThumbnailStart");
			param = params[0];
			File resultFile = new File(getThumbFolder(context), param.document.getBase64Handle() + ".tmp");
			if (resultFile.exists()) {
				resultFile.delete();
			}
			try {
				resultFile.createNewFile();
			} catch (IOException e) {
				return false;
			}

			Bitmap bitmap = decodeFile(param.file, THUMBNAIL_SIZE * 2);
			if (bitmap == null) {
				resultFile.delete();
				return false;
			}
			
			bitmap = fixExifOrientation(param.file, bitmap);

			if (Util.isLocalTemp(context, param.file)) {
				param.file.delete();
			}

			if (bitmap == null) {
				resultFile.delete();
				return false;
			}
			int srcWidth = bitmap.getWidth();
			int srcHeight = bitmap.getHeight();

			if (srcWidth == 0 || srcHeight == 0) {
				log("width or height is null");
				resultFile.delete();
				return true;
			}
			
			thumbFile = transformThumbnail(resultFile, bitmap, param.document);
			if(thumbFile == null) return false;
			
			if(megaApi==null) return false;
			
			thumbnailCache.remove(param.document.getHandle());
			
			return true;
		}

		@Override
		protected void onPostExecute(Boolean shouldContinueObject) {
			onThumbnailGenerated(megaApi, thumbFile, param.document);
		}
	}
	
	private static void onThumbnailGenerated(MegaApiAndroid megaApi, File thumbFile, MegaNode document){
		log("calling setThumbnail: " + thumbFile.getAbsolutePath() + " Size: " + thumbFile.length());
		
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
	
	private static void log(String log) {
		Util.log("ThumbnailUtils", log);
	}	
}
