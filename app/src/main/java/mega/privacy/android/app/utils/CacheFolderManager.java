package mega.privacy.android.app.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public final class CacheFolderManager {

    public static final String THUMBNAIL_FOLDER = "thumbnailsMEGA";

    public static final String PREVIEW_FOLDER = "previewsMEGA";

    public static final String AVATAR_FOLDER = "avatarsMEGA";

    public static final String QR_FOLDER = "qrMEGA";

    public static final String VOICE_CLIP_FOLDER = "voiceClipsMEGA";

    public static final String TEMPORAL_FOLDER = "tempMEGA";

    public static final String CHAT_TEMPORAL_FOLDER = "chatTempMEGA";

    public static final String OLD_TEMPORAL_PIC_DIR = "MEGA/MEGA AppTemp";

    public static final String OLD_PROFILE_PID_DIR = "MEGA/MEGA Profile Images";

    public static final String OLD_ADVANCES_DEVICES_DIR = "MEGA/MEGA Temp";

    public static final String OLD_CHAT_TEMPORAL_DIR = "MEGA/MEGA Temp/Chat";

    public static File getCacheFolder(Context context, String folderName) {
        logDebug("Create cache folder: " + folderName);
        File cacheFolder;
        if (folderName.equals(CHAT_TEMPORAL_FOLDER)) {
            cacheFolder = new File(context.getFilesDir(), folderName);
        } else {
            cacheFolder = new File(context.getCacheDir(), folderName);
        }

        if (cacheFolder == null) return null;

        if (cacheFolder.exists()) return cacheFolder;

        if (cacheFolder.mkdir()) {
            return cacheFolder;
        } else {
            return null;
        }
    }

    public static void createCacheFolders(Context context) {
        createCacheFolder(context, THUMBNAIL_FOLDER);
        createCacheFolder(context, PREVIEW_FOLDER);
        createCacheFolder(context, AVATAR_FOLDER);
        createCacheFolder(context, QR_FOLDER);
        createCacheFolder(context, VOICE_CLIP_FOLDER);
        removeOldTempFolders(context);
    }

    public static void clearPublicCache(final Context context) {
        new Thread() {
            @Override
            public void run() {
                File dir = context.getExternalCacheDir();
                if (dir != null) {
                    try {
                        deleteFolderAndSubfolders(context, dir);
                    } catch (IOException e) {
                        e.printStackTrace();
                        logError("IOException deleting external cache", e);
                    }
                }
            }
        }.start();
    }

    private static void createCacheFolder(Context context, String name) {
        File file = getCacheFolder(context, name);
        if (isFileAvailable(file)) {
            logDebug(file.getName() + " folder created: " + file.getAbsolutePath());
        } else {
            logError("Create " + name + " file failed");
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

    public static File buildVoiceClipFile(Context context, String fileName) {
        return getCacheFile(context, VOICE_CLIP_FOLDER, fileName);
    }

    public static File buildTempFile(Context context, String fileName) {
        return getCacheFile(context, TEMPORAL_FOLDER, fileName);
    }

    public static File buildChatTempFile(Context context, String fileName) {
        return getCacheFile(context, CHAT_TEMPORAL_FOLDER, fileName);
    }

    public static File getCacheFile(Context context, String folderName, String fileName) {
        File parent = getCacheFolder(context, folderName);
        if (!isFileAvailable(parent)) return null;

        return new File(parent, fileName);
    }

    public static String getCacheSize(Context context){
        logDebug("getCacheSize");
        File cacheIntDir = context.getCacheDir();
        File cacheExtDir = context.getExternalCacheDir();

        if(cacheIntDir!=null){
            logDebug("Path to check internal: " + cacheIntDir.getAbsolutePath());
        }
        long size = getDirSize(cacheIntDir)+getDirSize(cacheExtDir);

        return getSizeString(size);
    }

    public static void clearCache(Context context){
        logDebug("clearCache");
        File cacheIntDir = context.getCacheDir();

        try {
            deleteFolderAndSubfolders(context, cacheIntDir);
        } catch (IOException e) {
            e.printStackTrace();
            logError("Exception deleting private cache", e);
        }

        clearPublicCache(context);
    }

    public static void deleteCacheFolderIfEmpty (Context context, String folderName) {
        File folder = getCacheFolder(context, folderName);
        if (isFileAvailable(folder) && folder.list() != null && folder.list().length <= 0) {
            folder.delete();
        }
    }

    public static void removeOldTempFolders(final Context context) {
        new Thread() {
            @Override
            public void run() {
                removeOldTempFolder(context, OLD_TEMPORAL_PIC_DIR);
                removeOldTempFolder(context, OLD_PROFILE_PID_DIR);
                removeOldTempFolder(context, OLD_ADVANCES_DEVICES_DIR);
                removeOldTempFolder(context, OLD_CHAT_TEMPORAL_DIR);
                File oldOfflineFolder = getOldTempFolder(OLD_OFFLINE_DIR);
                if (isFileAvailable(oldOfflineFolder)) {
                    moveOfflineFiles(context);
                }
            }
        }.start();
    }

    public static void removeOldTempFolder(Context context, String folderName) {
        File oldTempFolder = getOldTempFolder(folderName);
        if (!isFileAvailable(oldTempFolder)) return;

        try {
            deleteFolderAndSubfolders(context, oldTempFolder);
        } catch (IOException e) {
            logError("Exception deleting" + oldTempFolder.getName() + "directory", e);
            e.printStackTrace();
        }
    }

    public static File getOldTempFolder(String folderName) {
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + folderName);
    }
}
