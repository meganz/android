package mega.privacy.android.app.utils;

import android.content.Context;

import java.io.File;

public final class CacheFolderManager {

    public static final String THUMBNAIL_FOLDER = "thumbnailsMEGA";

    public static final String PREVIEW_FOLDER = "previewsMEGA";

    public static final String AVATAR_FOLDER = "avatarsMEGA";

    public static final String QR_FOLDER = "qrMEGA";

    public static File getCacheFolder(Context context,String folderName) {
        log("create cache folder: " + folderName);
        File cacheFolder = new File(context.getCacheDir(),folderName);
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

    public static File buildQrFile(Context context,String fileName) {
        return getCacheFile(context,QR_FOLDER,fileName);
    }

    public static File buildPreviewFile(Context context,String fileName) {
        return getCacheFile(context,PREVIEW_FOLDER,fileName);
    }

    public static File buildAvatarFile(Context context,String fileName) {
        return getCacheFile(context,AVATAR_FOLDER,fileName);
    }

    private static File getCacheFile(Context context,String folderName,String fileName) {
        File parent = getCacheFolder(context,folderName);
        if (parent != null) {
            return new File(parent,fileName);
        }
        return null;
    }

    public static boolean isFileAvailable(File file) {
        return file != null && file.exists();
    }

    public static void log(String message) {
        Util.log("CacheFolderManager", message);
    }
}
