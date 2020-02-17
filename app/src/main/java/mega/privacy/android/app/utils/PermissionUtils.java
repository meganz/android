package mega.privacy.android.app.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import mega.privacy.android.app.R;

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
     * @param context Contex.
     * @return an OnClickListener, which leads to the APP info page, in where, users can grant MEGA permissions.
     */
    public static View.OnClickListener toAppInfo(Context context) {
        return v -> {
            if(context == null) {
                return;
            }
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                // in case few devices cannot hanle 'ACTION_APPLICATION_DETAILS_SETTINGS' action.
                Util.showSnackbar(context, context.getString(R.string.on_permanently_denied));
            }
        };
    }
 }
