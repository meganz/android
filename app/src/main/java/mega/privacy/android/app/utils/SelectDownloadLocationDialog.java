package mega.privacy.android.app.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import mega.privacy.android.app.R;

public class SelectDownloadLocationDialog {

    private Context context;

    private String[] sdCardOptions;

    private String titleDefaultDownloadLocation, titleDownloadLocation;

    private AlertDialog.Builder dialogBuilder;

    private boolean isDefaultLocation;

    public SelectDownloadLocationDialog(Context context) {
        this.context = context;
        sdCardOptions = context.getResources().getStringArray(R.array.settings_storage_download_location_array);
        titleDefaultDownloadLocation = context.getResources().getString(R.string.settings_storage_download_location);
        titleDownloadLocation = context.getResources().getString(R.string.title_select_download_location);
        dialogBuilder = new AlertDialog.Builder(context);
    }

    public void setIsDefaultLocation(boolean isDefaultLocation) {
        this.isDefaultLocation = isDefaultLocation;
    }

    public void initDialogBuilder(DialogInterface.OnClickListener listener) {
        setTitle();
        dialogBuilder.setNegativeButton(context.getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialogBuilder.setItems(sdCardOptions, listener);
    }

    private void setTitle() {
        if (isDefaultLocation) {
            dialogBuilder.setTitle(titleDefaultDownloadLocation);
        } else {
            dialogBuilder.setTitle(titleDownloadLocation);
        }
    }

    public void show() {
        dialogBuilder.create().show();
    }
}
