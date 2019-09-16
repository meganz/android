package mega.privacy.android.app.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;

import android.support.v4.app.ActivityCompat;

import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.Formatter;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import mega.privacy.android.app.AndroidLogger;
import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.interfaces.AbortPendingTransferCallback;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatFullScreenImageViewer;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;

import static android.content.Context.INPUT_METHOD_SERVICE;


public class Util {

    public static final String DATE_AND_TIME_PATTERN = "yyyy-MM-dd HH.mm.ss";
    public static int ONTRANSFERUPDATE_REFRESH_MILLIS = 1000;
	
	public static float dpWidthAbs = 360;
	public static float dpHeightAbs = 592;
	
	public static double percScreenLogin = 0.596283784; //The dimension of the grey zone (Login and Tour)
	public static double percScreenLoginReturning = 0.8;
	
	// Debug flag to enable logging and some other things
	public static boolean DEBUG = false;

	public static String base64EncodedPublicKey_1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0bZjbgdGRd6/hw5/J2FGTkdG";
	public static String base64EncodedPublicKey_2 = "tDTMdR78hXKmrxCyZUEvQlE/DJUR9a/2ZWOSOoaFfi9XTBSzxrJCIa+gjj5wkyIwIrzEi";
	public static String base64EncodedPublicKey_3 = "55k9FIh3vDXXTHJn4oM9JwFwbcZf1zmVLyes5ld7+G15SZ7QmCchqfY4N/a/qVcGFsfwqm";
	public static String base64EncodedPublicKey_4 = "RU3VzOUwAYHb4mV/frPctPIRlJbzwCXpe3/mrcsAP+k6ECcd19uIUCPibXhsTkNbAk8CRkZ";
	public static String base64EncodedPublicKey_5 = "KOy+czuZWfjWYx3Mp7srueyQ7xF6/as6FWrED0BlvmhJYj0yhTOTOopAXhGNEk7cUSFxqP2FKYX8e3pHm/uNZvKcSrLXbLUhQnULhn4WmKOQIDAQAB";
	
	/*public static String base64EncodedPublicKey_1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlxJdfjvhsCAK1Lu5n6WtQf";
	public static String base64EncodedPublicKey_2 = "MkjjOUCDDuM7zeiS3jsfCghG1bpwMmD4E8vQfPboyYtQBftdEG5GbWrqWJL+z6M/2SN";
	public static String base64EncodedPublicKey_3 = "+6pHqExFw8fjzP/4/CDzHLhmITKTOegm/6cfMUWcrghZuiHKfM6n4vmNYrHy4Bpx68RJW+J4B";
	public static String base64EncodedPublicKey_4 = "wL6PWE8ZGGeeJmU0eAJeRJMsNEwMrW2LATnIoJ4/qLYU4gKDINPMRaIE6/4pQnbd2NurWm8ZQT7XSMQZcisTqwRLS";
	public static String base64EncodedPublicKey_5 = "YgjYKCXtjloP8QnKu0IGOoo79Cfs3Z9eC3sQ1fcLQsMM2wExlbnYI2KPTs0EGCmcMXrrO5MimGjYeW8GQlrKsbiZ0UwIDAQAB";
	*/
	public static DatabaseHandler dbH;
	public static boolean fileLoggerSDK = false;
	public static boolean fileLoggerKarere = false;
	public static Context context;

	public static HashMap<String, String> countryCodeDisplay;

