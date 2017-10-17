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
import java.io.UnsupportedEncodingException;
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
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.GetLinkActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListener;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

public class NodeController {

    Context context;
    MegaApiAndroid megaApi;
    DatabaseHandler dbH;
    MegaPreferences prefs = null;

    public NodeController(Context context){
        log("NodeController created");
        this.context = context;
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public void chooseLocationToCopyNodes(ArrayList<Long> handleList){
        log("chooseLocationToCopyNodes");
        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("COPY_FROM", longArray);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_COPY_FOLDER);
    }

    public void copyNodes(long[] copyHandles, long toHandle) {
        log("copyNodes");

        if(!Util.isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            return;
        }

        MegaNode parent = megaApi.getNodeByHandle(toHandle);
        if(parent!=null) {
            MultipleRequestListener copyMultipleListener = null;
            if (copyHandles.length > 1) {
                log("Copy multiple files");
                copyMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_COPY, context);
                for (int i = 0; i < copyHandles.length; i++) {
                    MegaNode cN = megaApi.getNodeByHandle(copyHandles[i]);
                    if (cN != null){
                        log("cN != null, i = " + i + " of " + copyHandles.length);
                        megaApi.copyNode(cN, parent, copyMultipleListener);
                    }
                    else{
                        log("cN == null, i = " + i + " of " + copyHandles.length);
                    }
                }
            } else {
                log("Copy one file");
                MegaNode cN = megaApi.getNodeByHandle(copyHandles[0]);
                if (cN != null){
                    log("cN != null");
                    megaApi.copyNode(cN, parent, (ManagerActivityLollipop) context);
                }
                else{
                    log("cN == null");
                    if(context instanceof ManagerActivityLollipop){
                        ((ManagerActivityLollipop)context).copyError();
                    }
                }
            }
        }

    }

    public void chooseLocationToMoveNodes(ArrayList<Long> handleList){
        log("chooseLocationToMoveNodes");
        Intent intent = new Intent(context, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("MOVE_FROM", longArray);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_MOVE_FOLDER);
    }

    public void moveNodes(long[] moveHandles, long toHandle){
        log("moveNodes");

        if(!Util.isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            return;
        }

        MegaNode parent = megaApi.getNodeByHandle(toHandle);
        if(parent!=null){
            MultipleRequestListener moveMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_MOVE, context);

            if(moveHandles.length>1){
                log("MOVE multiple: "+moveHandles.length);

                for(int i=0; i<moveHandles.length;i++){
                    megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[i]), parent, moveMultipleListener);
                }
            }
            else{
                log("MOVE single");

                megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[0]), parent, (ManagerActivityLollipop) context);
            }
        }
    }

    public void selectContactToSendNode(MegaNode node){
        log("sentToInbox MegaNode");

        ((ManagerActivityLollipop) context).setSendToInbox(true);

        Intent intent = new Intent(AddContactActivityLollipop.ACTION_PICK_CONTACT_SEND_FILE);
        intent.setClass(context, AddContactActivityLollipop.class);
        //Multiselect=0
        intent.putExtra("MULTISELECT", 0);
        intent.putExtra("SEND_FILE",1);
        intent.putExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, node.getHandle());
        ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_CONTACT);
    }

    public void sendToInbox(long fileHandle, ArrayList<String> selectedContacts){
        log("sendToInbox");

        if(!Util.isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            return;
        }

        MultipleRequestListener sendMultipleListener = null;
        MegaNode node = megaApi.getNodeByHandle(fileHandle);
        if(node!=null)
        {
            ((ManagerActivityLollipop) context).setSendToInbox(true);
            log("File to send: "+node.getName());
            if(selectedContacts.size()>1){
                log("File to multiple contacts");
                sendMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_CONTACTS_SEND_INBOX, context);
                for (int i=0;i<selectedContacts.size();i++){
                    MegaUser user= megaApi.getContact(selectedContacts.get(i));

                    if(user!=null){
                        log("Send File to contact: "+user.getEmail());
                        megaApi.sendFileToUser(node, user, sendMultipleListener);
                    }
                    else{
                        log("Send File to a NON contact! ");
                        megaApi.sendFileToUser(node, selectedContacts.get(i), sendMultipleListener);
                    }
                }
            }
            else{
                log("File to a single contact");
                MegaUser user= megaApi.getContact(selectedContacts.get(0));
                if(user!=null){
                    log("Send File to contact: "+user.getEmail());
                    megaApi.sendFileToUser(node, user, (ManagerActivityLollipop) context);
                }
                else{
                    log("Send File to a NON contact! ");
                    megaApi.sendFileToUser(node, selectedContacts.get(0), (ManagerActivityLollipop) context);
                }
            }
        }

    }

    public void sendToInbox(long[] nodeHandles, ArrayList<String> selectedContacts) {

        if (!Util.isOnline(context)) {
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            return;
        }

        if(nodeHandles!=null){
            MultipleRequestListener sendMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_FILES_SEND_INBOX, context);
            MegaUser u = megaApi.getContact(selectedContacts.get(0));
            if(nodeHandles.length>1){
                log("many files to one contact");
                for(int j=0; j<nodeHandles.length;j++){

                    final MegaNode node = megaApi.getNodeByHandle(nodeHandles[j]);

                    if(u!=null){
                        log("Send: "+ node.getName() + " to "+ u.getEmail());
                        megaApi.sendFileToUser(node, u, sendMultipleListener);
                    }
                    else{
                        log("Send File to a NON contact! ");
                        megaApi.sendFileToUser(node, selectedContacts.get(0), sendMultipleListener);
                    }
                }
            }
            else{
                log("one file to many contacts");

                final MegaNode node = megaApi.getNodeByHandle(nodeHandles[0]);
                if(u!=null){
                    log("Send: "+ node.getName() + " to "+ u.getEmail());
                    megaApi.sendFileToUser(node, u, (ManagerActivityLollipop)context);
                }
                else{
                    log("Send File to a NON contact! ");
                    megaApi.sendFileToUser(node, selectedContacts.get(0), (ManagerActivityLollipop)context);
                }
            }
        }
    }

    public void selectContactToSendNodes(ArrayList<Long> handleList){
        log("sendToInboxNodes handleList");

        ((ManagerActivityLollipop) context).setSendToInbox(true);

        Intent intent = new Intent(AddContactActivityLollipop.ACTION_PICK_CONTACT_SEND_FILE);
        intent.setClass(context, AddContactActivityLollipop.class);
        //Multiselect=1
        intent.putExtra("MULTISELECT", 1);
        intent.putExtra("SEND_FILE",1);
        long[] handles=new long[handleList.size()];
        int j=0;
        for(int i=0; i<handleList.size();i++){
            handles[j] = handleList.get(i);
            j++;
        }
        intent.putExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, handles);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_CONTACT);
    }

    public void prepareForDownload(ArrayList<Long> handleList){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            prepareForDownloadLollipop(handleList);
        }
        else{
            prepareForDownloadPreLollipop(handleList);
        }
    }

    //Old onFileClick
    public void prepareForDownloadLollipop(ArrayList<Long> handleList){
        log("prepareForDownload: "+handleList.size()+" files to download");
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
                log("Error - nodeTemp is NULL");
            }

        }
        log("Number of files: "+hashes.length);

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        boolean askMe = true;
        String downloadLocationDefaultPath = Util.downloadDIR;
        prefs = dbH.getPreferences();
        if (prefs != null){
            log("prefs != null");
            if (prefs.getStorageAskAlways() != null){
                if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
                    log("askMe==false");
                    if (prefs.getStorageDownloadLocation() != null){
                        if (prefs.getStorageDownloadLocation().compareTo("") != 0){
                            askMe = false;
                            downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
                        }
                    }
                }
            }
        }

        if (askMe){
            log("askMe");
            File[] fs = context.getExternalFilesDirs(null);
            if (fs.length > 1){
                if (fs[1] == null){
                    Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
                    intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
                    intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
                    intent.setClass(context, FileStorageActivityLollipop.class);
                    intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
                    ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
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
                                    Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
                                    intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
                                    intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, sizeFinal);
                                    intent.setClass(context, FileStorageActivityLollipop.class);
                                    intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashesFinal);
                                    ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
                                    break;
                                }
                                case 1:{
                                    File[] fs = context.getExternalFilesDirs(null);
                                    if (fs.length > 1){
                                        String path = fs[1].getAbsolutePath();
                                        File defaultPathF = new File(path);
                                        defaultPathF.mkdirs();
                                        ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.general_download) + ": "  + defaultPathF.getAbsolutePath());
                                        checkSizeBeforeDownload(path, null, sizeFinal, hashesFinal);
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
                Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
                intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
                intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
                intent.setClass(context, FileStorageActivityLollipop.class);
                intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
                ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
            }
        }
        else{
            log("NOT askMe");
            File defaultPathF = new File(downloadLocationDefaultPath);
            defaultPathF.mkdirs();
            checkSizeBeforeDownload(downloadLocationDefaultPath, null, size, hashes);
        }
    }

    //Old onFileClick
    public void prepareForDownloadPreLollipop(ArrayList<Long> handleList){
        log("prepareForDownloadPreLollipop: "+handleList.size()+" files to download");
        long size = 0;
        long[] hashes = new long[handleList.size()];
        for (int i=0;i<handleList.size();i++){
            hashes[i] = handleList.get(i);
            MegaNode nodeTemp = megaApi.getNodeByHandle(hashes[i]);
            if (nodeTemp != null){
                size += nodeTemp.getSize();
            }
        }
        log("Number of files: "+hashes.length);

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        }

        boolean askMe = true;
        boolean advancedDevices=false;
        String downloadLocationDefaultPath = Util.downloadDIR;
        prefs = dbH.getPreferences();

        if (prefs != null){
            log("prefs != null");
            if (prefs.getStorageAskAlways() != null){
                if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
                    log("askMe==false");
                    if (prefs.getStorageDownloadLocation() != null){
                        if (prefs.getStorageDownloadLocation().compareTo("") != 0){
                            askMe = false;
                            downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
                        }
                    }
                }
                else
                {
                    log("askMe==true");
                    //askMe=true
                    if (prefs.getStorageAdvancedDevices() != null){
                        advancedDevices = Boolean.parseBoolean(prefs.getStorageAdvancedDevices());
                    }

                }
            }
        }

        if (askMe){
            log("askMe");
            if(advancedDevices){
                log("advancedDevices");
                //Launch Intent to SAF
                if(hashes.length==1){
                    downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
                    if(context instanceof ManagerActivityLollipop){
                        ((ManagerActivityLollipop) context).openAdvancedDevices(hashes[0]);
                    }
                   else{
                        log("ManagerActivityLollipop is not CONTEXT");
                    }
                }
                else
                {
                    //Show error message, just one file
                    Toast.makeText(context, context.getString(R.string.context_select_one_file), Toast.LENGTH_LONG).show();
                }
            }
            else{
                log("NOT advancedDevices");

                Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
                intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, context.getString(R.string.context_download_to));
                intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
                intent.setClass(context, FileStorageActivityLollipop.class);
                intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
                ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER);
            }
        }
        else{
            log("NOT askMe");
            File defaultPathF = new File(downloadLocationDefaultPath);
            defaultPathF.mkdirs();
            checkSizeBeforeDownload(downloadLocationDefaultPath, null, size, hashes);
        }

    }

    //Old downloadTo
    public void checkSizeBeforeDownload(String parentPath, String url, long size, long [] hashes){
        //Variable size is incorrect for folders, it is always -1 -> sizeTemp calculates the correct size
        log("checkSizeBeforeDownload - parentPath: "+parentPath+ " url: "+url+" size: "+size);
        log("files to download: "+hashes.length);
        log("SIZE to download before calculating: "+size);

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        long sizeTemp=0;

        for (long hash : hashes) {
            MegaNode node = megaApi.getNodeByHandle(hash);
            if(node!=null){
                if(node.isFolder()){
                    log("node to download is FOLDER");
                    sizeTemp=sizeTemp+ MegaApiUtils.getFolderSize(node, context);
                }
                else{
                    sizeTemp = sizeTemp+node.getSize();
                }
            }
        }

        final long sizeC = sizeTemp;
        log("the final size is: "+Util.getSizeString(sizeTemp));

        //Check if there is available space
        double availableFreeSpace = Double.MAX_VALUE;
        try{
            StatFs stat = new StatFs(parentPath);
            availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
        }
        catch(Exception ex){}

        log("availableFreeSpace: " + availableFreeSpace + "__ sizeToDownload: " + sizeC);
        if(availableFreeSpace < sizeC) {
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_not_enough_free_space));
            log("Not enough space");
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
            log("SIZE: Do not ask before downloading");
            checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC);
        }
        else{
            log("SIZE: Ask before downloading");
            //Check size to download
            //100MB=104857600
            //10MB=10485760
            //1MB=1048576
            if(sizeC>104857600){
                log("Show size confirmacion: "+sizeC);
                //Show alert
                ((ManagerActivityLollipop) context).askSizeConfirmationBeforeDownload(parentPathC, urlC, sizeC, hashesC);
            }
            else{
                checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC);
            }
        }
    }

    //Old proceedToDownload
    public void checkInstalledAppBeforeDownload(String parentPath, String url, long size, long [] hashes){
        log("checkInstalledAppBeforeDownload");
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
            log("ask==null");
            ask="true";
        }

        if(ask.equals("false")){
            log("INSTALLED APP: Do not ask before downloading");
            download(parentPathC, urlC, sizeC, hashesC);
        }
        else{
            log("INSTALLED APP: Ask before downloading");
            if (hashes != null){
                for (long hash : hashes) {
                    MegaNode node = megaApi.getNodeByHandle(hash);
                    if(node!=null){
                        log("Node: "+ node.getName());

                        if(node.isFile()){
                            Intent checkIntent = new Intent(Intent.ACTION_VIEW, null);
                            log("MimeTypeList: "+ MimeTypeList.typeForName(node.getName()).getType());

                            checkIntent.setType(MimeTypeList.typeForName(node.getName()).getType());

                            try{
                                if (!MegaApiUtils.isIntentAvailable(context, checkIntent)){
                                    confirmationToDownload = true;
                                    nodeToDownload=node.getName();
                                    break;
                                }
                            }catch(Exception e){
                                log("isIntent EXCEPTION");
                                confirmationToDownload = true;
                                nodeToDownload=node.getName();
                                break;
                            }
                        }
                    }
                    else{
                        log("ERROR - node is NULL");
                    }
                }
            }

            //Check if show the alert message
            if(confirmationToDownload){
                //Show message
                ((ManagerActivityLollipop) context).askConfirmationNoAppInstaledBeforeDownload(parentPathC, urlC, sizeC, hashesC, nodeToDownload);
            }
            else{
                download(parentPathC, urlC, sizeC, hashesC);
            }
        }
    }

    public void download(String parentPath, String url, long size, long [] hashes){
        log("download-----------");
        log("downloadTo, parentPath: "+parentPath+ "url: "+url+" size: "+size);
        log("files to download: "+hashes.length);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                ActivityCompat.requestPermissions(((ManagerActivityLollipop) context),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Constants.REQUEST_WRITE_STORAGE);
            }
        }

        if (hashes == null){
            log("hashes is null");
            if(url != null) {
                log("url NOT null");
                Intent service = new Intent(context, DownloadService.class);
                service.putExtra(DownloadService.EXTRA_URL, url);
                service.putExtra(DownloadService.EXTRA_SIZE, size);
                service.putExtra(DownloadService.EXTRA_PATH, parentPath);
                context.startService(service);
            }
        }
        else{
            log("hashes is NOT null");
            if(hashes.length == 1){
                log("hashes.length == 1");
                MegaNode tempNode = megaApi.getNodeByHandle(hashes[0]);

                if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
                    log("ISFILE");
                    String localPath = Util.getLocalFile(context, tempNode.getName(), tempNode.getSize(), parentPath);
                    //Check if the file is already downloaded
                    if(localPath != null){
                        log("localPath != null");
                        try {
                            log("Call to copyFile: localPath: "+localPath+" node name: "+tempNode.getName());
                            Util.copyFile(new File(localPath), new File(parentPath, tempNode.getName()));

                            if(Util.isVideoFile(parentPath+"/"+tempNode.getName())){
                                log("Is video!!!");
//								MegaNode videoNode = megaApi.getNodeByHandle(tempNode.getNodeHandle());
                                if (tempNode != null){
                                    if(!tempNode.hasThumbnail()){
                                        log("The video has not thumb");
                                        ThumbnailUtilsLollipop.createThumbnailVideo(context, localPath, megaApi, tempNode.getHandle());
                                    }
                                }
                            }
                            else{
                                log("NOT video!");
                            }
                        }
                        catch(Exception e) {
                            log("Exception!!");
                        }

                        if(MimeTypeList.typeForName(tempNode.getName()).isZip()){
                            log("MimeTypeList ZIP");
                            File zipFile = new File(localPath);

                            Intent intentZip = new Intent();
                            intentZip.setClass(context, ZipBrowserActivityLollipop.class);
                            intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, zipFile.getAbsolutePath());
                            intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_HANDLE_ZIP, tempNode.getHandle());

                            context.startActivity(intentZip);

                        }
                        else {
                            log("MimeTypeList other file");
                            try {
                                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    viewIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                } else {
                                    viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                }
                                viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                if (MegaApiUtils.isIntentAvailable(context, viewIntent)) {
                                    log("if isIntentAvailable");
                                    context.startActivity(viewIntent);
                                } else {
                                    log("ELSE isIntentAvailable");
                                    Intent intentShare = new Intent(Intent.ACTION_SEND);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        intentShare.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                    } else {
                                        intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                    }
                                    intentShare.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    if (MegaApiUtils.isIntentAvailable(context, intentShare)) {
                                        log("call to startActivity(intentShare)");
                                        context.startActivity(intentShare);
                                    }
                                    ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.general_already_downloaded));
                                }
                            }
                            catch (Exception e){
                                ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.general_already_downloaded));
                            }
                        }
                        return;
                    }
                    else{
                        log("localPath is NULL");
                    }
                }
            }

            int numberOfNodesToDownload = 0;
            int numberOfNodesAlreadyDownloaded = 0;
            int numberOfNodesPending = 0;

            for (long hash : hashes) {
                log("hashes.length more than 1");
                MegaNode node = megaApi.getNodeByHandle(hash);
                if(node != null){
                    log("node NOT null");
                    Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
                    if (node.getType() == MegaNode.TYPE_FOLDER) {
                        log("MegaNode.TYPE_FOLDER");
                        getDlList(dlFiles, node, new File(parentPath, new String(node.getName())));
                    } else {
                        log("MegaNode.TYPE_FILE");
                        dlFiles.put(node, parentPath);
                    }

                    for (MegaNode document : dlFiles.keySet()) {
                        String path = dlFiles.get(document);
                        log("path of the file: "+path);
                        numberOfNodesToDownload++;

                        File destDir = new File(path);
                        File destFile;
                        destDir.mkdirs();
                        if (destDir.isDirectory()){
                            destFile = new File(destDir, megaApi.escapeFsIncompatible(document.getName()));
                            log("destDir is Directory. destFile: " + destFile.getAbsolutePath());
                        }
                        else{
                            log("destDir is File");
                            destFile = destDir;
                        }

                        if(destFile.exists() && (document.getSize() == destFile.length())){
                            numberOfNodesAlreadyDownloaded++;
                            log(destFile.getAbsolutePath() + " already downloaded");
                        }
                        else {
                            numberOfNodesPending++;
                            log("start service");
                            Intent service = new Intent(context, DownloadService.class);
                            service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
                            service.putExtra(DownloadService.EXTRA_URL, url);
                            service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
                            service.putExtra(DownloadService.EXTRA_PATH, path);
                            context.startService(service);
                        }
                    }
                }
                else if(url != null) {
                    log("URL NOT null");
                    log("start service");
                    Intent service = new Intent(context, DownloadService.class);
                    service.putExtra(DownloadService.EXTRA_HASH, hash);
                    service.putExtra(DownloadService.EXTRA_URL, url);
                    service.putExtra(DownloadService.EXTRA_SIZE, size);
                    service.putExtra(DownloadService.EXTRA_PATH, parentPath);
                    context.startService(service);
                }
                else {
                    log("node NOT fOUND!!!!!");
                }
            }
            log("Total: " + numberOfNodesToDownload + " Already: " + numberOfNodesAlreadyDownloaded + " Pending: " + numberOfNodesPending);
            if (numberOfNodesAlreadyDownloaded > 0){
                String msg = context.getString(R.string.already_downloaded_multiple, numberOfNodesAlreadyDownloaded);
                if (numberOfNodesPending > 0){
                    msg = msg + context.getString(R.string.pending_multiple, numberOfNodesPending);
                }
                ((ManagerActivityLollipop) context).showSnackbar(msg);
            }
        }
    }

    /*
	 * Get list of all child files
	 */
    private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {
        log("getDlList");
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
        log("renameNode");
        if (newName.compareTo(document.getName()) == 0) {
            return;
        }

        if(!Util.isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            return;
        }

        log("renaming " + document.getName() + " to " + newName);

        megaApi.renameNode(document, newName, ((ManagerActivityLollipop) context));
    }

    public void importLink(String url) {

        try {
            url = URLDecoder.decode(url, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {}
        url.replace(' ', '+');
        if(url.startsWith("mega://")){
            url = url.replace("mega://", "https://mega.co.nz/");
        }

        log("url " + url);

        // Download link
        if (url != null && (url.matches("^https://mega.co.nz/#!.*!.*$") || url.matches("^https://mega.nz/#!.*!.*$"))) {
            log("open link url");

//			Intent openIntent = new Intent(this, ManagerActivityLollipop.class);
            Intent openFileIntent = new Intent(context, FileLinkActivityLollipop.class);
            openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openFileIntent.setAction(Constants.ACTION_OPEN_MEGA_LINK);
            openFileIntent.setData(Uri.parse(url));
            ((ManagerActivityLollipop) context).startActivity(openFileIntent);
//			finish();
            return;
        }

        // Folder Download link
        else if (url != null && (url.matches("^https://mega.co.nz/#F!.+$") || url.matches("^https://mega.nz/#F!.+$"))) {
            log("folder link url");
            Intent openFolderIntent = new Intent(context, FolderLinkActivityLollipop.class);
            openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openFolderIntent.setAction(Constants.ACTION_OPEN_MEGA_FOLDER_LINK);
            openFolderIntent.setData(Uri.parse(url));
            ((ManagerActivityLollipop) context).startActivity(openFolderIntent);
//			finish();
            return;
        }
        else{
            log("wrong url");
            Intent errorIntent = new Intent(context, ManagerActivityLollipop.class);
            errorIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ((ManagerActivityLollipop) context).startActivity(errorIntent);
        }
    }

    //old getPublicLinkAndShareIt
    public void exportLink(MegaNode document){
        log("exportLink");
        if(context instanceof ManagerActivityLollipop){
            if (!Util.isOnline(context)){
                ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }
            ((ManagerActivityLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, ((ManagerActivityLollipop) context));
        }
        if(context instanceof GetLinkActivityLollipop){
            if (!Util.isOnline(context)){
                ((GetLinkActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }
            megaApi.exportNode(document, ((GetLinkActivityLollipop) context));
        }
        else  if(context instanceof FullScreenImageViewerLollipop){
            if (!Util.isOnline(context)){
                ((FullScreenImageViewerLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }
            ((FullScreenImageViewerLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, ((FullScreenImageViewerLollipop) context));
        }
        else  if(context instanceof FileInfoActivityLollipop){
            if (!Util.isOnline(context)){
                ((FileInfoActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }
            ((FileInfoActivityLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, ((FileInfoActivityLollipop) context));
        }
    }

    public void exportLinkTimestamp(MegaNode document, int timestamp){
        log("exportLinkTimestamp: "+timestamp);
        if (context instanceof ManagerActivityLollipop){
            if (!Util.isOnline(context)){
                ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }
            ((ManagerActivityLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, timestamp, ((ManagerActivityLollipop) context));
        }
        else if (context instanceof GetLinkActivityLollipop){
            if (!Util.isOnline(context)){
                ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }
            megaApi.exportNode(document, timestamp, ((GetLinkActivityLollipop) context));
        }
        else if (context instanceof FullScreenImageViewerLollipop){
            if (!Util.isOnline(context)){
                ((FullScreenImageViewerLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }
            ((FullScreenImageViewerLollipop) context).setIsGetLink(true);
            megaApi.exportNode(document, timestamp, ((FullScreenImageViewerLollipop) context));
        }
        else if (context instanceof FileInfoActivityLollipop){
            if (!Util.isOnline(context)){
                ((FileInfoActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
                return;
            }
            megaApi.exportNode(document, timestamp, ((FileInfoActivityLollipop) context));
        }
    }

    public void removeLink(MegaNode document){
        log("removeLink");
        if (!Util.isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            return;
        }
        megaApi.disableExport(document, ((ManagerActivityLollipop) context));
    }


    public void selectContactToShareFolders(ArrayList<Long> handleList){
        log("shareFolders ArrayListLong");
        //TODO shareMultipleFolders

        if (!Util.isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            return;
        }

        Intent intent = new Intent(AddContactActivityLollipop.ACTION_PICK_CONTACT_SHARE_FOLDER);
        intent.setClass(context, AddContactActivityLollipop.class);

        long[] handles=new long[handleList.size()];
        int j=0;
        for(int i=0; i<handleList.size();i++){
            handles[j]=handleList.get(i);
            j++;
        }
        intent.putExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, handles);
        //Multiselect=1 (multiple folders)
        intent.putExtra("MULTISELECT", 1);
        intent.putExtra("SEND_FILE",0);
        ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_CONTACT);
    }

    public void selectContactToShareFolder(MegaNode node){
        log("shareFolder");

        Intent intent = new Intent(AddContactActivityLollipop.ACTION_PICK_CONTACT_SHARE_FOLDER);
        intent.setClass(context, AddContactActivityLollipop.class);
        //Multiselect=0
        intent.putExtra("MULTISELECT", 0);
        intent.putExtra("SEND_FILE",0);
        intent.putExtra(AddContactActivityLollipop.EXTRA_NODE_HANDLE, node.getHandle());
        ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_CONTACT);
    }

    public void shareFolder(long folderHandle, ArrayList<String> selectedContacts, int level){

        if(!Util.isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            return;
        }

        MegaNode parent = megaApi.getNodeByHandle(folderHandle);
        MultipleRequestListener shareMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_CONTACTS_SHARE, (ManagerActivityLollipop) context);
        if(parent!=null&parent.isFolder()){
            if(selectedContacts.size()>1){
                log("Share READ one file multiple contacts");
                for (int i=0;i<selectedContacts.size();i++){
                    MegaUser user= megaApi.getContact(selectedContacts.get(i));
                    if(user!=null){
                        megaApi.share(parent, user, level,shareMultipleListener);
                    }
                    else {
                        log("USER is NULL when sharing!->SHARE WITH NON CONTACT");
                        megaApi.share(parent, selectedContacts.get(i), level, shareMultipleListener);
                    }
                }
            }
            else{
                log("Share READ one file one contact");
                MegaUser user= megaApi.getContact(selectedContacts.get(0));
                if(user!=null){
                    megaApi.share(parent, user, level, (ManagerActivityLollipop) context);
                }
                else {
                    log("USER is NULL when sharing!->SHARE WITH NON CONTACT");
                    megaApi.share(parent, selectedContacts.get(0), level, (ManagerActivityLollipop) context);
                }
            }
        }
    }

    public void shareFolders(long[] nodeHandles, ArrayList<String> contactsData, int level){

        if(!Util.isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            return;
        }

        MultipleRequestListener shareMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_FILE_SHARE, context);

        for (int i=0;i<contactsData.size();i++){
            MegaUser u = megaApi.getContact(contactsData.get(i));
            if(nodeHandles.length>1){
                log("many folder to many contacts");
                for(int j=0; j<nodeHandles.length;j++){

                    final MegaNode node = megaApi.getNodeByHandle(nodeHandles[j]);
                    if(node!=null){
                        if(u!=null){
                            log("Share: "+ node.getName() + " to "+ u.getEmail());
                            megaApi.share(node, u, level, shareMultipleListener);
                        }
                        else{
                            log("USER is NULL when sharing!->SHARE WITH NON CONTACT");
                            megaApi.share(node, contactsData.get(i), level, shareMultipleListener);
                        }
                    }
                    else{
                        log("NODE NULL!!!");
                    }

                }
            }
            else{
                log("one folder to many contacts");
                for(int j=0; j<nodeHandles.length;j++){

                    final MegaNode node = megaApi.getNodeByHandle(nodeHandles[j]);
                    if(u!=null){
                        log("Share: "+ node.getName() + " to "+ u.getEmail());
                        megaApi.share(node, u, level, shareMultipleListener);
                    }
                    else{
                        log("USER is NULL when sharing!->SHARE WITH NON CONTACT");
                        megaApi.share(node, contactsData.get(i), level, shareMultipleListener);
                    }
                }
            }
        }
    }

    public void moveToTrash(final ArrayList<Long> handleList, boolean moveToRubbish){
        log("moveToTrash: "+moveToRubbish);

        MultipleRequestListener moveMultipleListener = null;
        MegaNode parent;
        //Check if the node is not yet in the rubbish bin (if so, remove it)
        if(handleList!=null){
            if(handleList.size()>1){
                log("MOVE multiple: "+handleList.size());
                if (moveToRubbish){
                    moveMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_SEND_RUBBISH, context);
                }
                else{
                    moveMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_MOVE, context);
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
                log("MOVE single");
                if (moveToRubbish){
                    megaApi.moveNode(megaApi.getNodeByHandle(handleList.get(0)), megaApi.getRubbishNode(), ((ManagerActivityLollipop) context));
                }
                else{
                    megaApi.remove(megaApi.getNodeByHandle(handleList.get(0)), ((ManagerActivityLollipop) context));
                }
            }
        }
        else{
            log("handleList NULL");
            return;
        }
    }

    public void openFolderFromSearch(long folderHandle){
        log("openFolderFromSearch: "+folderHandle);
        boolean firstNavigationLevel=true;
        int access = -1;
        ManagerActivityLollipop.DrawerItem drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
        if (folderHandle != -1) {
            MegaNode parentIntentN = megaApi.getParentNode(megaApi.getNodeByHandle(folderHandle));
            if (parentIntentN != null) {
                log("Check the parent node: "+parentIntentN.getName()+" handle: "+parentIntentN.getHandle());
                access = megaApi.getAccess(parentIntentN);
                switch (access) {
                    case MegaShare.ACCESS_OWNER:
                    case MegaShare.ACCESS_UNKNOWN: {
                        //Not incoming folder, check if Cloud or Rubbish tab
                        if(parentIntentN.getHandle()==megaApi.getRootNode().getHandle()){
                            drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                            log("Navigate to TAB CLOUD first level"+ parentIntentN.getName());
                            firstNavigationLevel=true;
                            ((ManagerActivityLollipop) context).setParentHandleBrowser(parentIntentN.getHandle());
                            ((ManagerActivityLollipop) context).setTabItemCloud(0);
                        }
                        else if(parentIntentN.getHandle()==megaApi.getRubbishNode().getHandle()){
                            drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                            log("Navigate to TAB RUBBISH first level"+ parentIntentN.getName());
                            firstNavigationLevel=true;
                            ((ManagerActivityLollipop) context).setParentHandleRubbish(parentIntentN.getHandle());
                            ((ManagerActivityLollipop) context).setTabItemCloud(1);
                        }
                        else if(parentIntentN.getHandle()==megaApi.getInboxNode().getHandle()){
                            log("Navigate to INBOX first level"+ parentIntentN.getName());
                            firstNavigationLevel=true;
                            ((ManagerActivityLollipop) context).setParentHandleInbox(parentIntentN.getHandle());
                            drawerItem = ManagerActivityLollipop.DrawerItem.INBOX;
                        }
                        else{
                            int parent = checkParentNodeToOpenFolder(parentIntentN.getHandle());
                            log("The parent result is: "+parent);

                            switch (parent){
                                case 0:{
                                    //ROOT NODE
                                    drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                                    log("Navigate to TAB CLOUD with parentHandle");
                                    ((ManagerActivityLollipop) context).setParentHandleBrowser(parentIntentN.getHandle());
                                    ((ManagerActivityLollipop) context).setTabItemCloud(0);
                                    firstNavigationLevel=false;
                                    break;
                                }
                                case 1:{
                                    log("Navigate to TAB RUBBISH");
                                    drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                                    ((ManagerActivityLollipop) context).setParentHandleRubbish(parentIntentN.getHandle());
                                    ((ManagerActivityLollipop) context).setTabItemCloud(1);
                                    firstNavigationLevel=false;
                                    break;
                                }
                                case 2:{
                                    log("Navigate to INBOX WITH parentHandle");
                                    drawerItem = ManagerActivityLollipop.DrawerItem.INBOX;
                                    ((ManagerActivityLollipop) context).setParentHandleInbox(parentIntentN.getHandle());
                                    firstNavigationLevel=false;
                                    break;
                                }
                                case -1:{
                                    drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                                    log("Navigate to TAB CLOUD general");
                                    ((ManagerActivityLollipop) context).setParentHandleBrowser(-1);
                                    ((ManagerActivityLollipop) context).setTabItemCloud(0);
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
                        log("GO to INCOMING TAB: " + parentIntentN.getName());
                        drawerItem = ManagerActivityLollipop.DrawerItem.SHARED_ITEMS;
                        if(parentIntentN.getHandle()==-1){
                            log("Level 0 of Incoming");
                            ((ManagerActivityLollipop) context).setParentHandleIncoming(-1);
                            ((ManagerActivityLollipop) context).setDeepBrowserTreeIncoming(0);
                            firstNavigationLevel=true;
                        }
                        else{
                            firstNavigationLevel=false;
                            ((ManagerActivityLollipop) context).setParentHandleIncoming(parentIntentN.getHandle());
                            int deepBrowserTreeIncoming = MegaApiUtils.calculateDeepBrowserTreeIncoming(parentIntentN, context);
                            ((ManagerActivityLollipop) context).setDeepBrowserTreeIncoming(deepBrowserTreeIncoming);
                            log("After calculating deepBrowserTreeIncoming: "+deepBrowserTreeIncoming);
                        }
                        ((ManagerActivityLollipop) context).setTabItemShares(0);
                        break;
                    }
                    default: {
                        log("DEFAULT: The intent set the parentHandleBrowser to " + parentIntentN.getHandle());
                        ((ManagerActivityLollipop) context).setParentHandleBrowser(parentIntentN.getHandle());
                        drawerItem = ManagerActivityLollipop.DrawerItem.CLOUD_DRIVE;
                        ((ManagerActivityLollipop) context).setTabItemCloud(0);
                        firstNavigationLevel=true;
                        break;
                    }
                }
            }
            else{
                log("Parent is already NULL");

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
        log("checkParentNodeToOpenFolder");
        MegaNode folderNode = megaApi.getNodeByHandle(folderHandle);
        MegaNode parentNode = megaApi.getParentNode(folderNode);
        if(parentNode!=null){
            log("parentName: "+parentNode.getName());
            if(parentNode.getHandle()==megaApi.getRootNode().getHandle()){
                log("The parent is the ROOT");
                return 0;
            }
            else if(parentNode.getHandle()==megaApi.getRubbishNode().getHandle()){
                log("The parent is the RUBBISH");
                return 1;
            }
            else if(parentNode.getHandle()==megaApi.getInboxNode().getHandle()){
                log("The parent is the INBOX");
                return 2;
            }
            else if(parentNode.getHandle()==-1){
                log("The parent is -1");
                return -1;
            }
            else{
                int result = checkParentNodeToOpenFolder(parentNode.getHandle());
                log("Call returns "+result);
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

    public void leaveIncomingShare (final MegaNode n){
        log("leaveIncomingShare");

        megaApi.remove(n);
    }

    public void leaveMultipleIncomingShares (final ArrayList<Long> handleList){
        log("leaveMultipleIncomingShares");

        MultipleRequestListener moveMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_LEAVE_SHARE, context);
        if(handleList.size()>1){
            log("handleList.size()>1");
            for (int i=0; i<handleList.size(); i++){
                MegaNode node = megaApi.getNodeByHandle(handleList.get(i));
                megaApi.remove(node, moveMultipleListener);
            }
        }
        else{
            log("handleList.size()<=1");
            MegaNode node = megaApi.getNodeByHandle(handleList.get(0));
            megaApi.remove(node, (ManagerActivityLollipop)context);
        }
    }

    public void removeAllSharingContacts (ArrayList<MegaShare> listContacts, MegaNode node){
        log("removeAllSharingContacts");

        MultipleRequestListener shareMultipleListener = new MultipleRequestListener(Constants.MULTIPLE_REMOVE_SHARING_CONTACTS, context);
        if(listContacts.size()>1){
            log("listContacts.size()>1");
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
            log("listContacts.size()<=1");
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
        log("cleanRubbishBin");
        megaApi.cleanRubbishBin((ManagerActivityLollipop) context);
    }

    public void deleteOffline(MegaOffline selectedNode, String pathNavigation){
        log("deleteOffline");
        if (selectedNode == null){
            log("Delete RK");
            String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
            File file= new File(path);
            if(file.exists()){
                file.delete();

                ArrayList<MegaOffline> mOffList=dbH.findByPath(pathNavigation);

                log("Number of elements: "+mOffList.size());

                for(int i=0; i<mOffList.size();i++){

                    MegaOffline checkOffline = mOffList.get(i);
                    File offlineDirectory = null;
                    if(checkOffline.getOrigin()==MegaOffline.INCOMING){
                        log("isIncomingOffline");

                        if (Environment.getExternalStorageDirectory() != null){
                            offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +checkOffline.getHandleIncoming() + "/" + checkOffline.getPath()+checkOffline.getName());
                            log("offlineDirectory: "+offlineDirectory);
                        }
                        else{
                            offlineDirectory = context.getFilesDir();
                        }
                    }
                    else if(checkOffline.getOrigin()==MegaOffline.INBOX){
                        if (Environment.getExternalStorageDirectory() != null){
                            offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in/" + checkOffline.getPath()+checkOffline.getName());
                            log("offlineDirectory: "+offlineDirectory);
                        }
                        else{
                            offlineDirectory = context.getFilesDir();
                        }
                    }
                    else{
                        log("OTHER Offline preference");

                        if (Environment.getExternalStorageDirectory() != null){
                            offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + checkOffline.getPath()+checkOffline.getName());
                        }
                        else{
                            offlineDirectory = context.getFilesDir();
                        }

                    }

                    if(offlineDirectory!=null){
                        if (!offlineDirectory.exists()){
                            log("Path to remove B: "+(mOffList.get(i).getPath()+mOffList.get(i).getName()));
                            //dbH.removeById(mOffList.get(i).getId());
                            mOffList.remove(i);
                            i--;
                        }
                    }

                }

                if(context instanceof ManagerActivityLollipop){
                    ((ManagerActivityLollipop)context).updateOfflineView(null);
                }
            }
        }
        else {
            if (selectedNode.getHandle().equals("0")) {
                log("Delete RK");
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + Util.rKFile;
                File file = new File(path);
                if (file.exists()) {
                    file.delete();

                    ArrayList<MegaOffline> mOffList = dbH.findByPath(pathNavigation);

                    log("Number of elements: " + mOffList.size());

                    for (int i = 0; i < mOffList.size(); i++) {

                        MegaOffline checkOffline = mOffList.get(i);
                        File offlineDirectory = null;
                        if(checkOffline.getOrigin()==MegaOffline.INCOMING){
                            log("isIncomingOffline");

                            if (Environment.getExternalStorageDirectory() != null){
                                offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +checkOffline.getHandleIncoming() + "/" + checkOffline.getPath()+checkOffline.getName());
                                log("offlineDirectory: "+offlineDirectory);
                            }
                            else{
                                offlineDirectory = context.getFilesDir();
                            }
                        }
                        else if(checkOffline.getOrigin()==MegaOffline.INBOX){
                            if (Environment.getExternalStorageDirectory() != null){
                                offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in/" + checkOffline.getPath()+checkOffline.getName());
                                log("offlineDirectory: "+offlineDirectory);
                            }
                            else{
                                offlineDirectory = context.getFilesDir();
                            }
                        }
                        else{
                            log("OTHER Offline preference");

                            if (Environment.getExternalStorageDirectory() != null){
                                offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + checkOffline.getPath()+checkOffline.getName());
                            }
                            else{
                                offlineDirectory = context.getFilesDir();
                            }

                        }

                        if(offlineDirectory!=null){
                            if (!offlineDirectory.exists()){
                                log("Path to remove B: "+(mOffList.get(i).getPath()+mOffList.get(i).getName()));
                                //dbH.removeById(mOffList.get(i).getId());
                                mOffList.remove(i);
                                i--;
                            }
                        }
                    }

                    if (context instanceof ManagerActivityLollipop) {
                        ((ManagerActivityLollipop) context).updateOfflineView(null);
                    }
                }
            } else {
                log("deleteOffline node");
                dbH = DatabaseHandler.getDbHandler(context);

                ArrayList<MegaOffline> mOffListParent = new ArrayList<MegaOffline>();
                ArrayList<MegaOffline> mOffListChildren = new ArrayList<MegaOffline>();
                MegaOffline parentNode = null;

                //Delete children
                mOffListChildren = dbH.findByParentId(selectedNode.getId());
                if (mOffListChildren.size() > 0) {
                    //The node have childrens, delete
                    deleteChildrenDB(mOffListChildren);
                }

                log("Remove the node physically");
                //Remove the node physically
                File destination = null;
                //Check if the node is incoming
                if(selectedNode.getOrigin()==MegaOffline.INCOMING){
                    log("isIncomingOffline");

                    if (Environment.getExternalStorageDirectory() != null){
                        destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +selectedNode.getHandleIncoming() + "/" + selectedNode.getPath());
                        log("destination: "+destination);
                    }
                    else{
                        destination = context.getFilesDir();
                    }
                }
                else if(selectedNode.getOrigin()==MegaOffline.INBOX){
                    if (Environment.getExternalStorageDirectory() != null){
                        destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in/" + selectedNode.getPath());
                        log("destination: "+destination);
                    }
                    else{
                        destination = context.getFilesDir();
                    }
                }
                else{
                    log("OTHER Offline preference");

                    if (Environment.getExternalStorageDirectory() != null){
                        destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + selectedNode.getPath());
                    }
                    else{
                        destination = context.getFilesDir();
                    }

                }

                try {
                    File offlineFile = new File(destination, selectedNode.getName());
                    log("Delete in phone: " + selectedNode.getName());
                    Util.deleteFolderAndSubfolders(context, offlineFile);
                } catch (Exception e) {
                    log("EXCEPTION: deleteOffline - adapter");
                }
                ;

                dbH.removeById(selectedNode.getId());

                //Check if the parent has to be deleted

                int parentId = selectedNode.getParentId();
                parentNode = dbH.findById(parentId);

                if (parentNode != null) {
                    log("Parent to check: " + parentNode.getName());
                    checkParentDeletion(parentNode);
                }

                if (context instanceof ManagerActivityLollipop) {
                    ((ManagerActivityLollipop) context).updateOfflineView(null);
                }
            }
        }
    }

    public void deleteChildrenDB(ArrayList<MegaOffline> mOffListChildren){

        log("deleteChildenDB: "+mOffListChildren.size());
        MegaOffline mOffDelete=null;

        for(int i=0; i<mOffListChildren.size(); i++){

            mOffDelete=mOffListChildren.get(i);

            log("Children "+i+ ": "+ mOffDelete.getName());
            ArrayList<MegaOffline> mOffListChildren2=dbH.findByParentId(mOffDelete.getId());
            if(mOffListChildren2.size()>0){
                //The node have children, delete
                deleteChildrenDB(mOffListChildren2);
            }

            int lines = dbH.removeById(mOffDelete.getId());
            log("Borradas; "+lines);
        }
    }

    public void checkParentDeletion (MegaOffline parentToDelete){
        log("checkParentDeletion: "+parentToDelete.getName());

        ArrayList<MegaOffline> mOffListChildren=dbH.findByParentId(parentToDelete.getId());
        File destination = null;
        if(mOffListChildren.size()<=0){
            log("The parent has NO children");
            //The node have NO childrens, delete it

            dbH.removeById(parentToDelete.getId());
            if(parentToDelete.getOrigin()==MegaOffline.INCOMING){
                if (Environment.getExternalStorageDirectory() != null){
                    destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + parentToDelete.getHandleIncoming() + parentToDelete.getPath());
                }
                else{
                    destination = context.getFilesDir();
                }
            }
            else if(parentToDelete.getOrigin()==MegaOffline.INBOX){
                if (Environment.getExternalStorageDirectory() != null){
                    destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in" + parentToDelete.getPath());
                }
                else{
                    destination = context.getFilesDir();
                }
            }
            else{
                if (Environment.getExternalStorageDirectory() != null){
                    destination = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + parentToDelete.getPath());
                }
                else{
                    destination = context.getFilesDir();
                }
            }

            try{
                File offlineFile = new File(destination, parentToDelete.getName());
                log("Delete in phone: "+parentToDelete.getName());
                Util.deleteFolderAndSubfolders(context, offlineFile);
            }
            catch(Exception e){
                log("EXCEPTION: deleteOffline - adapter");
            };

            int parentId = parentToDelete.getParentId();
            if(parentId==-1){
                File rootIncomingFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + parentToDelete.getHandleIncoming());

                if(rootIncomingFile!=null){

                    String[] fileList = rootIncomingFile.list();
                    if(fileList!=null){
                        if(rootIncomingFile.list().length==0){
                            try{
                                rootIncomingFile.delete();
                            }
                            catch(Exception e){
                                log("EXCEPTION: deleteParentIncoming: "+destination);
                            };
                        }
                    }
                }
                else{
                    log("rootIncomingFile is NULL");
                }
            }
            else{
                //Check if the parent has to be deleted

                parentToDelete = dbH.findById(parentId);
                if(parentToDelete != null){
                    log("Parent to check: "+parentToDelete.getName());
                    checkParentDeletion(parentToDelete);

                }
            }

        }
        else{
            log("The parent has children!!! RETURN!!");
            return;
        }

    }

    public static void log(String message) {
        Util.log("NodeController", message);
    }
}
