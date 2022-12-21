package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CacheFolderManager.getOldTempFolder;
import static mega.privacy.android.app.utils.CacheFolderManager.removeOldTempFolder;
import static mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER;
import static mega.privacy.android.app.utils.Constants.FROM_INBOX;
import static mega.privacy.android.app.utils.Constants.FROM_INCOMING_SHARES;
import static mega.privacy.android.app.utils.Constants.OFFLINE_ROOT;
import static mega.privacy.android.app.utils.Constants.SEPARATOR;
import static mega.privacy.android.app.utils.Constants.URL_INDICATOR;
import static mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION;
import static mega.privacy.android.app.utils.FileUtil.MAIN_DIR;
import static mega.privacy.android.app.utils.FileUtil.copyFile;
import static mega.privacy.android.app.utils.FileUtil.deleteFolderAndSubfolders;
import static mega.privacy.android.app.utils.FileUtil.getDirSize;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.FileUtil.isFileDownloadedLatest;
import static mega.privacy.android.app.utils.FileUtil.shareFile;
import static mega.privacy.android.app.utils.FileUtil.shareFiles;
import static mega.privacy.android.app.utils.MegaApiUtils.getNodePath;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.MegaNodeUtil.shareNode;
import static mega.privacy.android.app.utils.MegaNodeUtil.shareNodes;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;
import static mega.privacy.android.app.utils.Util.getSizeString;
import static mega.privacy.android.app.utils.Util.isOnline;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;

import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.Unit;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.LegacyDatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.di.DbHandlerModuleKt;
import mega.privacy.android.app.presentation.extensions.StorageStateExtensionsKt;
import mega.privacy.android.app.utils.permission.PermissionUtils;
import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.data.gateway.api.MegaApiGateway;
import mega.privacy.android.domain.entity.StorageState;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import timber.log.Timber;

public class OfflineUtils {

    public static final String OFFLINE_DIR = "MEGA Offline";
    public static final String OFFLINE_INBOX_DIR = OFFLINE_DIR + File.separator + "in";

    public static final String OLD_OFFLINE_DIR = MAIN_DIR + File.separator + OFFLINE_DIR;

    public static final String DB_FILE = "0";
    private static final String DB_FOLDER = "1";

