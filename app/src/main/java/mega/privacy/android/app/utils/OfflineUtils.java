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

public class OfflineUtils {

    public static final String offlineDIR = "MEGA Offline";
    public static final String offlineInboxDIR = offlineDIR + File.separator + "in";

    public static final String oldOfflineDIR = mainDIR + File.separator + offlineDIR;

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
        return setOfflinePath(path, offlineNode);
    }

    private static File setOfflinePath (String path, MegaOffline offlineNode) {
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
        if (offlineNode.getPath().equals(File.separator)) {
            path = path + File.separator + offlineNode.getName();
        }
        else {
            path = path + offlineNode.getPath() + offlineNode.getName();
        }
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

    public static void saveOffline (Context context, MegaApiAndroid megaApi, DatabaseHandler dbH, MegaNode node, String path){
        log("saveOffline destination: "+path);

        File destination = new File(path);
        destination.mkdirs();

        log("saveOffline: "+ destination.getAbsolutePath());
        log("Handle to save for offline : "+node.getHandle());

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

        if(path.contains(offlineInboxDIR)){
            insertDB(context, megaApi, dbH, nodesToDB, true);
        }
        else{
            insertDB(context, megaApi, dbH, nodesToDB, false);
        }
    }

    public static void saveOfflineChatFile (DatabaseHandler dbH, MegaTransfer transfer){
        log("saveOfflineChatFile: "+transfer.getNodeHandle()+ " " + transfer.getFileName());

        MegaOffline mOffInsert = new MegaOffline(Long.toString(transfer.getNodeHandle()), "/", transfer.getFileName(),-1, DB_FILE, 0, "-1");
        long checkInsert=dbH.setOfflineFile(mOffInsert);
        log("Test insert Chat File: "+checkInsert);

    }

    private static void insertDB (Context context, MegaApiAndroid megaApi, DatabaseHandler dbH, ArrayList<MegaNode> nodesToDB, boolean fromInbox){
        log("insertDB");

        MegaNode parentNode = null;
        MegaNode nodeToInsert = null;

        String path = "/";
        MegaOffline mOffParent=null;
        MegaOffline mOffNode = null;

        for(int i=nodesToDB.size()-1; i>=0; i--){

            nodeToInsert = nodesToDB.get(i);
            log("Node to insert: "+nodeToInsert.getName());

            //If I am the owner
            if (megaApi.checkAccess(nodeToInsert, MegaShare.ACCESS_OWNER).getErrorCode() == MegaError.API_OK){

                if(megaApi.getParentNode(nodeToInsert).getType() != MegaNode.TYPE_ROOT){

                    parentNode = megaApi.getParentNode(nodeToInsert);
                    log("ParentNode: "+parentNode.getName());
                    log("PARENT NODE nooot ROOT");

                    path = MegaApiUtils.createStringTree(nodeToInsert, context);
                    if(path==null){
                        path="/";
                    }
                    else{
                        path="/"+path;
                    }
                    log("PAth node to insert: --- "+path);
                    //Get the node parent
                    mOffParent = dbH.findByHandle(parentNode.getHandle());
                    //If the parent is not in the DB
                    //Insert the parent in the DB
                    if(mOffParent==null){
                        if(parentNode!=null){
                            insertParentDB(context, megaApi, dbH, parentNode, fromInbox);
                        }
                    }

                    mOffNode = dbH.findByHandle(nodeToInsert.getHandle());
                    mOffParent = dbH.findByHandle(parentNode.getHandle());
                    if(mOffNode == null){

                        if(mOffParent!=null){
                            log("Parent of the node is NOT null");
                            if(nodeToInsert.isFile()){
                                if(fromInbox){
                                    MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), mOffParent.getId(), DB_FILE,MegaOffline.INBOX, "-1");
                                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                                    log("Test insert A: "+checkInsert);
                                }
                                else{
                                    MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), mOffParent.getId(), DB_FILE,MegaOffline.OTHER, "-1");
                                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                                    log("Test insert A: "+checkInsert);
                                }
                            }
                            else{
                                if(fromInbox){
                                    MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), mOffParent.getId(), DB_FOLDER, MegaOffline.INBOX, "-1");
                                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                                    log("Test insert B1: "+checkInsert);
                                }
                                else{
                                    MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), mOffParent.getId(), DB_FOLDER, MegaOffline.OTHER, "-1");
                                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                                    log("Test insert B2: "+checkInsert);
                                }
                            }
                        }
                        else{
                            log("Parent of the node is NULL");
                            path="/";

                            if(nodeToInsert.isFile()){
                                if(fromInbox){
                                    MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(),-1, DB_FILE, MegaOffline.INBOX, "-1");
                                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                                    log("Test insert E1: "+checkInsert);
                                }
                                else{
                                    MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(),-1, DB_FILE, MegaOffline.OTHER, "-1");
                                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                                    log("Test insert E2: "+checkInsert);
                                }
                            }
                            else{
                                if(fromInbox){
                                    MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), -1, DB_FOLDER, MegaOffline.INBOX, "-1");
                                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                                    log("Test insert F1: "+checkInsert);
                                }
                                else{
                                    MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), -1, DB_FOLDER, MegaOffline.OTHER, "-1");
                                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                                    log("Test insert F2: "+checkInsert);
                                }
                            }
                        }
                    }

                }
                else{
                    path="/";

                    if(nodeToInsert.isFile()){
                        if(fromInbox){
                            MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(),-1, DB_FILE, MegaOffline.INBOX, "-1");
                            long checkInsert=dbH.setOfflineFile(mOffInsert);
                            log("Test insert C1: "+checkInsert);
                        }
                        else{
                            MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(),-1, DB_FILE, MegaOffline.OTHER, "-1");
                            long checkInsert=dbH.setOfflineFile(mOffInsert);
                            log("Test insert C2: "+checkInsert);
                        }

                    }
                    else{
                        if(fromInbox){
                            MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), -1, DB_FOLDER, MegaOffline.INBOX, "-1");
                            long checkInsert=dbH.setOfflineFile(mOffInsert);
                            log("Test insert D1: "+checkInsert);
                        }
                        else{
                            MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), -1, DB_FOLDER, MegaOffline.OTHER, "-1");
                            long checkInsert=dbH.setOfflineFile(mOffInsert);
                            log("Test insert D2: "+checkInsert);
                        }
                    }
                }

            }
            else{
                //If I am not the owner
                log("Im not the owner: "+megaApi.getParentNode(nodeToInsert));

                parentNode = megaApi.getParentNode(nodeToInsert);
                log("ParentNode: "+parentNode.getName());

                path = MegaApiUtils.createStringTree(nodeToInsert, context);
                if(path==null){
                    path="/";
                }
                else{
                    path="/"+path;
                }

                log("PAth node to insert: --- "+path);
                //Get the node parent
                mOffParent = dbH.findByHandle(parentNode.getHandle());
                //If the parent is not in the DB
                //Insert the parent in the DB
                if(mOffParent==null){
                    if(parentNode!=null){
                        insertIncomingParentDB(context, megaApi, dbH, parentNode);
                    }
                }

                mOffNode = dbH.findByHandle(nodeToInsert.getHandle());
                mOffParent = dbH.findByHandle(parentNode.getHandle());

                String handleIncoming = "";
                if(parentNode!=null){
                    MegaNode ownerNode = megaApi.getParentNode(parentNode);
                    if(ownerNode!=null){
                        MegaNode nodeWhile = ownerNode;
                        while (nodeWhile!=null){
                            ownerNode=nodeWhile;
                            nodeWhile = megaApi.getParentNode(nodeWhile);
                        }

                        handleIncoming=Long.toString(ownerNode.getHandle());
                    }
                    else{
                        handleIncoming=Long.toString(parentNode.getHandle());
                    }

                }

                if(mOffNode == null){
                    log("Inserto el propio nodo: "+ nodeToInsert.getName() + "handleIncoming: "+handleIncoming);

                    if(mOffParent!=null){
                        if(nodeToInsert.isFile()){
                            MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), mOffParent.getId(), DB_FILE,MegaOffline.INCOMING, handleIncoming);
                            long checkInsert=dbH.setOfflineFile(mOffInsert);
                            log("Test insert A: "+checkInsert);
                        }
                        else{
                            MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), mOffParent.getId(), DB_FOLDER, MegaOffline.INCOMING, handleIncoming);
                            long checkInsert=dbH.setOfflineFile(mOffInsert);
                            log("Test insert B: "+checkInsert);
                        }
                    }
                }
            }
        }
    }

    //Insert for incoming
    private static void insertIncomingParentDB (Context context, MegaApiAndroid megaApi, DatabaseHandler dbH, MegaNode parentNode){
        log("insertIncomingParentDB: Check SaveOffline: "+parentNode.getName());

        MegaOffline mOffParentParent = null;
        String path=MegaApiUtils.createStringTree(parentNode, context);
        if(path==null){
            path="/";
        }
        else{
            path="/"+path;
        }

        log("PATH   IncomingParentDB: "+path);

        MegaNode parentparentNode = megaApi.getParentNode(parentNode);

        if(parentparentNode==null){

            if(parentNode.isFile()){
                MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(),-1, DB_FILE, MegaOffline.INCOMING, Long.toString(parentNode.getHandle()));
                long checkInsert=dbH.setOfflineFile(mOffInsert);
                log("Test insert C: "+checkInsert);
            }
            else{
                MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), -1, DB_FOLDER, MegaOffline.INCOMING, Long.toString(parentNode.getHandle()));
                long checkInsert=dbH.setOfflineFile(mOffInsert);
                log("Test insert D: "+checkInsert);
            }
        }
        else{

            String handleIncoming = "";

            MegaNode ownerNode = megaApi.getParentNode(parentparentNode);
            if(ownerNode!=null){
                MegaNode nodeWhile = ownerNode;
                while (nodeWhile!=null){
                    ownerNode=nodeWhile;
                    nodeWhile = megaApi.getParentNode(nodeWhile);
                }

                handleIncoming=Long.toString(ownerNode.getHandle());
            }
            else{
                handleIncoming=Long.toString(parentparentNode.getHandle());
            }


            mOffParentParent = dbH.findByHandle(parentparentNode.getHandle());
            if(mOffParentParent==null){
                insertIncomingParentDB(context, megaApi, dbH, megaApi.getParentNode(parentNode));
                //Insert the parent node
                mOffParentParent = dbH.findByHandle(megaApi.getParentNode(parentNode).getHandle());
                if(mOffParentParent==null){
                    insertIncomingParentDB(context, megaApi, dbH, megaApi.getParentNode(parentNode));

                }
                else{

                    if(parentNode.isFile()){
                        MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FILE, MegaOffline.INCOMING, handleIncoming);
                        long checkInsert=dbH.setOfflineFile(mOffInsert);
                        log("Test insert E: "+checkInsert);
                    }
                    else{
                        MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FOLDER, MegaOffline.INCOMING, handleIncoming);
                        long checkInsert=dbH.setOfflineFile(mOffInsert);
                        log("Test insert F: "+checkInsert);
                    }
                }
            }
            else{

                if(parentNode.isFile()){
                    MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FILE, MegaOffline.INCOMING, handleIncoming);
                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                    log("Test insert G: "+checkInsert);
                }
                else{
                    MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FOLDER, MegaOffline.INCOMING, handleIncoming);
                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                    log("Test insert H: "+checkInsert);
                }
            }
        }
    }

    private static void insertParentDB (Context context, MegaApiAndroid megaApi, DatabaseHandler dbH, MegaNode parentNode, boolean fromInbox){
        log("insertParentDB: Check SaveOffline: "+parentNode.getName());

        MegaOffline mOffParentParent = null;
        String path=MegaApiUtils.createStringTree(parentNode, context);
        if(path==null){
            path="/";
        }
        else{
            path="/"+path;
        }

        MegaNode parentparentNode = megaApi.getParentNode(parentNode);
        if(parentparentNode==null){
            log("return insertParentDB");
            return;
        }

        if(parentparentNode.getType() != MegaNode.TYPE_ROOT){

            if(parentparentNode.getHandle()==megaApi.getInboxNode().getHandle()){
                log("---------------PARENT NODE INBOX------");
                if(parentNode.isFile()){
                    if(fromInbox){
                        MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(),-1, DB_FILE, MegaOffline.INBOX, "-1");
                        long checkInsert=dbH.setOfflineFile(mOffInsert);
                        log("Test insert M: "+checkInsert);
                    }
                    else{
                        MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(),-1, DB_FILE, MegaOffline.OTHER, "-1");
                        long checkInsert=dbH.setOfflineFile(mOffInsert);
                        log("Test insert M: "+checkInsert);
                    }
                }
                else{
                    if(fromInbox){
                        MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), -1, DB_FOLDER, MegaOffline.INBOX, "-1");
                        long checkInsert=dbH.setOfflineFile(mOffInsert);
                        log("Test insert N: "+checkInsert);
                    }
                    else{
                        MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), -1, DB_FOLDER, MegaOffline.OTHER, "-1");
                        long checkInsert=dbH.setOfflineFile(mOffInsert);
                        log("Test insert N: "+checkInsert);
                    }

                }
                return;
            }

            mOffParentParent = dbH.findByHandle(parentparentNode.getHandle());
            if(mOffParentParent==null){
                log("mOffParentParent==null");
                insertParentDB(context, megaApi, dbH, megaApi.getParentNode(parentNode), fromInbox);
                //Insert the parent node
                mOffParentParent = dbH.findByHandle(megaApi.getParentNode(parentNode).getHandle());
                if(mOffParentParent==null){
                    log("call again");
                    insertParentDB(context, megaApi, dbH, megaApi.getParentNode(parentNode), fromInbox);
                }
                else{
                    log("second check NOOOTTT mOffParentParent==null");
                    if(parentNode.isFile()){
                        if(fromInbox){
                            MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FILE, MegaOffline.INBOX, "-1");
                            long checkInsert=dbH.setOfflineFile(mOffInsert);
                            log("Test insert I1: "+checkInsert);
                        }
                        else{
                            MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FILE, MegaOffline.OTHER, "-1");
                            long checkInsert=dbH.setOfflineFile(mOffInsert);
                            log("Test insert I2: "+checkInsert);
                        }
                    }
                    else{
                        if(fromInbox){
                            MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FOLDER, MegaOffline.INBOX, "-1");
                            long checkInsert=dbH.setOfflineFile(mOffInsert);
                            log("Test insert J1: "+checkInsert);
                        }
                        else{
                            MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FOLDER, MegaOffline.OTHER, "-1");
                            long checkInsert=dbH.setOfflineFile(mOffInsert);
                            log("Test insert J2: "+checkInsert);
                        }
                    }
                }
            }
            else{
                log("NOOOTTT mOffParentParent==null");
                if(parentNode.isFile()){
                    if(fromInbox){
                        MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FILE, MegaOffline.INBOX, "-1");
                        long checkInsert=dbH.setOfflineFile(mOffInsert);
                        log("Test insert K1: "+checkInsert);
                    }
                    else{
                        MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FILE, MegaOffline.OTHER, "-1");
                        long checkInsert=dbH.setOfflineFile(mOffInsert);
                        log("Test insert K2: "+checkInsert);
                    }
                }
                else{
                    if(fromInbox){
                        MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FOLDER, MegaOffline.INBOX, "-1");
                        long checkInsert=dbH.setOfflineFile(mOffInsert);
                        log("Test insert L1: "+checkInsert);
                    }
                    else{
                        MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), mOffParentParent.getId(), DB_FOLDER, MegaOffline.OTHER, "-1");
                        long checkInsert=dbH.setOfflineFile(mOffInsert);
                        log("Test insert L2: "+checkInsert);
                    }
                }
            }
        }
        else{
            log("---------------PARENT NODE ROOT------");
            if(parentNode.isFile()){
                if(fromInbox){
                    MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(),-1, DB_FILE, MegaOffline.INBOX, "-1");
                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                    log("Test insert M1: "+checkInsert);
                }
                else{
                    MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(),-1, DB_FILE, MegaOffline.OTHER, "-1");
                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                    log("Test insert M2: "+checkInsert);
                }
            }
            else{
                if(fromInbox){
                    MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), -1, DB_FOLDER, MegaOffline.INBOX, "-1");
                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                    log("Test insert N1: "+checkInsert);
                }
                else{
                    MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), -1, DB_FOLDER, MegaOffline.OTHER, "-1");
                    long checkInsert=dbH.setOfflineFile(mOffInsert);
                    log("Test insert N2: "+checkInsert);
                }
            }
        }

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
        MegaApiAndroid megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
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
                log("moveOfflineFiles remove from database");
                dbH.removeById(offlineNode.getId());
                continue;
            }

            File newOfflineFile = getOfflineFile(context, offlineNode);
            new File(newOfflineFile.getParent()).mkdirs();
            if (!moveFile(oldOfflineFile, newOfflineFile)) {
                log("moveOfflineFiles error moving: " + offlineNode.getHandle());
                dbH.removeById(offlineNode.getId());
                oldOfflineFile.delete();
                continue;
            }

            log("moveOfflineFiles moved: " + offlineNode.getHandle());
        }

        removeOldTempFolder(context, oldOfflineDIR);
    }

    private static File findOldOfflineFile(MegaOffline offlineNode) {
        String path = getOldTempFolder(mainDIR).getAbsolutePath() + File.separator;
        return setOfflinePath(path, offlineNode);
    }

    public static void log(String message) {
        Util.log("OfflineUtils", message);
    }
}
