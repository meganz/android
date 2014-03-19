package com.mega.android;

import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Util {
	
	public static float dpWidthAbs = 360;
	public static float dpHeightAbs = 592;
	
	// Debug flag to enable logging and some other things
	public static boolean DEBUG = true;
	
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
		File tmp = context.getDir("tmp", 0);
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

}
