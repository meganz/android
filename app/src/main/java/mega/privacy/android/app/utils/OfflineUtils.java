package mega.privacy.android.app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.MegaApiUtils.getNodePath;
import static mega.privacy.android.app.utils.Util.getSizeString;
import static nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC;
import static nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_DESC;
import static nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_ASC;
import static nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC;
import static nz.mega.sdk.MegaApiJava.ORDER_SIZE_ASC;
import static nz.mega.sdk.MegaApiJava.ORDER_SIZE_DESC;

public class OfflineUtils {

    public static final String OFFLINE_DIR = "MEGA Offline";
    public static final String OFFLINE_INBOX_DIR = OFFLINE_DIR + File.separator + "in";

    public static final String OLD_OFFLINE_DIR = MAIN_DIR + File.separator + OFFLINE_DIR;

    private static final String DB_FILE = "0";
    private static final String DB_FOLDER = "1";

    public static void saveOffline (File destination, MegaNode node, Context context, Activity activity, MegaApiAndroid megaApi){
        log("saveOffline");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Constants.REQUEST_WRITE_STORAGE);
            }
        }

        destination.mkdirs();

        log ("DESTINATION!!!!!: " + destination.getAbsolutePath());

        double availableFreeSpace = Double.MAX_VALUE;
        try{
            StatFs stat = new StatFs(destination.getAbsolutePath());
            availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
        }
        catch(Exception ex){}

        Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
        if (node.getType() == MegaNode.TYPE_FOLDER) {
            log("saveOffline:isFolder");
            getDlList(dlFiles, node, new File(destination, node.getName()), megaApi);
        } else {
            log("saveOffline:isFile");
            dlFiles.put(node, destination.getAbsolutePath());
        }

        for (MegaNode document : dlFiles.keySet()) {

            String path = dlFiles.get(document);

            if(availableFreeSpace <document.getSize()){
                Util.showErrorAlertDialog(context.getString(R.string.error_not_enough_free_space) + " (" + document.getName() + ")", false, activity);
                continue;
            }

            String url = null;
            Intent service = new Intent(activity, DownloadService.class);
            service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
            service.putExtra(DownloadService.EXTRA_URL, url);
            service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
            service.putExtra(DownloadService.EXTRA_PATH, path);
            context.startService(service);
        }
    }

    /*
     * Get list of all child files
     */
    public static void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder, MegaApiAndroid megaApi) {

        if (megaApi.getRootNode() == null)
            return;

        folder.mkdir();
        ArrayList<MegaNode> nodeList = megaApi.getChildren(parent);
        for(int i=0; i<nodeList.size(); i++){
            MegaNode document = nodeList.get(i);
            if (document.getType() == MegaNode.TYPE_FOLDER) {
                File subfolder = new File(folder, new String(document.getName()));
                getDlList(dlFiles, document, subfolder, megaApi);
            }
            else {
                dlFiles.put(document, folder.getAbsolutePath());
            }
        }
    }

    public static void removeOffline(MegaOffline mOffDelete, DatabaseHandler dbH, Context context) {

        if (mOffDelete == null) {
            return;
        }

        log("removeOffline - file(type): " + mOffDelete.getName() + "(" + mOffDelete.getType() + ")");
        ArrayList<MegaOffline> mOffListChildren;

        if (mOffDelete.getType().equals(MegaOffline.FOLDER)) {
            log("Finding children... ");

            //Delete children in DB
            mOffListChildren = dbH.findByParentId(mOffDelete.getId());
            if (mOffListChildren.size() > 0) {
                log("Children: " + mOffListChildren.size());
                deleteChildrenDB(mOffListChildren, dbH);
            }
        } else {
            log("NOT children... ");
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
            deleteFolderAndSubfolders(context,offlineFile);
        } catch (Exception e) {
            log("EXCEPTION: removeOffline - file " + e.toString());
        }

    }

    public static void updateParentOfflineStatus(int parentId, DatabaseHandler dbH) {
        ArrayList<MegaOffline> offlineSiblings = dbH.findByParentId(parentId);

        if(offlineSiblings.size() > 0){
            //have other offline file within same folder, so no need to do anything to the folder
            return;
        }
        else{
            //keep checking if there is any parent folder should display red arrow
            MegaOffline parentNode = dbH.findById(parentId);
            if (parentNode != null) {
                int grandParentNodeId = parentNode.getParentId();
                dbH.removeById(parentId);
                updateParentOfflineStatus(grandParentNodeId, dbH);
            }
        }
    }

    public static void deleteChildrenDB(ArrayList<MegaOffline> mOffList, DatabaseHandler dbH){

        log("deleteChildenDB");
        MegaOffline mOffDelete=null;

        for(int i=0; i< mOffList.size(); i++){

            mOffDelete=mOffList.get(i);
            ArrayList<MegaOffline> mOffListChildren2=dbH.findByParentId(mOffDelete.getId());
            if(mOffList.size()>0){
                //The node have children, delete
                deleteChildrenDB(mOffListChildren2, dbH);

            }
            dbH.removeById(mOffDelete.getId());
        }
    }

    public static boolean availableOffline (Context context, MegaNode node) {

        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);

        if(dbH.exists(node.getHandle())) {
            log("Exists OFFLINE in the DB!!!");

            MegaOffline offlineNode = dbH.findByHandle(node.getHandle());
            if (offlineNode != null) {
                File offlineFile = getOfflineFile(context, offlineNode);
                log("YESS FOUND: " + node.getName());

                if (isFileAvailable(offlineFile)) return true;
            }

        }

        log("Not found offLineFile");
        return false;
    }

    public static long findIncomingParentHandle(MegaNode nodeToFind, MegaApiAndroid megaApi){
        log("findIncomingParentHandle");

        MegaNode parentNodeI = megaApi.getParentNode(nodeToFind);
        long result=-1;
        if(parentNodeI==null){
            log("findIncomingParentHandle A: "+nodeToFind.getHandle());
            return nodeToFind.getHandle();
        }
        else{
            result=findIncomingParentHandle(parentNodeI, megaApi);
            while(result==-1){
                result=findIncomingParentHandle(parentNodeI, megaApi);
            }
            log("findIncomingParentHandle B: "+nodeToFind.getHandle());
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

    public static String getOfflineAbsolutePath(Context context, MegaOffline offlineNode) {

        switch (offlineNode.getOrigin()) {
            case MegaOffline.INCOMING: {
                return context.getFilesDir().getAbsolutePath() + File.separator + OFFLINE_DIR + File.separator + offlineNode.getHandleIncoming();
            }
            case MegaOffline.INBOX: {
                return context.getFilesDir().getAbsolutePath() + File.separator + OFFLINE_INBOX_DIR;
            }
            default: {
                return context.getFilesDir().getAbsolutePath() + File.separator + OFFLINE_DIR;
            }
        }
    }

    public static File getOfflineFile(Context context, MegaOffline offlineNode) {
        String path = context.getFilesDir().getAbsolutePath() + File.separator;
        if (offlineNode.isFolder()) {
            return new File(getOfflinePath(path, offlineNode) + File.separator + offlineNode.getName());
        }

        return new File(getOfflinePath(path, offlineNode), offlineNode.getName());
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
            case Constants.FROM_INCOMING_SHARES: {
                path = path + OFFLINE_DIR + File.separator + OfflineUtils.findIncomingParentHandle(node, megaApi);
                break;
            }
            case Constants.FROM_INBOX: {
                path = path + OFFLINE_INBOX_DIR;
                break;
            }
            default: {
                path = path + OFFLINE_DIR;
            }
        }

        return new File(path + File.separator + MegaApiUtils.createStringTree(node, context));
    }

    public static String getOfflineSize(Context context) {
        log("getOfflineSize");
        File offline = getOfflineFolder(context, OFFLINE_DIR);
        long size;
        if (isFileAvailable(offline)) {
            size = getDirSize(offline);
            return getSizeString(size);
        }

        return getSizeString(0);
    }

    public static void clearOffline(Context context) {
        log("clearOffline");
        File offline = getOfflineFolder(context, OFFLINE_DIR);
        if (isFileAvailable(offline)) {
            try {
                deleteFolderAndSubfolders(context, offline);
            } catch (IOException e) {
                e.printStackTrace();
                log("Exception deleting offline folder");
            }
        }
    }

    public static void saveOffline(Context context, MegaApiAndroid megaApi, DatabaseHandler dbH, MegaNode node, String path) {
        log("saveOffline destination: " + path);

        File destination = new File(path);
        destination.mkdirs();

        log("saveOffline: " + destination.getAbsolutePath());
        log("Handle to save for offline : " + node.getHandle());

        Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
        if (node.getType() == MegaNode.TYPE_FOLDER) {
            log("saveOffline:isFolder");
            getDlList(dlFiles, node, new File(destination, node.getName()), megaApi);
        } else {
            log("saveOffline:isFile");
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

    public static void saveOfflineChatFile(DatabaseHandler dbH, MegaTransfer transfer) {
        log("saveOfflineChatFile: " + transfer.getNodeHandle() + " " + transfer.getFileName());

        MegaOffline mOffInsert = new MegaOffline(Long.toString(transfer.getNodeHandle()), "/", transfer.getFileName(), -1, DB_FILE, 0, "-1");
        long checkInsert = dbH.setOfflineFile(mOffInsert);
        log("Test insert Chat File: " + checkInsert);

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

    private static void insertDB(Context context, MegaApiAndroid megaApi, DatabaseHandler dbH, ArrayList<MegaNode> nodesToDB, boolean fromInbox) {
        log("insertDB");

        MegaNode parentNode = null;
        MegaNode nodeToInsert = null;
        String path = "/";
        MegaOffline mOffParent = null;
        MegaOffline mOffNode = null;

        for (int i = nodesToDB.size() - 1; i >= 0; i--) {
            nodeToInsert = nodesToDB.get(i);
            String fileOrFolder = isFileOrFolder(nodeToInsert);
            int origin = comesFromInbox(fromInbox);
            int parentId = -1;
            String handleIncoming = "-1";

            //If I am the owner
            if (megaApi.checkAccess(nodeToInsert, MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK) {

                if (megaApi.getParentNode(nodeToInsert).getType() != MegaNode.TYPE_ROOT) {

                    parentNode = megaApi.getParentNode(nodeToInsert);
                    log("PARENT NODE nooot ROOT");

                    path = getNodePath(context, nodeToInsert);

                    //Get the node parent
                    mOffParent = dbH.findByHandle(parentNode.getHandle());
                    //If the parent is not in the DB
                    //Insert the parent in the DB
                    if (mOffParent == null && parentNode != null) {
                        insertParentDB(context, megaApi, dbH, parentNode, fromInbox);
                    }

                    mOffNode = dbH.findByHandle(nodeToInsert.getHandle());
                    mOffParent = dbH.findByHandle(parentNode.getHandle());
                    if (mOffNode != null) return;

                    if (mOffParent != null) {
                        parentId = mOffParent.getId();
                    }
                } else {
                    path = "/";
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
            log("Test insert A: " + checkInsert);
        }
    }

    //Insert for incoming
    private static void insertIncomingParentDB(Context context, MegaApiAndroid megaApi, DatabaseHandler dbH, MegaNode parentNode) {
        log("insertIncomingParentDB");

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
        log("Test insert B: " + checkInsert);
    }

    private static void insertParentDB(Context context, MegaApiAndroid megaApi, DatabaseHandler dbH, MegaNode parentNode, boolean fromInbox) {
        log("insertParentDB");

        String fileOrFolder = isFileOrFolder(parentNode);
        int origin = comesFromInbox(fromInbox);
        MegaOffline mOffParentParent = null;
        String path = getNodePath(context, parentNode);
        MegaNode parentparentNode = megaApi.getParentNode(parentNode);
        int parentId = -1;

        if (parentparentNode == null) {
            log("return insertParentDB");
            return;
        }

        if (parentparentNode.getType() != MegaNode.TYPE_ROOT && parentparentNode.getHandle() != megaApi.getInboxNode().getHandle()) {
            mOffParentParent = dbH.findByHandle(parentparentNode.getHandle());
            if (mOffParentParent == null) {
                log("mOffParentParent==null");
                insertParentDB(context, megaApi, dbH, megaApi.getParentNode(parentNode), fromInbox);
                //Insert the parent node
                mOffParentParent = dbH.findByHandle(megaApi.getParentNode(parentNode).getHandle());
                if (mOffParentParent == null) {
                    log("call again");
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
        log("Test insert C: " + checkInsert);
    }

    /**
     * This method move the old offline files that exist in database into the private space. The conditions to move each file are:
     * 1.- Exist a MegaOffline representing the file with id diferent to -1
     * 2.- Exist a MegaNode on Cloud with the same handle that the MegaOffline object
     * 3.- Exist the File to move
     * 4.- The size of the MegaNode and the File are the same
     * 5.- The path of the MegaNode and the MegaOffline are the same
     *
     * If any of these conditions are not comply, any file will not be moved
     * and will be removed from database. If some error happens when moving the file,
     * it will be removed from database too.
     *
     * @param context
     */
    public static void moveOfflineFiles(Context context) {
        log("moveOfflineFiles");

        String nodePath = File.separator;
        MegaApiAndroid megaApi;

        megaApi = MegaApplication.getInstance().getMegaApi();

        if (megaApi == null || megaApi.getRootNode() == null) return;

        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
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
                log("moveOfflineFiles file not founded or not equal to the saved in database --> Remove");
                deleteOldOfflineReference(dbH, oldOfflineFile, offlineNode);
                continue;
            }

            File newOfflineFileDir = getOfflineFolder(context, getOfflinePath("", offlineNode));
            File newOfflineFile = getOfflineFile(context, offlineNode);
            if (!isFileAvailable(newOfflineFileDir) || newOfflineFile == null) {
                log("moveOfflineFiles error creating new directory or creating new file");
                deleteOldOfflineReference(dbH, oldOfflineFile, offlineNode);
                continue;
            }

            try {
                copyFile(oldOfflineFile, newOfflineFile);
            } catch (IOException e) {
                e.printStackTrace();
                log("moveOfflineFiles error copying: " + offlineNode.getHandle() + " trace: " + e.getMessage());
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


    public static void sort(int order, ArrayList<MegaOffline> mOffList, final Context context) {
        ArrayList<MegaOffline> foldersOrder = new ArrayList<>();
        ArrayList<MegaOffline> filesOrder = new ArrayList<>();
        ArrayList<MegaOffline> tempOffline = new ArrayList<>();

        //Remove MK before sorting
        if (mOffList.size() > 0) {
            MegaOffline lastItem = mOffList.get(mOffList.size() - 1);
            if (lastItem.getHandle().equals("0")) {
                mOffList.remove(mOffList.size() - 1);
            }
        } else {
            return;
        }

        for (MegaOffline node : mOffList) {
            if (node.getType().equals("1")) {
                foldersOrder.add(node);
            } else {
                filesOrder.add(node);
            }
        }

        Comparator<MegaOffline> modificationDateComparator = new Comparator<MegaOffline>() {
            @Override
            public int compare(MegaOffline o1, MegaOffline o2) {
                return Long.compare(o2.getModificationDate(context), o1.getModificationDate(context));
            }
        };

        Comparator<MegaOffline> sizeComparator = new Comparator<MegaOffline>() {
            @Override
            public int compare(MegaOffline o1, MegaOffline o2) {
                return Long.compare(o2.getSize(context), o1.getSize(context));
            }
        };

        Comparator<MegaOffline> nameComparator = new Comparator<MegaOffline>() {
            @Override
            public int compare(MegaOffline o1, MegaOffline o2) {
                String name1 = o1.getName();
                String name2 = o2.getName();
                if (name1.length() > name2.length()) {
                    return 1;
                } else if (name1.length() < name2.length()) {
                    return -1;
                } else return name1.compareTo(name2);
            }
        };

        Comparator comparator = nameComparator;
        switch (order) {
            case ORDER_DEFAULT_ASC:
            case ORDER_DEFAULT_DESC: {
                comparator = nameComparator;
                break;
            }
            case ORDER_MODIFICATION_ASC:
            case ORDER_MODIFICATION_DESC: {
                comparator = modificationDateComparator;
                break;
            }
            case ORDER_SIZE_ASC:
            case ORDER_SIZE_DESC: {
                comparator = sizeComparator;
                break;
            }
            default: {
                break;
            }
        }

        Collections.sort(foldersOrder, comparator);

        Collections.sort(filesOrder, comparator);

        Boolean isDescending = false;
        switch (order) {
            case ORDER_DEFAULT_DESC:
            case ORDER_MODIFICATION_DESC:
            case ORDER_SIZE_DESC: {
                isDescending = true;
                break;
            }
            default: {
                break;
            }
        }


        if (isDescending) {
            Collections.reverse(foldersOrder);
            Collections.reverse(filesOrder);
        }

        tempOffline.addAll(foldersOrder);

        tempOffline.addAll(filesOrder);

        mOffList.clear();
        mOffList.addAll(tempOffline);
    }

    public static void log(String message) {
        Util.log("OfflineUtils", message);
    }
}
