package mega.privacy.android.app.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;

import static mega.privacy.android.app.lollipop.PermissionsFragment.PERMISSIONS_FRAGMENT;
import static mega.privacy.android.app.utils.LogUtil.logError;

@TargetApi(Build.VERSION_CODES.M)
public class PermissionUtils {

    /**
     * Check if the user ticket 'Don't ask again' and deny a permission request.
     * In this case, the system request permission dialog can no longer show up.
     *
     * @param activity the Context.
     * @param permission which permission to check.
     * @return false if the user ticket 'Don't ask again' and deny, otherwise true.
     */
    public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    /**
     * Provide an OnClickListener for snackbar's action.
     *
     * @param context Context.
     * @return an OnClickListener, which leads to the APP info page, in where, users can grant MEGA permissions.
     */
    public static View.OnClickListener toAppInfo(Context context) {
        return v -> {
            if(context == null) {
                return;
            }

            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);

            try {
                context.startActivity(intent);
            } catch (Exception e) {
                if (context instanceof ManagerActivityLollipop) {
                    // in case few devices cannot handle 'ACTION_APPLICATION_DETAILS_SETTINGS' action.
                    Util.showSnackbar(context, context.getString(R.string.on_permanently_denied));
                } else {
                    logError("Exception opening device settings", e);
                }
            }
        };
    }

    /**
     * Check permissions whether are granted
     * @param context Context
     * @param permissions one or more permission strings
     * @return whether permissions has ben granted
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                        (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                                permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE))) {
                    if (!Environment.isExternalStorageManager()) {
                        return false;
                    }
                } else if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Ask permissions
     * @param activity The activity
     * @param requestCode request code of permission asking
     * @param permissions requested permissions
     */
    public static void requestPermission(Activity activity, int requestCode, String... permissions) {
        if (permissions != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                for (String permission : permissions) {
                    if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                            permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        if (!Environment.isExternalStorageManager()) {
                            requestManageExternalStoragePermission(activity);
                            return;
                        }
                    }
                }
            }

            ActivityCompat.requestPermissions(activity,
                    permissions,
                    requestCode);
        }
    }

    /**
     * Ask for the MANAGE_EXTERNAL_STORAGE special permission required by the app since Android 11
     * @param context Context
     */
    @TargetApi(Build.VERSION_CODES.R)
    public static void requestManageExternalStoragePermission(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setData(Uri.parse(String.format("package:%s", context.getPackageName())));
            ((ManagerActivityLollipop) context).startActivityForResult(intent, PERMISSIONS_FRAGMENT);
        } catch (Exception e) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            ((ManagerActivityLollipop) context).startActivityForResult(intent, PERMISSIONS_FRAGMENT);
        }
    }
 }
