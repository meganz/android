package mega.privacy.android.app.utils;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.MegaApplication;

import static mega.privacy.android.app.utils.Constants.APP_DATA_INDICATOR;
import static mega.privacy.android.app.utils.Constants.APP_DATA_SD_CARD;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

public class SDCardUtils {

    public static final int APP_DATA_TARGET_PATH_POSITION = 1;
    public static final int APP_DATA_TARGET_URI_POSITION = 2;
    public static final int APP_DATA_SD_CARD_PARTS = 2;

    public static String getSDCardRoot(String path) {
        int i = 0, x = 0;
        char[] chars = path.toCharArray();
        for (; x < chars.length; x++) {
            char c = chars[x];
            if (c == '/') {
                i++;
            }
            if (i == 3) {
                break;
            }
        }
        return path.substring(0, x);
    }

    public static boolean isLocalFolderOnSDCard(Context context, String localPath) {
        File[] fs = context.getExternalFilesDirs(null);
        if(fs.length > 1 && fs[1] != null) {
            String sdRoot = getSDCardRoot(fs[1].getAbsolutePath());
            return localPath.startsWith(sdRoot);
        }
        return false;
    }

    /**
     * Gets the name of a SD card folder from an Uri.
     *
     * @param treeUri   the Uri to get the name of the folder
     * @return The name of the SD card folder.
     */
    public static String getSDCardDirName(Uri treeUri) {
        DocumentFile pickedDir = DocumentFile.fromTreeUri(MegaApplication.getInstance(), treeUri);
        return pickedDir != null && pickedDir.canWrite() ? pickedDir.getName() : null;
    }

    public static String getSDCardTargetPath(String appData) {
        String[] appDataParts = getSDCardAppDataParts(appData);
        if (appDataParts == null) {
            return null;
        }

        return appDataParts[APP_DATA_TARGET_PATH_POSITION];
    }

    public static String getSDCardTargetUri(String appData) {
        String[] appDataParts = getSDCardAppDataParts(appData);
        if (appDataParts == null || appDataParts.length <= APP_DATA_SD_CARD_PARTS) {
            return null;
        }

        return appDataParts[APP_DATA_TARGET_URI_POSITION];
    }

    public static String[] getSDCardAppDataParts(String appData) {
        if (isTextEmpty(appData) || !appData.contains(APP_DATA_SD_CARD)) {
            return null;
        }

        String[] appDataParts = appData.split(APP_DATA_INDICATOR);
        if (appDataParts != null && appDataParts.length >= APP_DATA_SD_CARD_PARTS) {
            return appDataParts;
        }

        return null;
    }

    /**
     * Checks if there are incomplete movements of SD card downloads and tries to complete them.
     */
    public static void checkSDCardCompletedTransfers() {
        ArrayList<AndroidCompletedTransfer> completedTransfers = MegaApplication.getInstance().getDbH().getCompletedTransfers();
        if (completedTransfers == null || completedTransfers.isEmpty()) {
            return;
        }

        for (AndroidCompletedTransfer transfer : completedTransfers) {
            String appData = transfer.getAppData();
            if (isTextEmpty(appData) || !appData.contains(APP_DATA_SD_CARD)) {
                continue;
            }

            File originalDownload = new File(transfer.getOriginalPath());
            if (!isFileAvailable(originalDownload)) {
                continue;
            }

            String targetPath = getSDCardTargetPath(appData);
            File finalDownload = new File(targetPath + File.separator + originalDownload.getName());
            if (finalDownload.exists() && finalDownload.length() == originalDownload.length()) {
                originalDownload.delete();
            }

            logWarning("Movement incomplete");

            try {
                SDCardOperator sdCardOperator = new SDCardOperator(MegaApplication.getInstance());
                sdCardOperator.moveDownloadedFileToDestinationPath(originalDownload, targetPath,
                        getSDCardTargetUri(transfer.getAppData()));
            } catch (Exception e) {
                logError("Error moving file to the sd card path", e);
            }
        }
    }
}
