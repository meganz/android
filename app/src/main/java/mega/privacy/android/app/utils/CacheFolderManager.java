package mega.privacy.android.app.utils;

import android.content.Context;
import java.io.File;

import mega.privacy.android.app.MegaOffline;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.Util.getSizeString;

public final class CacheFolderManager {

    public static final String THUMBNAIL_FOLDER = "thumbnailsMEGA";

    public static final String PREVIEW_FOLDER = "previewsMEGA";

    public static final String AVATAR_FOLDER = "avatarsMEGA";

    public static final String QR_FOLDER = "qrMEGA";

    public static final String temporalPicDIR = "appTempMEGA";

    public static final String profilePicDIR = "profileImagesMEGA";

    public static final String advancesDevicesDIR = "tempMEGA";

    public static final String chatTempDIR = "chatTempMEGA";

    public static final String mainDIR = File.separator + "MEGA";

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

    private static void createCacheFolder(Context context, String name) {
        File file = getCacheFolder(context, name);
        if (isFileAvailable(file)) {
            log(file.getName() + " folder created: " + file.getAbsolutePath());
        } else {
            log("create file failed");
        }
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

    public static String getCacheSize(Context context){
        log("getCacheSize");
        File cacheIntDir = context.getCacheDir();
        File cacheExtDir = context.getExternalCacheDir();

        if(cacheIntDir!=null){
            log("Path to check internal: "+cacheIntDir.getAbsolutePath());
        }
        long size = getDirSize(cacheIntDir)+getDirSize(cacheExtDir);

        return getSizeString(size);
    }

    public static void clearCache(Context context){
        log("clearCache");
        File cacheIntDir = context.getCacheDir();
        File cacheExtDir = context.getExternalCacheDir();

        cleanDir(cacheIntDir);
        cleanDir(cacheExtDir);
    }

    public static void log(String message) {
        Util.log("CacheFolderManager", message);
    }
}
