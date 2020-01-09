package mega.privacy.android.app.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.R;

public class AskForDisplayOverDialog {

    private AlertDialog dialog;

    @TargetApi(IncomingCallNotification.ANDROID_10_Q)
    public AskForDisplayOverDialog(final Context context) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        final View dialogView = inflater.inflate(R.layout.ask_for_display_over_dialog_layout, null);
        dialogBuilder.setView(dialogView);

        dialogView.findViewById(R.id.btn_not_now).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.cancel();
                DatabaseHandler.getDbHandler(context).dontAskForDisplayOver();
                Toast.makeText(context , R.string.ask_for_display_over_explain, Toast.LENGTH_LONG).show();
            }
        });
        dialogView.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
                dialog.cancel();
            }
        });
        if (dialog == null) {
            dialog = dialogBuilder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        }
    }

    @TargetApi(IncomingCallNotification.ANDROID_10_Q)
    public void showDialog() {
        dialog.show();
    }

    public void recycle() {
        dialog.cancel();
    }
}
