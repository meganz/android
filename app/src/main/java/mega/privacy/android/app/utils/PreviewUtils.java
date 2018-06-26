package mega.privacy.android.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;

import java.io.File;

import mega.privacy.android.app.PreviewCache;
import nz.mega.sdk.MegaNode;


public class PreviewUtils {
	
	public static File previewDir;
	public static PreviewCache previewCache = new PreviewCache();

	/*
	 * Get preview folder
	 */	
	public static File getPreviewFolder(Context context) {
		if (previewDir == null) {
			if (context.getExternalCacheDir() != null){
				previewDir = new File (context.getExternalCacheDir(), "previewsMEGA");
			}
			else{
				previewDir = context.getDir("previewsMEGA", 0);
			}
		}

		if (previewDir != null){
			previewDir.mkdirs();
		}

		return previewDir;
	}

	public static Bitmap getPreviewFromCache(MegaNode node){
		return previewCache.get(node.getHandle());
	}
	
	public static Bitmap getPreviewFromCache(long handle){
		return previewCache.get(handle);
	}
	
	public static void setPreviewCache(long handle, Bitmap bitmap){
		previewCache.put(handle, bitmap);
	}
	
	public static Bitmap getPreviewFromFolder(MegaNode node, Context context){
		File previewDir = getPreviewFolder(context);
		File preview = new File(previewDir, node.getBase64Handle()+".jpg");
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

	public static Bitmap createVideoPreview(String filePath, int kind) {
		Bitmap bitmap = null;
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		try {
			retriever.setDataSource(filePath);
			bitmap = retriever.getFrameAtTime(-1);
		} catch (IllegalArgumentException ex) {
			// Assume this is a corrupt video file
		} catch (RuntimeException ex) {
			// Assume this is a corrupt video file.
		} finally {
			try {
				retriever.release();
			} catch (RuntimeException ex) {
				// Ignore failures while cleaning up.
			}
		}

		if (bitmap == null) return null;

		if (kind == MediaStore.Images.Thumbnails.FULL_SCREEN_KIND) {
			// Scale down the bitmap if it's too large.
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			int max = Math.max(width, height);
			if (max > 1000) {
				float scale = 1000f / max;
				int w = Math.round(scale * width);
				int h = Math.round(scale * height);
				bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
			}
		}
		return bitmap;
	}

	/*
	 * Load Bitmap for cache
	 */
	public static Bitmap getBitmapForCache(File bmpFile, Context context) {
		BitmapFactory.Options bOpts = new BitmapFactory.Options();
		bOpts.inPurgeable = true;
		bOpts.inInputShareable = true;
		log("PREVIEW_SIZE " + bmpFile.getAbsolutePath() + "____ " + bmpFile.length());
		Bitmap bmp = BitmapFactory.decodeFile(bmpFile.getAbsolutePath(), bOpts);
		return bmp;
	}

	public static Bitmap resizeBitmapUpload(Bitmap bitmap, int width, int height) {
		Bitmap resizeBitmap = null;
		int resizeWidth, resizeHeight;
		float resize;

		if (width > height) {
			resize = (float) 1000 / width;
		}
		else {
			resize = (float) 1000 / height;
		}
		resizeWidth = (int) (width * resize);
		resizeHeight = (int) (height * resize);

		resizeBitmap = Bitmap.createScaledBitmap(bitmap, resizeWidth, resizeHeight, false);

		return resizeBitmap;
	}
	
	private static void log(String log) {
		Util.log("PreviewUtils", log);
	}	
}
