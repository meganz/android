package mega.privacy.android.app.utils;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;

import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.LogUtil.*;

public class ProgressDialogUtil {

    private static ProgressDialog dialog;

    public static boolean shouldShowDialog;
    private static boolean isPl;

    public static void showProcessFileDialog(Context context,Intent intent) {
        dialog = new ProgressDialog(context){

            @Override
            public void onDetachedFromWindow() {
                dismiss();
            }
        };
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        if (intent != null) {
            ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            isPl = (imageUris != null && imageUris.size() > 1);
            if (!isPl) {
                ClipData clipData = intent.getClipData();
                isPl = (clipData != null && clipData.getItemCount() > 1);
            }
        }
        int i = (isPl ? 2 : 1);
        String message = context.getResources().getQuantityString(R.plurals.upload_prepare,i);
        dialog.setMessage(message);
        shouldShowDialog = true;
        dialog.show();
    }

    public static void dissmisDialog() {
        shouldShowDialog = false;
        if(dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public static ProgressDialog getProgressDialog(Context context, String message) {
      ProgressDialog temp = null;
      
        try {
            temp = new ProgressDialog(context);
            temp.setMessage(message);
            temp.show();
        } catch (Exception e) {
            logWarning("Exception creating progress dialog: " + message, e);
        }

        return temp;
    }
}
