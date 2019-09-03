package mega.privacy.android.app.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;
import android.view.Display;

import java.io.File;

import mega.privacy.android.app.PreviewCache;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;


public class PreviewUtils {
	
	public static File previewDir;
	public static PreviewCache previewCache = new PreviewCache();

	//10mb
	private static final long THRESHOLD = 10 * 1024 * 1024;

	/*
	 * Get preview folder
	 */	
	public static File getPreviewFolder(Context context) {
	    if(!isFileAvailable(previewDir)) {
            previewDir = getCacheFolder(context, PREVIEW_FOLDER);
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
	    Bitmap bmp = previewCache.get(node.getHandle());
	    if(bmp == null) {
            File previewDir = getPreviewFolder(context);
            File preview = new File(previewDir, node.getBase64Handle()+".jpg");
            if (preview.exists()){
                if (preview.length() > 0){
                    bmp = getBitmapForCache(preview, context);
                    if (bmp == null) {
                        preview.delete();
                    }
                    else{
                        previewCache.put(node.getHandle(), bmp);
                    }
                }
            }
        }
        return bmp;
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

    public static Bitmap getBitmapForCache(File bmpFile,Context context) {
        BitmapFactory.Options bOpts = new BitmapFactory.Options();
        bOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(bmpFile.getAbsolutePath(),bOpts);

        // set inSampleSize to avoid OOM
        int inSampleSize = 1;
        if (context instanceof Activity) {
            Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            //half of the screen size.
            inSampleSize = calculateInSampleSize(bOpts,size.x / 2,size.y / 2);
        }
        log("inSampleSize: " + inSampleSize);
        bOpts.inJustDecodeBounds = false;
        bOpts.inSampleSize = inSampleSize;
        log("PREVIEW_SIZE " + bmpFile.getAbsolutePath() + "____ " + bmpFile.length());
        return BitmapFactory.decodeFile(bmpFile.getAbsolutePath(),bOpts);
    }

    public static Bitmap getPreviewFromFolderFullImage(MegaNode node, Context context){
        Bitmap bmp = previewCache.get(node.getHandle());
        if(bmp == null) {
            File previewDir = getPreviewFolder(context);
            File preview = new File(previewDir, node.getBase64Handle()+".jpg");
            if (preview.exists()){
                if (preview.length() > 0){
                    bmp = getBitmapForCacheFullImage(preview, context);
                    if (bmp == null) {
                        preview.delete();
                    }
                    else{
                        previewCache.put(node.getHandle(), bmp);
                    }
                }
            }
        }
        return bmp;
    }

    public static Bitmap getBitmapForCacheFullImage(File bmpFile, Context context) {
	    if(!testAllocation()) {
            return null;
        } else {
	        return getBitmapForCache(bmpFile, context);
        }
    }

    public static boolean testAllocation() {
	    Runtime runtime = Runtime.getRuntime();
        long usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        long maxHeapSizeInMB = runtime.maxMemory() / 1048576L;
        long availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB;
        log("maxHeapSizeInMB " + maxHeapSizeInMB + " availHeapSizeInMB is " + availHeapSizeInMB + " usedMemInMB is" + usedMemInMB);
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory()) > THRESHOLD;
    }

	/*code from developer.android, https://developer.android.com/topic/performance/graphics/load-bitmap.html*/
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
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
