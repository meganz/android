package com.mega.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.mega.sdk.MegaError;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.Formatter;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import android.widget.Toast;

public class Util {
	
	public static float dpWidthAbs = 360;
	public static float dpHeightAbs = 592;
	
	public static double percScreenLogin = 0.596283784; //The dimension of the grey zone (Login and Tour)
	public static double percScreenLoginReturning = 0.8;
	
	// Debug flag to enable logging and some other things
	public static boolean DEBUG = true;
	
	public static String offlineDIR = "MEGA/MEGA Offline";  
	
	/*
	 * Create progress dialog helper
	 */
	public static ProgressDialog createProgress(Context context, String message) {
		ProgressDialog progress = new ProgressDialog(context);
		progress.setMessage(message);
		progress.setCancelable(false);
		progress.setCanceledOnTouchOutside(false);
		return progress;
	}
	
	/*
	 * Create progress dialog with resId
	 */
	public static ProgressDialog createProgress(Context context, int stringResId) {
		return createProgress(context, context.getString(stringResId));
	}
	
	public static void showErrorAlertDialogFinish(MegaError error, Activity activity) {
		showErrorAlertDialog(error.getErrorString(), true, activity);
	}
	
	/*
	 * Build error dialog
	 * @param message Message to display
	 * @param finish Should activity finish after dialog dismis
	 * @param activity Source activity
	 */
	public static void showErrorAlertDialog(String message, final boolean finish, final Activity activity){
		if(activity == null){
			return;
		}
		
		try{ 
			AlertDialog.Builder dialogBuilder = getCustomAlertBuilder(activity, "Error", message, null);
			dialogBuilder.setPositiveButton(
				activity.getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						if (finish) {
							activity.finish();
						}
					}
				});
			dialogBuilder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (finish) {
						activity.finish();
					}
				}
			});
		
		
			AlertDialog dialog = dialogBuilder.create();
			dialog.show(); 
		}
		catch(Exception ex){
			Util.showToast(activity, message); 
		}
	}
	
	public static void showErrorAlertDialog(MegaError error, Activity activity) {
		showErrorAlertDialog(error.getErrorString(), false, activity);
	}
	
	public static void showErrorAlertDialog(int errorCode, Activity activity) {
		showErrorAlertDialog(MegaError.getErrorString(errorCode), false, activity);
	}
	
	public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
           case ExifInterface.ORIENTATION_ROTATE_90:
               matrix.setRotate(90);
               break;
           case ExifInterface.ORIENTATION_TRANSVERSE:
               matrix.setRotate(-90);
               matrix.postScale(-1, 1);
               break;
           case ExifInterface.ORIENTATION_ROTATE_270:
               matrix.setRotate(-90);
               break;
           default:
               return bitmap;
        }
	     
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
	}
	
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
	        while ((halfHeight / inSampleSize) > reqHeight
	                && (halfWidth / inSampleSize) > reqWidth) {
	            inSampleSize *= 2;
	        }
	    }

	    return inSampleSize;
	}
	
	/*
	 * Build custom dialog
	 * @param activity Source activity
	 * @param title Dialog title
	 * @param message To display, could be null
	 * @param view Custom view to display in the dialog
	 */
	public static AlertDialog.Builder getCustomAlertBuilder(Activity activity, String title, String message, View view) {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
		ViewGroup customView = getCustomAlertView(activity, title, message);
		if (view != null) {
			customView.addView(view);
		}
		dialogBuilder.setView(customView);
		dialogBuilder.setInverseBackgroundForced(true);
		return dialogBuilder;
	}
	
	/*
	 * Create custom alert dialog view
	 */
	private static ViewGroup getCustomAlertView(Activity activity, String title, String message) {
		View customView = activity.getLayoutInflater().inflate(R.layout.alert_dialog, null);
		
		TextView titleView = (TextView)customView.findViewById(R.id.dialog_title);
		titleView.setText(title);
		
		TextView messageView = (TextView)customView.findViewById(R.id.message);
		if (message == null) {
			messageView.setVisibility(View.GONE);
		} else {
			messageView.setText(message);
		}
		return (ViewGroup)customView;
	}
	
	/*
	 * Show Toast message with resId
	 */
	public static void showToast(Context context, int resId) {
		try { Toast.makeText(context, resId, Toast.LENGTH_LONG).show(); } catch(Exception ex) {};
	}

	/*
	 * Show Toast message with String
	 */
	public static void showToast(Context context, String message) {
		try { Toast.makeText(context, message, Toast.LENGTH_LONG).show(); } catch(Exception ex) {};
	}
	
	
	public static float getScaleW(DisplayMetrics outMetrics, float density){
		
		float scale = 0;
		
		float dpWidth  = outMetrics.widthPixels / density;		
	    scale = dpWidth / dpWidthAbs;	    
		
	    return scale;
	}
	
	public static float getScaleH(DisplayMetrics outMetrics, float density){
		
		float scale = 0;
		
		float dpHeight  = outMetrics.heightPixels / density;		
	    scale = dpHeight / dpHeightAbs;	    
		
	    return scale;
	}
	
	public static int px2dp (float dp, DisplayMetrics outMetrics){
	
		return (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, outMetrics));
	}
	
	/*
	 * AES encryption
	 */
	public static byte[] aes_encrypt(byte[] raw, byte[] clear) throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		byte[] encrypted = cipher.doFinal(clear);
		return encrypted;
	}
	
	/*
	 * AES decryption
	 */
	public static byte[] aes_decrypt(byte[] raw, byte[] encrypted)
			throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		byte[] decrypted = cipher.doFinal(encrypted);
		return decrypted;
	}
	
	/*
	 * Check is device on WiFi
	 */
	public static boolean isOnWifi(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = null;
		if (connectivityManager != null) {
			networkInfo = connectivityManager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		}
		return networkInfo == null ? false : networkInfo.isConnected();
	}
	
	static public boolean isOnline(Context context) {
	    if(context == null) return true;
		
		ConnectivityManager cm =
	        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
	
	public static String getSizeString(long size){
		String sizeString = "";
		DecimalFormat decf = new DecimalFormat("###.##");

		float KB = 1024;
		float MB = KB * 1024;
		float GB = MB * 1024;
		float TB = GB * 1024;
		
		if (size < KB){
			sizeString = size + " B";
		}
		else if (size < MB){
			sizeString = decf.format(size/KB) + " KB";
		}
		else if (size < GB){
			sizeString = decf.format(size/MB) + " MB";
		}
		else if (size < TB){
			sizeString = decf.format(size/GB) + " GB";
		}
		else{
			sizeString = decf.format(size/TB) + " TB";
		}
		
		return sizeString;
	}
	
	public static String getDateString(long date){
		DateFormat datf = DateFormat.getDateTimeInstance();
		String dateString = "";
		
		dateString = datf.format(new Date(date*1000));
		
		return dateString;
	}
	
	/*
	 * Global log handler
	 */
	public static void log(String origin, String message) {
		if (DEBUG) {
			Log.e(origin, message + "");
		}
	}
	
	public static String getLocalFile(Context context, String fileName, long fileSize,
			String destDir)
	{
		Cursor cursor = null;
		try 
		{
			if(MimeType.typeForName(fileName).isImage())
			{
				final String[] projection = { MediaStore.Images.Media.DATA };
				final String selection = MediaStore.Images.Media.DISPLAY_NAME + " = ? AND " + MediaStore.Images.Media.SIZE + " = ?";
				final String[] selectionArgs = { fileName, String.valueOf(fileSize) };
				
		        cursor = context.getContentResolver().query(
		                        Images.Media.EXTERNAL_CONTENT_URI, projection, selection,
		                        selectionArgs, null);
				if (cursor != null && cursor.moveToFirst()) {
			        int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			        String path =  cursor.getString(dataColumn);
			        cursor.close();
			        cursor = null;
			        if(new File(path).exists()){
			        	return path;
			        }
				}
				if(cursor != null) cursor.close();
			
				cursor = context.getContentResolver().query(
	                    Images.Media.INTERNAL_CONTENT_URI, projection, selection,
	                    selectionArgs, null);
				if (cursor != null && cursor.moveToFirst()) {
			        int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			        String path =  cursor.getString(dataColumn);
			        cursor.close();
			        cursor = null;
			        if(new File(path).exists()) return path;
				}
				if(cursor != null) cursor.close();
			}
			else if(MimeType.typeForName(fileName).isVideo())
			{
				final String[] projection = { MediaStore.Video.Media.DATA };
				final String selection = MediaStore.Video.Media.DISPLAY_NAME + " = ? AND " + MediaStore.Video.Media.SIZE + " = ?";
				final String[] selectionArgs = { fileName, String.valueOf(fileSize) };
				
		        cursor = context.getContentResolver().query(
                        Video.Media.EXTERNAL_CONTENT_URI, projection, selection,
                        selectionArgs, null);
				if (cursor != null && cursor.moveToFirst()) {
			        int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
			        String path =  cursor.getString(dataColumn);
			        cursor.close();
			        cursor = null;
			        if(new File(path).exists()) return path;
				}
				if(cursor != null) cursor.close();
			
				cursor = context.getContentResolver().query(
		                Video.Media.INTERNAL_CONTENT_URI, projection, selection,
		                selectionArgs, null);
				if (cursor != null && cursor.moveToFirst()) {
			        int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
			        String path =  cursor.getString(dataColumn);
			        cursor.close();
			        cursor = null;
			        if(new File(path).exists()) return path;
				}
				if(cursor != null) cursor.close();
			}	
		} catch (Exception e) 
		{
			if(cursor != null) cursor.close();
		}
		
		//Not found, searching in the download folder
		if(destDir != null)
		{
			File file = new File(destDir, fileName);
			if(file.exists() && (file.length() == fileSize))
				return file.getAbsolutePath();
		}
		return null;
	}
	
	/*
	 * Check is file belongs to the app
	 */
	public static boolean isLocal(Context context, File file) {
		File tmp = null;
		if (context.getExternalCacheDir() != null){
			tmp = new File (context.getExternalCacheDir(), "tmp");
		}
		else{
			tmp = context.getDir("tmp", 0);
		}
			
		return file.getAbsolutePath().contains(tmp.getParent());
	}
	
	/*
	 * Check is file belongs to the app and temporary
	 */
	public static boolean isLocalTemp(Context context, File file) {
		return isLocal(context, file) && file.getAbsolutePath().endsWith(".tmp");
	}
	
	/*
	 * Get localized progress size
	 */
	public static String getProgressSize(Context context, long progress,
			long size) {
		return String.format("%s/%s",
				Formatter.formatFileSize(context, progress),
				Formatter.formatFileSize(context, size));
	}
	
	/*
	 * Set alpha transparency for view
	 */
	@SuppressLint("NewApi")
	public static void setViewAlpha(View view, float alpha) {
		if (Build.VERSION.SDK_INT >= 11) {
			view.setAlpha(alpha);
		} else {
			AlphaAnimation anim = new AlphaAnimation(alpha, alpha);
			anim.setDuration(0);
			anim.setFillAfter(true);
			view.startAnimation(anim);
		}
	}
	
	/*
	 * Make part of the string bold
	 */
	public static SpannableStringBuilder makeBold(String text, String boldText) {
		SpannableStringBuilder sb = new SpannableStringBuilder(text);
		StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);
		sb.setSpan(bss, text.length() - boldText.length(), text.length(),
				Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		return sb;
	}
	
	/*
	 * Delete file if it belongs to the app
	 */
	public static void deleteIfLocal(Context context, File file) {
		if (isLocal(context, file) && file.exists()) {
			log("delete!");
			file.delete();
		}
	}
	
	public static void copyFile(File source, File dest) throws IOException{

		if (!source.getAbsolutePath().equals(dest.getAbsolutePath())){
			FileChannel inputChannel = null;
			FileChannel outputChannel = null;
			FileInputStream inputStream = new FileInputStream(source);
			FileOutputStream outputStream = new FileOutputStream(dest);
			inputChannel = inputStream.getChannel();
			outputChannel = outputStream.getChannel();
			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());    
			inputChannel.close();
			outputChannel.close();
			inputStream.close();
			outputStream.close();
		}
	}
	
	/*
	 * Start activity to open URL
	 */
	public static void openUrl(Context context, String url) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		context.startActivity(intent);
	}
	
	public static void deleteFolderAndSubfolders(Context context, File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles()){
				deleteFolderAndSubfolders(context, c);
			}
		}
		
		if (!f.delete()){
			throw new FileNotFoundException("Failed to delete file: " + f);
		}
		else{
			
			Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			File fileToDelete = new File(f.getAbsolutePath());
		    Uri contentUri = Uri.fromFile(fileToDelete);
		    mediaScanIntent.setData(contentUri);
		    context.sendBroadcast(mediaScanIntent);
		    
//			MediaScannerConnection.scanFile(context,
//					new String[] { f.getAbsolutePath() }, null,
//			        new MediaScannerConnection.OnScanCompletedListener() {
//			      		public void onScanCompleted(String path, Uri uri) {
//			      			log("Scanned: " + path);
//			      		}
//			 		}
//			);
		}
	}
	
	public static String getSpeedString (long speed){
		String speedString = "";
		double speedDouble = 0;
		DecimalFormat df = new DecimalFormat("#.##");
		
		if (speed > 1024){
			if (speed > 1024*1024){
				if (speed > 1024*1024*1024){
					speedDouble = speed / (1024.0*1024.0*1024.0);
					speedString = df.format(speedDouble) + " GB/s";
				}
				else{
					speedDouble = speed / (1024.0*1024.0);
					speedString = df.format(speedDouble) + " MB/s";
				}
			}
			else{
				speedDouble = speed / 1024.0;
				speedString = df.format(speedDouble) + " KB/s";	
			}
		}
		else{
			speedDouble = speed;
			speedString = df.format(speedDouble) + " B/s";
		}
		
		return speedString;
	}
	
	private static void log(String message) {
		log("Util", message);
	}
}
