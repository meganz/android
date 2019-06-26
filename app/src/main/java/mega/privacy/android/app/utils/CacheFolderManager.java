package mega.privacy.android.app.utils;

import android.content.Context;

import java.io.File;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaPreferences;

import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.Util.getSizeString;

public final class CacheFolderManager {

    public static final String THUMBNAIL_FOLDER = "thumbnailsMEGA";

    public static final String PREVIEW_FOLDER = "previewsMEGA";

    public static final String AVATAR_FOLDER = "avatarsMEGA";

    public static final String QR_FOLDER = "qrMEGA";

    public static final String mainDIR = "/MEGA";

    public static final String offlineDIR = "MEGA/MEGA Offline";

    public static final String downloadDIR = "MEGA/MEGA Downloads";

    public static final String temporalPicDIR = "MEGA/MEGA AppTemp";

    public static final String profilePicDIR = "MEGA/MEGA Profile Images";

    public static final String logDIR = "MEGA/MEGA Logs";

    public static final String advancesDevicesDIR = "MEGA/MEGA Temp";

    public static final String chatTempDIR = "MEGA/MEGA Temp/Chat";

    public static final String oldMKFile = "/MEGA/MEGAMasterKey.txt";

    public static final String rKFile = "/MEGA/MEGARecoveryKey.txt";

    public static File getCacheFolder(Context context, String folderName) {
        log("create cache folder: " + folderName);
        File cacheFolder = new File(context.getCacheDir(), folderName);
        if (cacheFolder == null) return null;

        if (cacheFolder.exists()) {
            return cacheFolder;
        } else {
            if (cacheFolder.mkdir()) {
                return cacheFolder;
            } else {
                return null;
            }
        }
    }

    public static void createCacheFolders(Context context) {
        createCacheFolder(context, THUMBNAIL_FOLDER);
        createCacheFolder(context, PREVIEW_FOLDER);
        createCacheFolder(context, AVATAR_FOLDER);
        createCacheFolder(context, QR_FOLDER);
    }

    private static void createCacheFolder(Context context, String name) {
        File file = getCacheFolder(context, name);
        if (isFileAvailable(file)) {
            log(file.getName() + " folder created: " + file.getAbsolutePath());
        } else {
            log("create file failed");
        }
    }

    public static void clearPublicCache(final Context context) {
        new Thread() {

            @Override
            public void run() {
                File dir = context.getExternalCacheDir();
                if (dir != null) {
                    cleanDir(dir);
                }
            }
        }.start();
    }

    public static File buildQrFile(Context context, String fileName) {
        return getCacheFile(context, QR_FOLDER, fileName);
    }

    public static File buildPreviewFile(Context context, String fileName) {
        return getCacheFile(context, PREVIEW_FOLDER, fileName);
    }

    public static File buildAvatarFile(Context context, String fileName) {
        return getCacheFile(context, AVATAR_FOLDER, fileName);
    }

    public static File getCacheFile(Context context, String folderName, String fileName) {
        File parent = getCacheFolder(context, folderName);
        if (parent != null) {
            return new File(parent, fileName);
        }
        return null;
    }

    public static boolean isFileAvailable(File file) {
        return file != null && file.exists();
    }

    public static String getCacheSize(Context context){
        log("getCacheSize");
        File cacheIntDir = context.getCacheDir();
        File cacheExtDir = context.getExternalCacheDir();

        if(cacheIntDir!=null){
            log("Path to check internal: "+cacheIntDir.getAbsolutePath());
        }
        long size = getDirSize(cacheIntDir)+getDirSize(cacheExtDir);

        String sizeCache = getSizeString(size);
        return sizeCache;
    }

    public static void clearCache(Context context){
        log("clearCache");
        File cacheIntDir = context.getCacheDir();
        File cacheExtDir = context.getExternalCacheDir();

        cleanDir(cacheIntDir);
        cleanDir(cacheExtDir);
    }

    public static String getOfflineSize(Context context){
        log("getOfflineSize");
        File offline = getCacheFolder(context, offlineDIR);
        long size = 0;
        if(isFileAvailable(offline)){
            size = getDirSize(offline);

            String sizeOffline = getSizeString(size);
            return sizeOffline;
        }
        else{
            return getSizeString(0);
        }
    }

    public static void clearOffline(Context context){
        log("clearOffline");
        File offline = getCacheFolder(context, offlineDIR);
        if(isFileAvailable(offline)){
            cleanDir(offline);
            offline.delete();
        }
    }

    public static String getDownloadLocation (Context context) {
        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
        MegaPreferences prefs = dbH.getPreferences();

        if (prefs != null){
            log("prefs != null");
            if (prefs.getStorageAskAlways() != null){
                if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
                    log("askMe==false");
                    if (prefs.getStorageDownloadLocation() != null){
                        if (prefs.getStorageDownloadLocation().compareTo("") != 0){
                            return prefs.getStorageDownloadLocation();
                        }
                    }
                }
            }
        }
        return downloadDIR;
    }

    public static void log(String message) {
        Util.log("CacheFolderManager", message);
    }
}
