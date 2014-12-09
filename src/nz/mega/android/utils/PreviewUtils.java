package nz.mega.android.utils;

import java.io.File;

import nz.mega.android.PreviewCache;
import nz.mega.sdk.MegaNode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


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
				previewDir.mkdirs();
			}
			else{
				previewDir = context.getDir("previewsMEGA", 0);
			}
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
	
	private static void log(String log) {
		Util.log("PreviewUtils", log);
	}	
}
