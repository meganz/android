package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.Constants.FROM_BACKUPS;
import static mega.privacy.android.app.utils.Constants.FROM_INCOMING_SHARES;
import static mega.privacy.android.app.utils.FileUtil.deleteFolderAndSubFolders;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.FileUtil.isFileDownloadedLatest;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.LegacyDatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.R;
import mega.privacy.android.app.di.DbHandlerModuleKt;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

@Deprecated
public class OfflineUtils {

    public static final String OFFLINE_DIR = "MEGA Offline";
    public static final String OFFLINE_BACKUPS_DIR = OFFLINE_DIR + File.separator + "in";

    /**
     * @deprecated
     * use RemoveOfflineNodeUseCase instead
     */
    @Deprecated
    public static void removeOffline(MegaOffline mOffDelete, LegacyDatabaseHandler dbH, Context context) {

        if (mOffDelete == null) {
            return;
        }

        Timber.d("File(type): %s(%s)", mOffDelete.getName(), mOffDelete.getType());
        ArrayList<MegaOffline> mOffListChildren;

        if (mOffDelete.getType().equals(MegaOffline.FOLDER)) {
            Timber.d("Finding children... ");

            //Delete children in DB
            mOffListChildren = dbH.findByParentId(mOffDelete.getId());
            if (mOffListChildren.size() > 0) {
                Timber.d("Children: %s", mOffListChildren.size());
                deleteChildrenDB(mOffListChildren, dbH);
            }
        } else {
            Timber.d("NOT children... ");
        }

        //remove red arrow from current item
        int parentId = mOffDelete.getParentId();
        dbH.removeById(mOffDelete.getId());
        if (parentId != -1) {
            updateParentOfflineStatus(parentId, dbH);
        }

        //Remove the node physically
        File offlineFile = getOfflineFile(context, mOffDelete);
        try {
            deleteFolderAndSubFolders(offlineFile);
        } catch (Exception e) {
            Timber.e(e, "Exception deleting folder");
        }

    }

    public static void updateParentOfflineStatus(int parentId, LegacyDatabaseHandler dbH) {
        ArrayList<MegaOffline> offlineSiblings = dbH.findByParentId(parentId);

        if (offlineSiblings.size() > 0) {
            //have other offline file within same folder, so no need to do anything to the folder
            return;
        } else {
            //keep checking if there is any parent folder should display red arrow
            MegaOffline parentNode = dbH.findById(parentId);
            if (parentNode != null) {
                int grandParentNodeId = parentNode.getParentId();
                dbH.removeById(parentId);
                updateParentOfflineStatus(grandParentNodeId, dbH);
            }
        }
    }

    public static void deleteChildrenDB(ArrayList<MegaOffline> mOffList, LegacyDatabaseHandler dbH) {

        Timber.d("deleteChildenDB");
        MegaOffline mOffDelete;

        for (int i = 0; i < mOffList.size(); i++) {

            mOffDelete = mOffList.get(i);
            ArrayList<MegaOffline> mOffListChildren2 = dbH.findByParentId(mOffDelete.getId());
            if (mOffList.size() > 0) {
                //The node have children, delete
                deleteChildrenDB(mOffListChildren2, dbH);

            }
            Timber.d("deleting %s", mOffDelete.getName());
            dbH.removeById(mOffDelete.getId());
        }
    }

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

    public static long findIncomingParentHandle(MegaNode nodeToFind, MegaApiAndroid megaApi) {
        Timber.d("findIncomingParentHandle");

        MegaNode parentNodeI = megaApi.getParentNode(nodeToFind);
        long result = -1;

        if (nodeToFind == null)
            return result;

        if (parentNodeI == null) {
            Timber.d("A: %s", nodeToFind.getHandle());
            return nodeToFind.getHandle();
        } else {
            result = findIncomingParentHandle(parentNodeI, megaApi);
            while (result == -1) {
                result = findIncomingParentHandle(parentNodeI, megaApi);
            }
            Timber.d("B: %s", nodeToFind.getHandle());
            return result;
        }
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

    public static File getOfflineParentFile(Context context, int from, MegaNode node, MegaApiAndroid megaApi) {
        String path = context.getFilesDir().getAbsolutePath() + File.separator;

        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        switch (from) {
            case FROM_INCOMING_SHARES: {
                path = path + OFFLINE_DIR + File.separator + findIncomingParentHandle(node, megaApi);
                break;
            }
            case FROM_BACKUPS: {
                path = path + OFFLINE_BACKUPS_DIR;
                break;
            }
            default: {
                MegaNode parentNode = MegaNodeUtil.getRootParentNode(megaApi, node);
                if (parentNode.isInShare()) {
                    path = path + OFFLINE_DIR + File.separator + findIncomingParentHandle(node, megaApi);
                } else {
                    path = path + OFFLINE_DIR;
                }
            }
        }

        return new File(path + File.separator + MegaApiUtils.createStringTree(node, context));
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
