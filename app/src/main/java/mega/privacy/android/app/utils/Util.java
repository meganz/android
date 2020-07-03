package mega.privacy.android.app.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;

import android.provider.MediaStore;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.content.ContextCompat;
import android.text.Html;
import androidx.appcompat.app.ActionBar;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import androidx.core.content.FileProvider;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
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

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.GetLinkActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatFullScreenImageViewer;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.google.android.material.textfield.TextInputLayout.*;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.IncomingCallNotification.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;

public class Util {

    public static final String DATE_AND_TIME_PATTERN = "yyyy-MM-dd HH.mm.ss";
    public static int ONTRANSFERUPDATE_REFRESH_MILLIS = 1000;
	
	public static float dpWidthAbs = 360;
	public static float dpHeightAbs = 592;
	
	public static double percScreenLogin = 0.596283784; //The dimension of the grey zone (Login and Tour)
	public static double percScreenLoginReturning = 0.8;
	
	// Debug flag to enable logging and some other things
	public static boolean DEBUG = false;

	public static HashMap<String, String> countryCodeDisplay;

	// 150ms, a smaller value may cause the keyboard to fail to open
	private final static int SHOW_IM_DELAY = 150;

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

	public static String getCountryCodeByNetwork(Context context) {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		if (tm != null) {
			return tm.getNetworkCountryIso();
		}
		return null;
	}

	public static boolean isRoaming(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm != null) {
			NetworkInfo ni = cm.getActiveNetworkInfo();
			if(ni != null) {
                return ni.isRoaming();
            }
		}
		return true;
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
        src = converterShortCodes(src);
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
			logDebug("secStore: " + secStore);
	        File path = new File(secStore);
			logDebug("getFreeSize: " + path.getUsableSpace());
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

