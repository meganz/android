package mega.privacy.android.app.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.R;
import mega.privacy.android.app.di.DbHandlerModuleKt;

@TargetApi(Build.VERSION_CODES.Q)
public class AskForDisplayOverDialog {

    private AlertDialog dialog;
    private Context context;

    public AskForDisplayOverDialog(final Context context) {
        this.context = context;

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(context);
        dialogBuilder.setView(R.layout.ask_for_display_over_dialog_layout);
        dialogBuilder.setPositiveButton(R.string.general_allow, (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
            dismiss();
        });
        dialogBuilder.setNegativeButton(R.string.verify_account_not_now_button, (dialog, which) -> {
            Toast.makeText(context, R.string.ask_for_display_over_explain, Toast.LENGTH_LONG).show();
            dismiss();
        });

        if (dialog == null) {
            dialog = dialogBuilder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        }
    }

    private void dismiss() {
        DbHandlerModuleKt.getDbHandler().dontAskForDisplayOver();
        dialog.cancel();
    }


    public void showDialog() {
        if (IncomingCallNotification.shouldNotify(context) && DbHandlerModuleKt.getDbHandler().shouldAskForDisplayOver()) {
            dialog.show();
        }
    }

    public void recycle() {
        dialog.cancel();
    }
}
