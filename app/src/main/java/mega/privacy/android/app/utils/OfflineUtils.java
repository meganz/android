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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

public class OfflineUtils {

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
            getDlList(dlFiles, node, new File(destination, new String(node.getName())));
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

    public static void removeOffline(MegaOffline mOffDelete, DatabaseHandler dbH, Context context, int from) {

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
        File destination;
        log("Path: " + mOffDelete.getPath());
        if (mOffDelete.getOrigin() == MegaOffline.INCOMING) {
            if (Environment.getExternalStorageDirectory() != null) {
                destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + mOffDelete.getHandleIncoming() + "/" + mOffDelete.getPath());
            } else {
                destination = new File(context.getFilesDir(),mOffDelete.getHandle() + "");
            }

            log("Remove incoming: " + destination.getAbsolutePath());

            try {
                File offlineFile = new File(destination,mOffDelete.getName());
                Util.deleteFolderAndSubfolders(context,offlineFile);
            } catch (Exception e) {
                log("EXCEPTION: removeOffline - file " + e.toString());
            }
            ;

            dbH.removeById(mOffDelete.getId());
        } else {
            if (from == Constants.FROM_INBOX) {
                if (Environment.getExternalStorageDirectory() != null) {
                    destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in/" + mOffDelete.getPath());
                    log("offline File INCOMING: " + destination.getAbsolutePath());
                } else {
                    destination = context.getFilesDir();
                }
            } else {
                if (Environment.getExternalStorageDirectory() != null) {
                    destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + mOffDelete.getPath());
                } else {
                    destination = new File(context.getFilesDir(),mOffDelete.getHandle() + "");
                }
            }

            log("Remove node: " + destination.getAbsolutePath());

            try {
                File offlineFile = new File(destination,mOffDelete.getName());
                Util.deleteFolderAndSubfolders(context,offlineFile);
            } catch (Exception e) {
                log("EXCEPTION: removeOffline - file");
            }

            dbH.removeById(mOffDelete.getId());
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

    public static boolean availableOffline (int from, MegaNode node, Context context, MegaApiAndroid megaApi) {
        File offlineFile = null;
        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }

        if(dbH.exists(node.getHandle())) {
            log("Exists OFFLINE in the DB!!!");

            MegaOffline offlineNode = dbH.findByHandle(node.getHandle());
            if (offlineNode != null) {
                log("YESS FOUND: " + node.getName());
                if (from == Constants.INCOMING_SHARES_ADAPTER) {
                    log("FROM_INCOMING_SHARES");
                    //Find in the filesystem
                    if (Environment.getExternalStorageDirectory() != null) {
                        offlineFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + offlineNode.getHandleIncoming() + offlineNode.getPath()+ "/" + node.getName());
                        log("offline File INCOMING: " + offlineFile.getAbsolutePath());
                    }
                    else {
                        offlineFile = context.getFilesDir();
                    }
                }
                else if(from==Constants.INBOX_ADAPTER){

                    if (Environment.getExternalStorageDirectory() != null) {
                        offlineFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in/" + offlineNode.getPath()+ "/" + node.getName());
                        log("offline File INCOMING: " + offlineFile.getAbsolutePath());
                    }
                    else {
                        offlineFile = context.getFilesDir();
                    }
                }
                else {
                    log("NOT INCOMING NEITHER INBOX");
                    //Find in the filesystem
                    if (Environment.getExternalStorageDirectory() != null) {
                        offlineFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + megaApi.getNodePath(node));
                        log("offline File: " + offlineFile.getAbsolutePath());
                    }
                    else {
                        offlineFile = context.getFilesDir();
                    }
                }
                if (offlineFile != null) {
                    if (offlineFile.exists()) {
                        log("FOUND!!!: " + node.getHandle() + " " + node.getName());
                        return true;
                    }
                    else {
                        log("Not found: " + node.getHandle() + " " + node.getName());
                        return false;
                    }
                }
                else {
                    log("Not found offLineFile is NULL");
                    return false;
                }
            }
            else{
                log("offLineNode is NULL");
                return false;
            }
        }
        else{
            log("NOT Exists in DB OFFLINE: setChecket FALSE: "+node.getHandle());
            return false;
        }
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

    public static void log(String message) {
        Util.log("OfflineUtils", message);
    }
}