		Context context = MegaApplication.getInstance().getApplicationContext();
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
				System.gc();
			}
			return bmRotated;
		} catch (Exception e) {
			logError("Exception creating rotated bitmap", e);
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

		Context context = MegaApplication.getInstance().getApplicationContext();
		if (size < KB){
			sizeString = context.getString(R.string.label_file_size_byte, Long.toString(size));
		}
		else if (size < MB){
			sizeString = context.getString(R.string.label_file_size_kilo_byte, decf.format(size/KB));
		}
		else if (size < GB){
			sizeString = context.getString(R.string.label_file_size_mega_byte, decf.format(size/MB));
		}
		else if (size < TB){
			sizeString = context.getString(R.string.label_file_size_giga_byte, decf.format(size/GB));
		}
		else{
			sizeString = context.getString(R.string.label_file_size_tera_byte, decf.format(size/TB));
		}
		
		return sizeString;
	}

    public static String getSizeStringGBBased(long gbSize){
        String sizeString = "";
        DecimalFormat decf = new DecimalFormat("###.##");

        float TB = 1024;

		Context context = MegaApplication.getInstance().getApplicationContext();
        if (gbSize < TB){
            sizeString = context.getString(R.string.label_file_size_giga_byte, decf.format(gbSize));
        }
        else{
            sizeString = context.getString(R.string.label_file_size_tera_byte, decf.format(gbSize/TB));
        }

        return sizeString;
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
				getSizeString(progress),
				getSizeString(size));
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
			  logError("Error getting local IP address", ex);
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

		Context context = MegaApplication.getInstance().getApplicationContext();

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
		logDebug("checkBitSet");
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
		logDebug("getEmailError");
		if (value.length() == 0) {
			return context.getString(R.string.error_enter_email);
		}
		if (!Constants.EMAIL_ADDRESS.matcher(value).matches()) {
			return context.getString(R.string.error_invalid_email);
		}
		return null;
	}

	/*
	 * compare the current mail to newly changed email
	 */
	public static String comparedToCurrentEmail(String value, Context context) {
		DatabaseHandler dbH = MegaApplication.getInstance().getDbH();
		if (value.equals(dbH.getCredentials().getEmail())) {
			return context.getString(R.string.mail_same_as_old);
		}
		return null;
	}

    public static AlertDialog showAlert(Context context, String message, String title) {
        logDebug("showAlert");
        return showAlert(context, message, title, null);
    }

    /**
     * Show a simple alert dialog with a 'OK' button to dismiss itself.
     *
     * @param context Context
     * @param message the text content.
     * @param title the title of the dialog, optional.
     * @param listener callback when press 'OK' button, optional.
     * @return the created alert dialog, the caller should cancel the dialog when the context destoried, otherwise window will leak.
     */
    public static AlertDialog showAlert(Context context, String message, @Nullable String title, @Nullable DialogInterface.OnDismissListener listener) {
        logDebug("showAlert");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (title != null) {
            builder.setTitle(title);
        }
        builder.setMessage(message);
        builder.setPositiveButton(context.getString(R.string.general_ok), null);
        if (listener != null) {
            builder.setOnDismissListener(listener);
        }
        return builder.show();
    }

	public static long calculateTimestampMinDifference(String timeStamp) {
		logDebug("calculateTimestampDifference");

		Long actualTimestamp = System.currentTimeMillis()/1000;

		Long oldTimestamp = Long.parseLong(timeStamp);

		Long difference = actualTimestamp - oldTimestamp;

		difference = difference/60;

		return difference;
	}

	public static int getVersion() {
		try {
			Context context = MegaApplication.getInstance().getApplicationContext();
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			return pInfo.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			return 0;
		}
	}

	/**
	 * Checks if the app has been upgraded and store the new version code.
	 */
	public static void checkAppUpgrade() {
		final String APP_INFO_FILE = "APP_INFO";
		final String APP_VERSION_CODE_KEY = "APP_VERSION_CODE";

		Context context = MegaApplication.getInstance().getApplicationContext();
		SharedPreferences preferences = context.getSharedPreferences(APP_INFO_FILE, Context.MODE_PRIVATE);

		int oldVersionCode = preferences.getInt(APP_VERSION_CODE_KEY, 0);
		int newVersionCode = getVersion();
		if (oldVersionCode == 0 || oldVersionCode < newVersionCode) {
			if (oldVersionCode == 0) {
				logInfo("App Version: " + newVersionCode);
			} else {
				logInfo("App upgraded from " + oldVersionCode + " to " + newVersionCode);
			}
			preferences.edit().putInt(APP_VERSION_CODE_KEY, newVersionCode).apply();
		} else {
			logInfo("App Version: " + newVersionCode);
		}
	}

	public static long calculateTimestamp(String time)
	{
		logDebug("calculateTimestamp: " + time);
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
			logError("ParseException!!!", e);
		}
		return 0;
	}

	public static Calendar calculateDateFromTimestamp (long timestamp){
		logDebug("calculateTimestamp: " + timestamp);
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp*1000);
		logDebug("Calendar: " + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.MONTH));
		return cal;
	}

	public static boolean canVoluntaryVerifyPhoneNumber() {
		// If account is in ODQ Paywall state avoid ask for SMS verification because request will fail.
		if (MegaApplication.getInstance().getStorageState() == STORAGE_STATE_PAYWALL) {
			return false;
		}

        MegaApiAndroid api = MegaApplication.getInstance().getMegaApi();
	    boolean hasNotVerified = api.smsVerifiedPhoneNumber() == null;
	    boolean allowVerify = api.smsAllowedState() == 2;
	    return hasNotVerified && allowVerify;
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
		logDebug("System font size scale is " + scale);

		float newScale;

		if (scale <= 1.1) {
			newScale = scale;
		} else {
			newScale = (float) 1.1;
		}

		logDebug("New font size new scale is " + newScale);
		Configuration configuration = activity.getResources().getConfiguration();
		configuration.fontScale = newScale;

		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		metrics.scaledDensity = configuration.fontScale * metrics.density;
		activity.getBaseContext().getResources().updateConfiguration(configuration, metrics);
	}
    
    //reduce font size for scale mode to prevent title and subtitle overlap
    public static SpannableString adjustForLargeFont(String original) {
		Context context = MegaApplication.getInstance().getApplicationContext();
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

	/**
	 *Check if exist ongoing transfers
	 *
	 * @param megaApi
	 * @return true if exist ongoing transfers, false otherwise
	 */
	public static boolean existOngoingTransfers(MegaApiAndroid megaApi) {
		return megaApi.getNumPendingDownloads() > 0 || megaApi.getNumPendingUploads() > 0;
	}

	public static void changeStatusBarColorActionMode (final Context context, final Window window, Handler handler, int option) {
		logDebug("changeStatusBarColor");
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
    public static Bitmap createAvatarBackground(int color) {
        Bitmap circle = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(circle);
        Paint paintCircle = new Paint();
        paintCircle.setAntiAlias(true);
        paintCircle.setColor(color);
        int radius = circle.getWidth() / 2;
        c.drawCircle(radius, radius, radius, paintCircle);
        return circle;
    }

    public static void changeStatusBarColor(Context context, Window window, int color) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(context, color));
    }

	public static MegaPreferences getPreferences (Context context) {
		return DatabaseHandler.getDbHandler(context).getPreferences();
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

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Method to display a NOT_SPACE_SNACKBAR_TYPE Snackbar
     *
     * Use this method only from controllers or services or when ut does not know what the context is.
     *
     * @param context Class where the Snackbar has to be shown
     */
	public static void showNotEnoughSpaceSnackbar(Context context) {
		showSnackbar(context, NOT_SPACE_SNACKBAR_TYPE, null, INVALID_HANDLE);
	}

    /**
     * Method to display a simple Snackbar
     *
     * Use this method only from controllers or services or when ut does not know what the context is.
     *
     * @param context Class where the Snackbar has to be shown
     * @param message Text to shown in the snackbar
     */
	public static void showSnackbar(Context context, String message) {
		showSnackbar(context, SNACKBAR_TYPE, message, INVALID_HANDLE);
	}

    /**
     * Method to display a simple or action Snackbar.
     *
     * Use this method only from controllers or services or when ut does not know what the context is.
     *
     * @param context Class where the Snackbar has to be shown
     * @param snackbarType specifies the type of the Snackbar.
     *                     It can be SNACKBAR_TYPE, MESSAGE_SNACKBAR_TYPE or NOT_SPACE_SNACKBAR_TYPE
     * @param message Text to shown in the snackbar
     * @param idChat Chat ID. If this param has a valid value, different to -1, the function of MESSAGE_SNACKBAR_TYPE ends in the specified chat
     */
	public static void showSnackbar(Context context, int snackbarType, String message, long idChat) {
		if (context instanceof FullScreenImageViewerLollipop) {
			((FullScreenImageViewerLollipop) context).showSnackbar(snackbarType, message, idChat);
		} else if (context instanceof FileInfoActivityLollipop) {
			((FileInfoActivityLollipop) context).showSnackbar(snackbarType, message, idChat);
		} else if (context instanceof ContactFileListActivityLollipop) {
			((ContactFileListActivityLollipop) context).showSnackbar(snackbarType, message);
		} else if (context instanceof ContactInfoActivityLollipop) {
			((ContactInfoActivityLollipop) context).showSnackbar(snackbarType, message, idChat);
		} else if (context instanceof GetLinkActivityLollipop) {
			((GetLinkActivityLollipop) context).showSnackbar(message);
		} else if (context instanceof ChatFullScreenImageViewer) {
			((ChatFullScreenImageViewer) context).showSnackbar(snackbarType, message);
		} else if (context instanceof AudioVideoPlayerLollipop) {
			((AudioVideoPlayerLollipop) context).showSnackbar(snackbarType, message, idChat);
		} else if (context instanceof PdfViewerActivityLollipop) {
			((PdfViewerActivityLollipop) context).showSnackbar(snackbarType, message, idChat);
		} else if (context instanceof ChatActivityLollipop) {
			((ChatActivityLollipop) context).showSnackbar(snackbarType, message, idChat);
		} else if (context instanceof NodeAttachmentHistoryActivity) {
			((NodeAttachmentHistoryActivity) context).showSnackbar(snackbarType, message);
		} else if (context instanceof ManagerActivityLollipop) {
			((ManagerActivityLollipop) context).showSnackbar(snackbarType, message, idChat);
		} else if (context instanceof BaseActivity) {
			View rootView = getRootViewFromContext(context);
			if (rootView != null) {
				((BaseActivity) context).showSnackbar(snackbarType, rootView, message, idChat);
				return;
			}

			logWarning("Unable to show snack bar, view does not exist or context is not instance of BaseActivity");
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
			logError("ERROR", e);
        }
        return rootView;
    }

    public static String normalizePhoneNumber(String phoneNumber,String countryCode) {
        return PhoneNumberUtils.formatNumberToE164(phoneNumber, countryCode);
    }

    public static String normalizePhoneNumberByNetwork(Context context,String phoneNumber) {
        String countryCode = getCountryCodeByNetwork(context);
        if(countryCode == null) {
            return null;
        }
        return normalizePhoneNumber(phoneNumber, countryCode.toUpperCase());
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
			logWarning("Error rounding seconds in coordinates", e);
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

	public static boolean isScreenInPortrait(Context context) {
		if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			return true;
		} else {
			return false;
		}
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

	/**
	 * This method decodes a url and formats it before its treatment
	 *
	 * @param url the passed url to be decoded
	 */
	public static String decodeURL(String url) {
		try {
			url = URLDecoder.decode(url, "UTF-8");
		} catch (Exception e) {
			logDebug("Exception decoding url: "+url);
			e.printStackTrace();
		}

		url = url.replace(' ', '+');

		if (url.startsWith("mega://")) {
			url = url.replaceFirst("mega://", "https://mega.nz/");
		} else if (url.startsWith("mega.")) {
			url = url.replaceFirst("mega.", "https://mega.");
		}

		if (url.startsWith("https://www.mega.co.nz")) {
			url = url.replaceFirst("https://www.mega.co.nz", "https://mega.co.nz");
		}

		if (url.startsWith("https://www.mega.nz")) {
			url = url.replaceFirst("https://www.mega.nz", "https://mega.nz");
		}

		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}

		logDebug("URL decoded: " + url);
		return url;
	}

    /**
     * Convert color integer to corresponding string in hex format.
     *
     * @param color An integer which represents a color.
     * @return The color string in hex format, e.g., #FFABCDEF.
     */
	public static String getHexValue(int color){
		return String.format("#%06X", 0xFFFFFF & color);
	}

    public static void showKeyboardDelayed(final View view) {
		if (view == null) return;

		Handler handler = new Handler();
        handler.postDelayed(() -> {
			// The view needs to request the focus or the keyboard may not pops up
			if (view.requestFocus()) {
				InputMethodManager imm = (InputMethodManager)
						MegaApplication.getInstance().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, SHOW_IM_DELAY);
    }

    public static Spanned getSpannedHtmlText(String string) {

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
			return Html.fromHtml(string, Html.FROM_HTML_MODE_LEGACY);
		}

		return Html.fromHtml(string);
	}

	public static void checkTakePicture(Activity activity, int option) {
		if (isNecessaryDisableLocalCamera() != -1) {
			if(option == TAKE_PHOTO_CODE) {
				showConfirmationOpenCamera(activity, ACTION_TAKE_PICTURE, false);
			}else if(option == TAKE_PICTURE_PROFILE_CODE){
				showConfirmationOpenCamera(activity, ACTION_TAKE_PROFILE_PICTURE, false);
			}
			return;
		}
		takePicture(activity, option);
	}

	/**
	 * This method is to start camera from Activity
	 *
	 * @param activity the activity the camera would start from
	 */
	public static void takePicture(Activity activity, int option) {
		logDebug("takePicture");
		File newFile = buildTempFile(activity, "picture.jpg");
		try {
			newFile.createNewFile();
		} catch (IOException e) {}

		//This method is in the v4 support library, so can be applied to all devices
		Uri outputFileUri = FileProvider.getUriForFile(activity, AUTHORITY_STRING_FILE_PROVIDER, newFile);

		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
		cameraIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		activity.startActivityForResult(cameraIntent, option);
	}

	public static void resetActionBar(ActionBar aB) {
		if (aB != null) {
			View customView = aB.getCustomView();
			if(customView != null){
				ViewParent parent = customView.getParent();
				if(parent != null){
					((ViewGroup) parent).removeView(customView);
				}
			}
			aB.setDisplayShowCustomEnabled(false);
			aB.setDisplayShowTitleEnabled(true);
		}
	}

	public static boolean isAndroid10() {
		return Build.VERSION.SDK_INT >= ANDROID_10_Q;
	}

	public static void setPasswordToggle(TextInputLayout textInputLayout, boolean focus){
		if (focus) {
			textInputLayout.setEndIconMode(END_ICON_PASSWORD_TOGGLE);
			textInputLayout.setEndIconDrawable(R.drawable.password_toggle);
		} else {
			textInputLayout.setEndIconMode(END_ICON_NONE);
		}
	}

	/**
	 * Changes the elevation of the the ActionBar passed as parameter.
	 *
	 * @param aB				ActionBar in which the elevation has to be applied.
	 * @param withElevation	true if should apply elevation, false otherwise.
	 * @param outMetrics	DisplayMetrics of the current device.
	 */
	public static void changeViewElevation(ActionBar aB, boolean withElevation, DisplayMetrics outMetrics) {
		float elevation = px2dp(4, outMetrics);

		if (withElevation) {
			aB.setElevation(elevation);
		} else {
			aB.setElevation(0);
		}
	}
}
