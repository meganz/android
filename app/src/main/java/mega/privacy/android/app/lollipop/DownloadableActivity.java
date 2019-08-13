package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.DownloadInfo;
import mega.privacy.android.app.utils.SDCardOperator;
import mega.privacy.android.app.utils.Util;

public class DownloadableActivity extends PinActivityLollipop {

    private DownloadInfo downloadInfo;

    public void setDownloadInfo(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    protected void onRequestSDCardWritePermission(Intent intent, int resultCode, NodeController nC) {
        if (intent == null) {
            log("intent NULL");
            if (resultCode != Activity.RESULT_OK) {
                Util.showSnackBar(this, Constants.SNACKBAR_TYPE, getString(R.string.download_requires_permission), -1);
            } else {
                Util.showSnackBar(this, Constants.SNACKBAR_TYPE, getString(R.string.donot_support_write_on_sdcard), -1);
            }
            return;
        }
        Uri treeUri = intent.getData();
        if (treeUri != null) {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            if (pickedDir.canWrite()) {
                log("sd card root uri is " + treeUri);
                //save the sd card root uri string
                DatabaseHandler.getDbHandler(this).setUriExternalSDCard(treeUri.toString());
                try {
                    SDCardOperator sdCardOperator = new SDCardOperator(this);
                    if (nC != null && downloadInfo != null) {
                        nC.requestLocalFolder(downloadInfo, sdCardOperator.getSDCardRoot(), null);
                    }
                } catch (SDCardOperator.SDCardException e) {
                    e.printStackTrace();
                    log(e.getMessage());
                }
            }
        } else {
            log("tree uri is null!");
            Util.showSnackBar(this, Constants.SNACKBAR_TYPE, getString(R.string.donot_support_write_on_sdcard), -1);
        }
    }

}
