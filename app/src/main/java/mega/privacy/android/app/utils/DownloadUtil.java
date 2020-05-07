package mega.privacy.android.app.utils;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import java.io.File;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class DownloadUtil {

    /**
     * Checks if the node is already downloaded in the current selected folder
     *
     * @param node file to check
     * @param localPath path of the local file already downloaded
     * @param currenParentPath path of the current selected folder where the file is going to be downloaded
     * @return true if the file is already downloaded in the selected folder, false otherwise
     */

    private static boolean isAlreadyDownloadedInCurrentPath(MegaNode node, String localPath, String currenParentPath, boolean downloadToSDCard, SDCardOperator sdCardOperator) {
        File file = new File(localPath);

        if (isFileAvailable(file) && !file.getParent().equals(currenParentPath)) {
            try {
                new Thread(new CopyFileThread(downloadToSDCard, localPath, currenParentPath, node.getName(), sdCardOperator)).start();
            } catch (Exception e) {
                logWarning("Exception copying file", e);
            }
            return false;
        }

        return true;
    }

    /**
     * Shows a snackbar to alert the file was already downloaded and creates the video thumbnail if needed.
     *
     * @param context activity where the snackbar has to be shown
     * @param node file to download
     * @param localPath path where the file was already downloaded
     * @param parentPath path where the file has to be downloaded this time
     */
    public static void checkDownload (Context context, MegaNode node, String localPath, String parentPath, boolean checkVideo, boolean downloadToSDCard, SDCardOperator sdCardOperator){
        if (isAlreadyDownloadedInCurrentPath(node, localPath, parentPath, downloadToSDCard, sdCardOperator)) {
            showSnackbar(context, context.getString(R.string.general_already_downloaded));
        } else if (isFileAvailable(new File(localPath))){
            showSnackbar(context, context.getString(R.string.copy_already_downloaded));
        }

        if (!checkVideo) return;

        if (node != null && isVideoFile(parentPath + "/" + node.getName()) && !node.hasThumbnail()) {
            MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
            try {
                ThumbnailUtilsLollipop.createThumbnailVideo(context, localPath, megaApi, node.getHandle());
            } catch (Exception e) {
                logWarning("Exception creating video thumbnail", e);
            }
        }
    }

    /**
     * Shows an snackbar to alert if:
     *      only one file has to be downloaded and is not downloaded yet
     *      several files have to be downloaded and some of them are already downloaded
     *
     * @param context activity where the snackbar has to be shown
     * @param numberOfNodesPending pending downloads
     * @param numberOfNodesAlreadyDownloaded files already downloaded
     */
    public static void showSnackBarWhenDownloading(Context context, int numberOfNodesPending, int numberOfNodesAlreadyDownloaded) {
        logDebug(" Already downloaded: " + numberOfNodesAlreadyDownloaded + " Pending: " + numberOfNodesPending);

        if (numberOfNodesAlreadyDownloaded == 0) {
            showSnackbar(context, context.getResources().getQuantityString(R.plurals.download_began, numberOfNodesPending, numberOfNodesPending));
        } else {
            String msg;
            msg = context.getResources().getQuantityString(R.plurals.file_already_downloaded, numberOfNodesAlreadyDownloaded, numberOfNodesAlreadyDownloaded);
            if (numberOfNodesPending > 0) {
                msg = msg + context.getResources().getQuantityString(R.plurals.file_pending_download, numberOfNodesPending, numberOfNodesPending);
            }
            showSnackbar(context, msg);
        }
    }

    public static SDCardOperator checkDownloadPath(Context context, String downloadPath) {
        DatabaseHandler dbH = MegaApplication.getInstance().getDbH();
        boolean isSDCardPath = SDCardOperator.isSDCardPath(downloadPath);
        SDCardOperator sdCardOperator = null;

        try {
            sdCardOperator = new SDCardOperator(context);
        } catch (SDCardOperator.SDCardException e) {
            e.printStackTrace();
            logError("Initialize SDCardOperator failed", e);
            // user uninstall the sd card. but default download location is still on the sd card
            if (isSDCardPath) {
                logDebug("select new path as download location.");
                return null;
            }
        }
        if (sdCardOperator != null && isSDCardPath) {
            //user has installed another sd card.
            if (sdCardOperator.isNewSDCardPath(downloadPath)) {
                logDebug("new sd card, check permission.");
                new Handler().postDelayed(() -> Toast.makeText(MegaApplication.getInstance(), R.string.old_sdcard_unavailable, Toast.LENGTH_LONG).show(), 1000);
                return null;
            }

            if (!sdCardOperator.canWriteWithFile(downloadPath)) {
                try {
                    sdCardOperator.initDocumentFileRoot(dbH.getSDCardUri());
                } catch (SDCardOperator.SDCardException e) {
                    e.printStackTrace();
                    logError("SDCardOperator initDocumentFileRoot failed requestSDCardPermission", e);
                    return null;
                }
            }
        }
        return sdCardOperator;
    }
}
