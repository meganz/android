package mega.privacy.android.app.utils;

import android.app.ProgressDialog;
import android.content.Context;

import mega.privacy.android.app.R;

public class ProgressDialogUtil {

    private static ProgressDialog dialog;

    public static boolean isShowDialog;

    public static void showProcessFileDialog(Context context) {
        isShowDialog = true;
        dialog = new ProgressDialog(context){

            @Override
            public void onDetachedFromWindow() {
                dismiss();
            }
        };
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage(context.getString(R.string.upload_prepare));
        dialog.show();
    }

    public static void dissmisDialog() {
        isShowDialog = false;
        if(dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