    public static boolean checkFingerprint(MegaApiAndroid megaApi, MegaNode node, String localPath) {
        String nodeFingerprint = node.getFingerprint();
        String nodeOriginalFingerprint = node.getOriginalFingerprint();

        String fileFingerprint = megaApi.getFingerprint(localPath);
        if (fileFingerprint != null) {
            return fileFingerprint.equals(nodeFingerprint) || fileFingerprint.equals(nodeOriginalFingerprint);
        }
        return false;
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
			AlertDialog.Builder dialogBuilder = getCustomAlertBuilder(activity, activity.getString(R.string.general_error_word), message, null);
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
			brandAlertDialog(dialog);
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

	public static int countMatches(Pattern pattern, String string)
	{
		Matcher matcher = pattern.matcher(string);

		int count = 0;
		int pos = 0;
		while (matcher.find(pos))
		{
			count++;
			pos = matcher.start() + 1;
		}

		return count;
	}
	
	public static boolean showMessageRandom(){
		Random r = new Random(System.currentTimeMillis());
		int randomInt = r.nextInt(100) + 1;
		
		if(randomInt<5){
			return true;
		}
		else{
			return false;
		}
//		return true;
	}

    public static String toCDATA(String src) {
        if (src != null) {
            //solution from web client
            src = src.replaceAll("&","&amp;")
                    .replaceAll("\"","&quot;")
                    .replaceAll("'","&#39;")
                    .replaceAll("<","&lt;")
                    .replaceAll(">","&gt;");
            //another solution
//            src = src.replaceAll("]]>", "] ]>");
//            src = "<![CDATA[" + src + "]]>";
        }
        return src;
    }

	public static String getExternalCardPath() {

        String secStore = System.getenv("SECONDARY_STORAGE");
        if (secStore == null){
        	return null;
        }
        else{
        	if (secStore.compareTo("") == 0){
        		return null;
        	}
			LogUtil.logDebug("secStore: " + secStore);
	        File path = new File(secStore);
			LogUtil.logDebug("getFreeSize: " + path.getUsableSpace());
	        if(path.getUsableSpace()>0)
	        {
	        	return path.getAbsolutePath();
	        }
        }

        return null;
	}

	public static String getNumberItemChildren(File file){
		File[] list = file.listFiles();
		int count = 0;
		if(list!=null){
			count =  list.length;
		}

		String numChilden = count + " " + context.getResources().getQuantityString(R.plurals.general_num_items, count);

		return numChilden;
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
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null; 
            }
            return bmRotated;
        }
        catch (Exception e) {
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

	/*
	 * Check is device on Mobile Data
	 */
	public static boolean isOnMobileData(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = null;
		if (connectivityManager != null) {
			networkInfo = connectivityManager
					.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
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
			sizeString = size + " " + context.getString(R.string.label_file_size_byte);
		}
		else if (size < MB){
			sizeString = decf.format(size/KB) + " " + context.getString(R.string.label_file_size_kilo_byte);
		}
		else if (size < GB){
			sizeString = decf.format(size/MB) + " " + context.getString(R.string.label_file_size_mega_byte);
		}
		else if (size < TB){
			sizeString = decf.format(size/GB) + " " + context.getString(R.string.label_file_size_giga_byte);
		}
		else{
			sizeString = decf.format(size/TB) + " " + context.getString(R.string.label_file_size_tera_byte);
		}
		
		return sizeString;
	}
    
	public static String getDateString(long date){
		DateFormat datf = DateFormat.getDateTimeInstance();
		String dateString = "";
		
		dateString = datf.format(new Date(date*1000));
		
		return dateString;
	}

	public static void setContext(Context c){
		context = c;
	}

	public static void setDBH(DatabaseHandler d){
		dbH = d;
	}

	public static void setFileLoggerSDK(boolean fL){
		fileLoggerSDK = fL;
	}

	public static boolean getFileLoggerSDK(){
		return fileLoggerSDK;
	}

	public static void setFileLoggerKarere(boolean fL){
		fileLoggerKarere = fL;
	}

    public static boolean getFileLoggerKarere(){
        return fileLoggerKarere;
    }

	public static void brandAlertDialog(AlertDialog dialog) {
	    try {
	        Resources resources = dialog.getContext().getResources();

	        int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");

	        TextView alertTitle = (TextView) dialog.getWindow().getDecorView().findViewById(alertTitleId);
	        if (alertTitle != null){	        	
	        	alertTitle.setTextColor(ContextCompat.getColor(dialog.getContext(), R.color.mega)); // change title text color
	        }

	        int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
	        View titleDivider = dialog.getWindow().getDecorView().findViewById(titleDividerId);
	        if (titleDivider != null){
	        	titleDivider.setBackgroundColor(ContextCompat.getColor(dialog.getContext(), R.color.mega)); // change divider color
	        }
	    } catch (Exception ex) {
	    	Toast.makeText(dialog.getContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
	        ex.printStackTrace();
	    }
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



	public static String getPhotoSyncName (long timeStamp, String fileName){
        DateFormat sdf = new SimpleDateFormat(DATE_AND_TIME_PATTERN,Locale.getDefault());
        return sdf.format(new Date(timeStamp)) + fileName.substring(fileName.lastIndexOf('.'));
	}
	
	public static String getPhotoSyncNameWithIndex (long timeStamp, String fileName, int photoIndex){
        if(photoIndex == 0) {
            return getPhotoSyncName(timeStamp, fileName);
        }
        DateFormat sdf = new SimpleDateFormat(DATE_AND_TIME_PATTERN,Locale.getDefault());
        return sdf.format(new Date(timeStamp)) + "_" + photoIndex + fileName.substring(fileName.lastIndexOf('.'));
	}
	
	public static int getNumberOfNodes (MegaNode parent, MegaApiAndroid megaApi){
		int numberOfNodes = 0;
		
		ArrayList<MegaNode> children = megaApi.getChildren(parent);
		for (int i=0; i<children.size(); i++){
			if (children.get(i).isFile()){
				numberOfNodes++;
			}
			else{
				numberOfNodes = numberOfNodes + getNumberOfNodes(children.get(i), megaApi);
			}
		}
		
		return numberOfNodes;
	}
	
	public static String getLocalIpAddress(Context context)
  {
		  try {
			  for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				  NetworkInterface intf = en.nextElement();
				  String interfaceName = intf.getName();

				  // Ensure get the IP from the current active network interface
				  if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					  ConnectivityManager cm =
							  (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
					  String activeInterfaceName = cm.getLinkProperties(cm.getActiveNetwork()).getInterfaceName();
					  if (interfaceName.compareTo(activeInterfaceName) != 0) {
					  	continue;
					  }
				  }
				  else {
					  if ((isOnWifi(context) && !interfaceName.contains("wlan") && !interfaceName.contains("ath")) ||
							  (isOnMobileData(context) && !interfaceName.contains("data") && !interfaceName.contains("rmnet"))) {
					  	continue;
					  }
				  }

				  for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					  InetAddress inetAddress = enumIpAddr.nextElement();
					  if (inetAddress != null && !inetAddress.isLoopbackAddress()) {
					  	return inetAddress.getHostAddress();
					  }
				  }
			  }
		  } catch (Exception ex) {
			  LogUtil.logError("Error getting local IP address", ex);
		  }
		  return null;
   }
	
	@SuppressLint("InlinedApi") 
	public static boolean isCharging(Context context) {
		final Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

		if (Build.VERSION.SDK_INT < 17) {
			return status == BatteryManager.BATTERY_PLUGGED_AC || status == BatteryManager.BATTERY_PLUGGED_USB;
		} else {
			return status == BatteryManager.BATTERY_PLUGGED_AC || status == BatteryManager.BATTERY_PLUGGED_USB || status == BatteryManager.BATTERY_PLUGGED_WIRELESS;
		}
		
	}
	
	/** Returns the consumer friendly device name */
	public static String getDeviceName() {
	    final String manufacturer = Build.MANUFACTURER;
	    final String model = Build.MODEL;
	    if (model.startsWith(manufacturer)) {
	        return model;
	    }
	    if (manufacturer.equalsIgnoreCase("HTC")) {
	        // make sure "HTC" is fully capitalized.
	        return "HTC " + model;
	    }
	    return manufacturer + " " + model;
	}
	
	public static String getCountryCode(String countryString){
		String countryCode= "";
		
		countryCode = countryCodeDisplay.get(countryString);
		
		return countryCode;
	}
	
	public static ArrayList<String> getCountryList(Context context){
		ArrayList<String> countryCodes = new ArrayList<String>();
		
		countryCodes.add("US");
		countryCodes.add("GB");
		countryCodes.add("CA");
		countryCodes.add("AX");
		countryCodes.add("AF");
		countryCodes.add("AP");
		countryCodes.add("AL");
		countryCodes.add("DZ");
		countryCodes.add("AS");
		countryCodes.add("AD");
		countryCodes.add("AO");
		countryCodes.add("AI");
		countryCodes.add("AQ");
		countryCodes.add("AG");
		countryCodes.add("AR");
		countryCodes.add("AM");
		countryCodes.add("AW");
		countryCodes.add("AU");
		countryCodes.add("AT");
		countryCodes.add("AZ");
		countryCodes.add("BS");
		countryCodes.add("BH");
		countryCodes.add("BD");
		countryCodes.add("BB");
		countryCodes.add("BY");
		countryCodes.add("BE");
		countryCodes.add("BZ");
		countryCodes.add("BJ");
		countryCodes.add("BM");
		countryCodes.add("BT");
		countryCodes.add("BO");
		countryCodes.add("BA");
		countryCodes.add("BW");
		countryCodes.add("BV");
		countryCodes.add("BR");
		countryCodes.add("IO");
		countryCodes.add("BN");
		countryCodes.add("BG");
		countryCodes.add("BF");
		countryCodes.add("BI");
		countryCodes.add("KH");
		countryCodes.add("CM");
		countryCodes.add("CV");
		countryCodes.add("KY");
		countryCodes.add("CF");
		countryCodes.add("TD");
		countryCodes.add("CL");
		countryCodes.add("CN");
		countryCodes.add("CX");
		countryCodes.add("CC");
		countryCodes.add("CO");
		countryCodes.add("KM");
		countryCodes.add("CG");
		countryCodes.add("CD");
		countryCodes.add("CK");
		countryCodes.add("CR");
		countryCodes.add("CI");
		countryCodes.add("HR");
		countryCodes.add("CU");
		countryCodes.add("CY");
		countryCodes.add("CZ");
		countryCodes.add("DK");
		countryCodes.add("DJ");
		countryCodes.add("DM");
		countryCodes.add("DO");
		countryCodes.add("TL");
		countryCodes.add("EC");
		countryCodes.add("EG");
		countryCodes.add("SV");
		countryCodes.add("GQ");
		countryCodes.add("ER");
		countryCodes.add("EE");
		countryCodes.add("ET");
		countryCodes.add("FK");
		countryCodes.add("FO");
		countryCodes.add("FJ");
		countryCodes.add("FI");
		countryCodes.add("FR");
		countryCodes.add("GF");
		countryCodes.add("PF");
		countryCodes.add("TF");
		countryCodes.add("GA");
		countryCodes.add("GM");
		countryCodes.add("GE");
		countryCodes.add("DE");
		countryCodes.add("GH");
		countryCodes.add("GI");
		countryCodes.add("GR");
		countryCodes.add("GL");
		countryCodes.add("GD");
		countryCodes.add("GP");
		countryCodes.add("GU");
		countryCodes.add("GG");
		countryCodes.add("GT");
		countryCodes.add("GN");
		countryCodes.add("GW");
		countryCodes.add("GY");
		countryCodes.add("HT");
		countryCodes.add("HN");
		countryCodes.add("HK");
		countryCodes.add("HU");
		countryCodes.add("IS");
		countryCodes.add("IN");
		countryCodes.add("ID");
		countryCodes.add("IR");
		countryCodes.add("IQ");
		countryCodes.add("IE");
		countryCodes.add("IM");
		countryCodes.add("IL");
		countryCodes.add("IT");
		countryCodes.add("JM");
		countryCodes.add("JP");
		countryCodes.add("JE");
		countryCodes.add("JO");
		countryCodes.add("KZ");
		countryCodes.add("KE");
		countryCodes.add("KI");
		countryCodes.add("KW");
		countryCodes.add("KG");
		countryCodes.add("LA");
		countryCodes.add("LV");
		countryCodes.add("LB");
		countryCodes.add("LS");
		countryCodes.add("LR");
		countryCodes.add("LY");
		countryCodes.add("LI");
		countryCodes.add("LT");
		countryCodes.add("LU");
		countryCodes.add("MO");
		countryCodes.add("MK");
		countryCodes.add("MG");
		countryCodes.add("MW");
		countryCodes.add("MY");
		countryCodes.add("MV");
		countryCodes.add("ML");
		countryCodes.add("MT");
		countryCodes.add("MH");
		countryCodes.add("MQ");
		countryCodes.add("MR");
		countryCodes.add("MU");
		countryCodes.add("YT");
		countryCodes.add("MX");
		countryCodes.add("FM");
		countryCodes.add("MD");
		countryCodes.add("MC");
		countryCodes.add("MN");
		countryCodes.add("ME");
		countryCodes.add("MS");
		countryCodes.add("MA");
		countryCodes.add("MZ");
		countryCodes.add("MM");
		countryCodes.add("NA");
		countryCodes.add("NR");
		countryCodes.add("NP");
		countryCodes.add("NL");
		countryCodes.add("AN");
		countryCodes.add("NC");
		countryCodes.add("NZ");
		countryCodes.add("NI");
		countryCodes.add("NE");
		countryCodes.add("NG");
		countryCodes.add("NU");
		countryCodes.add("NF");
		countryCodes.add("KP");
		countryCodes.add("MP");
		countryCodes.add("NO");
		countryCodes.add("OM");
		countryCodes.add("PK");
		countryCodes.add("PW");
		countryCodes.add("PS");
		countryCodes.add("PA");
		countryCodes.add("PG");
		countryCodes.add("PY");
		countryCodes.add("PE");
		countryCodes.add("PH");
		countryCodes.add("PN");
		countryCodes.add("PL");
		countryCodes.add("PT");
		countryCodes.add("PR");
		countryCodes.add("QA");
		countryCodes.add("RE");
		countryCodes.add("RO");
		countryCodes.add("RU");
		countryCodes.add("RW");
		countryCodes.add("MF");
		countryCodes.add("KN");
		countryCodes.add("LC");
		countryCodes.add("VC");
		countryCodes.add("WS");
		countryCodes.add("SM");
		countryCodes.add("ST");
		countryCodes.add("SA");
		countryCodes.add("SN");
		countryCodes.add("RS");
		countryCodes.add("SC");
		countryCodes.add("SL");
		countryCodes.add("SG");
		countryCodes.add("SK");
		countryCodes.add("SI");
		countryCodes.add("SB");
		countryCodes.add("SO");
		countryCodes.add("ZA");
		countryCodes.add("GS");
		countryCodes.add("KR");
		countryCodes.add("SS");
		countryCodes.add("ES");
		countryCodes.add("LK");
		countryCodes.add("SH");
		countryCodes.add("PM");
		countryCodes.add("SD");
		countryCodes.add("SR");
		countryCodes.add("SJ");
		countryCodes.add("SZ");
		countryCodes.add("SE");
		countryCodes.add("CH");
		countryCodes.add("SY");
		countryCodes.add("TW");
		countryCodes.add("TJ");
		countryCodes.add("TZ");
		countryCodes.add("TH");
		countryCodes.add("TG");
		countryCodes.add("TK");
		countryCodes.add("TO");
		countryCodes.add("TT");
		countryCodes.add("TN");
		countryCodes.add("TR");
		countryCodes.add("TM");
		countryCodes.add("TC");
		countryCodes.add("TV");
		countryCodes.add("UG");
		countryCodes.add("UA");
		countryCodes.add("AE");
		countryCodes.add("UM");
		countryCodes.add("UY");
		countryCodes.add("UZ");
		countryCodes.add("VU");
		countryCodes.add("VA");
		countryCodes.add("VE");
		countryCodes.add("VN");
		countryCodes.add("VG");
		countryCodes.add("VI");
		countryCodes.add("WF");
		countryCodes.add("EH");
		countryCodes.add("YE");
		countryCodes.add("ZM");
		countryCodes.add("ZW");
		
		Locale currentLocale = Locale.getDefault();
//		Toast.makeText(context, currentLocale.getLanguage(), Toast.LENGTH_LONG).show();
		
		countryCodeDisplay = new HashMap<String, String>();
		
		ArrayList<String> countryList = new ArrayList<String>();
		for (int i=0;i<countryCodes.size();i++){
			Locale l = new Locale (currentLocale.getLanguage(), countryCodes.get(i));
			String country = l.getDisplayCountry();
			if (country.length() > 0 && !countryList.contains(country)){
				countryList.add(country);
				countryCodeDisplay.put(country, countryCodes.get(i));				
			}
		}
		
//		Toast.makeText(context, "CONTRYLIST: " + countryList.size() + "___ " + countryCodes.size(), Toast.LENGTH_LONG).show();
		Collections.sort(countryList, String.CASE_INSENSITIVE_ORDER);
		countryList.add(0, context.getString(R.string.country_cc));
		
		return countryList;
		
//		Locale[] locale = Locale.getAvailableLocales();
//		String country;
//		Toast.makeText(context, "LOCALEAAAAAA: " + locale.length, Toast.LENGTH_LONG).show();
//		for (Locale loc : locale){
//			country = loc.getCountry();
//			if (country.length() > 0 && !countryList.contains(country)){
//				countryList.add(country);
//			}
//		}
//		Toast.makeText(context, "CONTRYLIST: " + countryList.size(), Toast.LENGTH_LONG).show();
//				
//		Collections.sort(countryList, String.CASE_INSENSITIVE_ORDER);
//		countryList.add(0, context.getString(R.string.country_cc));
//		
//		return countryList;
		
	}
	
	public static ArrayList<String> getMonthListInt(Context context){
		ArrayList<String> monthList = new ArrayList<String>();
		
		monthList.add(context.getString(R.string.month_cc));
		
		monthList.add("01");
		monthList.add("02");
		monthList.add("03");
		monthList.add("04");
		monthList.add("05");
		monthList.add("06");
		monthList.add("07");
		monthList.add("08");
		monthList.add("09");
		monthList.add("10");
		monthList.add("11");
		monthList.add("12");
		
		return monthList;
	}
	
	public static ArrayList<String> getYearListInt(Context context){
		ArrayList<String> yearList = new ArrayList<String>();
		
		yearList.add(context.getString(R.string.year_cc));
		
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		
		for (int i=year;i<=(year+20);i++){
			yearList.add(i+"");
		}
		
		return yearList;
	}


	public static String getSubtitleDescription(ArrayList<MegaNode> nodes){
		int numFolders = 0;
		int numFiles = 0;

		for (int i=0;i<nodes.size();i++){
			MegaNode c = nodes.get(i);
			if (c.isFolder()){
				numFolders++;
			}
			else{
				numFiles++;
			}
		}

		String info = "";
		if (numFolders > 0){
			info = numFolders +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		else {
			if (numFiles == 0){
				info = context.getString(R.string.no_folders_shared);
			}
			else{
				info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}

		return info;
	}

	public static BitSet convertToBitSet(long value) {
	    BitSet bits = new BitSet();
	    int index = 0;
	    while (value != 0L) {
	      if (value % 2L != 0) {
	        bits.set(index);
	      }
	      ++index;
	      value = value >>> 1;
	    }
	    return bits;
	}
	
	public static boolean checkBitSet(BitSet paymentBitSet, int position){
		LogUtil.logDebug("checkBitSet");
		if (paymentBitSet != null){
			if (paymentBitSet.get(position)){
				return true;
			}
			else{
				return false;
			}
		}
		else{
			return false;
		}
	}

	public static long getLastPublicHandle(MegaAttributes attributes){
		long lastPublicHandle = -1;

		if (attributes != null){
			if (attributes.getLastPublicHandle() != null){
				try{
					long currentTime = System.currentTimeMillis()/1000;
					long lastPublicHandleTimeStamp = Long.parseLong(attributes.getLastPublicHandleTimeStamp());
					LogUtil.logDebug("currentTime: " + currentTime + " _ " + lastPublicHandleTimeStamp);
					if ((currentTime - lastPublicHandleTimeStamp) < 86400){
						if (Long.parseLong(attributes.getLastPublicHandle()) != -1){
							lastPublicHandle = Long.parseLong(attributes.getLastPublicHandle());
						}
					}
				}
				catch (Exception e){
					lastPublicHandle = -1;
				}
			}
		}

		return lastPublicHandle;
	}
	
	public static boolean isPaymentMethod(BitSet paymentBitSet, int plan){
		
		boolean r = false;
		if (paymentBitSet != null){
			if (!Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
				r = true;
			}
			if (!Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
				r = true;
			}
			if (!Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
				if (plan == 4){
					r = true;
				}
			}
			if (!Util.checkBitSet(paymentBitSet, MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
				if (plan == 4){
					r = true;
				}
			}
		}
		
		return r;
	}
	
	public static int scaleHeightPx(int px, DisplayMetrics metrics){
		int myHeightPx = metrics.heightPixels;
		
		return px*myHeightPx/548; //Based on Eduardo's measurements				
	}
	
	public static int scaleWidthPx(int px, DisplayMetrics metrics){
		int myWidthPx = metrics.widthPixels;
		
		return px*myWidthPx/360; //Based on Eduardo's measurements		
		
	}

	/*
	 * Validate email
	 */
	public static String getEmailError(String value, Context context) {
		LogUtil.logDebug("getEmailError");
		if (value.length() == 0) {
			return context.getString(R.string.error_enter_email);
		}
		if (!Constants.EMAIL_ADDRESS.matcher(value).matches()) {
			return context.getString(R.string.error_invalid_email);
		}
		return null;
	}

	public static int getAvatarTextSize (float density){
		float textSize = 0.0f;

		if (density > 3.0){
			textSize = density * (DisplayMetrics.DENSITY_XXXHIGH / 72.0f);
		}
		else if (density > 2.0){
			textSize = density * (DisplayMetrics.DENSITY_XXHIGH / 72.0f);
		}
		else if (density > 1.5){
			textSize = density * (DisplayMetrics.DENSITY_XHIGH / 72.0f);
		}
		else if (density > 1.0){
			textSize = density * (72.0f / DisplayMetrics.DENSITY_HIGH / 72.0f);
		}
		else if (density > 0.75){
			textSize = density * (72.0f / DisplayMetrics.DENSITY_MEDIUM / 72.0f);
		}
		else{
			textSize = density * (72.0f / DisplayMetrics.DENSITY_LOW / 72.0f);
		}

		return (int)textSize;
	}

	public static void showAlert(Context context, String message, String title) {
		LogUtil.logDebug("showAlert");
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		if(title!=null){
			builder.setTitle(title);
		}
		builder.setMessage(message);
		builder.setPositiveButton("OK",null);
		builder.show();
	}

	public static long calculateTimestampMinDifference(String timeStamp) {
		LogUtil.logDebug("calculateTimestampDifference");

		Long actualTimestamp = System.currentTimeMillis()/1000;

		Long oldTimestamp = Long.parseLong(timeStamp);

		Long difference = actualTimestamp - oldTimestamp;

		difference = difference/60;

		return difference;
	}

	public static int getVersion(Context context) {
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			return pInfo.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			return 0;
		}
	}

	public static long calculateTimestamp(String time)
	{
		LogUtil.logDebug("calculateTimestamp: " + time);
		long unixtime;
		DateFormat dfm = new SimpleDateFormat("yyyyMMddHHmm");
		dfm.setTimeZone( TimeZone.getDefault());//Specify your timezone
		try
		{
			unixtime = dfm.parse(time).getTime();
			unixtime=unixtime/1000;
			return unixtime;
		}
		catch (ParseException e)
		{
			LogUtil.logError("ParseException!!!", e);
		}
		return 0;
	}

	public static Calendar calculateDateFromTimestamp (long timestamp){
		LogUtil.logDebug("calculateTimestamp: " + timestamp);
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp*1000);
		LogUtil.logDebug("Calendar: " + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.MONTH));
		return cal;
	}

	public static boolean isChatEnabled (){
		LogUtil.logDebug("isChatEnabled");
		if (dbH == null){
			dbH = DatabaseHandler.getDbHandler(context);
		}
		ChatSettings chatSettings = dbH.getChatSettings();
		boolean chatEnabled;

		if(chatSettings!=null){
			if(chatSettings.getEnabled()!=null){
				chatEnabled = Boolean.parseBoolean(chatSettings.getEnabled());
				LogUtil.logDebug("A - chatEnabled: " + chatEnabled);
				return chatEnabled;
			}
			else{
				chatEnabled=true;
				LogUtil.logDebug("B - chatEnabled: " + chatEnabled);
				return chatEnabled;
			}
		}
		else{
			chatEnabled=true;
			LogUtil.logDebug("C - chatEnabled: " + chatEnabled);
			return chatEnabled;
		}
	}

	public static void resetAndroidLogger(){

		MegaApiAndroid.addLoggerObject(new AndroidLogger(AndroidLogger.LOG_FILE_NAME, Util.getFileLoggerSDK()));
		MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);

		boolean fileLogger = false;

		if (dbH == null){
			dbH = DatabaseHandler.getDbHandler(context);
		}

		if (dbH != null) {
			MegaAttributes attrs = dbH.getAttributes();
			if (attrs != null) {
				if (attrs.getFileLoggerSDK() != null) {
					try {
						fileLogger = Boolean.parseBoolean(attrs.getFileLoggerSDK());
					} catch (Exception e) {
						fileLogger = false;
					}
				} else {
					fileLogger = false;
				}
			} else {
				fileLogger = false;
			}
		}

		if (Util.DEBUG){
			MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);
		}
		else {
			setFileLoggerSDK(fileLogger);
			if (fileLogger) {
				MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);
			} else {
				MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL);
			}
		}
	}

	public static Bitmap getCircleBitmap(Bitmap bitmap) {
		final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(output);

		final int color = Color.RED;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawOval(rectF, paint);

		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		bitmap.recycle();

		return output;
	}

	//restrict the scale factor to below 1.1 to allow user to have some level of freedom and also prevent ui issues
	public static void setAppFontSize(Activity activity) {
		float scale = activity.getResources().getConfiguration().fontScale;
		LogUtil.logDebug("System font size scale is " + scale);

		float newScale;

		if (scale <= 1.1) {
			newScale = scale;
		} else {
			newScale = (float) 1.1;
		}

		LogUtil.logDebug("New font size new scale is " + newScale);
		Configuration configuration = activity.getResources().getConfiguration();
		configuration.fontScale = newScale;

		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		metrics.scaledDensity = configuration.fontScale * metrics.density;
		activity.getBaseContext().getResources().updateConfiguration(configuration, metrics);
	}
    
    //reduce font size for scale mode to prevent title and subtitle overlap
    public static SpannableString adjustForLargeFont(String original) {
        float scale = context.getResources().getConfiguration().fontScale;
        if(scale > 1){
            scale = (float)0.9;
        }
        SpannableString spannableString = new SpannableString(original);
        spannableString.setSpan(new RelativeSizeSpan(scale),0, original.length(),0);
        return spannableString;
    }

	public static Drawable mutateIcon (Context context, int idDrawable, int idColor) {

		Drawable icon = ContextCompat.getDrawable(context, idDrawable);
		icon = icon.mutate();
		icon.setColorFilter(ContextCompat.getColor(context, idColor), PorterDuff.Mode.MULTIPLY);

		return icon;
	}

	public static Drawable mutateIconSecondary(Context context, int idDrawable, int idColor) {
		Drawable icon = ContextCompat.getDrawable(context, idDrawable);
		icon = icon.mutate();
		icon.setColorFilter(ContextCompat.getColor(context, idColor), PorterDuff.Mode.SRC_ATOP);

		return icon;
	}

	//Notice user that any transfer prior to login will be destroyed
	public static void checkPendingTransfer(MegaApiAndroid megaApi, Context context, final AbortPendingTransferCallback callback){
		if(megaApi.getNumPendingDownloads() > 0 || megaApi.getNumPendingUploads() > 0){
			AlertDialog.Builder builder = new AlertDialog.Builder(context);

			if(context instanceof ManagerActivityLollipop){
				LogUtil.logDebug("Show dialog to cancel transfers before logging OUT");
				builder.setMessage(R.string.logout_warning_abort_transfers);
				builder.setPositiveButton(R.string.action_logout, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						callback.onAbortConfirm();
					}
				});
			}
			else{
				LogUtil.logDebug("Show dialog to cancel transfers before logging IN");
				builder.setMessage(R.string.login_warning_abort_transfers);
				builder.setPositiveButton(R.string.login_text, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						callback.onAbortConfirm();
					}
				});
			}

			builder.setNegativeButton(R.string.general_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					callback.onAbortCancel();
				}
			});
			builder.show();
		}else{
			callback.onAbortConfirm();
		}
	}

	public static void changeStatusBarColorActionMode (final Context context, final Window window, Handler handler, int option) {
		LogUtil.logDebug("changeStatusBarColor");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			if (option ==  1) {
				window.setStatusBarColor(ContextCompat.getColor(context, R.color.accentColorDark));
			}
			else if (option == 2) {
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						window.setStatusBarColor(0);
					}
				}, 500);
			}
			else if (option == 3) {
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						window.setStatusBarColor(ContextCompat.getColor(context, R.color.status_bar_search));
					}
				}, 500);
			}
			else {
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						window.setStatusBarColor(ContextCompat.getColor(context, R.color.dark_primary_color_secondary));
					}
				}, 500);
			}
		}
	}

	public static Bitmap createDefaultAvatar (String color, String firstLetter) {
		LogUtil.logDebug("color: '" + color + "' firstLetter: '" + firstLetter + "'");

		Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(defaultAvatar);
		Paint paintText = new Paint();
		Paint paintCircle = new Paint();

		paintText.setColor(Color.WHITE);
		paintText.setTextSize(150);
		paintText.setAntiAlias(true);
		paintText.setTextAlign(Paint.Align.CENTER);
		Typeface face = Typeface.SANS_SERIF;
		paintText.setTypeface(face);
		paintText.setAntiAlias(true);
		paintText.setSubpixelText(true);
		paintText.setStyle(Paint.Style.FILL);

		if(color!=null){
			LogUtil.logDebug("The color to set the avatar is " + color);
			paintCircle.setColor(Color.parseColor(color));
			paintCircle.setAntiAlias(true);
		}
		else{
			LogUtil.logDebug("Default color to the avatar");
			paintCircle.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
			paintCircle.setAntiAlias(true);
		}


		int radius;
		if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
			radius = defaultAvatar.getWidth()/2;
		else
			radius = defaultAvatar.getHeight()/2;

		c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius,paintCircle);

		LogUtil.logDebug("Draw letter: " + firstLetter);
		Rect bounds = new Rect();

		paintText.getTextBounds(firstLetter,0,firstLetter.length(),bounds);
		int xPos = (c.getWidth()/2);
		int yPos = (int)((c.getHeight()/2)-((paintText.descent()+paintText.ascent()/2))+20);
		c.drawText(firstLetter.toUpperCase(Locale.getDefault()), xPos, yPos, paintText);

		return defaultAvatar;
	}

    public static boolean askMe (Context context) {
		DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
		MegaPreferences prefs = dbH.getPreferences();

		if (prefs != null){
			if (prefs.getStorageAskAlways() != null){
				if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
					if (prefs.getStorageDownloadLocation() != null){
						if (prefs.getStorageDownloadLocation().compareTo("") != 0){
							return false;
						}
					}
				}
			}
		}
		return true;
	}
    
    public static void showSnackBar(Context context,int snackbarType,String message,int idChat) {
        if (context instanceof ChatFullScreenImageViewer) {
            ((ChatFullScreenImageViewer)context).showSnackbar(snackbarType,message,idChat);
        } else if (context instanceof AudioVideoPlayerLollipop) {
            ((AudioVideoPlayerLollipop)context).showSnackbar(snackbarType,message,idChat);
        } else if (context instanceof PdfViewerActivityLollipop) {
            ((PdfViewerActivityLollipop)context).showSnackbar(snackbarType,message,idChat);
        } else if (context instanceof ChatActivityLollipop) {
            ((ChatActivityLollipop)context).showSnackbar(snackbarType,message,idChat);
        } else if (context instanceof NodeAttachmentHistoryActivity) {
            ((NodeAttachmentHistoryActivity)context).showSnackbar(snackbarType,message,idChat);
        } else if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop)context).showSnackbar(snackbarType,message,idChat);
        } else if (context instanceof BaseActivity) {
            View rootView = getRootViewFromContext(context);
            if (rootView == null) {
				LogUtil.logWarning("Unable to show snack bar, view does not exist");
            } else {
                ((BaseActivity)context).showSnackbar(snackbarType,rootView,message,idChat);
            }
        }
    }
    
    private static View getRootViewFromContext(Context context) {
        BaseActivity activity = (BaseActivity)context;
        View rootView = null;
        try {
            rootView = activity.findViewById(android.R.id.content);
            if (rootView == null) {
                rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
            }
            if (rootView == null) {
                rootView = ((ViewGroup)((BaseActivity)context).findViewById(android.R.id.content)).getChildAt(0);//get first view
            }
        } catch (Exception e) {
			LogUtil.logError("ERROR", e);
        }
        return rootView;
    }

	/**
	 * This method formats the coordinates of a location in degrees, minutes and seconds
	 * and returns a string with it
	 *
	 * @param latitude latitude of the location to format
	 * @param longitude longitude of the location to format
	 * @return string with the location formatted in degrees, minutes and seconds
	 */
	public static String convertToDegrees(float latitude, float longitude) {
        StringBuilder builder = new StringBuilder();

		formatCoordinate(builder, latitude);
        if (latitude < 0) {
            builder.append("S ");
        } else {
            builder.append("N ");
        }

		formatCoordinate(builder, longitude);
        if (longitude < 0) {
            builder.append("W");
        } else {
            builder.append("E");
        }

        return builder.toString();
    }

	/**
	 * This method formats a coordinate in degrees, minutes and seconds
	 *
	 * @param builder StringBuilder where the string formatted it's going to be built
	 * @param coordinate coordinate to format
	 */
	private static void formatCoordinate (StringBuilder builder, float coordinate) {
		String degrees = Location.convert(Math.abs(coordinate), Location.FORMAT_SECONDS);
		String[] degreesSplit = degrees.split(":");
		builder.append(degreesSplit[0]);
		builder.append("°");
		builder.append(degreesSplit[1]);
		builder.append("'");

		try {
			builder.append(Math.round(Float.parseFloat(degreesSplit[2].replace(",", "."))));
		} catch (Exception e) {
			LogUtil.logWarning("Error rounding seconds in coordinates", e);
			builder.append(degreesSplit[2]);
		}

		builder.append("''");
	}

	public static void hideKeyboard(Activity activity, int flag){

		View v = activity.getCurrentFocus();
		if (v != null){
			InputMethodManager imm = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(v.getWindowToken(), flag);
		}
	}

	public static void hideKeyboardView(Context context, View v, int flag){

		if (v != null){
			InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(v.getWindowToken(), flag);
		}

	}

	public static boolean isPermissionGranted(Context context, String permission){
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

	/**
	 * This method detects whether the android device is tablet
	 *
	 * @param context the passed Activity to be detected
	 */
	public static boolean isTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK)
				>= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	/**
	 * This method detects whether the url matches certain URL regular expressions
	 * @param url the passed url to be detected
	 * @param regexs the array of URL regular expressions
	 */

	public static boolean matchRegexs(String url, String[] regexs) {
		if (url == null) {
			return false;
		}
		for (String regex : regexs) {
			if (url.matches(regex)) {
				return true;
			}
		}
		return false;
	}

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void showKeyboardDelayed(final View view) {
		LogUtil.logDebug("showKeyboardDelayed");
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 50);
    }
}