    public static void saveOffline(File destination, MegaNode node, Activity activity) {
        if (StorageStateExtensionsKt.getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning();
            return;
        }

        destination.mkdirs();

        double availableFreeSpace = Double.MAX_VALUE;
        try {
            StatFs stat = new StatFs(destination.getAbsolutePath());
            availableFreeSpace = (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
        } catch (Exception ex) {
        }

        Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
        if (node.getType() == MegaNode.TYPE_FOLDER) {
            Timber.d("Is Folder");
            MegaApiAndroid megaApi = MegaApplication.getInstance().getMegaApi();
            MegaNodeUtil.getDlList(megaApi, dlFiles, node, new File(destination, node.getName()));
        } else {
            Timber.d("Is File");
            dlFiles.put(node, destination.getAbsolutePath());
        }

        PermissionUtils.checkNotificationsPermission(activity);

        for (MegaNode document : dlFiles.keySet()) {

            String path = dlFiles.get(document);

            if (availableFreeSpace < document.getSize()) {
                RunOnUIThreadUtils.INSTANCE.post(() -> {
                    Util.showErrorAlertDialog(
                            getString(R.string.error_not_enough_free_space)
                                    + " (" + document.getName() + ")",
                            false, activity);
                    return Unit.INSTANCE;
                });
                return;
            }

            String url = null;
            Intent service = new Intent(activity, DownloadService.class);
            service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
            service.putExtra(DownloadService.EXTRA_URL, url);
            service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
            service.putExtra(DownloadService.EXTRA_PATH, path);
            service.putExtra(DownloadService.EXTRA_DOWNLOAD_FOR_OFFLINE, true);
            activity.startService(service);
        }
    }

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
            deleteFolderAndSubfolders(context, offlineFile);
        } catch (Exception e) {
            Timber.e(e, "EXCEPTION: file");
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
        MegaOffline mOffDelete = null;

        for (int i = 0; i < mOffList.size(); i++) {

            mOffDelete = mOffList.get(i);
            ArrayList<MegaOffline> mOffListChildren2 = dbH.findByParentId(mOffDelete.getId());
            if (mOffList.size() > 0) {
                //The node have children, delete
                deleteChildrenDB(mOffListChildren2, dbH);

            }
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

    /**
     * Get folder name of an offline node, or `Offline` if it's in root offline folder.
     *
     * @param context Android context
     * @param handle  handle of the offline node
     */
    public static String getOfflineFolderName(Context context, long handle) {
        LegacyDatabaseHandler dbHandler = DbHandlerModuleKt.getDbHandler();
        MegaOffline node = dbHandler.findByHandle(handle);
        if (node == null) {
            return "";
        }

        File file = getOfflineFile(context, node);
        if (!file.exists()) {
            return "";
        }

        File parentFile = file.getParentFile();
        if (parentFile == null) {
            return "";
        }

        File grandParentFile = parentFile.getParentFile();
        if ((grandParentFile != null && OFFLINE_INBOX_DIR.equals(grandParentFile.getName()
                + File.separator + parentFile.getName()))
                || OFFLINE_DIR.equals(parentFile.getName())) {
            return getString(R.string.section_saved_for_offline_new);
        } else {
            return parentFile.getName();
        }
    }

    public static File getOfflineFile(Context context, MegaOffline offlineNode) {
        String path = context.getFilesDir().getAbsolutePath() + File.separator;
        if (offlineNode.isFolder()) {
            return new File(getOfflinePath(path, offlineNode) + File.separator + offlineNode.getName());
        }

        return new File(getOfflinePath(path, offlineNode), offlineNode.getName());
    }

    public static File getThumbnailFile(Context context, MegaOffline node, MegaApiGateway megaApiGateway) {
        return getThumbnailFile(context, node.getHandle(), megaApiGateway);
    }

    public static File getThumbnailFile(Context context, String handle, MegaApiGateway megaApiGateway) {
        File thumbDir = ThumbnailUtils.getThumbFolder(context);
        String thumbName = megaApiGateway.handleToBase64(Long.parseLong(handle));
        return new File(thumbDir, thumbName + JPG_EXTENSION);
    }

    private static String getOfflinePath(String path, MegaOffline offlineNode) {
        switch (offlineNode.getOrigin()) {
            case MegaOffline.INCOMING: {
                path = path + OFFLINE_DIR + File.separator + offlineNode.getHandleIncoming();
                break;
            }
            case MegaOffline.INBOX: {
                path = path + OFFLINE_INBOX_DIR;
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

        switch (from) {
            case FROM_INCOMING_SHARES: {
                path = path + OFFLINE_DIR + File.separator + findIncomingParentHandle(node, megaApi);
                break;
            }
            case FROM_INBOX: {
                path = path + OFFLINE_INBOX_DIR;
                break;
            }
            default: {
                path = path + OFFLINE_DIR;
            }
        }

        return new File(path + File.separator + MegaApiUtils.createStringTree(node, context));
    }

    public static File getOfflineParentFileName(Context context, MegaNode node) {
        return new File(File.separator + MegaApiUtils.createStringTree(node, context));
    }

    public static String getOfflineSize(Context context) {
        Timber.d("getOfflineSize");
        File offline = getOfflineFolder(context, OFFLINE_DIR);
        long size;
        if (isFileAvailable(offline)) {
            size = getDirSize(offline);
            return getSizeString(size);
        }

        return getSizeString(0);
    }

    public static void clearOffline(Context context) {
        Timber.d("clearOffline");
        File offline = getOfflineFolder(context, OFFLINE_DIR);
        if (isFileAvailable(offline)) {
            try {
                deleteFolderAndSubfolders(context, offline);
            } catch (IOException e) {
                Timber.e(e, "Exception deleting offline folder");
                e.printStackTrace();
            }
        }
    }

    public static void saveOffline(Context context, MegaApiAndroid megaApi, LegacyDatabaseHandler dbH, MegaNode node, String path) {
        Timber.d("Destination: %s", path);

        File destination = new File(path);
        destination.mkdirs();
        Timber.d("Destination absolute path: %s", destination.getAbsolutePath());
        Timber.d("Handle to save for offline: %s", node.getHandle());

        Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
        if (node.getType() == MegaNode.TYPE_FOLDER) {
            Timber.d("Is Folder");
            MegaNodeUtil.getDlList(megaApi, dlFiles, node, new File(destination, node.getName()));
        } else {
            Timber.d("Is File");
            dlFiles.put(node, destination.getAbsolutePath());
        }

        ArrayList<MegaNode> nodesToDB = new ArrayList<MegaNode>();

        for (MegaNode document : dlFiles.keySet()) {
            nodesToDB.add(document);
        }

        if (path.contains(OFFLINE_INBOX_DIR)) {
            insertDB(context, megaApi, dbH, nodesToDB, true);
        } else {
            insertDB(context, megaApi, dbH, nodesToDB, false);
        }
    }

    public static void saveOfflineChatFile(LegacyDatabaseHandler dbH, MegaTransfer transfer) {
        Timber.d("saveOfflineChatFile: %d %s", transfer.getNodeHandle(), transfer.getFileName());

        MegaOffline mOffInsert = new MegaOffline(Long.toString(transfer.getNodeHandle()), "/", transfer.getFileName(), -1, DB_FILE, 0, "-1");
        long checkInsert = dbH.setOfflineFile(mOffInsert);
        Timber.d("Test insert Chat File: %s", checkInsert);

    }

    private static String isFileOrFolder(MegaNode node) {
        if (node.isFile()) {
            return DB_FILE;
        }

        return DB_FOLDER;
    }

    private static int comesFromInbox(boolean fromInbox) {
        if (fromInbox) {
            return MegaOffline.INBOX;
        }

        return MegaOffline.OTHER;
    }

    private static void insertDB(Context context, MegaApiAndroid megaApi, LegacyDatabaseHandler dbH, ArrayList<MegaNode> nodesToDB, boolean fromInbox) {
        Timber.d("insertDB");

        MegaNode parentNode = null;
        MegaNode nodeToInsert = null;
        String path;
        MegaOffline mOffParent = null;
        MegaOffline mOffNode = null;

        for (int i = nodesToDB.size() - 1; i >= 0; i--) {
            nodeToInsert = nodesToDB.get(i);
            String fileOrFolder = isFileOrFolder(nodeToInsert);
            int origin = comesFromInbox(fromInbox);
            int parentId = -1;
            String handleIncoming = "-1";

            //If I am the owner
            if (megaApi.checkAccessErrorExtended(nodeToInsert, MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK) {

                if (megaApi.getParentNode(nodeToInsert).getType() != MegaNode.TYPE_ROOT) {

                    parentNode = megaApi.getParentNode(nodeToInsert);
                    Timber.d("PARENT NODE not ROOT");

                    path = getNodePath(context, nodeToInsert);

                    //Get the node parent
                    mOffParent = dbH.findByHandle(parentNode.getHandle());
                    //If the parent is not in the DB
                    //Insert the parent in the DB
                    if (mOffParent == null) {
                        insertParentDB(context, megaApi, dbH, parentNode, fromInbox);
                    }

                    mOffNode = dbH.findByHandle(nodeToInsert.getHandle());
                    mOffParent = dbH.findByHandle(parentNode.getHandle());
                    if (mOffNode != null) return;

                    if (mOffParent != null) {
                        parentId = mOffParent.getId();
                    }
                } else {
                    path = OFFLINE_ROOT;
                }
            } else {
                //If I am not the owner
                parentNode = megaApi.getParentNode(nodeToInsert);
                path = getNodePath(context, nodeToInsert);
                //Get the node parent
                mOffParent = dbH.findByHandle(parentNode.getHandle());
                //If the parent is not in the DB
                //Insert the parent in the DB
                if (mOffParent == null && parentNode != null) {
                    insertIncomingParentDB(context, megaApi, dbH, parentNode);
                }

                mOffNode = dbH.findByHandle(nodeToInsert.getHandle());
                mOffParent = dbH.findByHandle(parentNode.getHandle());

                if (parentNode != null) {
                    MegaNode ownerNode = megaApi.getParentNode(parentNode);
                    if (ownerNode != null) {
                        MegaNode nodeWhile = ownerNode;
                        while (nodeWhile != null) {
                            ownerNode = nodeWhile;
                            nodeWhile = megaApi.getParentNode(nodeWhile);
                        }

                        handleIncoming = Long.toString(ownerNode.getHandle());
                    } else {
                        handleIncoming = Long.toString(parentNode.getHandle());
                    }

                }

                if (mOffNode != null || mOffParent == null) return;

                parentId = mOffParent.getId();
                origin = MegaOffline.INCOMING;
            }

            MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), parentId, fileOrFolder, origin, handleIncoming);
            long checkInsert = dbH.setOfflineFile(mOffInsert);
            Timber.d("Test insert A: %s", checkInsert);
        }
    }

    //Insert for incoming
    private static void insertIncomingParentDB(Context context, MegaApiAndroid megaApi, LegacyDatabaseHandler dbH, MegaNode parentNode) {
        Timber.d("insertIncomingParentDB");

        String fileOrFolder = isFileOrFolder(parentNode);
        MegaOffline mOffParentParent = null;
        String path = getNodePath(context, parentNode);
        MegaNode parentparentNode = megaApi.getParentNode(parentNode);
        int parentId = -1;
        String handleIncoming;

        if (parentparentNode == null) {
            handleIncoming = Long.toString(parentNode.getHandle());
        } else {
            MegaNode ownerNode = megaApi.getParentNode(parentparentNode);
            if (ownerNode != null) {
                MegaNode nodeWhile = ownerNode;
                while (nodeWhile != null) {
                    ownerNode = nodeWhile;
                    nodeWhile = megaApi.getParentNode(nodeWhile);
                }

                handleIncoming = Long.toString(ownerNode.getHandle());
            } else {
                handleIncoming = Long.toString(parentparentNode.getHandle());
            }


            mOffParentParent = dbH.findByHandle(parentparentNode.getHandle());
            if (mOffParentParent == null) {
                insertIncomingParentDB(context, megaApi, dbH, megaApi.getParentNode(parentNode));
                //Insert the parent node
                mOffParentParent = dbH.findByHandle(megaApi.getParentNode(parentNode).getHandle());
                if (mOffParentParent == null) {
                    insertIncomingParentDB(context, megaApi, dbH, megaApi.getParentNode(parentNode));
                    return;
                }
            }
            parentId = mOffParentParent.getId();
        }

        MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), parentId, fileOrFolder, MegaOffline.INCOMING, handleIncoming);
        long checkInsert = dbH.setOfflineFile(mOffInsert);
        Timber.d("Test insert B: %s", checkInsert);
    }

    private static void insertParentDB(Context context, MegaApiAndroid megaApi, LegacyDatabaseHandler dbH, MegaNode parentNode, boolean fromInbox) {
        Timber.d("insertParentDB");

        String fileOrFolder = isFileOrFolder(parentNode);
        int origin = comesFromInbox(fromInbox);
        MegaOffline mOffParentParent = null;
        String path = getNodePath(context, parentNode);
        MegaNode parentparentNode = megaApi.getParentNode(parentNode);
        int parentId = -1;

        if (parentparentNode == null) {
            Timber.w("return insertParentNode == null");
            return;
        }

        if (parentparentNode.getType() != MegaNode.TYPE_ROOT && parentparentNode.getHandle() != megaApi.getInboxNode().getHandle()) {
            mOffParentParent = dbH.findByHandle(parentparentNode.getHandle());
            if (mOffParentParent == null) {
                Timber.w("mOffParentParent==null");
                insertParentDB(context, megaApi, dbH, megaApi.getParentNode(parentNode), fromInbox);
                //Insert the parent node
                mOffParentParent = dbH.findByHandle(megaApi.getParentNode(parentNode).getHandle());
                if (mOffParentParent == null) {
                    Timber.d("call again");
                    insertParentDB(context, megaApi, dbH, megaApi.getParentNode(parentNode), fromInbox);
                    return;
                } else {
                    parentId = mOffParentParent.getId();
                }
            } else {
                parentId = mOffParentParent.getId();
            }
        }

        MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), parentId, fileOrFolder, origin, "-1");
        long checkInsert = dbH.setOfflineFile(mOffInsert);
        Timber.d("Test insert C: %s", checkInsert);
    }

    /**
     * This method move the old offline files that exist in database into the private space. The conditions to move each file are:
     * 1.- Exist a MegaOffline representing the file with id diferent to -1
     * 2.- Exist a MegaNode on Cloud with the same handle that the MegaOffline object
     * 3.- Exist the File to move
     * 4.- The size of the MegaNode and the File are the same
     * 5.- The path of the MegaNode and the MegaOffline are the same
     * <p>
     * If any of these conditions are not comply, any file will not be moved
     * and will be removed from database. If some error happens when moving the file,
     * it will be removed from database too.
     *
     * @param context
     */
    public static void moveOfflineFiles(Context context) {
        Timber.d("moveOfflineFiles");

        String nodePath = File.separator;
        MegaApiAndroid megaApi;

        megaApi = MegaApplication.getInstance().getMegaApi();

        if (megaApi == null || megaApi.getRootNode() == null) return;

        LegacyDatabaseHandler dbH = DbHandlerModuleKt.getDbHandler();
        ArrayList<MegaOffline> offlineFiles = dbH.getOfflineFiles();

        if (offlineFiles == null || offlineFiles.isEmpty()) return; //No files to move

        for (MegaOffline offlineNode : offlineFiles) {
            if (offlineNode.getHandle() == "-1" || offlineNode.isFolder()) continue;

            MegaNode node = megaApi.getNodeByHandle(Long.parseLong(offlineNode.getHandle()));
            if (node != null) {
                nodePath = getNodePath(context, node);
            }

            File oldOfflineFile = findOldOfflineFile(offlineNode);

            if (node == null
                    || !isFileAvailable(oldOfflineFile)
                    || node.getSize() != oldOfflineFile.length()
                    || !node.getName().equals(oldOfflineFile.getName())
                    || !nodePath.equals(offlineNode.getPath())) {
                Timber.w("File not founded or not equal to the saved in database --> Remove");
                deleteOldOfflineReference(dbH, oldOfflineFile, offlineNode);
                continue;
            }

            File newOfflineFileDir = getOfflineFolder(context, getOfflinePath("", offlineNode));
            File newOfflineFile = getOfflineFile(context, offlineNode);
            if (!isFileAvailable(newOfflineFileDir) || newOfflineFile == null) {
                Timber.w("Error creating new directory or creating new file");
                deleteOldOfflineReference(dbH, oldOfflineFile, offlineNode);
                continue;
            }

            try {
                copyFile(oldOfflineFile, newOfflineFile);
            } catch (IOException e) {
                e.printStackTrace();
                Timber.w(e, "Error copying: %s", offlineNode.getHandle());
                deleteOldOfflineReference(dbH, oldOfflineFile, offlineNode);
                continue;
            }
        }

        removeOldTempFolder(context, OLD_OFFLINE_DIR);
    }

    private static void deleteOldOfflineReference(DatabaseHandler dbH, File oldOfflineFile, MegaOffline oldOfflineNode) {
        dbH.removeById(oldOfflineNode.getId());
        if (isFileAvailable(oldOfflineFile)) {
            oldOfflineFile.delete();
        }
    }

    private static File findOldOfflineFile(MegaOffline offlineNode) {
        String path = getOldTempFolder(MAIN_DIR).getAbsolutePath() + File.separator;
        return new File(getOfflinePath(path, offlineNode), offlineNode.getName());
    }

    public static boolean existsOffline(Context context) {
        File offlineFolder = OfflineUtils.getOfflineFolder(context, OFFLINE_DIR);
        return isFileAvailable(offlineFolder)
                && offlineFolder.length() > 0
                && offlineFolder.listFiles() != null
                && offlineFolder.listFiles().length > 0;
    }

    /**
     * Replaces the root parent path by "Offline" in the offline path received.
     * Used to show the location of an offline node in the app.
     *
     * @param path   path from which the root parent path has to be replaced
     * @param handle identifier of the offline node
     * @return The path with the root parent path replaced by "Offline".
     */
    public static String removeInitialOfflinePath(String path, long handle) {
        MegaApplication app = MegaApplication.getInstance();
        MegaApiAndroid megaApi = app.getMegaApi();

        File filesDir = app.getFilesDir();
        File inboxOfflineFolder = new File(filesDir + SEPARATOR + OFFLINE_INBOX_DIR);
        MegaNode transferNode = megaApi.getNodeByHandle(handle);
        File incomingFolder = new File(filesDir + SEPARATOR + OFFLINE_DIR + SEPARATOR
                + findIncomingParentHandle(transferNode, megaApi));

        if (inboxOfflineFolder.exists() && path.startsWith(inboxOfflineFolder.getAbsolutePath())) {
            path = path.replace(inboxOfflineFolder.getPath(), "");
        } else if (incomingFolder.exists() && path.startsWith(incomingFolder.getAbsolutePath())) {
            path = path.replace(incomingFolder.getPath(), "");
        } else {
            path = path.replace(getOfflineFolder(app, OFFLINE_DIR).getPath(), "");
        }

        return getString(R.string.section_saved_for_offline_new) + path;
    }

    /**
     * Removes the "Offline" root parent of a path.
     * Used to open the location of an offline node in the app.
     *
     * @param path path from which the "Offline" root parent has to be removed
     * @return The path without the "Offline" root parent.
     */
    public static String removeInitialOfflinePath(String path) {
        return path.replace(getString(R.string.section_saved_for_offline_new), "");
    }

    /**
     * Shares a offline node.
     * If the node is a folder and the app has network connection, shares a folder link.
     * If the node is a file, shares the file.
     *
     * @param context    Required to build the intent
     * @param nodeHandle Offline node handle to be shared
     */
    public static void shareOfflineNode(Context context, Long nodeHandle) {
        LegacyDatabaseHandler dbH = DbHandlerModuleKt.getDbHandler();
        MegaOffline node = dbH.findByHandle(nodeHandle);
        if (node == null) return;

        if (node.isFolder()) {
            if (isOnline(context)) {
                shareNode(context, MegaApplication.getInstance().getMegaApi().getNodeByHandle(Long.parseLong(node.getHandle())));
            }
        } else {
            shareFile(context, getOfflineFile(context, node));
        }
    }

    /**
     * Shares multiple offline nodes. If any node is a folder and the app has network connection,
     * then share links, otherwise share files.
     *
     * @param context      the current Context
     * @param offlineNodes offline nodes to share
     */
    public static void shareOfflineNodes(Context context, List<MegaOffline> offlineNodes) {
        boolean allFiles = true;
        for (MegaOffline offlineNode : offlineNodes) {
            if (offlineNode.isFolder()) {
                allFiles = false;
                break;
            }
        }
        if (allFiles) {
            List<File> files = new ArrayList<>();
            for (MegaOffline offlineNode : offlineNodes) {
                files.add(getOfflineFile(context, offlineNode));
            }
            shareFiles(context, files);
        } else if (isOnline(context)) {
            List<MegaNode> nodes = new ArrayList<>();
            for (MegaOffline offlineNode : offlineNodes) {
                MegaNode node = MegaApplication.getInstance().getMegaApi()
                        .getNodeByHandle(Long.parseLong(offlineNode.getHandle()));
                if (node != null) {
                    nodes.add(node);
                }
            }
            shareNodes(context, nodes);
        }
    }

    /**
     * Reads an URL file content.
     *
     * @param file File to read its content.
     * @return The content of the file.
     */
    public static String getURLOfflineFileContent(File file) {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            if (reader.readLine() != null) {
                String line = reader.readLine();
                return line.replace(URL_INDICATOR, "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "";
    }

    /**
     * Open offline file with Media Intent
     *
     * @param context    Required to build the intent
     * @param nodeHandle Offline node handle to be open with
     */
    public static void openWithOffline(Context context, Long nodeHandle) {
        LegacyDatabaseHandler dbH = DbHandlerModuleKt.getDbHandler();
        MegaOffline node = dbH.findByHandle(nodeHandle);
        if (node == null) return;

        File file = getOfflineFile(context, node);
        if (!isFileAvailable(file)) return;

        if (MimeTypeList.typeForName(node.getName()).isURL()) {
            Uri uri = Uri.parse(getURLOfflineFileContent(file));

            if (uri != null) {
                context.startActivity(new Intent(Intent.ACTION_VIEW).setData(uri));
                return;
            }
        }

        String type = MimeTypeList.typeForName(node.getName()).getType();
        Intent mediaIntent = new Intent(Intent.ACTION_VIEW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaIntent.setDataAndType(FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, file), type);
        } else {
            mediaIntent.setDataAndType(Uri.fromFile(file), type);
        }
        mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (isIntentAvailable(context, mediaIntent)) {
            context.startActivity(mediaIntent);
        } else {
            Util.showSnackbar(context, getString(R.string.intent_not_available_file));
        }
    }
}
