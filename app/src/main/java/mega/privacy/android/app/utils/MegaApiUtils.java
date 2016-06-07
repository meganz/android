package mega.privacy.android.app.utils;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;


public class MegaApiUtils {

    public static long getFolderSize(MegaNode parent, Context context) {
        log("getFolderSize: "+parent.getName());
        MegaApiAndroid megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        long size = 0;
//        File[] files = dir.listFiles();
        ArrayList<MegaNode> nodeList = megaApi.getChildren(parent);
        for (MegaNode node : nodeList) {
            if (node.isFile()) {
                size += node.getSize();
            }
            else{
                size += getFolderSize(node, context);
            }
        }
        return size;
    }

    public static String getInfoNodeOnlyFolders(ArrayList<MegaNode> nodes, Context context) {
        log("getInfoNodeOnlyFolders");
        int numFolders = nodes.size();

        String info = "";
        if (numFolders > 0) {
            info = numFolders
                    + " "
                    + context.getResources().getQuantityString(
                    R.plurals.general_num_folders, numFolders);

        }
        return info;
    }

    public static String getInfoNode(ArrayList<MegaNode> nodes, Context context) {
        log("getInfoNode");
        int numFolders = 0;
        int numFiles = 0;

        for (int i=0;i<nodes.size();i++){
            MegaNode n = nodes.get(i);
            if (n.isFolder()){
                numFolders++;
            }
            else{
                numFiles++;
            }
        }

        String info = "";
        if (numFolders > 0) {
            info = numFolders
                    + " "
                    + context.getResources().getQuantityString(
                    R.plurals.general_num_folders, numFolders);
            if (numFiles > 0) {
                info = info
                        + ", "
                        + numFiles
                        + " "
                        + context.getResources().getQuantityString(
                        R.plurals.general_num_files, numFiles);
            }
        } else {
            info = numFiles
                    + " "
                    + context.getResources().getQuantityString(
                    R.plurals.general_num_files, numFiles);
        }

        return info;
    }

    public static String getInfoFolder(MegaNode n, Context context) {
        log("getInfoFolder");
        MegaApiAndroid megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        int numFolders = megaApi.getNumChildFolders(n);
        int numFiles = megaApi.getNumChildFiles(n);
        log("numFolders, numFiles: "+numFolders+ " " + numFiles);
        String info = "";
        if (numFolders > 0) {
            info = numFolders
                    + " "
                    + context.getResources().getQuantityString(
                    R.plurals.general_num_folders, numFolders);
            if (numFiles > 0) {
                info = info
                        + ", "
                        + numFiles
                        + " "
                        + context.getResources().getQuantityString(
                        R.plurals.general_num_files, numFiles);
            }
        } else {
            info = numFiles
                    + " "
                    + context.getResources().getQuantityString(
                    R.plurals.general_num_files, numFiles);
        }

        return info;
    }

    /*
 * If there is an application that can manage the Intent, returns true. Otherwise, false.
 */
    public static boolean isIntentAvailable(Context ctx, Intent intent) {
        log("isIntentAvailable");
        final PackageManager mgr = ctx.getPackageManager();
        List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public static int calculateDeepBrowserTreeIncoming(MegaNode node, Context context){
        log("calculateDeepBrowserTreeIncoming");
        MegaApiAndroid megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        String path = megaApi.getNodePath(node);
        log("The path is: "+path);

        Pattern pattern = Pattern.compile("/");
        int count = Util.countMatches(pattern, path);

        return count+1;
    }

    public static String createStringTree (MegaNode node, Context context){
        log("createStringTree");
        MegaApiAndroid megaApi = null;
        if (context instanceof Service){
            megaApi = ((MegaApplication) ((Service)context).getApplication()).getMegaApi();
        }
        else if (context instanceof Activity){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        if(megaApi==null){
            log("ERROR megaApi is null");
            return null;
        }

        ArrayList<MegaNode> dTreeList = new ArrayList<MegaNode>();
        MegaNode parentNode = null;
        MegaNode nodeTemp = node;
        StringBuilder dTree = new StringBuilder();
        String s;

        dTreeList.add(node);

        if(node.getType() != MegaNode.TYPE_ROOT){
            parentNode=megaApi.getParentNode(nodeTemp);

//			if(parentNode!=null){
//				while (parentNode.getType() != MegaNode.TYPE_ROOT){
//					if(parentNode!=null){
//						dTreeList.add(parentNode);
//						dTree.insert(0, parentNode.getName()+"/");
//						nodeTemp=parentNode;
//						parentNode=megaApi.getParentNode(nodeTemp);
//					}
//				}
//			}

            if(parentNode!=null){

                if((parentNode.getType() != MegaNode.TYPE_ROOT) & (parentNode.getHandle()!=megaApi.getInboxNode().getHandle())){
                    do{

                        dTreeList.add(parentNode);
                        dTree.insert(0, parentNode.getName()+"/");
                        nodeTemp=parentNode;

                        parentNode=megaApi.getParentNode(nodeTemp);
                        if(parentNode==null){
                            break;
                        }
                    }while ((parentNode.getType() != MegaNode.TYPE_ROOT) & (parentNode.getHandle()!=megaApi.getInboxNode().getHandle()));
                }
            }
        }

        if(dTree.length()>0){
            s = dTree.toString();
        }
        else{
            s="";
        }

        log("createStringTree: "+s);
        return s;
    }

    private static void log(String message) {

        Util.log("MegaApiUtils", message);
    }

}
