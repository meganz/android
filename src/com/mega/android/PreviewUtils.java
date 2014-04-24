package com.mega.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.os.StatFs;
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
import com.mega.sdk.MegaTransfer;
import com.mega.sdk.MegaTransferListenerInterface;
import com.mega.sdk.MegaUtils;

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
		log("TAM_IMAGEN_PREVIA " + bmpFile.getAbsolutePath() + "____ " + bmpFile.length());
		Bitmap bmp = BitmapFactory.decodeFile(bmpFile.getAbsolutePath(), bOpts);
		return bmp;
	}
	
	private static void log(String log) {
		Util.log("PreviewUtils", log);
	}	
}
