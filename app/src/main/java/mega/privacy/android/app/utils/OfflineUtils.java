package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.FileUtil.isFileDownloadedLatest;

import android.content.Context;

import java.io.File;

import mega.privacy.android.app.LegacyDatabaseHandler;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.R;
import mega.privacy.android.app.di.DbHandlerModuleKt;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

@Deprecated
public class OfflineUtils {

    public static final String OFFLINE_DIR = "MEGA Offline";
    public static final String OFFLINE_BACKUPS_DIR = OFFLINE_DIR + File.separator + "in";


    public static boolean availableOffline(Context context, MegaNode node) {
        LegacyDatabaseHandler dbH = DbHandlerModuleKt.getDbHandler();

        if (dbH.exists(node.getHandle())) {
            Timber.d("Exists OFFLINE in the DB!!!");

            MegaOffline offlineNode = dbH.findByHandle(node.getHandle());
            if (offlineNode != null) {
                File offlineFile = getOfflineFile(context, offlineNode);
                if (isFileAvailable(offlineFile) && isFileDownloadedLatest(offlineFile, node))
                    return true;
            }
        }

        Timber.d("Not found offline file");
        return false;
    }

    @Deprecated
    public static File getOfflineFolder(Context context, String path) {
        File offlineFolder = new File(context.getFilesDir() + File.separator + path);

        if (offlineFolder == null) return null;

        if (offlineFolder.exists()) return offlineFolder;

        if (offlineFolder.mkdirs()) {
            return offlineFolder;
        } else {
            return null;
        }
    }

    @Deprecated
    public static File getOfflineFile(Context context, MegaOffline offlineNode) {
        String path = context.getFilesDir().getAbsolutePath() + File.separator;
        if (offlineNode.isFolder()) {
            return new File(getOfflinePath(path, offlineNode) + File.separator + offlineNode.getName());
        }

        return new File(getOfflinePath(path, offlineNode), offlineNode.getName());
    }

    @Deprecated
    private static String getOfflinePath(String path, MegaOffline offlineNode) {
        switch (offlineNode.getOrigin()) {
            case MegaOffline.INCOMING: {
                path = path + OFFLINE_DIR + File.separator + offlineNode.getHandleIncoming();
                break;
            }
            case MegaOffline.BACKUPS: {
                path = path + OFFLINE_BACKUPS_DIR;
                break;
            }
            default: {
                path = path + OFFLINE_DIR;
            }
        }
        if (offlineNode.getPath().equals(File.separator)) {
            return path;
        }

        return path + offlineNode.getPath();
    }

    /**
     * existsOffline
     * @param context current context
     * @deprecated Use HasOfflineFilesUseCase instead
     */
    @Deprecated
    public static boolean existsOffline(Context context) {
        File offlineFolder = OfflineUtils.getOfflineFolder(context, OFFLINE_DIR);
        return isFileAvailable(offlineFolder)
                && offlineFolder.length() > 0
                && offlineFolder.listFiles() != null
                && offlineFolder.listFiles().length > 0;
    }

    /**
     * Removes the "Offline" root parent of a path.
     * Used to open the location of an offline node in the app.
     *
     * @param path path from which the "Offline" root parent has to be removed
     * @return The path without the "Offline" root parent.
     */
    public static String removeInitialOfflinePath(String path, Context context) {
        return path.replace(context.getString(R.string.section_saved_for_offline_new), "");
    }
}
