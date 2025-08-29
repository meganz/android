package mega.privacy.android.app.utils;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.google.android.material.textfield.TextInputLayout.END_ICON_NONE;
import static com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE;
import static mega.privacy.android.app.utils.CacheFolderManager.buildTempFile;
import static mega.privacy.android.app.utils.CallUtil.isNecessaryDisableLocalCamera;
import static mega.privacy.android.app.utils.CallUtil.showConfirmationOpenCamera;
import static mega.privacy.android.app.utils.ChatUtil.converterShortCodes;
import static mega.privacy.android.app.utils.Constants.ACTION_TAKE_PICTURE;
import static mega.privacy.android.app.utils.Constants.ACTION_TAKE_PROFILE_PICTURE;
import static mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.TAKE_PHOTO_CODE;
import static mega.privacy.android.app.utils.Constants.TAKE_PICTURE_PROFILE_CODE;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.di.DbHandlerModuleKt;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.presentation.extensions.StorageStateExtensionsKt;
import mega.privacy.android.data.model.MegaPreferences;
import mega.privacy.android.domain.entity.StorageState;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import timber.log.Timber;

public class Util {

    public static final String DATE_AND_TIME_PATTERN = "yyyy-MM-dd HH.mm.ss";
    public static float dpWidthAbs = 360;

    public static HashMap<String, String> countryCodeDisplay;

    private static long lastClickTime;

    // 150ms, a smaller value may cause the keyboard to fail to open
    public final static long SHOW_IM_DELAY = 150;

    /**
     * Language tag for simplified Chinese.
     */
    private static final String HANS = "Hans";

    /*
     * Build error dialog
     * @param message Message to display
     * @param finish Should activity finish after dialog dismis
     * @param activity Source activity
     */
    public static void showErrorAlertDialog(String message, final boolean finish, final Activity activity) {
        if (activity == null) {
            return;
        }

        try {
            MaterialAlertDialogBuilder dialogBuilder = getCustomAlertBuilder(activity, activity.getString(R.string.general_error_word), message, null);
            dialogBuilder.setPositiveButton(activity.getString(android.R.string.ok), (dialog, which) -> {
                dialog.dismiss();
                if (finish) {
                    activity.finish();
                }
            });
            dialogBuilder.setOnCancelListener(dialog -> {
                if (finish) {
                    activity.finish();
                }
            });

            AlertDialog dialog = dialogBuilder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();
            brandAlertDialog(dialog);
        } catch (Exception ex) {
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

    /**
     * Indicates whether the device is currently roaming on this network
     *
     * @param context
     * @return Boolean. True if the device is currently roaming on this network otherwise false
     * @deprecated <p> Use {@link mega.privacy.android.domain.usecase.environment.IsConnectivityInRoamingStateUseCase} instead.
     */
    @Deprecated
    public static boolean isRoaming(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null) {
                return ni.isRoaming();
            }
        }
        return true;
    }

