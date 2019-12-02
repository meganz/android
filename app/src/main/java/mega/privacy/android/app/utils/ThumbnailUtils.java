package mega.privacy.android.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

import mega.privacy.android.app.ThumbnailCache;
import mega.privacy.android.app.lollipop.adapters.MegaFullScreenImageAdapterLollipop;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;

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
}
