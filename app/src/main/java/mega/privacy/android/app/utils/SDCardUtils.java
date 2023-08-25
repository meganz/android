package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.Constants.APP_DATA_INDICATOR;
import static mega.privacy.android.app.utils.Constants.APP_DATA_SD_CARD;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kotlin.coroutines.Continuation;
import mega.privacy.android.app.AndroidCompletedTransfer;
import mega.privacy.android.app.LegacyDatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.objects.SDTransfer;
import mega.privacy.android.app.presentation.transfers.model.mapper.LegacyCompletedTransferMapper;
import mega.privacy.android.domain.entity.transfer.CompletedTransfer;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaTransfer;
import timber.log.Timber;

public class SDCardUtils {

    public static final int APP_DATA_TARGET_PATH_POSITION = 1;
    public static final int APP_DATA_TARGET_URI_POSITION = 2;
    public static final int APP_DATA_SD_CARD_PARTS = 2;

    /**
     * Retrieves the Root SD Card path
     *
     * @param path the File path
     * @return the Root SD Card path
     * @deprecated This function is no longer acceptable to retrieve the Root SD Card path. Use
     * {@link mega.privacy.android.data.gateway.SDCardGateway#getRootSDCardPath(String, Continuation)}
     * instead
     */
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

    /**
     * Checks whether the Local Folder is inside the SD Card or not
     *
     * @param context   The Context
     * @param localPath The Folder Local Path
     * @return true if the Local Folder is inside the SD Card, and false if otherwise
     * @deprecated This function is no longer acceptable to check whether the Local Folder is inside
     * the SD Card or not. Use
     * {@link mega.privacy.android.data.gateway.SDCardGateway#doesFolderExists(String, Continuation)}
     * instead
     */
    @Deprecated
    public static boolean isLocalFolderOnSDCard(Context context, String localPath) {
        File[] fs = context.getExternalFilesDirs(null);
        if (fs.length > 1 && fs[1] != null) {
            String sdRoot = getSDCardRoot(fs[1].getAbsolutePath());
            return localPath.startsWith(sdRoot);
        }
        return false;
    }

    /**
     * Extracts from appData the target path in SD card of a download.
     *
     * @param appData Info contained on MegaTransfer object to identify the transfer.
     *                In this case should be SD card data.
     * @return The target path of a download. It's the real path the user chose to download.
     */
    public static String getSDCardTargetPath(String appData) {
        String[] appDataParts = getSDCardAppDataParts(appData);
        if (appDataParts == null) {
            return null;
        }

        return appDataParts[APP_DATA_TARGET_PATH_POSITION];
    }

    /**
     * Extracts from appData the target uri in a SD card of a download.
     *
     * @param appData Info contained on MegaTransfer object to identify the transfer.
     *                In this case should be SD card data.
     * @return The target uri of a download. It's the path where the transfer is downloaded due to
     * security matters of SD cars. It will be moved to target path when the download get complete.
     */
    public static String getSDCardTargetUri(String appData) {
        String[] appDataParts = getSDCardAppDataParts(appData);
        if (appDataParts == null || appDataParts.length <= APP_DATA_SD_CARD_PARTS) {
            Timber.d("App data doesn't contain SD card uri.");
            return null;
        }

        return appDataParts[APP_DATA_TARGET_URI_POSITION];
    }

    /**
     * Splits appData to get all relevant info of an SD card download.
     *
     * @param appData Info contained on MegaTransfer object to identify the transfer.
     *                In this case should be SD card data.
     * @return The String array containing each part of appData.
     */
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
    public static List<CompletedTransfer> checkSDCardCompletedTransfers() {
        MegaApplication app = MegaApplication.getInstance();
        MegaApiJava megaApi = app.getMegaApi();
        LegacyDatabaseHandler dbH = app.getDbH();
        ArrayList<SDTransfer> sdTransfers = dbH.getSdTransfers();
        if (sdTransfers == null || sdTransfers.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<CompletedTransfer> completedTransfers = new ArrayList<>();
        for (SDTransfer sdtransfer : sdTransfers) {
            if (megaApi == null) {
                return Collections.emptyList();
            }

            MegaTransfer transfer = megaApi.getTransferByTag(sdtransfer.getTag());
            if (transfer != null && transfer.getState() < MegaTransfer.STATE_COMPLETED) {
                continue;
            }

            File originalDownload = new File(sdtransfer.getPath());
            if (!isFileAvailable(originalDownload)) {
                dbH.removeSDTransfer(sdtransfer.getTag());
                continue;
            }

            String appData = sdtransfer.getAppData();
            String targetPath = getSDCardTargetPath(appData);
            File finalDownload = new File(targetPath + File.separator + originalDownload.getName());
            if (finalDownload.exists() && finalDownload.length() == originalDownload.length()) {
                originalDownload.delete();
                dbH.removeSDTransfer(sdtransfer.getTag());
                continue;
            }

            Timber.w("Movement incomplete");

            try {
                SDCardOperator sdCardOperator = new SDCardOperator(MegaApplication.getInstance());
                sdCardOperator.moveDownloadedFileToDestinationPath(originalDownload, targetPath,
                        getSDCardTargetUri(appData), sdtransfer.getTag());
            } catch (Exception e) {
                Timber.e(e, "Error moving file to the sd card path");
            }

            AndroidCompletedTransfer androidCompletedTransfer = new AndroidCompletedTransfer(sdtransfer, app);
            CompletedTransfer completedTransfer = new LegacyCompletedTransferMapper().invoke(androidCompletedTransfer);
            completedTransfers.add(completedTransfer);
        }

        return completedTransfers;
    }
}