    public static int countMatches(Pattern pattern, String string) {
        int count = 0;
        int pos = 0;
        try {
            Matcher matcher = pattern.matcher(string);

            while (matcher.find(pos)) {
                count++;
                pos = matcher.start() + 1;
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return count;
    }

    public static String toCDATA(String src) {
        if (src != null) {
            //solution from web client
            src = src.replaceAll("&", "&amp;")
                    .replaceAll("\"", "&quot;")
                    .replaceAll("'", "&#39;")
                    .replaceAll("<", "&lt;")
                    .replaceAll(">", "&gt;");
            //another solution
        }
        src = converterShortCodes(src);
        return src;
    }

    public static String getNumberItemChildren(File file, Context context) {
        File[] list = file.listFiles();
        int count = 0;
        if (list != null) {
            count = list.length;
        }

        return context.getResources().getQuantityString(R.plurals.general_num_items, count, count);
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
    public static MaterialAlertDialogBuilder getCustomAlertBuilder(Activity activity, String title, String message, View view) {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(activity);
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

        TextView titleView = (TextView) customView.findViewById(R.id.dialog_title);
        titleView.setText(title);

        TextView messageView = (TextView) customView.findViewById(R.id.message);
        if (message == null) {
            messageView.setVisibility(View.GONE);
        } else {
            messageView.setText(message);
        }
        return (ViewGroup) customView;
    }

    /*
     * Show Toast message with String
     */
    public static void showToast(Context context, String message) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
        }
        ;
    }

    public static float getScaleW(DisplayMetrics outMetrics, float density) {

        float scale = 0;

        float dpWidth = outMetrics.widthPixels / density;
        scale = dpWidth / dpWidthAbs;

        return scale;
    }

    /**
     * Convert dp to px.
     *
     * @param dp         dp value
     * @param outMetrics display metrics
     * @return corresponding dp value
     */
    public static int dp2px(float dp, DisplayMetrics outMetrics) {
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, outMetrics));
    }

    public static int dp2px(float dp) {
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                Resources.getSystem().getDisplayMetrics()));
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

    /**
     * Check if device connect to network
     *
     * @deprecated use MonitorConnectivityUseCase instead
     */
    @Deprecated
    static public boolean isOnline(Context context) {
        if (context == null) return true;

        ConnectivityManager cm =
                (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    /**
     * Gets a size string.
     *
     * @param size the size to show in the string
     * @return The size string.
     * @deprecated Use [mega.privacy.android.app.presentation.mapper.file.FileSizeMapper] instead
     */
    @Deprecated
    public static String getSizeString(long size, Context context) {
        DecimalFormat df = new DecimalFormat("#.##");

        float KB = 1024;
        float MB = KB * 1024;
        float GB = MB * 1024;
        float TB = GB * 1024;
        float PB = TB * 1024;
        float EB = PB * 1024;

        if (size < KB) {
            return context.getString(R.string.label_file_size_byte, Long.toString(size));
        } else if (size < MB) {
            return context.getString(R.string.label_file_size_kilo_byte, df.format(size / KB));
        } else if (size < GB) {
            return context.getString(R.string.label_file_size_mega_byte, df.format(size / MB));
        } else if (size < TB) {
            return context.getString(R.string.label_file_size_giga_byte, df.format(size / GB));
        } else if (size < PB) {
            return context.getString(R.string.label_file_size_tera_byte, df.format(size / TB));
        } else if (size < EB) {
            return context.getString(R.string.label_file_size_peta_byte, df.format(size / PB));
        } else {
            return context.getString(R.string.label_file_size_exa_byte, df.format(size / EB));
        }
    }

    public static String getSizeStringGBBased(long gbSize) {
        String sizeString = "";
        DecimalFormat decf = new DecimalFormat("###.##");

        float TB = 1024;

        Context context = MegaApplication.getInstance().getApplicationContext();
        if (gbSize < TB) {
            sizeString = context.getString(R.string.label_file_size_giga_byte, decf.format(gbSize));
        } else {
            sizeString = context.getString(R.string.label_file_size_tera_byte, decf.format(gbSize / TB));
        }

        return sizeString;
    }

    public static void brandAlertDialog(AlertDialog dialog) {
        try {
            Resources resources = dialog.getContext().getResources();

            int alertTitleId = resources.getIdentifier("alertTitle", "id", "android");

            TextView alertTitle = (TextView) dialog.getWindow().getDecorView().findViewById(alertTitleId);
            if (alertTitle != null) {
                alertTitle.setTextColor(ContextCompat.getColor(dialog.getContext(), R.color.red_600_red_300)); // change title text color
            }

            int titleDividerId = resources.getIdentifier("titleDivider", "id", "android");
            View titleDivider = dialog.getWindow().getDecorView().findViewById(titleDividerId);
            if (titleDivider != null) {
                titleDivider.setBackgroundColor(ContextCompat.getColor(dialog.getContext(), R.color.red_600_red_300)); // change divider color
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
                getSizeString(progress, context),
                getSizeString(size, context));
    }

    /*
     * Set alpha transparency for view
     */
    @SuppressLint("NewApi")
    public static void setViewAlpha(View view, float alpha) {
        view.setAlpha(alpha);
    }

    public static String getPhotoSyncName(long timeStamp, String fileName) {
        DateFormat sdf = new SimpleDateFormat(DATE_AND_TIME_PATTERN, Locale.getDefault());
        return sdf.format(new Date(timeStamp)) + fileName.substring(fileName.lastIndexOf('.'));
    }

    public static String getLocalIpAddress(Context context) {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                String interfaceName = intf.getName();

                // Ensure get the IP from the current active network interface
                ConnectivityManager cm =
                        (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                String activeInterfaceName = cm.getLinkProperties(cm.getActiveNetwork()).getInterfaceName();
                if (interfaceName.compareTo(activeInterfaceName) != 0) {
                    continue;
                }

                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress != null && !inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            Timber.e(ex, "Error getting local IP address");
        }
        return null;
    }

    /**
     * Returns the consumer friendly device name.
     * If Android version is above 7, the name is manufacturer + custom name set by user, otherwise, will be manufacturer + model.
     *
     * @return Device name, always starts with manufacturer, prefer user set name.
     */
    public static String getDeviceName() {
        return Build.MANUFACTURER + " " + Settings.Global.getString(MegaApplication.getInstance().getContentResolver(), Settings.Global.DEVICE_NAME);
    }

    public static int scaleHeightPx(int px, DisplayMetrics metrics) {
        int myHeightPx = metrics.heightPixels;

        return px * myHeightPx / 548; //Based on Eduardo's measurements
    }

    public static int scaleWidthPx(int px, DisplayMetrics metrics) {
        int myWidthPx = metrics.widthPixels;

        return px * myWidthPx / 360; //Based on Eduardo's measurements

    }


    public static AlertDialog showAlert(Context context, String message, String title) {
        Timber.d("showAlert");
        return showAlert(context, message, title, null);
    }

    /**
     * Show a simple alert dialog with a 'OK' button to dismiss itself.
     *
     * @param context  Context
     * @param message  the text content.
     * @param title    the title of the dialog, optional.
     * @param listener callback when press 'OK' button, optional.
     * @return the created alert dialog, the caller should cancel the dialog when the context destoried, otherwise window will leak.
     */
    public static AlertDialog showAlert(Context context, String message, @Nullable String title, @Nullable DialogInterface.OnDismissListener listener) {
        Timber.d("showAlert");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (title != null) {
            builder.setTitle(title);
        }
        builder.setMessage(message);
        builder.setPositiveButton(context.getString(mega.privacy.android.shared.resources.R.string.general_ok), null);
        if (listener != null) {
            builder.setOnDismissListener(listener);
        }
        return builder.show();
    }

    public static long calculateTimestamp(String time) {
        Timber.d("calculateTimestamp: %s", time);
        long unixtime;
        DateFormat dfm = new SimpleDateFormat("yyyyMMddHHmm");
        dfm.setTimeZone(TimeZone.getDefault());//Specify your timezone
        try {
            unixtime = dfm.parse(time).getTime();
            unixtime = unixtime / 1000;
            return unixtime;
        } catch (ParseException e) {
            Timber.e(e);
        }
        return 0;
    }

    public static Calendar calculateDateFromTimestamp(long timestamp) {
        Timber.d("calculateTimestamp: %s", timestamp);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp * 1000);
        Timber.d("Calendar: %d %d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
        return cal;
    }

    public static boolean canVoluntaryVerifyPhoneNumber() {
        // If account is in ODQ Paywall state avoid ask for SMS verification because request will fail.
        if (StorageStateExtensionsKt.getStorageState() == StorageState.PayWall) {
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
        Timber.d("System font size scale is %s", scale);

        float newScale;

        if (scale <= 1.1) {
            newScale = scale;
        } else {
            newScale = (float) 1.1;
        }

        Timber.d("New font size new scale is %s", newScale);
        Configuration configuration = activity.getResources().getConfiguration();
        configuration.fontScale = newScale;

        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        activity.getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    public static Drawable mutateIconSecondary(Context context, int idDrawable, int idColor) {
        Drawable icon = ContextCompat.getDrawable(context, idDrawable);
        icon = icon.mutate();
        icon.setColorFilter(ContextCompat.getColor(context, idColor), PorterDuff.Mode.SRC_ATOP);

        return icon;
    }

    /**
     * Gets the status bar height if available.
     *
     * @return The status bar height if available.
     */
    public static int getStatusBarHeight() {
        return getSystemBarHeight("status_bar_height");
    }

    /**
     * Gets the navigation bar height if available.
     *
     * @return The status bar height if available.
     */
    public static int getNavigationBarHeight() {
        return getSystemBarHeight("navigation_bar_height");
    }

    /**
     * Gets a system bar height if available.
     *
     * @param systemBarName The system bar name.
     * @return The system bar height if available.
     */
    public static int getSystemBarHeight(String systemBarName) {
        Context context = MegaApplication.getInstance().getBaseContext();
        int resourceId = context.getResources().getIdentifier(systemBarName, "dimen",
                "android");

        return resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId)
                : 0;
    }

    public static MegaPreferences getPreferences() {
        return DbHandlerModuleKt.getDbHandler().getPreferences();
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
     * Method to display a simple Snackbar
     * <p>
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
     * <p>
     * Use this method only from controllers or services or when ut does not know what the context is.
     *
     * @param context      Class where the Snackbar has to be shown
     * @param snackbarType specifies the type of the Snackbar.
     *                     It can be SNACKBAR_TYPE, MESSAGE_SNACKBAR_TYPE or NOT_SPACE_SNACKBAR_TYPE
     * @param message      Text to shown in the snackbar
     * @param idChat       Chat ID. If this param has a valid value, different to -1, the function of MESSAGE_SNACKBAR_TYPE ends in the specified chat
     */
    public static void showSnackbar(Context context, int snackbarType, String message, long idChat) {
        if (context instanceof SnackbarShower) {
            ((SnackbarShower) context).showSnackbar(snackbarType, message, idChat);
        } else {
            Timber.w("Unable to show snack bar, view does not exist or context is not instance of SnackbarShower");
        }
    }

    public static View getRootViewFromContext(Context context) {
        BaseActivity activity = (BaseActivity) context;
        View rootView = null;
        try {
            rootView = activity.findViewById(android.R.id.content);
            if (rootView == null) {
                rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
            }
            if (rootView == null) {
                rootView = ((ViewGroup) ((BaseActivity) context).findViewById(android.R.id.content)).getChildAt(0);//get first view
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return rootView;
    }

    public static String normalizePhoneNumber(String phoneNumber, String countryCode) {
        return PhoneNumberUtils.formatNumberToE164(phoneNumber, countryCode);
    }

    /**
     * Get normalized phone number by network
     *
     * @param context
     * @param phoneNumber
     * @return String. Normalized phone number
     * @deprecated <p> Use {@link mega.privacy.android.domain.usecase.contact.GetNormalizedPhoneNumberByNetworkUseCase} instead.
     */
    @Deprecated
    public static String normalizePhoneNumberByNetwork(Context context, String phoneNumber) {
        String countryCode = getCountryCodeByNetwork(context);
        if (countryCode == null) {
            return null;
        }
        return normalizePhoneNumber(phoneNumber, countryCode.toUpperCase());
    }

    /**
     * This method formats the coordinates of a location in degrees, minutes and seconds
     * and returns a string with it
     *
     * @param latitude  latitude of the location to format
     * @param longitude longitude of the location to format
     * @return string with the location formatted in degrees, minutes and seconds
     * @deprecated Use ChatLocationMessageView.getGPSCoordinates instead.
     */
    @Deprecated
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
     * @param builder    StringBuilder where the string formatted it's going to be built
     * @param coordinate coordinate to format
     * @deprecated Use ChatLocationMessageView.formatCoordinate instead.
     */
    @Deprecated
    private static void formatCoordinate(StringBuilder builder, float coordinate) {
        String degrees = Location.convert(Math.abs(coordinate), Location.FORMAT_SECONDS);
        String[] degreesSplit = degrees.split(":");
        builder.append(degreesSplit[0]);
        builder.append("°");
        builder.append(degreesSplit[1]);
        builder.append("'");

        try {
            builder.append(Math.round(Float.parseFloat(degreesSplit[2].replace(",", "."))));
        } catch (Exception e) {
            Timber.w(e, "Error rounding seconds in coordinates");
            builder.append(degreesSplit[2]);
        }

        builder.append("''");
    }

    public static void hideKeyboard(Activity activity, int flag) {
        View v = activity.getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), flag);
        }
    }

    public static void hideKeyboardView(Context context, View v, int flag) {
        if (v != null) {
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
     * This method detects whether the url matches certain URL regular expressions
     *
     * @param url    the passed url to be detected
     * @param regexs the array of URL regular expressions
     * @deprecated use {@link mega.privacy.android.domain.usecase.IsUrlMatchesRegexUseCase} instead.
     */
    @Deprecated()
    public static boolean matchRegexs(String url, String[] regexs) {
        if (url == null) {
            return false;
        }
        for (String regex : regexs) {
            if (url.matches(regex)) {
                Timber.d("REGEX MATCH: %s", regex);
                return true;
            }
        }
        return false;

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

    public static void checkTakePicture(Activity activity, int option) {
        if (isNecessaryDisableLocalCamera() != MEGACHAT_INVALID_HANDLE) {
            if (option == TAKE_PHOTO_CODE) {
                showConfirmationOpenCamera(activity, ACTION_TAKE_PICTURE, false);
            } else if (option == TAKE_PICTURE_PROFILE_CODE) {
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
        Timber.d("takePicture");
        File newFile = buildTempFile("picture.jpg");
        try {
            newFile.createNewFile();
        } catch (IOException e) {
        }

        //This method is in the v4 support library, so can be applied to all devices
        Uri outputFileUri = FileProvider.getUriForFile(activity, AUTHORITY_STRING_FILE_PROVIDER, newFile);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        cameraIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cameraIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            activity.startActivityForResult(cameraIntent, option);
        } catch (Exception e) {
            Timber.d("Can not handle action MediaStore.ACTION_IMAGE_CAPTURE");
        }
    }

    public static void resetActionBar(ActionBar aB) {
        if (aB != null) {
            View customView = aB.getCustomView();
            if (customView != null) {
                ViewParent parent = customView.getParent();
                if (parent != null) {
                    ((ViewGroup) parent).removeView(customView);
                }
            }
            aB.setDisplayShowCustomEnabled(false);
            aB.setDisplayShowTitleEnabled(true);
        }
    }

    /**
     * Checks if the current Android version is Android 11 or upper.
     *
     * @return True if the current Android version is Android 11 or upper, false otherwise.
     */
    public static boolean isAndroid11OrUpper() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    public static boolean isAndroid10OrUpper() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    public static void setPasswordToggle(TextInputLayout textInputLayout, boolean focus) {
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
     * @param aB            ActionBar in which the elevation has to be applied.
     * @param withElevation true if should apply elevation, false otherwise.
     * @param outMetrics    DisplayMetrics of the current device.
     */
    public static void changeViewElevation(ActionBar aB, boolean withElevation, DisplayMetrics outMetrics) {
        float elevation = dp2px(4, outMetrics);

        if (withElevation) {
            aB.setElevation(elevation);
        } else {
            aB.setElevation(0);
        }
    }

    /**
     * Gets a reference to a given drawable and prepares it for use with tinting through.
     *
     * @param resId the resource id for the given drawable
     * @return a wrapped drawable ready fo use
     * with {@link DrawableCompat}'s tinting methods
     * @throws Resources.NotFoundException
     */
    public static Drawable getWrappedDrawable(Context context, @DrawableRes int resId)
            throws Resources.NotFoundException {
        return DrawableCompat.wrap(ResourcesCompat.getDrawable(context.getResources(),
                resId, null));
    }

    /**
     * Judge if current mode is Dark mode
     *
     * @param context the Context
     * @return true if it is dark mode, false for light mode
     */
    public static boolean isDarkMode(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Method for displaying a snack bar when is Offline.
     *
     * @return True, is is Offline. False it is Online.
     */
    public static boolean isOffline(Context context) {
        if (!isOnline(context)) {
            Util.showSnackbar(context, context.getString(R.string.error_server_connection_problem));
            return true;
        }
        return false;
    }

    /**
     * Create a RecyclerView.ItemAnimator that doesn't support change animation.
     *
     * @return the RecyclerView.ItemAnimator
     */
    public static RecyclerView.ItemAnimator noChangeRecyclerViewItemAnimator() {
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setSupportsChangeAnimations(false);
        return itemAnimator;
    }

    /**
     * Judge if an activity is on the top of the running app task
     *
     * @param className the class name of the activity
     * @param context   the Context
     * @return true if the activity is on the task top, false otherwise
     */
    public static boolean isTopActivity(String className, Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.AppTask> tasks = am.getAppTasks();
        for (ActivityManager.AppTask task : tasks) {
            ActivityManager.RecentTaskInfo taskInfo = task.getTaskInfo();
            if (taskInfo.id != -1) {  // Task is running
                return taskInfo.topActivity.getClassName().contains(className);
            }
        }

        return false;
    }

    public static boolean isSimplifiedChinese() {
        return Locale.getDefault().toLanguageTag().contains(HANS);
    }

    /**
     * Method to know the current orientation of the device
     *
     * @return current orientation of the device
     */
    public static int getCurrentOrientation() {
        return MegaApplication.getInstance().getApplicationContext().getResources().getConfiguration().orientation;
    }
}
