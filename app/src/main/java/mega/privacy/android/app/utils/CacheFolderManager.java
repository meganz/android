package mega.privacy.android.app.utils;

import android.content.Context;

import java.io.File;

public final class CacheFolderManager {

    public static final String THUMBNAIL_FOLDER = "thumbnailsMEGA";

    public static final String PREVIEW_FOLDER = "previewsMEGA";

    public static final String AVATAR_FOLDER = "avatarsMEGA";

    public static final String QR_FOLDER = "qrMEGA";

    public static final String VOICE_CLIP_FOLDER = "voiceClipsMEGA";

    public static File getCacheFolder(Context context, String folderName) {
        log("create cache folder: " + folderName);
        File cacheFolder = new File(context.getCacheDir(), folderName);
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
        File thumbDir = getCacheFolder(context, THUMBNAIL_FOLDER);
        if (isFileAvailable(thumbDir)) {
            log("thumbnailsMEGA folder created: " + thumbDir.getAbsolutePath());
        } else {
            log("create thumbnailsMEGA failed");
        }

        File previewDir = getCacheFolder(context, PREVIEW_FOLDER);
        if (isFileAvailable(previewDir)) {
            log("previewsMEGA folder created: " + previewDir.getAbsolutePath());
        } else {
            log("create previewsMEGA failed");
        }

        File avatarDir = getCacheFolder(context, AVATAR_FOLDER);
        if (isFileAvailable(avatarDir)) {
            log("avatarsMEGA folder created: " + avatarDir.getAbsolutePath());
        } else {
            log("create avatarsMEGA failed");
        }

        File qrDir = getCacheFolder(context, QR_FOLDER);
        if (isFileAvailable(qrDir)) {
            log("qrMEGA folder created: " + qrDir.getAbsolutePath());
        } else {
            log("create qrMEGA failed");
        }

        File voiceClipDir = getCacheFolder(context, VOICE_CLIP_FOLDER);
        if (isFileAvailable(voiceClipDir)) {
            log("voiceClipsMEGA folder created: " + voiceClipDir.getAbsolutePath());
        } else {
            log("create voiceClipsMEGA failed");
        }
    }

    public static void clearPublicCache(final Context context) {
        new Thread() {

            @Override
            public void run() {
                File dir = context.getExternalCacheDir();
                if(dir != null) {
                    Util.cleanDir(dir);
                }
            }
        }.start();
    }

    public static File buildVoiceClipFile(Context context, String fileName) {
        return getCacheFile(context, VOICE_CLIP_FOLDER, fileName);
    }

    public static File buildQrFile(Context context, String fileName) {
        return getCacheFile(context,QR_FOLDER, fileName);
    }

    public static File buildPreviewFile(Context context, String fileName) {
        return getCacheFile(context,PREVIEW_FOLDER, fileName);
    }

    public static File buildAvatarFile(Context context, String fileName) {
        return getCacheFile(context,AVATAR_FOLDER, fileName);
    }

    private static File getCacheFile(Context context, String folderName, String fileName) {
        File parent = getCacheFolder(context, folderName);
        if (parent != null) {
            return new File(parent, fileName);
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
