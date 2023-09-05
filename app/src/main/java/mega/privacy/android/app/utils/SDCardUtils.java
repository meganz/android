package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.Constants.APP_DATA_INDICATOR;
import static mega.privacy.android.app.utils.Constants.APP_DATA_SD_CARD;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

import android.content.Context;

import java.io.File;

import kotlin.coroutines.Continuation;
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
}
