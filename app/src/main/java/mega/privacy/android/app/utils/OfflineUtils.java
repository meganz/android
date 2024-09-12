package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.FileUtil.isFileDownloadedLatest;

import android.content.Context;

import java.io.File;

import mega.privacy.android.app.LegacyDatabaseHandler;
import mega.privacy.android.app.MegaOffline;
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
}
