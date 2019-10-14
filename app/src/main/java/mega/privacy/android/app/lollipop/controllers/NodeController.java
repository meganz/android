package mega.privacy.android.app.lollipop.controllers;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.GetLinkActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.CopyAndSendToChatListener;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListener;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaRichLinkMessage;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop.IS_PLAYLIST;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtilsLollipop.*;
import static mega.privacy.android.app.utils.Util.*;

public class NodeController {

    Context context;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH;
    MegaPreferences prefs = null;

    boolean isFolderLink = false;

    public NodeController(Context context){
        logDebug("NodeController created");
        this.context = context;
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public NodeController(Context context, boolean isFolderLink){
        logDebug("NodeController created");
        this.context = context;
        this.isFolderLink = isFolderLink;
        if (megaApi == null){
            if (isFolderLink) {
                megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApiFolder();
            }
            else {
                megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
            }
        }
        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public void chooseLocationToCopyNodes(ArrayList<Long> handleList){
        logDebug("chooseLocationToCopyNodes");
        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("COPY_FROM", longArray);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
    }

    public void copyNodes(long[] copyHandles, long toHandle) {
        logDebug("copyNodes");

        if(!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        MegaNode parent = megaApi.getNodeByHandle(toHandle);
        if(parent!=null) {
            MultipleRequestListener copyMultipleListener = null;
            if (copyHandles.length > 1) {
                logDebug("Copy multiple files");
                copyMultipleListener = new MultipleRequestListener(MULTIPLE_COPY, context);
                for (int i = 0; i < copyHandles.length; i++) {
                    MegaNode cN = megaApi.getNodeByHandle(copyHandles[i]);
                    if (cN != null){
                        logDebug("cN != null, i = " + i + " of " + copyHandles.length);
                        megaApi.copyNode(cN, parent, copyMultipleListener);
                    }
                    else{
                        logWarning("cN == null, i = " + i + " of " + copyHandles.length);
                    }
                }
            } else {
                logDebug("Copy one file");
                MegaNode cN = megaApi.getNodeByHandle(copyHandles[0]);
                if (cN != null){
                    logDebug("cN != null");
                    megaApi.copyNode(cN, parent, (ManagerActivityLollipop) context);
                }
                else{
                    logWarning("cN == null");
                    if(context instanceof ManagerActivityLollipop){
                        ((ManagerActivityLollipop)context).copyError();
                    }
                }
            }
        }

    }

    public void chooseLocationToMoveNodes(ArrayList<Long> handleList){
        logDebug("chooseLocationToMoveNodes");
        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("MOVE_FROM", longArray);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
    }

    public void moveNodes(long[] moveHandles, long toHandle){
        logDebug("moveNodes");

        if(!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        MegaNode parent = megaApi.getNodeByHandle(toHandle);
        if(parent!=null){
            MultipleRequestListener moveMultipleListener = new MultipleRequestListener(MULTIPLE_MOVE, context);

            if(moveHandles.length>1){
                logDebug("MOVE multiple: " + moveHandles.length);

                for(int i=0; i<moveHandles.length;i++){
                    megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[i]), parent, moveMultipleListener);
                }
            }
            else{
                logDebug("MOVE single");

                megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[0]), parent, (ManagerActivityLollipop) context);
            }
        }
    }

    public void checkIfNodeIsMineAndSelectChatsToSendNode(MegaNode node) {
        logDebug("checkIfNodeIsMineAndSelectChatsToSendNode");
        ArrayList<MegaNode> nodes = new ArrayList<>();
        nodes.add(node);
        checkIfNodesAreMineAndSelectChatsToSendNodes(nodes);
    }

    public void checkIfNodesAreMineAndSelectChatsToSendNodes(ArrayList<MegaNode> nodes) {
        logDebug("checkIfNodesAreMineAndSelectChatsToSendNodes");

        MegaNode currentNode;
        ArrayList<MegaNode> ownerNodes = new ArrayList<>();
        ArrayList<MegaNode> notOwnerNodes = new ArrayList<>();

        if (nodes == null) {
            return;
        }

        for (int i=0; i<nodes.size(); i++) {
            currentNode = nodes.get(i);
            if (currentNode != null) {
                if (currentNode.getOwner() == megaApi.getMyUserHandleBinary()) {
                    ownerNodes.add(currentNode);
                }
                else {
                    String nodeFP = megaApi.getFingerprint(currentNode);
                    ArrayList<MegaNode> fNodes = megaApi.getNodesByFingerprint(nodeFP);
                    MegaNode nodeOwner = null;
                    if (fNodes != null) {
                        for (int j=0; j<fNodes.size(); j++) {
                            if (fNodes.get(j).getOwner() == megaApi.getMyUserHandleBinary()){
                                nodeOwner = fNodes.get(j);
                                break;
                            }
                        }
                    }
                    if (nodeOwner != null) {
                        ownerNodes.add(nodeOwner);
                    }
                    else {
                        notOwnerNodes.add(currentNode);
                    }
                }
            }
        }

        if (notOwnerNodes.size() == 0) {
            selectChatsToSendNodes(ownerNodes);
        }
        else {
            CopyAndSendToChatListener copyAndSendToChatListener = new CopyAndSendToChatListener(context);
            copyAndSendToChatListener.copyNodes(notOwnerNodes, ownerNodes);
        }
    }

