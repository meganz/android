package mega.privacy.android.app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class OfflineUtils {

    public static final String offlineDIR = "MEGA Offline";
    public static final String offlineInboxDIR = offlineDIR + File.separator + "in";

    public static final String oldOfflineDIR = mainDIR + File.separator + offlineDIR;

    static DatabaseHandler dbH;
    static MegaApiAndroid megaApi;

    public static void saveOffline (File destination, MegaNode node, Context context, Activity activity, MegaApiAndroid megaApi){
        log("saveOffline");

        OfflineUtils.megaApi = megaApi;

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
            getDlList(dlFiles, node, new File(destination, node.getName()));
        } else {
            log("saveOffline:isFile");
            dlFiles.put(node, destination.getAbsolutePath());
        }

        for (MegaNode document : dlFiles.keySet()) {

            String path = dlFiles.get(document);

            if(availableFreeSpace <document.getSize()){
                Util.showErrorAlertDialog(context.getString(R.string.error_not_enough_free_space) + " (" + new String(document.getName()) + ")", false, activity);
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
    public static void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {

        if (megaApi.getRootNode() == null)
            return;

        folder.mkdir();
        ArrayList<MegaNode> nodeList = megaApi.getChildren(parent);
        for(int i=0; i<nodeList.size(); i++){
            MegaNode document = nodeList.get(i);
            if (document.getType() == MegaNode.TYPE_FOLDER) {
                File subfolder = new File(folder, new String(document.getName()));
                getDlList(dlFiles, document, subfolder);
            }
            else {
                dlFiles.put(document, folder.getAbsolutePath());
            }
        }
    }

    public static void removeOffline(MegaOffline mOffDelete, DatabaseHandler dbH, Context context) {

        OfflineUtils.dbH = dbH;

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
                deleteChildrenDB(mOffListChildren);
            }
        } else {
            log("NOT children... ");
        }

        //remove red arrow from current item
        int parentId = mOffDelete.getParentId();
        dbH.removeById(mOffDelete.getId());
        if (parentId != -1) {
            updateParentOfflineStatus(parentId);
        }

        //Remove the node physically
        File offlineFile = getOfflineFile(context, mOffDelete);
        try {
            deleteFolderAndSubfolders(context,offlineFile);
        } catch (Exception e) {
            log("EXCEPTION: removeOffline - file " + e.toString());
        }

    }

    public static void updateParentOfflineStatus(int parentId) {
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
                updateParentOfflineStatus(grandParentNodeId);
            }
        }
    }

    public static void deleteChildrenDB(ArrayList<MegaOffline> mOffList){

        log("deleteChildenDB");
        MegaOffline mOffDelete=null;

        for(int i=0; i< mOffList.size(); i++){

            mOffDelete=mOffList.get(i);
            ArrayList<MegaOffline> mOffListChildren2=dbH.findByParentId(mOffDelete.getId());
            if(mOffList.size()>0){
                //The node have children, delete
                deleteChildrenDB(mOffListChildren2);

            }
            dbH.removeById(mOffDelete.getId());
        }
    }

    public static boolean availableOffline (Context context, MegaNode node) {

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }

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

    public static String getOfflinePath(Context context, MegaOffline offlineNode) {

        switch (offlineNode.getOrigin()) {
            case MegaOffline.INCOMING: {
                return context.getFilesDir().getAbsolutePath() + File.separator + offlineDIR + File.separator + offlineNode.getHandleIncoming();
            }
            case MegaOffline.INBOX: {
                return context.getFilesDir().getAbsolutePath() + File.separator + offlineInboxDIR;
            }
            default: {
                return context.getFilesDir().getAbsolutePath() + File.separator + offlineDIR;
            }
        }
    }

    public static File getOfflineFile(Context context, MegaOffline offlineNode) {
        String path =  context.getFilesDir().getAbsolutePath() + File.separator;

        switch (offlineNode.getOrigin()) {
            case MegaOffline.INCOMING: {
                path = path + offlineDIR + File.separator + offlineNode.getHandleIncoming();
                break;
            }
            case MegaOffline.INBOX: {
                path = path + offlineInboxDIR;
                break;
            }
            default: {
                path = path + offlineDIR;
            }
        }
        path =  path + File.separator + offlineNode.getPath() + File.separator + offlineNode.getName();

       return new File(path);
    }

    public static File getOfflineFile(Context context, int from, MegaNode node, boolean onlyParent, MegaApiAndroid megaApi) {
        String path = context.getFilesDir().getAbsolutePath() + File.separator;

        switch (from) {
            case Constants.FROM_INCOMING_SHARES: {
                path = path + offlineDIR + File.separator + OfflineUtils.findIncomingParentHandle(node, megaApi);
                break;
            }
            case Constants.FROM_INBOX: {
                path = path + offlineInboxDIR;
                break;
            }
            default: {
                path = path + offlineDIR;
            }
        }
        if (onlyParent) {
            path = path + File.separator + MegaApiUtils.createStringTree(node, context);
        } else {
            path = path + File.separator + MegaApiUtils.createStringTree(node, context) + File.separator + node.getName();
        }

        return new File(path);
    }

    public static String getOfflineSize(Context context){
        log("getOfflineSize");
        File offline = getOfflineFolder(context, offlineDIR);
        long size;
        if(isFileAvailable(offline)){
            size = getDirSize(offline);
            return getSizeString(size);
        }
        else{
            return getSizeString(0);
        }
    }

    public static void clearOffline(Context context){
        log("clearOffline");
        File offline = getOfflineFolder(context, offlineDIR);
        if(isFileAvailable(offline)){
            try {
                deleteFolderAndSubfolders(context, offline);
            } catch (IOException e) {
                e.printStackTrace();
                log("Exception deleting offline folder");
            }
        }
    }

    public static void log(String message) {
        Util.log("OfflineUtils", message);
    }
}