    public void selectChatsToSendNodes(ArrayList<MegaNode> nodes){
        logDebug("selectChatsToSendNodes");

        int size = nodes.size();
        long[] longArray = new long[size];

        for(int i=0;i<nodes.size();i++){
            longArray[i] = nodes.get(i).getHandle();
        }

        Intent i = new Intent(context, ChatExplorerActivity.class);
        i.putExtra("NODE_HANDLES", longArray);

        if(context instanceof FullScreenImageViewerLollipop){
            ((FullScreenImageViewerLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
        else if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
        else if (context instanceof PdfViewerActivityLollipop){
            ((PdfViewerActivityLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
        else if (context instanceof AudioVideoPlayerLollipop){
            ((AudioVideoPlayerLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
        else if (context instanceof FileInfoActivityLollipop) {
            ((FileInfoActivityLollipop) context).startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
        }
    }

    public boolean nodeComesFromIncoming (MegaNode node) {
        MegaNode parent = getParent(node);

        if (parent.getHandle() == megaApi.getRootNode().getHandle() ||
                parent.getHandle() == megaApi.getRubbishNode().getHandle() ||
                parent.getHandle() == megaApi.getInboxNode().getHandle()){
            return false;
        }
        else {
            return true;
        }
    }

    public MegaNode getParent (MegaNode node) {
        MegaNode parent = node;

        while (megaApi.getParentNode(parent) != null){
            parent = megaApi.getParentNode(parent);
        }

        return parent;
    }

    public int getIncomingLevel(MegaNode node) {
        int dBT = 0;
        MegaNode parent = node;

        while (megaApi.getParentNode(parent) != null){
            dBT++;
            parent = megaApi.getParentNode(parent);
        }

        return dBT;
    }

    public void prepareForDownload(ArrayList<Long> handleList, boolean highPriority){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            prepareForDownloadLollipop(handleList, highPriority);
        }
        else{
            prepareForDownloadPreLollipop(handleList, highPriority);
        }
    }

    void requestLocalFolder (boolean highPriority, long size, long[] hashes) {
        Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, context.getString(R.string.general_select));
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
        intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
        intent.setClass(context, FileStorageActivityLollipop.class);
        intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
        intent.putExtra(HIGH_PRIORITY_TRANSFER, highPriority);

        if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof FullScreenImageViewerLollipop){
            ((FullScreenImageViewerLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof FileInfoActivityLollipop){
            ((FileInfoActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof ContactFileListActivityLollipop){
            ((ContactFileListActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof PdfViewerActivityLollipop){
            ((PdfViewerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if(context instanceof AudioVideoPlayerLollipop){
            ((AudioVideoPlayerLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
    }

    //Old onFileClick
    public void prepareForDownloadLollipop(ArrayList<Long> handleList, final boolean highPriority){
        logDebug("prepareForDownload: " + handleList.size() + " files to download");
        long size = 0;
        long[] hashes = new long[handleList.size()];
        for (int i=0;i<handleList.size();i++){
            hashes[i] = handleList.get(i);
            MegaNode nodeTemp = megaApi.getNodeByHandle(hashes[i]);
            if (nodeTemp != null){
                if (nodeTemp.isFile()){
                    size += nodeTemp.getSize();
                }
            }
            else{
                logWarning("Error - nodeTemp is NULL");
            }

        }
        logDebug("Number of files: " + hashes.length);

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        boolean askMe = askMe(context);
        String downloadLocationDefaultPath = getDownloadLocation(context);

        if (askMe){
            logDebug("askMe");
            File[] fs = context.getExternalFilesDirs(null);
            if (fs.length > 1){
                if (fs[1] == null){
                    requestLocalFolder(highPriority, size, hashes);
                }
                else{
                    Dialog downloadLocationDialog;
                    String[] sdCardOptions = context.getResources().getStringArray(R.array.settings_storage_download_location_array);
                    AlertDialog.Builder b=new AlertDialog.Builder(context);

                    b.setTitle(context.getResources().getString(R.string.settings_storage_download_location));
                    final long sizeFinal = size;
                    final long[] hashesFinal = new long[hashes.length];
                    for (int i=0; i< hashes.length; i++){
                        hashesFinal[i] = hashes[i];
                    }

                    b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch(which){
                                case 0:{
                                    requestLocalFolder(highPriority, sizeFinal, hashesFinal);
                                    break;
                                }
                                case 1:{
                                    File[] fs = context.getExternalFilesDirs(null);
                                    if (fs.length > 1){
                                        String path = fs[1].getAbsolutePath();
                                        File defaultPathF = new File(path);
                                        defaultPathF.mkdirs();
                                        showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_save_to_device) + ": "  + defaultPathF.getAbsolutePath());
                                        checkSizeBeforeDownload(path, null, sizeFinal, hashesFinal, highPriority);
                                    }
                                    break;
                                }
                            }
                        }
                    });
                    b.setNegativeButton(context.getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    downloadLocationDialog = b.create();
                    downloadLocationDialog.show();
                }
            }
            else{
                requestLocalFolder(highPriority, size, hashes);
            }
        }
        else{
            logDebug("NOT askMe");
            File defaultPathF = new File(downloadLocationDefaultPath);
            defaultPathF.mkdirs();
            checkSizeBeforeDownload(downloadLocationDefaultPath, null, size, hashes, highPriority);
        }
    }

    private void showSnackbar(int type, String s) {
        if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop) context).showSnackbar(type, s, -1);
        } else if (context instanceof FullScreenImageViewerLollipop) {
            ((FullScreenImageViewerLollipop) context).showSnackbar(type, s, -1);
        } else if (context instanceof FileInfoActivityLollipop) {
            ((FileInfoActivityLollipop) context).showSnackbar(type, s, -1);
        } else if (context instanceof ContactFileListActivityLollipop) {
            ((ContactFileListActivityLollipop) context).showSnackbar(type, s);
        } else if (context instanceof PdfViewerActivityLollipop) {
            ((PdfViewerActivityLollipop) context).showSnackbar(type, s, -1);
        } else if (context instanceof AudioVideoPlayerLollipop) {
            ((AudioVideoPlayerLollipop) context).showSnackbar(type, s, -1);
        } else if (context instanceof ContactInfoActivityLollipop) {
            ((ContactInfoActivityLollipop) context).showSnackbar(type, s, -1);
        } else if (context instanceof GetLinkActivityLollipop) {
            ((GetLinkActivityLollipop) context).showSnackbar(s);
        } else {
            showSnackBar(context, type, s, -1);
        }
    }

    //Old onFileClick
    public void prepareForDownloadPreLollipop(ArrayList<Long> handleList, boolean highPriority){
        logDebug(handleList.size() + " files to download");
        long size = 0;
        long[] hashes = new long[handleList.size()];
        for (int i=0;i<handleList.size();i++){
            hashes[i] = handleList.get(i);
            MegaNode nodeTemp = megaApi.getNodeByHandle(hashes[i]);
            if (nodeTemp != null){
                size += nodeTemp.getSize();
            }
        }
        logDebug("Number of files: " + hashes.length);

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        boolean askMe = askMe(context);
        boolean advancedDevices=false;
        String downloadLocationDefaultPath = getDownloadLocation(context);
        prefs = dbH.getPreferences();

        if (prefs != null){
            if (prefs.getStorageAskAlways() != null){
                if (Boolean.parseBoolean(prefs.getStorageAskAlways())){
                    if (askMe == true && prefs.getStorageAdvancedDevices() != null){
                        advancedDevices = Boolean.parseBoolean(prefs.getStorageAdvancedDevices());
                    }
                }
            }
        }

        if (askMe){
            logDebug("askMe");
            if(advancedDevices){
                logDebug("advancedDevices");
                //Launch Intent to SAF
                if(hashes.length==1){
                    if(context instanceof ManagerActivityLollipop){
                        ((ManagerActivityLollipop) context).openAdvancedDevices(hashes[0], highPriority);
                    }
                    else if(context instanceof FullScreenImageViewerLollipop){
                        ((FullScreenImageViewerLollipop) context).openAdvancedDevices(hashes[0], highPriority);
                    }
                    else if(context instanceof FileInfoActivityLollipop){
                        ((FileInfoActivityLollipop) context).openAdvancedDevices(hashes[0], highPriority);
                    }
                    else if(context instanceof ContactFileListActivityLollipop){
                        ((ContactFileListActivityLollipop) context).openAdvancedDevices(hashes[0], highPriority);
                    }
                    else if(context instanceof PdfViewerActivityLollipop){
                        ((PdfViewerActivityLollipop) context).openAdvancedDevices(hashes[0], highPriority);
                    }
                    else if(context instanceof AudioVideoPlayerLollipop){
                        ((AudioVideoPlayerLollipop) context).openAdvancedDevices(hashes[0], highPriority);
                    }
                }
                else
                {
                    //Show error message, just one file
                    Toast.makeText(context, context.getString(R.string.context_select_one_file), Toast.LENGTH_LONG).show();
                }
            }
            else{
                logDebug("NOT advancedDevices");
                requestLocalFolder(highPriority, size, hashes);
            }
        }
        else{
            logDebug("NOT askMe");
            File defaultPathF = new File(downloadLocationDefaultPath);
            defaultPathF.mkdirs();
            checkSizeBeforeDownload(downloadLocationDefaultPath, null, size, hashes, highPriority);
        }

    }

    //Old downloadTo
    public void checkSizeBeforeDownload(String parentPath, String url, long size, long [] hashes, boolean highPriority){
        //Variable size is incorrect for folders, it is always -1 -> sizeTemp calculates the correct size
        logDebug("parentPath: " + parentPath + ", url: " + url + ", size: " + size);
        logDebug("Files to download: " + hashes.length);
        logDebug("SIZE to download before calculating: " + size);

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        long sizeTemp=0;

        for (long hash : hashes) {
            MegaNode node = megaApi.getNodeByHandle(hash);
            if(node!=null){
                if(node.isFolder()){
                    logDebug("Node to download is FOLDER");
                    sizeTemp=sizeTemp+ getFolderSize(node, context);
                }
                else{
                    sizeTemp = sizeTemp+node.getSize();
                }
            }
        }

        final long sizeC = sizeTemp;
        logDebug("The final size is: " + getSizeString(sizeTemp));

        //Check if there is available space
        double availableFreeSpace = Double.MAX_VALUE;
        try{
            StatFs stat = new StatFs(parentPath);
            availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
        }
        catch(Exception ex){}

        logDebug("availableFreeSpace: " + availableFreeSpace + "__ sizeToDownload: " + sizeC);

        if(availableFreeSpace < sizeC) {
            showSnackbar(NOT_SPACE_SNACKBAR_TYPE, null);
            logWarning("Not enough space");
            return;
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        String ask=dbH.getAttributes().getAskSizeDownload();

        if(ask==null){
            ask="true";
        }

        if(ask.equals("false")){
            logDebug("SIZE: Do not ask before downloading");
            checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
        }
        else{
            logDebug("SIZE: Ask before downloading");
            //Check size to download
            //100MB=104857600
            //10MB=10485760
            //1MB=1048576
            if(sizeC>104857600) {
                logDebug("Show size confirmacion: " + sizeC);
                //Show alert
                if (context instanceof ManagerActivityLollipop) {
                    ((ManagerActivityLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                } else if (context instanceof FullScreenImageViewerLollipop) {
                    ((FullScreenImageViewerLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                }
                else if(context instanceof FileInfoActivityLollipop){
                    ((FileInfoActivityLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                }
                else if(context instanceof ContactFileListActivityLollipop){
                    ((ContactFileListActivityLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                }
                else if(context instanceof PdfViewerActivityLollipop){
                    ((PdfViewerActivityLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                }
                else if(context instanceof AudioVideoPlayerLollipop){
                    ((AudioVideoPlayerLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                }
                else if(context instanceof ContactInfoActivityLollipop){
                    ((ContactInfoActivityLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
                }
            }
            else{
                checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
            }
        }
    }

    //Old proceedToDownload
    public void checkInstalledAppBeforeDownload(String parentPath, String url, long size, long [] hashes, boolean highPriority){
        logDebug("checkInstalledAppBeforeDownload");
        boolean confirmationToDownload = false;
        final String parentPathC = parentPath;
        final String urlC = url;
        final long sizeC = size;
        final long [] hashesC = hashes;
        String nodeToDownload = null;

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        String ask=dbH.getAttributes().getAskNoAppDownload();

        if(ask==null){
            logDebug("ask==null");
            ask="true";
        }

        if(ask.equals("false")){
            logDebug("INSTALLED APP: Do not ask before downloading");
            download(parentPathC, urlC, sizeC, hashesC, highPriority);
        }
        else{
            logDebug("INSTALLED APP: Ask before downloading");
            if (hashes != null){
                for (long hash : hashes) {
                    MegaNode node = megaApi.getNodeByHandle(hash);
                    if(node!=null){
                        logDebug("Node: " + node.getHandle());

                        if(node.isFile()){
                            Intent checkIntent = new Intent(Intent.ACTION_GET_CONTENT, null);
                            logDebug("MimeTypeList: " + MimeTypeList.typeForName(node.getName()).getType());

                            checkIntent.setType(MimeTypeList.typeForName(node.getName()).getType());

                            try{
                                if (!isIntentAvailable(context, checkIntent)){
                                    confirmationToDownload = true;
                                    nodeToDownload=node.getName();
                                    break;
                                }
                            }catch(Exception e){
                                logWarning("isIntent EXCEPTION", e);
                                confirmationToDownload = true;
                                nodeToDownload=node.getName();
                                break;
                            }
                        }
                    }
                    else{
                        logWarning("ERROR - node is NULL");
                    }
                }
            }

            //Check if show the alert message
            if(confirmationToDownload){
                //Show message
                if(context instanceof ManagerActivityLollipop){
                    ((ManagerActivityLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC, urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
                else if(context instanceof FullScreenImageViewerLollipop){
                    ((FullScreenImageViewerLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC, urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
                else if(context instanceof FileInfoActivityLollipop){
                    ((FileInfoActivityLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC, urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
                else if(context instanceof ContactFileListActivityLollipop){
                    ((ContactFileListActivityLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC, urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
                else if(context instanceof PdfViewerActivityLollipop){
                    ((PdfViewerActivityLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC, urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
                else if(context instanceof AudioVideoPlayerLollipop){
                    ((AudioVideoPlayerLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC, urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
                else if(context instanceof ContactInfoActivityLollipop){
                    ((ContactInfoActivityLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC, urlC, sizeC, hashesC, nodeToDownload, highPriority);
                }
            }
            else{
                download(parentPathC, urlC, sizeC, hashesC, highPriority);
            }
        }
    }

    void askForPermissions () {
        if(context instanceof ManagerActivityLollipop){
            ActivityCompat.requestPermissions(((ManagerActivityLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if (context instanceof FileLinkActivityLollipop) {
            ActivityCompat.requestPermissions((FileLinkActivityLollipop)context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if(context instanceof FullScreenImageViewerLollipop){
            ActivityCompat.requestPermissions(((FullScreenImageViewerLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if(context instanceof FileInfoActivityLollipop){
            ActivityCompat.requestPermissions(((FileInfoActivityLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if(context instanceof ContactFileListActivityLollipop){
            ActivityCompat.requestPermissions(((ContactFileListActivityLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if(context instanceof PdfViewerActivityLollipop){
            ActivityCompat.requestPermissions(((PdfViewerActivityLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if(context instanceof AudioVideoPlayerLollipop){
            ActivityCompat.requestPermissions(((AudioVideoPlayerLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        else if(context instanceof ContactInfoActivityLollipop){
            ActivityCompat.requestPermissions(((ContactInfoActivityLollipop) context), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
    }

    public void download(String parentPath, String url, long size, long [] hashes, boolean highPriority){
        logDebug("parentPath: " + parentPath + ", url: " + url + ", size: " + size);
        logDebug("files to download: " + hashes.length);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                askForPermissions();
            }
        }

        if (hashes == null){
            logWarning("hashes is null");
            if(url != null) {
                logDebug("url NOT null");
                Intent service = new Intent(context, DownloadService.class);
                service.putExtra(DownloadService.EXTRA_URL, url);
                service.putExtra(DownloadService.EXTRA_SIZE, size);
                service.putExtra(DownloadService.EXTRA_PATH, parentPath);
                service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
                if(highPriority){
                    service.putExtra(HIGH_PRIORITY_TRANSFER, true);
                }
                if (context instanceof AudioVideoPlayerLollipop || context instanceof PdfViewerActivityLollipop || context instanceof FullScreenImageViewerLollipop){
                    service.putExtra("fromMV", true);
                }
                context.startService(service);
            }
        }
        else{
            logDebug("hashes is NOT null");
            if(hashes.length == 1){
                logDebug("hashes.length == 1");
                MegaNode tempNode = megaApi.getNodeByHandle(hashes[0]);

                if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
                    logDebug("ISFILE");
                    String localPath = getLocalFile(context, tempNode.getName(), tempNode.getSize(), parentPath);
                    //Check if the file is already downloaded
                    MegaApplication app = ((MegaApplication) ((Activity)context).getApplication());
                    if(localPath != null){
                        logDebug("localPath != null");
                        try {
                            logDebug("Call to copyFile: localPath: " + localPath+" node name: " + tempNode.getName());
                            copyFile(new File(localPath), new File(parentPath, tempNode.getName()));

                            if(isVideoFile(parentPath+"/"+tempNode.getName())){
                                logDebug("Is video!!!");
                                if (tempNode != null){
                                    if(!tempNode.hasThumbnail()){
                                        logWarning("The video has not thumb");
                                        createThumbnailVideo(context, localPath, megaApi, tempNode.getHandle());
                                    }
                                }
                            }
                            else{
                                logDebug("NOT video!");
                            }
                        }
                        catch(Exception e) {
                            logError("Exception!!", e);
                        }
    
                        boolean autoPlayEnabled = Boolean.parseBoolean(dbH.getAutoPlayEnabled());
                        if (!autoPlayEnabled) {
                            logDebug("Auto play disabled");
                            showSnackBar(context,SNACKBAR_TYPE,context.getString(R.string.general_already_downloaded),-1);
                            return;
                        }
                        if(MimeTypeList.typeForName(tempNode.getName()).isZip()){
                            logDebug("MimeTypeList ZIP");
                            File zipFile = new File(localPath);

                            Intent intentZip = new Intent();
                            intentZip.setClass(context, ZipBrowserActivityLollipop.class);
                            intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, zipFile.getAbsolutePath());
                            intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_HANDLE_ZIP, tempNode.getHandle());

                            context.startActivity(intentZip);

                        }
                        else if (MimeTypeList.typeForName(tempNode.getName()).isPdf()){
                            logDebug("Pdf file");
                            if (context instanceof PdfViewerActivityLollipop){
                                ((PdfViewerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded), -1);
                            }
                            else {
                                File pdfFile = new File(localPath);

                                Intent pdfIntent = new Intent(context, PdfViewerActivityLollipop.class);
                                pdfIntent.putExtra("HANDLE", tempNode.getHandle());
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                    pdfIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", pdfFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                } else {
                                    pdfIntent.setDataAndType(Uri.fromFile(pdfFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                }
                                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                pdfIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                pdfIntent.putExtra("inside", true);
                                pdfIntent.putExtra("isUrl", false);
                                context.startActivity(pdfIntent);
                            }
                        }
                        else if (MimeTypeList.typeForName(tempNode.getName()).isVideoReproducible() || MimeTypeList.typeForName(tempNode.getName()).isAudio()) {
                            logDebug("Video/Audio file");
                            if (context instanceof AudioVideoPlayerLollipop){
                                ((AudioVideoPlayerLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded), -1);
                            }
                            else {
                                File mediaFile = new File(localPath);

                                Intent mediaIntent;
                                boolean internalIntent;
                                boolean opusFile = false;
                                if (MimeTypeList.typeForName(mediaFile.getName()).isVideoNotSupported() || MimeTypeList.typeForName(mediaFile.getName()).isAudioNotSupported()) {
                                    mediaIntent = new Intent(Intent.ACTION_VIEW);
                                    internalIntent = false;
                                    String[] s = mediaFile.getName().split("\\.");
                                    if (s != null && s.length > 1 && s[s.length-1].equals("opus")) {
                                        opusFile = true;
                                    }
                                } else {
                                    internalIntent = true;
                                    mediaIntent = new Intent(context, AudioVideoPlayerLollipop.class);
                                }
                                mediaIntent.putExtra(IS_PLAYLIST, false);
                                mediaIntent.putExtra("HANDLE", tempNode.getHandle());
                                mediaIntent.putExtra(AudioVideoPlayerLollipop.PLAY_WHEN_READY,app.isActivityVisible());
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                    mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());

                                } else {
                                    mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                }
                                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                mediaIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                if (opusFile){
                                    mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
                                }
                                if (internalIntent) {
                                    context.startActivity(mediaIntent);
                                }
                                else {
                                    if (isIntentAvailable(context, mediaIntent)){
                                        context.startActivity(mediaIntent);
                                    }
                                    else {
                                        showSnackbar(SNACKBAR_TYPE, context.getString(R.string.intent_not_available));
                                        Intent intentShare = new Intent(Intent.ACTION_SEND);
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                            intentShare.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                        }
                                        else {
                                            intentShare.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(tempNode.getName()).getType());
                                        }
                                        intentShare.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        if (isIntentAvailable(context, intentShare)) {
                                            logDebug("Call to startActivity(intentShare)");
                                            context.startActivity(intentShare);
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            logDebug("MimeTypeList other file");
                            if(context instanceof FullScreenImageViewerLollipop){
                                ((FullScreenImageViewerLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded), -1);
                            }
                            else {
                                try {
                                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        viewIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                    } else {
                                        viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                    }
                                    viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    if (isIntentAvailable(context, viewIntent)) {
                                        logDebug("IF isIntentAvailable");
                                        context.startActivity(viewIntent);
                                    } else {
                                        logDebug("ELSE isIntentAvailable");
                                        Intent intentShare = new Intent(Intent.ACTION_SEND);
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            intentShare.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                        } else {
                                            intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                        }
                                        intentShare.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        if (isIntentAvailable(context, intentShare)) {
                                            logDebug("Call to startActivity(intentShare)");
                                            context.startActivity(intentShare);
                                        }
                                        showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded));
                                    }
                                }
                                catch (Exception e){
                                    showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded));
                                }
                            }
                        }
                        return;
                    }
                    else{
                        logWarning("localPath is NULL");
                    }
                }
            }

            int numberOfNodesToDownload = 0;
            int numberOfNodesAlreadyDownloaded = 0;
            int numberOfNodesPending = 0;

            for (long hash : hashes) {
                logDebug("hashes.length more than 1");
                MegaNode node = megaApi.getNodeByHandle(hash);
                if(node != null){
                    logDebug("node NOT null");
                    Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
                    if (node.getType() == MegaNode.TYPE_FOLDER) {
                        logDebug("MegaNode.TYPE_FOLDER");
                        getDlList(dlFiles, node, new File(parentPath, new String(node.getName())));
                    } else {
                        logDebug("MegaNode.TYPE_FILE");
                        dlFiles.put(node, parentPath);
                    }

                    for (MegaNode document : dlFiles.keySet()) {
                        String path = dlFiles.get(document);
                        logDebug("Path of the file: " + path);
                        numberOfNodesToDownload++;

                        File destDir = new File(path);
                        File destFile;
                        destDir.mkdirs();
                        if (destDir.isDirectory()){
                            destFile = new File(destDir, megaApi.escapeFsIncompatible(document.getName()));
                            logDebug("destDir is Directory. destFile: " + destFile.getAbsolutePath());
                        }
                        else{
                            logDebug("destDir is File");
                            destFile = destDir;
                        }

                        if(destFile.exists() && (document.getSize() == destFile.length())){
                            numberOfNodesAlreadyDownloaded++;
                            logWarning(destFile.getAbsolutePath() + " already downloaded");
                        }
                        else {
                            numberOfNodesPending++;
                            logDebug("Start service");
                            Intent service = new Intent(context, DownloadService.class);
                            service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
                            service.putExtra(DownloadService.EXTRA_URL, url);
                            service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
                            service.putExtra(DownloadService.EXTRA_PATH, path);
                            service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
                            if(highPriority){
                                service.putExtra(HIGH_PRIORITY_TRANSFER, true);
                            }
                            if (context instanceof AudioVideoPlayerLollipop || context instanceof PdfViewerActivityLollipop || context instanceof FullScreenImageViewerLollipop){
                                service.putExtra("fromMV", true);
                            }
                            context.startService(service);
                        }
                    }
                }
                else if(url != null) {
                    logDebug("URL NOT null");
                    logDebug("Start service");
                    Intent service = new Intent(context, DownloadService.class);
                    service.putExtra(DownloadService.EXTRA_HASH, hash);
                    service.putExtra(DownloadService.EXTRA_URL, url);
                    service.putExtra(DownloadService.EXTRA_SIZE, size);
                    service.putExtra(DownloadService.EXTRA_PATH, parentPath);
                    service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
                    if(highPriority){
                        service.putExtra(HIGH_PRIORITY_TRANSFER, true);
                    }
                    if (context instanceof AudioVideoPlayerLollipop || context instanceof PdfViewerActivityLollipop || context instanceof FullScreenImageViewerLollipop){
                        service.putExtra("fromMV", true);
                    }
                    context.startService(service);
                }
                else {
                    logWarning("Node NOT fOUND!!!!!");
                }
            }
            logDebug("Total: " + numberOfNodesToDownload + " Already: " + numberOfNodesAlreadyDownloaded + " Pending: " + numberOfNodesPending);
            if (numberOfNodesAlreadyDownloaded > 0) {
                String msg;
                msg = context.getResources().getQuantityString(R.plurals.file_already_downloaded,numberOfNodesAlreadyDownloaded,numberOfNodesAlreadyDownloaded);
                if (numberOfNodesPending > 0) {
                    msg = msg + context.getResources().getQuantityString(R.plurals.file_pending_download,numberOfNodesPending,numberOfNodesPending);
                }
                showSnackbar(SNACKBAR_TYPE,msg);
            }
        }
    }

    /*
	 * Get list of all child files
	 */
    private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {
        logDebug("getDlList");
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

    public void renameNode(MegaNode document, String newName){
        logDebug("renameNode");
        if (newName.compareTo(document.getName()) == 0) {
            return;
        }

        if(!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        logDebug("Renaming " + document.getName() + " to " + newName);

        megaApi.renameNode(document, newName, ((ManagerActivityLollipop) context));
    }

    public int importLink(String url) {
        try {
            url = URLDecoder.decode(url, "UTF-8");
        }
        catch (Exception e) {
            logError("Error decoding URL: " + url, e);
        }

        url.replace(' ', '+');
        if(url.startsWith("mega://")){
            url = url.replace("mega://", "https://mega.co.nz/");
        }

        logDebug("url " + url);

        // Download link
        if (AndroidMegaRichLinkMessage.isFileLink(url)) {
            Intent openFileIntent = new Intent(context, FileLinkActivityLollipop.class);
            openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openFileIntent.setAction(ACTION_OPEN_MEGA_LINK);
            openFileIntent.setData(Uri.parse(url));
            ((ManagerActivityLollipop) context).startActivity(openFileIntent);
            return FILE_LINK;
        }
        else if (AndroidMegaRichLinkMessage.isFolderLink(url)) {
            Intent openFolderIntent = new Intent(context, FolderLinkActivityLollipop.class);
            openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openFolderIntent.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
            openFolderIntent.setData(Uri.parse(url));
            ((ManagerActivityLollipop) context).startActivity(openFolderIntent);
            return FOLDER_LINK;
        }
        else if (AndroidMegaRichLinkMessage.isChatLink(url)) {
            return CHAT_LINK;
        }
        else if (AndroidMegaRichLinkMessage.isContactLink(url)) {
            return CONTACT_LINK;
        }

        logWarning("wrong url");
        return ERROR_LINK;
    }

    //old getPublicLinkAndShareIt
    public void exportLink(MegaNode document){
        logDebug("exportLink");
        if (!isOnline(context)) {
            showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem));
            return;
        }
        else if(context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, ((ManagerActivityLollipop) context));
        }
        else if(context instanceof GetLinkActivityLollipop){
            megaApi.exportNode(document, ((GetLinkActivityLollipop) context));
        }
        else  if(context instanceof FullScreenImageViewerLollipop){
            ((FullScreenImageViewerLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, ((FullScreenImageViewerLollipop) context));
        }
        else  if(context instanceof FileInfoActivityLollipop){
            ((FileInfoActivityLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, ((FileInfoActivityLollipop) context));
        }
    }

    public void exportLinkTimestamp(MegaNode document, int timestamp){
        logDebug("exportLinkTimestamp: " + timestamp);
        if (!isOnline(context)) {
            showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem));
        }
        else if (context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, timestamp, ((ManagerActivityLollipop) context));
        }
        else if (context instanceof GetLinkActivityLollipop){
            megaApi.exportNode(document, timestamp, ((GetLinkActivityLollipop) context));
        }
        else if (context instanceof FullScreenImageViewerLollipop){
            ((FullScreenImageViewerLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, timestamp, ((FullScreenImageViewerLollipop) context));
        }
        else if (context instanceof FileInfoActivityLollipop){
            megaApi.exportNode(document, timestamp, ((FileInfoActivityLollipop) context));
        }
    }

    public void removeLink(MegaNode document){
        logDebug("removeLink");
        if (!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }
        megaApi.disableExport(document, ((ManagerActivityLollipop) context));
    }


    public void selectContactToShareFolders(ArrayList<Long> handleList){
        logDebug("shareFolders ArrayListLong");
        //TODO shareMultipleFolders

        if (!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        Intent intent = new Intent();
        intent.setClass(context, AddContactActivityLollipop.class);
        intent.putExtra("contactType", CONTACT_TYPE_BOTH);

        long[] handles=new long[handleList.size()];
        int j=0;
        for(int i=0; i<handleList.size();i++){
            handles[j]=handleList.get(i);
            j++;
        }
        intent.putExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, handles);
        //Multiselect=1 (multiple folders)
        intent.putExtra("MULTISELECT", 1);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
    }

    public void selectContactToShareFolder(MegaNode node){
        logDebug("shareFolder");

        Intent intent = new Intent();
        intent.setClass(context, AddContactActivityLollipop.class);
        intent.putExtra("contactType", CONTACT_TYPE_BOTH);
        //Multiselect=0
        intent.putExtra("MULTISELECT", 0);
        intent.putExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, node.getHandle());
        ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_CONTACT);
    }

    public void shareFolder(long folderHandle, ArrayList<String> selectedContacts, int level){

        if(!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        MegaNode parent = megaApi.getNodeByHandle(folderHandle);
        MultipleRequestListener shareMultipleListener = new MultipleRequestListener(MULTIPLE_CONTACTS_SHARE, (ManagerActivityLollipop) context);
        if(parent!=null&parent.isFolder()){
            if(selectedContacts.size()>1){
                logDebug("Share READ one file multiple contacts");
                for (int i=0;i<selectedContacts.size();i++){
                    MegaUser user= megaApi.getContact(selectedContacts.get(i));
                    if(user!=null){
                        megaApi.share(parent, user, level,shareMultipleListener);
                    }
                    else {
                        logDebug("USER is NULL when sharing!->SHARE WITH NON CONTACT");
                        megaApi.share(parent, selectedContacts.get(i), level, shareMultipleListener);
                    }
                }
            }
            else{
                logDebug("Share READ one file one contact");
                MegaUser user= megaApi.getContact(selectedContacts.get(0));
                if(user!=null){
                    megaApi.share(parent, user, level, (ManagerActivityLollipop) context);
                }
                else {
                    logDebug("USER is NULL when sharing!->SHARE WITH NON CONTACT");
                    megaApi.share(parent, selectedContacts.get(0), level, (ManagerActivityLollipop) context);
                }
            }
        }
    }

    public void shareFolders(long[] nodeHandles, ArrayList<String> contactsData, int level){

        if(!isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            return;
        }

        MultipleRequestListener shareMultipleListener = null;

        if(nodeHandles.length>1){
            shareMultipleListener = new MultipleRequestListener(MULTIPLE_FILE_SHARE, context);
        }
        else{
            shareMultipleListener = new MultipleRequestListener(MULTIPLE_CONTACTS_SHARE, context);
        }

        for (int i=0;i<contactsData.size();i++){
            MegaUser u = megaApi.getContact(contactsData.get(i));
            if(nodeHandles.length>1){
                logDebug("Many folder to many contacts");
                for(int j=0; j<nodeHandles.length;j++){

                    final MegaNode node = megaApi.getNodeByHandle(nodeHandles[j]);
                    if(node!=null){
                        if(u!=null){
                            logDebug("Share: "+ node.getName() + " to "+ u.getEmail());
                            megaApi.share(node, u, level, shareMultipleListener);
                        }
                        else{
                            logDebug("USER is NULL when sharing!->SHARE WITH NON CONTACT");
                            megaApi.share(node, contactsData.get(i), level, shareMultipleListener);
                        }
                    }
                    else{
                        logWarning("NODE NULL!!!");
                    }

                }
            }
            else{
                logDebug("One folder to many contacts");

                for(int j=0; j<nodeHandles.length;j++){

                    final MegaNode node = megaApi.getNodeByHandle(nodeHandles[j]);
                    if(u!=null){
                        logDebug("Share: "+ node.getName() + " to "+ u.getEmail());
                        megaApi.share(node, u, level, shareMultipleListener);
                    }
                    else{
                        logDebug("USER is NULL when sharing!->SHARE WITH NON CONTACT");
                        megaApi.share(node, contactsData.get(i), level, shareMultipleListener);
                    }
                }
            }
        }
    }

    public void moveToTrash(final ArrayList<Long> handleList, boolean moveToRubbish){
        logDebug("moveToTrash: " + moveToRubbish);

        MultipleRequestListener moveMultipleListener = null;
        MegaNode parent;
        //Check if the node is not yet in the rubbish bin (if so, remove it)
        if(handleList!=null){
            if(handleList.size()>1){
                logDebug("MOVE multiple: " + handleList.size());
                if (moveToRubbish){
                    moveMultipleListener = new MultipleRequestListener(MULTIPLE_SEND_RUBBISH, context);
                }
                else{
                    moveMultipleListener = new MultipleRequestListener(MULTIPLE_MOVE, context);
                }
                for (int i=0;i<handleList.size();i++){
                    if (moveToRubbish){
                        megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(i)), megaApi.getRubbishNode(), moveMultipleListener);

                    }
                    else{
                        megaApi.remove(megaApi.getNodeByHandle(handleList.get(i)), moveMultipleListener);
                    }
                }
            }
            else{
                logDebug("MOVE single");
                if (moveToRubbish){
                    megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(0)), megaApi.getRubbishNode(), ((ManagerActivityLollipop) context));
                }
                else{
                    megaApi.remove(megaApi.getNodeByHandle(handleList.get(0)), ((ManagerActivityLollipop) context));
                }
            }
        }
        else{
            logWarning("handleList NULL");
            return;
        }
    }

    public void openFolderFromSearch(long folderHandle){
        logDebug("openFolderFromSearch: " + folderHandle);
        ((ManagerActivityLollipop)context).textSubmitted = true;
        ((ManagerActivityLollipop)context).openFolderRefresh = true;
        boolean firstNavigationLevel=true;
        int access = -1;
        ManagerActivityLollipop.DrawerItem drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
        if (folderHandle != -1) {
            MegaNode parentIntentN = megaApi.getParentNode(megaApi.getNodeByHandle(folderHandle));
            if (parentIntentN != null) {
                logDebug("Check the parent node: " + parentIntentN.getName() + " handle: " + parentIntentN.getHandle());
                access = megaApi.getAccess(parentIntentN);
                switch (access) {
                    case MegaShare.ACCESS_OWNER:
                    case MegaShare.ACCESS_UNKNOWN: {
                        //Not incoming folder, check if Cloud or Rubbish tab
                        if(parentIntentN.getHandle()==megaApi.getRootNode().getHandle()){
                            drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                            logDebug("Navigate to TAB CLOUD first level" + parentIntentN.getName());
                            firstNavigationLevel=true;
                            ((ManagerActivityLollipop) context).setParentHandleBrowser(parentIntentN.getHandle());
                        }
                        else if(parentIntentN.getHandle()==megaApi.getRubbishNode().getHandle()){
                            drawerItem = ManagerActivityLollipop.DrawerItem.RUBBISH_BIN;
                            logDebug("Navigate to TAB RUBBISH first level" + parentIntentN.getName());
                            firstNavigationLevel=true;
                            ((ManagerActivityLollipop) context).setParentHandleRubbish(parentIntentN.getHandle());
                        }
                        else if(parentIntentN.getHandle()==megaApi.getInboxNode().getHandle()){
                            logDebug("Navigate to INBOX first level" + parentIntentN.getName());
                            firstNavigationLevel=true;
                            ((ManagerActivityLollipop) context).setParentHandleInbox(parentIntentN.getHandle());
                            drawerItem = ManagerActivityLollipop.DrawerItem.INBOX;
                        }
                        else{
                            int parent = checkParentNodeToOpenFolder(parentIntentN.getHandle());
                            logDebug("The parent result is: " + parent);

                            switch (parent){
                                case 0:{
                                    //ROOT NODE
                                    drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                                    logDebug("Navigate to TAB CLOUD with parentHandle");
                                    ((ManagerActivityLollipop) context).setParentHandleBrowser(parentIntentN.getHandle());
                                    firstNavigationLevel=false;
                                    break;
                                }
                                case 1:{
                                    logDebug("Navigate to TAB RUBBISH");
                                    drawerItem = ManagerActivityLollipop.DrawerItem.RUBBISH_BIN;
                                    ((ManagerActivityLollipop) context).setParentHandleRubbish(parentIntentN.getHandle());
                                    firstNavigationLevel=false;
                                    break;
                                }
                                case 2:{
                                    logDebug("Navigate to INBOX WITH parentHandle");
                                    drawerItem = ManagerActivityLollipop.DrawerItem.INBOX;
                                    ((ManagerActivityLollipop) context).setParentHandleInbox(parentIntentN.getHandle());
                                    firstNavigationLevel=false;
                                    break;
                                }
                                case -1:{
                                    drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                                    logDebug("Navigate to TAB CLOUD general");
                                    ((ManagerActivityLollipop) context).setParentHandleBrowser(-1);
                                    firstNavigationLevel=true;
                                    break;
                                }
                            }
                        }
                        break;
                    }

                    case MegaShare.ACCESS_READ:
                    case MegaShare.ACCESS_READWRITE:
                    case MegaShare.ACCESS_FULL: {
                        logDebug("GO to INCOMING TAB: " + parentIntentN.getName());
                        drawerItem = ManagerActivityLollipop.DrawerItem.SHARED_ITEMS;
                        if(parentIntentN.getHandle()==-1){
                            logDebug("Level 0 of Incoming");
                            ((ManagerActivityLollipop) context).setParentHandleIncoming(-1);
                            ((ManagerActivityLollipop) context).setDeepBrowserTreeIncoming(0);
                            firstNavigationLevel=true;
                        }
                        else{
                            firstNavigationLevel=false;
                            ((ManagerActivityLollipop) context).setParentHandleIncoming(parentIntentN.getHandle());
                            int deepBrowserTreeIncoming = calculateDeepBrowserTreeIncoming(parentIntentN, context);
                            ((ManagerActivityLollipop) context).setDeepBrowserTreeIncoming(deepBrowserTreeIncoming);
                            logDebug("After calculating deepBrowserTreeIncoming: " + deepBrowserTreeIncoming);
                        }
                        ((ManagerActivityLollipop) context).setTabItemShares(0);
                        break;
                    }
                    default: {
                        logDebug("DEFAULT: The intent set the parentHandleBrowser to " + parentIntentN.getHandle());
                        ((ManagerActivityLollipop) context).setParentHandleBrowser(parentIntentN.getHandle());
                        drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                        firstNavigationLevel=true;
                        break;
                    }
                }
            }
            else{
                logWarning("Parent is already NULL");

                drawerItem = ManagerActivityLollipop.DrawerItem.SHARED_ITEMS;
                ((ManagerActivityLollipop) context).setParentHandleIncoming(-1);
                ((ManagerActivityLollipop) context).setDeepBrowserTreeIncoming(0);
                firstNavigationLevel=true;
                ((ManagerActivityLollipop) context).setTabItemShares(0);
            }
            ((ManagerActivityLollipop) context).setFirstNavigationLevel(firstNavigationLevel);
            ((ManagerActivityLollipop) context).setDrawerItem(drawerItem);
            ((ManagerActivityLollipop) context).selectDrawerItemLollipop(drawerItem);
        }
    }

    public int checkParentNodeToOpenFolder(long folderHandle){
        logDebug("Folder handle: " + folderHandle);
        MegaNode folderNode = megaApi.getNodeByHandle(folderHandle);
        MegaNode parentNode = megaApi.getParentNode(folderNode);
        if(parentNode!=null){
            logDebug("Parent handle: "+parentNode.getHandle());
            if(parentNode.getHandle()==megaApi.getRootNode().getHandle()){
                logDebug("The parent is the ROOT");
                return 0;
            }
            else if(parentNode.getHandle()==megaApi.getRubbishNode().getHandle()){
                logDebug("The parent is the RUBBISH");
                return 1;
            }
            else if(parentNode.getHandle()==megaApi.getInboxNode().getHandle()){
                logDebug("The parent is the INBOX");
                return 2;
            }
            else if(parentNode.getHandle()==-1){
                logWarning("The parent is -1");
                return -1;
            }
            else{
                int result = checkParentNodeToOpenFolder(parentNode.getHandle());
                logDebug("Call returns " + result);
                switch(result){
                    case -1:
                        return -1;
                    case 0:
                        return 0;
                    case 1:
                        return 1;
                    case 2:
                        return 2;
                }
            }
        }
        return -1;
    }

    public void leaveIncomingShare (Context context, final MegaNode n){
        logDebug("Node handle: " + n.getHandle());

        if (context instanceof ManagerActivityLollipop) {
            megaApi.remove(n, (ManagerActivityLollipop) context);
            return;
        }

        megaApi.remove(n);
    }

    public void leaveMultipleIncomingShares (final ArrayList<Long> handleList){
        logDebug("Leaving " + handleList.size() + " incoming shares");

        MultipleRequestListener moveMultipleListener = new MultipleRequestListener(MULTIPLE_LEAVE_SHARE, context);
        if(handleList.size()>1){
            logDebug("handleList.size()>1");
            for (int i=0; i<handleList.size(); i++){
                MegaNode node = megaApi.getNodeByHandle(handleList.get(i));
                megaApi.remove(node, moveMultipleListener);
            }
        }
        else{
            logDebug("handleList.size()<=1");
            MegaNode node = megaApi.getNodeByHandle(handleList.get(0));
            megaApi.remove(node, (ManagerActivityLollipop)context);
        }
    }

    public void removeAllSharingContacts (ArrayList<MegaShare> listContacts, MegaNode node){
        logDebug("removeAllSharingContacts");

        MultipleRequestListener shareMultipleListener = new MultipleRequestListener(MULTIPLE_REMOVE_SHARING_CONTACTS, context);
        if(listContacts.size()>1){
            logDebug("listContacts.size()>1");
            for(int j=0; j<listContacts.size();j++){
                String cMail = listContacts.get(j).getUser();
                if(cMail!=null){
                    MegaUser c = megaApi.getContact(cMail);
                    if (c != null){
                        megaApi.share(node, c, MegaShare.ACCESS_UNKNOWN, shareMultipleListener);
                    }
                    else{
                        ((ManagerActivityLollipop)context).setIsGetLink(false);
                        megaApi.disableExport(node);
                    }
                }
                else{
                    ((ManagerActivityLollipop)context).setIsGetLink(false);
                    megaApi.disableExport(node);
                }
            }
        }
        else{
            logDebug("listContacts.size()<=1");
            for(int j=0; j<listContacts.size();j++){
                String cMail = listContacts.get(j).getUser();
                if(cMail!=null){
                    MegaUser c = megaApi.getContact(cMail);
                    if (c != null){
                        megaApi.share(node, c, MegaShare.ACCESS_UNKNOWN, ((ManagerActivityLollipop)context));
                    }
                    else{
                        ((ManagerActivityLollipop)context).setIsGetLink(false);
                        megaApi.disableExport(node);
                    }
                }
                else{
                    ((ManagerActivityLollipop)context).setIsGetLink(false);
                    megaApi.disableExport(node);
                }
            }
        }
    }

    public void cleanRubbishBin(){
        logDebug("cleanRubbishBin");
        megaApi.cleanRubbishBin((ManagerActivityLollipop) context);
    }

    public void clearAllVersions(){
        logDebug("clearAllVersions");
        megaApi.removeVersions((ManagerActivityLollipop) context);
    }

    public void deleteOffline(MegaOffline selectedNode){
        logDebug("deleteOffline");
        dbH = DatabaseHandler.getDbHandler(context);

        //Delete children
        ArrayList<MegaOffline> mOffListChildren = dbH.findByParentId(selectedNode.getId());
        if (mOffListChildren.size() > 0) {
            //The node have childrens, delete
            deleteChildrenDB(mOffListChildren);
        }

        removeNodePhysically(selectedNode);

        dbH.removeById(selectedNode.getId());

        //Check if the parent has to be deleted

        int parentId = selectedNode.getParentId();
        MegaOffline parentNode = dbH.findById(parentId);

        if (parentNode != null) {
            logDebug("Parent to check: " + parentNode.getName());
            checkParentDeletion(parentNode);
        }

        if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop) context).updateOfflineView(null);
        }
    }

    private void removeNodePhysically(MegaOffline megaOffline) {
        logDebug("Remove the node physically");
        try {
            File offlineFile = getOfflineFile(context, megaOffline);
            deleteFolderAndSubfolders(context, offlineFile);
        } catch (Exception e) {
            logError("EXCEPTION: deleteOffline - adapter", e);
        }
    }

    public void deleteChildrenDB(ArrayList<MegaOffline> mOffListChildren){

        logDebug("Size: " + mOffListChildren.size());
        MegaOffline mOffDelete=null;

        for(int i=0; i<mOffListChildren.size(); i++){

            mOffDelete=mOffListChildren.get(i);

            logDebug("Children " + i + ": "+ mOffDelete.getHandle());
            ArrayList<MegaOffline> mOffListChildren2=dbH.findByParentId(mOffDelete.getId());
            if(mOffListChildren2.size()>0){
                //The node have children, delete
                deleteChildrenDB(mOffListChildren2);
            }

            int lines = dbH.removeById(mOffDelete.getId());
            logDebug("Deleted: " + lines);
        }
    }

    public void checkParentDeletion (MegaOffline parentToDelete){
        logDebug("parentToDelete: " + parentToDelete.getHandle());

        ArrayList<MegaOffline> mOffListChildren=dbH.findByParentId(parentToDelete.getId());
        File destination = null;
        if(mOffListChildren.size()<=0){
            logDebug("The parent has NO children");
            //The node have NO childrens, delete it

            dbH.removeById(parentToDelete.getId());

            removeNodePhysically(parentToDelete);

            int parentId = parentToDelete.getParentId();
            if(parentId==-1){
                File rootIncomingFile = getOfflineFile(context, parentToDelete);

                if(isFileAvailable(rootIncomingFile)){
                    String[] fileList = rootIncomingFile.list();
                    if(fileList!=null){
                        if(rootIncomingFile.list().length==0){
                            try{
                                rootIncomingFile.delete();
                            }
                            catch(Exception e){
                                logError("EXCEPTION: deleteParentIncoming: " + destination, e);
                            };
                        }
                    }
                }
                else{
                    logWarning("rootIncomingFile is NULL");
                }
            }
            else{
                //Check if the parent has to be deleted

                parentToDelete = dbH.findById(parentId);
                if(parentToDelete != null){
                    logDebug("Parent to check: " + parentToDelete.getHandle());
                    checkParentDeletion(parentToDelete);

                }
            }

        }
        else{
            logDebug("The parent has children!!! RETURN!!");
            return;
        }

    }

    public void downloadFileLink (final MegaNode document, final String url) {
        logDebug("downloadFileLink");

        if (document == null){
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                askForPermissions();
                return;
            }
        }


        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }

        if (dbH.getCredentials() == null || dbH.getPreferences() == null){

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                File[] fs = context.getExternalFilesDirs(null);
                if (fs.length > 1){
                    if (fs[1] == null){
                        intentPickFolder(document, url);
                    }else{
                        Dialog downloadLocationDialog;
                        String[] sdCardOptions = context.getResources().getStringArray(R.array.settings_storage_download_location_array);
                        android.app.AlertDialog.Builder b=new android.app.AlertDialog.Builder(context);

                        b.setTitle(context.getString(R.string.settings_storage_download_location));
                        b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch(which){
                                    case 0:{
                                        intentPickFolder(document, url);
                                        break;
                                    }
                                    case 1:{
                                        File[] fs = context.getExternalFilesDirs(null);
                                        if (fs.length > 1){
                                            String path = fs[1].getAbsolutePath();
                                            File defaultPathF = new File(path);
                                            defaultPathF.mkdirs();
                                            Toast.makeText(context, context.getString(R.string.general_save_to_device) + ": "  + defaultPathF.getAbsolutePath() , Toast.LENGTH_LONG).show();
                                            downloadTo(document, path, url);
                                        }
                                        break;
                                    }
                                }
                            }
                        });
                        b.setNegativeButton(context.getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        downloadLocationDialog = b.create();
                        downloadLocationDialog.show();
                    }
                }
                else{
                    intentPickFolder(document, url);
                }
            }
            else{
                intentPickFolder(document, url);
            }
            return;
        }

        boolean askMe = askMe(context);
        String downloadLocationDefaultPath = getDownloadLocation(context);

        if (askMe){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                File[] fs = context.getExternalFilesDirs(null);
                if (fs.length > 1){
                    if (fs[1] == null){
                        intentPickFolder(document, url);
                    }
                    else{
                        Dialog downloadLocationDialog;
                        String[] sdCardOptions = context.getResources().getStringArray(R.array.settings_storage_download_location_array);
                        android.app.AlertDialog.Builder b=new android.app.AlertDialog.Builder(context);

                        b.setTitle(context.getString(R.string.settings_storage_download_location));
                        b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch(which){
                                    case 0:{
                                        intentPickFolder(document, url);
                                        break;
                                    }
                                    case 1:{
                                        File[] fs = context.getExternalFilesDirs(null);
                                        if (fs.length > 1){
                                            String path = fs[1].getAbsolutePath();
                                            File defaultPathF = new File(path);
                                            defaultPathF.mkdirs();
                                            Toast.makeText(context, context.getString(R.string.general_save_to_device) + ": "  + defaultPathF.getAbsolutePath() , Toast.LENGTH_LONG).show();
                                            downloadTo(document, path, url);
                                        }
                                        break;
                                    }
                                }
                            }
                        });
                        b.setNegativeButton(context.getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        downloadLocationDialog = b.create();
                        downloadLocationDialog.show();
                    }
                }
                else{
                    intentPickFolder(document, url);
                }
            }
            else{
                intentPickFolder(document, url);
            }
        }
        else{
            downloadTo(document, downloadLocationDefaultPath, url);
        }
    }

    void intentPickFolder(MegaNode document, String url) {
        Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, context.getString(R.string.context_download_to));
        intent.setClass(context, FileStorageActivityLollipop.class);
        intent.putExtra(FileStorageActivityLollipop.EXTRA_URL, url);
        intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, document.getSize());
        if (context instanceof FileLinkActivityLollipop) {
            ((FileLinkActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if (context instanceof AudioVideoPlayerLollipop) {
            ((AudioVideoPlayerLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if (context instanceof FullScreenImageViewerLollipop) {
            ((FullScreenImageViewerLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
        else if (context instanceof PdfViewerActivityLollipop) {
            ((PdfViewerActivityLollipop) context).startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
        }
    }

    public void downloadTo(MegaNode currentDocument, String parentPath, String url){
        logDebug("downloadTo");
        double availableFreeSpace = Double.MAX_VALUE;
        try{
            StatFs stat = new StatFs(parentPath);
            availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
        }catch(Exception ex){}

        MegaNode tempNode = currentDocument;
        if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
            logDebug("is file");
            String localPath = getLocalFile(context, tempNode.getName(), tempNode.getSize(), parentPath);
            if(localPath != null){
                File file = new File(localPath);
                try {
                    copyFile(file, new File(parentPath, tempNode.getName()));
                }catch(Exception e) {}

                if (file != null && file.getParent().equals(parentPath)) {
                    showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_already_downloaded));
                }
                else {
                    showSnackbar(SNACKBAR_TYPE, context.getString(R.string.copy_already_downloaded));
                }

            }
            else{
                logDebug("LocalPath is NULL");
                showSnackbar(SNACKBAR_TYPE, context.getString(R.string.download_began));

                if(tempNode != null){
                    logDebug("Node!=null: "+tempNode.getName());
                    Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
                    dlFiles.put(tempNode, parentPath);

                    for (MegaNode document : dlFiles.keySet()) {
                        String path = dlFiles.get(document);

                        if(availableFreeSpace < document.getSize()){
                            showSnackbar(NOT_SPACE_SNACKBAR_TYPE, null);
                            continue;
                        }

                        Intent service = new Intent(context, DownloadService.class);
                        service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
                        service.putExtra(EXTRA_SERIALIZE_STRING, currentDocument.serialize());
                        service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
                        service.putExtra(DownloadService.EXTRA_PATH, path);
                        logDebug("intent to DownloadService");
                        if (context instanceof AudioVideoPlayerLollipop || context instanceof FullScreenImageViewerLollipop || context instanceof PdfViewerActivityLollipop) {
                            service.putExtra("fromMV", true);
                        }
                        context.startService(service);
                    }
                }
                else if(url != null) {
                    if(availableFreeSpace < currentDocument.getSize()) {
                        showSnackbar(NOT_SPACE_SNACKBAR_TYPE, null);
                    }

                    Intent service = new Intent(context, DownloadService.class);
                    service.putExtra(DownloadService.EXTRA_HASH, currentDocument.getHandle());
                    service.putExtra(EXTRA_SERIALIZE_STRING, currentDocument.serialize());
                    service.putExtra(DownloadService.EXTRA_SIZE, currentDocument.getSize());
                    service.putExtra(DownloadService.EXTRA_PATH, parentPath);
                    if (context instanceof AudioVideoPlayerLollipop || context instanceof FullScreenImageViewerLollipop || context instanceof PdfViewerActivityLollipop) {
                        service.putExtra("fromMV", true);
                    }
                    context.startService(service);
                }
                else {
                    logWarning("Node not found. Let's try the document");
                }
            }
        }
    }
}
