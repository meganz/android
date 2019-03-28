package mega.privacy.android.app.jobservices;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.provider.DocumentFile;
import android.text.format.Time;
import android.widget.RemoteViews;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

public class CameraUploadsService extends JobService implements MegaGlobalListenerInterface, MegaChatRequestListenerInterface, MegaRequestListenerInterface, MegaTransferListenerInterface {

    private static String PHOTO_SYNC = "PhotoSync";
    private static String CAMERA_UPLOADS = "Camera Uploads";
    private static String SECONDARY_UPLOADS = "Media Uploads";

    private NotificationCompat.Builder mBuilder;
    NotificationManager mNotificationManager;

    private int notificationId = Constants.NOTIFICATION_CAMERA_UPLOADS;
    private int notificationIdFinal = Constants.NOTIFICATION_CAMERA_UPLOADS_FINAL;
    private String notificationChannelId = Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_ID;
    private String notificationChannelName = Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_NAME;

    Thread task;

    static public boolean running = false;
    private Handler handler;

    WifiManager.WifiLock lock;
    PowerManager.WakeLock wl;

    private boolean isOverquota = false;
    private boolean canceled;
    private boolean isForeground;
    boolean newFileList = false;

    DatabaseHandler dbH;

    MegaPreferences prefs;
    String localPath = "";
    boolean isExternalSDCard = false;
    ChatSettings chatSettings;
    long cameraUploadHandle = -1;
    boolean secondaryEnabled= false;
    String localPathSecondary = "";
    long secondaryUploadHandle = -1;
    MegaNode secondaryUploadNode = null;

    boolean isLoggingIn = false;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    MegaApplication app;

    JobParameters globalParams;

    int LOGIN_IN = 12;

    static CameraUploadsService cameraUploadsService;
    static String gSession;

    private class Media {
        public String filePath;
        public long timestamp;
    }

    Queue<Media> cameraFiles = new LinkedList<Media>();
    Queue<Media> mediaFilesSecondary = new LinkedList<Media>();
    ArrayList<DocumentFile> cameraFilesExternalSDCardList = new ArrayList<DocumentFile>();
    Queue<DocumentFile> cameraFilesExternalSDCardQueue;
    MegaNode cameraUploadNode = null;
    private int totalUploaded;
    private long totalSizeToUpload;
    private int totalToUpload;
    private long totalSizeUploaded;
    private int successCount;

    long currentTimeStamp = 0;

    @Override
    public boolean onStartJob(JobParameters params) {
        log("onStartJob");

        cameraUploadsService = this;

        globalParams = params;

        handler = new Handler();

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        log("startCameraSyncService");
                        startService(new Intent(getApplicationContext(), CameraSyncService.class));
                    }
                }, 30000);
            }
            else{
                log("Start service here");

                initService();

                task = new Thread()
                {
                    @Override
                    public void run()
                    {
                        try{
                            int result = shouldRun();
                            if (result == 0){
                                startCameraUploads();
                            }
                        } catch (Exception e) {}
                    }
                };

                task.start();

                return true;
            }
        }
        catch (Exception e){
            log("CameraUploadsService Exception: " + e.getMessage() + "_" + e.getStackTrace());
        }

        return false;
    }

    private void startCameraUploads(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);

            mBuilder = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

            mBuilder
                    .setSmallIcon(R.drawable.ic_stat_camera_sync)
                    .setOngoing(false)
                    .setContentTitle(getString(R.string.section_photo_sync))
//                    .setSubText(getString(R.string.section_photo_sync))
                    .setContentText(getString(R.string.settings_camera_notif_title))
                    .setOnlyAlertOnce(true);

            Notification notification = mBuilder.build();

            isForeground = true;
            startForeground(notificationId, notification);

            try{
                startUploads();
            } catch (Exception e) {}

//            stopForeground(true);
//            if (mNotificationManager != null){
//                mNotificationManager.cancel(notificationId);
//            }
//            jobFinished(globalParams, true);
        }
    }

    private void startUploads(){
        log("startUploads()");
        if(!wl.isHeld()){
            wl.acquire();
        }
        if(!lock.isHeld()){
            lock.acquire();
        }

        if (!isExternalSDCard) {
            log("if (!isExternalSDCard)");
            String projection[] = {MediaStore.MediaColumns.DATA,
                    //MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.DATE_ADDED,
                    MediaStore.MediaColumns.DATE_MODIFIED};

            String selectionCamera = null;
            String selectionSecondary = null;
            String[] selectionArgs = null;

            prefs = dbH.getPreferences();

            if (prefs != null){
                log("if (prefs != null)");
                if (prefs.getCamSyncTimeStamp() != null){
                    log("if (prefs.getCamSyncTimeStamp() != null)");
                    long camSyncTimeStamp = Long.parseLong(prefs.getCamSyncTimeStamp());
                    selectionCamera = "(" + MediaStore.MediaColumns.DATE_MODIFIED + "*1000) > " + camSyncTimeStamp + " OR " + "(" + MediaStore.MediaColumns.DATE_ADDED + "*1000) > " + camSyncTimeStamp;
                    log("SELECTION: " + selectionCamera);
                }
            }

            String order = MediaStore.MediaColumns.DATE_MODIFIED + " ASC";

            ArrayList<Uri> uris = new ArrayList<Uri>();
            if (prefs.getCamSyncFileUpload() == null){
                log("if (prefs.getCamSyncFileUpload() == null)");
                dbH.setCamSyncFileUpload(MegaPreferences.ONLY_PHOTOS);
                uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            }
            else{
                log("if (prefs.getCamSyncFileUpload() != null)");
                switch(Integer.parseInt(prefs.getCamSyncFileUpload())){
                    case MegaPreferences.ONLY_PHOTOS:{
                        log("case MegaPreferences.ONLY_PHOTOS:");
                        uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                        break;
                    }
                    case MegaPreferences.ONLY_VIDEOS:{
                        log("case MegaPreferences.ONLY_VIDEOS:");
                        uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                        uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI);
                        break;
                    }
                    case MegaPreferences.PHOTOS_AND_VIDEOS:{
                        log("case MegaPreferences.PHOTOS_AND_VIDEOS:");
                        uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                        uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                        uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI);
                        break;
                    }
                }
            }

            for(int i=0; i<uris.size(); i++){
                log("for(int i=0; i<uris.size(); i++)");
                Cursor cursorCamera = app.getContentResolver().query(uris.get(i), projection, selectionCamera, selectionArgs, order);
                if (cursorCamera != null){
                    log("if (cursorCamera != null)");
                    int dataColumn = cursorCamera.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                    int timestampColumn = 0;
                    if(cursorCamera.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)==0){
                        log("if(cursorCamera.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED) == 0)");
                        timestampColumn = cursorCamera.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);
                    }
                    else
                    {
                        log("if(cursorCamera.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED) != 0)");
                        timestampColumn = cursorCamera.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);
                    }

                    while(cursorCamera.moveToNext()){

                        Media media = new Media();
                        media.filePath = cursorCamera.getString(dataColumn);
                        //			        log("Tipo de fichero:--------------------------: "+media.filePath);
                        media.timestamp = cursorCamera.getLong(timestampColumn) * 1000;

                        log("while(cursorCamera.moveToNext()) - media.filePath: " + media.filePath + "_localPath: " + localPath);

                        //Check files of the Camera Uploads
                        if (checkFile(media,localPath)){
                            log("if (checkFile(media," + localPath + "))");
                            cameraFiles.add(media);
                            log("Camera Files added: "+media.filePath);
                        }
                    }
                }

                //Secondary Media Folder
                if(secondaryEnabled){
                    log("if(secondaryEnabled)");
                    Cursor cursorSecondary = app.getContentResolver().query(uris.get(i), projection, selectionSecondary, selectionArgs, order);
                    if (cursorSecondary != null){
                        try {
                            log("SecondaryEnabled en initsync COUNT: "+cursorSecondary.getCount());
                            int dataColumn = cursorSecondary.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                            int timestampColumn = 0;
                            if(cursorCamera.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)==0){
                                timestampColumn = cursorSecondary.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);
                            }
                            else
                            {
                                timestampColumn = cursorSecondary.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);
                            }
                            while(cursorSecondary.moveToNext()){

                                Media media = new Media();
                                media.filePath = cursorSecondary.getString(dataColumn);
                                media.timestamp = cursorSecondary.getLong(timestampColumn) * 1000;
                                log("Check: " + media.filePath + " in localPath: " + localPathSecondary);
                                //Check files of Secondary Media Folder
                                if (checkFile(media, localPathSecondary)) {
                                    mediaFilesSecondary.add(media);
                                    log("-----SECONDARY MEDIA Files added: " + media.filePath + " in localPath: " + localPathSecondary);
                                }
                            }
                        }
                        catch (Exception e){
                            log("Exception cursorSecondary:" + e.getMessage() + "____" + e.getStackTrace());
                        }
                    }
                }
            }

            cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
            if(cameraUploadNode == null){
                log("ERROR: cameraUploadNode == null");
                //			showSyncError(R.string.settings_camera_notif_error_no_folder);
                finish();
                return;
            }

            totalToUpload = cameraFiles.size();
            totalUploaded=0;
            totalSizeUploaded=0;

            totalSizeToUpload = 0;
            Iterator<Media> itCF = cameraFiles.iterator();
            while (itCF.hasNext()){
                Media m = itCF.next();
                File f = new File(m.filePath);
                totalSizeToUpload = totalSizeToUpload + f.length();
                log("m.filePath -> " + m.filePath);
            }

            Iterator<Media> itmFS = mediaFilesSecondary.iterator();
            while (itmFS.hasNext()){
                Media m = itmFS.next();
                File f = new File(m.filePath);
                totalSizeToUpload = totalSizeToUpload + f.length();
                log("mS.filePath -> " + m.filePath);
            }

            uploadNext();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else{
            log("isExternalSDCard");
            DocumentFile pickedDir = null;
            if (prefs != null){
                if (prefs.getUriExternalSDCard() != null){
                    String uriString = prefs.getUriExternalSDCard();
                    Uri uri = Uri.parse(uriString);
                    pickedDir = DocumentFile.fromTreeUri(getApplicationContext(), uri);
                    log("PICKEDDIR: " + pickedDir.getName());
                    DocumentFile[] files = pickedDir.listFiles();
                    if(files!=null){
                        log("The number of files is: "+files.length);
                    }
                    else{
                        log("files is NULL!");
                    }
                    ArrayList<DocumentFile> auxCameraFilesExternalSDCard = new ArrayList<DocumentFile>();
                    for (int i=0;i<files.length;i++){
                        log("Name to check: "+ files[i].getName());
                        switch(Integer.parseInt(prefs.getCamSyncFileUpload())){
                            case MegaPreferences.ONLY_PHOTOS:{
                                String fileType = files[i].getType();
                                if (fileType != null){
                                    if (fileType.startsWith("image/")){
                                        auxCameraFilesExternalSDCard.add(files[i]);
                                    }
                                    else{
                                        log("No image");
                                    }
                                }
                                else{
                                    log("File is null");
                                }
                                break;
                            }
                            case MegaPreferences.ONLY_VIDEOS:{
                                String fileType = files[i].getType();
                                String fileName = files[i].getName();
                                if (fileType != null){
                                    if (fileName != null){
                                        if (fileType.startsWith("video/") || (fileName.endsWith(".mkv"))) {
                                            auxCameraFilesExternalSDCard.add(files[i]);
                                        }
                                    }
                                }
                                break;
                            }
                            case MegaPreferences.PHOTOS_AND_VIDEOS:{
                                String fileType = files[i].getType();
                                String fileName = files[i].getName();
                                if (fileType != null){
                                    if (fileName != null) {
                                        if (fileType.startsWith("image/") || fileType.startsWith("video/") || (fileName.endsWith(".mkv"))) {
                                            auxCameraFilesExternalSDCard.add(files[i]);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }

                    log("auxCameraFilesExternalSDCard.size() = " + auxCameraFilesExternalSDCard.size());
                    int j=0;
                    for (int i=0;i<auxCameraFilesExternalSDCard.size();i++){
                        if (cameraUploadNode == null){
                            log("Camera Upload Node null");
                            cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
                        }
                        if (cameraUploadNode != null){
                            log("Camera Upload Node not null");
                            if (megaApi.getChildNode(cameraUploadNode, auxCameraFilesExternalSDCard.get(i).getName()) == null){
                                cameraFilesExternalSDCardList.add(j, auxCameraFilesExternalSDCard.get(i));
                                log("FILE ADDED: " + auxCameraFilesExternalSDCard.get(i).getName());
                                j++;
                            }
                        }
                        else{
                            log("Camera Upload null");
                        }
                    }


//					Collections.sort(auxCameraFilesExternalSDCard, new MediaComparator());

//					for (int i=0;i<auxCameraFilesExternalSDCard.size();i++){
//						long camSyncTimeStamp = Long.parseLong(prefs.getCamSyncTimeStamp());
//						log("CAMSYNCTIMESTAMP: " + camSyncTimeStamp + "___" + auxCameraFilesExternalSDCard.get(i).lastModified());
//						if (auxCameraFilesExternalSDCard.get(i).lastModified() > camSyncTimeStamp){
//							int j = 0;
//							for ( ; j<cameraFilesExternalSDCardList.size(); j++){
//								if (auxCameraFilesExternalSDCard.get(i).lastModified() < cameraFilesExternalSDCardList.get(j).lastModified()){
//									break;
//								}
//							}
//							cameraFilesExternalSDCardList.add(j, auxCameraFilesExternalSDCard.get(i));
//						}
////						if (cameraFilesExternalSDCardList.size() == 25){
////							break;
////						}
//						log("NAME: " + auxCameraFilesExternalSDCard.get(i).getName() + "_LAST_ " + auxCameraFilesExternalSDCard.get(i).lastModified());
//					}

                    for (int i=0;i<cameraFilesExternalSDCardList.size();i++){
                        log("ORD_NAME: " + cameraFilesExternalSDCardList.get(i).getName() + "____" + cameraFilesExternalSDCardList.get(i).lastModified());
                    }

                    cameraFilesExternalSDCardQueue = new LinkedList<DocumentFile>(cameraFilesExternalSDCardList);

                    totalToUpload = cameraFilesExternalSDCardQueue.size();
                    totalUploaded=0;
                    totalSizeUploaded=0;

                    totalSizeToUpload = 0;
                    Iterator<DocumentFile> itCF = cameraFilesExternalSDCardQueue.iterator();
                    while (itCF.hasNext()){
                        DocumentFile dF = itCF.next();
                        totalSizeToUpload = totalSizeToUpload + dF.length();
                    }
                    uploadNextSDCard();
                }
                else{
                    finish();
                }
            }
            else{
                finish();
            }
            //TODO: The secondary media folder has to be here implemented also (or separate in two pieces - isExternal !isExternal)
        }
    }

    public void uploadNext(){
        log("uploadNext()");

        if (cameraUploadNode == null){
            log("if (cameraUploadNode == null)");
            cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (cameraFiles.size() > 0){
            log("if (cameraFiles.size() > 0)");
            uploadNextImage();
        }
        else if(mediaFilesSecondary.size() > 0){
            log("else if(mediaFilesSecondary.size() > 0)");
            uploadNextMediaFile();
        }
        else{
            log("else");
            onQueueComplete(true, totalUploaded);
            finish();
        }
    }

    private void onQueueComplete(boolean success, int totalUploaded) {
        log("onQueueComplete");
        log("Stopping foreground!");
        log("stopping service! success: " + successCount + " total: " + totalToUpload);
        megaApi.resetTotalUploads();

        if((lock != null) && (lock.isHeld()))
            try{ lock.release(); } catch(Exception ex) {}
        if((wl != null) && (wl.isHeld()))
            try{ wl.release(); } catch(Exception ex) {}

        //Sleep so the SDK keeps alive
        //TODO: Must create a method to know if the SDK is waiting for any operation
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}

        if (totalUploaded == 0) {
            log("TotalUploaded == 0");
        } else {
            log("stopping service!");
            if (success){
                if (totalSizeUploaded != 0){
//					showCompleteSuccessNotification();
                }
            }
        }

        log("stopping service!!!!!!!!!!:::::::::::::::!!!!!!!!!!!!");
        isForeground = false;
        finish();
        stopForeground(true);
        if (mNotificationManager != null){
            mNotificationManager.cancel(notificationId);
        }
        jobFinished(globalParams, true);
    }

    void uploadNextMediaFile(){

        //Check problem with secondary

        totalUploaded++;
        log("uploadNextMediaFile: "+totalUploaded +" of "+totalToUpload);

        int result = shouldRun();
        if (result != 0){
            return;
        }

        if (cameraUploadNode == null){
            cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
        }

        if (mediaFilesSecondary.size() > 0){
            final Media mediaSecondary = mediaFilesSecondary.poll();

            File file = new File(mediaSecondary.filePath);
            if(!file.exists()){
                uploadNext();
            }

            log("mediaSecondary.filePath: "+mediaSecondary.filePath);
            String localFingerPrint = megaApi.getFingerprint(mediaSecondary.filePath);

            MegaNode nodeExists = null;
            //Source file
            File sourceFile = new File(mediaSecondary.filePath);

            nodeExists = megaApi.getNodeByFingerprint(localFingerPrint, secondaryUploadNode);

            if(nodeExists == null)
            {
                log("nodeExists1==null");
                //Check if the file is already uploaded in the correct folder but without a fingerprint
                int photoIndex = 0;
                MegaNode possibleNode = null;
                String photoFinalName;
                do {
                    //Create the final name taking into account the
                    if(Boolean.parseBoolean(prefs.getKeepFileNames())){
                        //Keep the file names as device

                        photoFinalName = mediaSecondary.filePath;
                        log("Keep the secondary name: "+photoFinalName);
                    }
                    else{
                        photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);
                        log("CHANGE the secondary name: "+photoFinalName);
                    }

                    //Iterate between all files with the correct target name

                    possibleNode = megaApi.getChildNode(secondaryUploadNode, photoFinalName);
                    // If the file matches name, mtime and size, and doesn't have a fingerprint,
                    // => we consider that it's the correct one
                    if(possibleNode != null &&
                            sourceFile.length() == possibleNode.getSize() &&
                            megaApi.getFingerprint(possibleNode) == null)
                    {
                        log("nodeExists = possibleNode: "+possibleNode);
                        nodeExists = possibleNode;
                        break;
                    }

                    //Continue iterating
                    photoIndex++;
                } while(possibleNode != null);

                if(nodeExists == null)
                {
                    // If the file wasn't found by fingerprint nor in the destination folder,
                    // take a look in the folder from v1
                    SharedPreferences prefs = this.getSharedPreferences("prefs_main.xml", 0);
                    if(prefs != null)
                    {
                        String handle = prefs.getString("camera_sync_folder_hash", null);
                        if(handle != null)
                        {
                            MegaNode prevFolder = megaApi.getNodeByHandle(MegaApiAndroid.base64ToHandle(handle));
                            if(prevFolder != null)
                            {
                                // If we reach this code, the app is an updated v1 and the previously selected
                                // folder still exists

                                // If the file matches name, mtime and size, and doesn't have a fingerprint,
                                // => we consider that it's the correct one
                                possibleNode = megaApi.getChildNode(prevFolder, sourceFile.getName());
                                if(possibleNode != null &&
                                        sourceFile.length() == possibleNode.getSize() &&
                                        megaApi.getFingerprint(possibleNode) == null)
                                {
                                    nodeExists = possibleNode;
                                }
                            }
                        }
                    }
                }
            }

            if (nodeExists == null){
                log("SECONDARY MEDIA: SUBIR EL FICHERO: " + mediaSecondary.filePath);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(mediaSecondary.timestamp);
                log("YYYY-MM-DD HH.MM.SS -- " + cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.HOUR_OF_DAY) + "." + cal.get(Calendar.MINUTE) + "." + cal.get(Calendar.SECOND));
                boolean photoAlreadyExists = false;
                ArrayList<MegaNode> nL = megaApi.getChildren(secondaryUploadNode, MegaApiJava.ORDER_ALPHABETICAL_ASC);
                for (int i=0;i<nL.size();i++){
                    if ((nL.get(i).getName().compareTo(Util.getPhotoSyncName(mediaSecondary.timestamp, mediaSecondary.filePath)) == 0) && (nL.get(i).getSize() == file.length())){
                        photoAlreadyExists = true;
                    }
                }

                if (!photoAlreadyExists){

                    if(Boolean.parseBoolean(prefs.getKeepFileNames())){
                        //Keep the file names as device
                        currentTimeStamp = mediaSecondary.timestamp;
                        megaApi.startUpload(file.getAbsolutePath(), secondaryUploadNode, file.getName(), this);
                        log("NOOOT CHANGED!!!! MediaFinalName: " + file.getName());
                    }
                    else{
                        int photoIndex = 0;
                        String photoFinalName = null;
                        do {
                            photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);
                            photoIndex++;
                        }while(megaApi.getChildNode(secondaryUploadNode, photoFinalName) != null);

                        log("photoFinalName: " + photoFinalName + "______" + photoIndex);
                        currentTimeStamp = mediaSecondary.timestamp;

                        megaApi.startUpload(file.getAbsolutePath(), secondaryUploadNode, photoFinalName, this);
                        log("CHANGED!!!! MediaFinalName: " + photoFinalName + "______" + photoIndex);
                    }
                }
                else{
                    currentTimeStamp = mediaSecondary.timestamp;
                    dbH.setSecSyncTimeStamp(currentTimeStamp);
                    File f = new File(mediaSecondary.filePath);
                    totalSizeUploaded += f.length();
                    uploadNext();
                }
            }
            else{
                log("nodeExists=!null");

                if(megaApi.getParentNode(nodeExists)!=null){
                    if (megaApi.getParentNode(nodeExists).getHandle() != secondaryUploadHandle){

                        if(Boolean.parseBoolean(prefs.getKeepFileNames())){
                            //Keep the file names as device
                            currentTimeStamp = mediaSecondary.timestamp;
                            megaApi.copyNode(nodeExists, secondaryUploadNode, file.getName(), this);
                            log("NOOOT CHANGED!!!! MediaFinalName: " + file.getName());
                        }
                        else{
                            int photoIndex = 0;
                            String photoFinalName = null;
                            do {
                                photoFinalName = Util.getPhotoSyncNameWithIndex(mediaSecondary.timestamp, mediaSecondary.filePath, photoIndex);
                                photoIndex++;
                            }while(megaApi.getChildNode(secondaryUploadNode, photoFinalName) != null);

                            currentTimeStamp = mediaSecondary.timestamp;

                            megaApi.copyNode(nodeExists, secondaryUploadNode, photoFinalName, this);
                            log("CHANGED!!!! SecondaryFinalName: " + photoFinalName + "______" + photoIndex);
                        }
                    }
                    else{
                        if(!(Boolean.parseBoolean(prefs.getKeepFileNames()))){
                            //Change the file names as device
                            log("Call Look for Rename Task");
                            final MegaNode existingNode = nodeExists;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    new LookForRenameTask(mediaSecondary, secondaryUploadNode).rename(existingNode);
                                }
                            });
                        }
                    }
                }
                else{
                    //What if the parent node is null
                    log("This is an error!!!");
                }
            }
        }
        else{
            uploadNext();
        }
    }

    void uploadNextImage(){
        totalUploaded++;
        log("uploadNextImage: "+totalUploaded +" of "+totalToUpload);

        int result = shouldRun();
        if (result != 0){
            return;
        }

        if (cameraUploadNode == null){
            log("if (cameraUploadNode == null)");
            cameraUploadNode = megaApi.getNodeByHandle(cameraUploadHandle);
        }

        if (cameraFiles.size() > 0){
            log("if (cameraFiles.size() > 0)");
            final Media media = cameraFiles.poll();

            File file = new File(media.filePath);
            if(!file.exists()){
                uploadNext();
            }

            String localFingerPrint = megaApi.getFingerprint(media.filePath);

            MegaNode nodeExists = null;
            //Source file
            File sourceFile = new File(media.filePath);

            nodeExists = megaApi.getNodeByFingerprint(localFingerPrint, cameraUploadNode);
            if(nodeExists == null)
            {
                log("if(nodeExists == null)");
                //Check if the file is already uploaded in the correct folder but without a fingerprint
                int photoIndex = 0;
                MegaNode possibleNode = null;
                String photoFinalName;
                do {
                    //Iterate between all files with the correct target name

                    //Create the final name taking into account the
                    if(Boolean.parseBoolean(prefs.getKeepFileNames())){
                        //Keep the file names as device

                        photoFinalName = media.filePath;
                        log("Keep the camera file name: "+photoFinalName);
                    }
                    else{
                        photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
                        log("CHANGE the camera file name: "+photoFinalName);
                    }

                    possibleNode = megaApi.getChildNode(cameraUploadNode, photoFinalName);

                    // If the file matches name, mtime and size, and doesn't have a fingerprint,
                    // => we consider that it's the correct one
                    if(possibleNode != null &&
                            sourceFile.length() == possibleNode.getSize() &&
                            megaApi.getFingerprint(possibleNode) == null)
                    {
                        nodeExists = possibleNode;
                        log("nodeExists = possibleNode;");
                        break;
                    }

                    //Continue iterating
                    photoIndex++;
                } while(possibleNode != null);

                if(nodeExists == null)
                {
                    log("if(nodeExists == null)");
                    // If the file wasn't found by fingerprint nor in the destination folder,
                    // take a look in the folder from v1
                    SharedPreferences prefs = this.getSharedPreferences("prefs_main.xml", 0);
                    if(prefs != null)
                    {
                        String handle = prefs.getString("camera_sync_folder_hash", null);
                        if(handle != null)
                        {
                            MegaNode prevFolder = megaApi.getNodeByHandle(MegaApiAndroid.base64ToHandle(handle));
                            if(prevFolder != null)
                            {
                                // If we reach this code, the app is an updated v1 and the previously selected
                                // folder still exists

                                // If the file matches name, mtime and size, and doesn't have a fingerprint,
                                // => we consider that it's the correct one
                                possibleNode = megaApi.getChildNode(prevFolder, sourceFile.getName());
                                if(possibleNode != null &&
                                        sourceFile.length() == possibleNode.getSize() &&
                                        megaApi.getFingerprint(possibleNode) == null)
                                {
                                    nodeExists = possibleNode;
                                }
                            }
                        }
                    }
                }
            }

            if (nodeExists == null){
                log("UPLOAD THE FILE: " + media.filePath);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(media.timestamp);
                log("YYYY-MM-DD HH.MM.SS -- " + cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.HOUR_OF_DAY) + "." + cal.get(Calendar.MINUTE) + "." + cal.get(Calendar.SECOND));
                boolean photoAlreadyExists = false;
                ArrayList<MegaNode> nL = megaApi.getChildren(cameraUploadNode, MegaApiJava.ORDER_ALPHABETICAL_ASC);
                for (int i=0;i<nL.size();i++){
                    if ((nL.get(i).getName().compareTo(Util.getPhotoSyncName(media.timestamp, media.filePath)) == 0) && (nL.get(i).getSize() == file.length())){
                        photoAlreadyExists = true;
                    }
                }

                if (!photoAlreadyExists){
                    log("if (!photoAlreadyExists)");

                    if(Boolean.parseBoolean(prefs.getKeepFileNames())){
                        //Keep the file names as device
                        currentTimeStamp = media.timestamp;
                        megaApi.startUpload(file.getAbsolutePath(), cameraUploadNode, file.getName(), this);
                        log("NOOOT CHANGED!!!! MediaFinalName: " + file.getName());
                    }
                    else{
                        int photoIndex = 0;
                        String photoFinalName = null;
                        do {
                            photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
                            photoIndex++;
                        }while(megaApi.getChildNode(cameraUploadNode, photoFinalName) != null);

                        log("photoFinalName: " + photoFinalName + "______" + photoIndex);
                        currentTimeStamp = media.timestamp;

                        megaApi.startUpload(file.getAbsolutePath(), cameraUploadNode, photoFinalName, this);
                        log("CHANGEEEEEEE: filePath: "+file.getAbsolutePath()+" Change finalName: "+photoFinalName);
                    }
                }
                else{
                    log("if (photoAlreadyExists)");
                    currentTimeStamp = media.timestamp;
                    dbH.setCamSyncTimeStamp(currentTimeStamp);
                    File f = new File(media.filePath);
                    totalSizeUploaded += f.length();
                    uploadNext();
                }
            }
            else{
                log("NODE EXISTS: " + megaApi.getParentNode(nodeExists).getName() + "___" + nodeExists.getName());
                if (megaApi.getParentNode(nodeExists).getHandle() != cameraUploadHandle){

                    if(Boolean.parseBoolean(prefs.getKeepFileNames())){
                        //Keep the file names as device
                        currentTimeStamp = media.timestamp;
                        megaApi.copyNode(nodeExists, cameraUploadNode, file.getName(), this);
                        log("NOOOT CHANGED!!!! MediaFinalName: " + file.getName());
                    }
                    else{
                        int photoIndex = 0;
                        String photoFinalName = null;
                        do {
                            photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
                            photoIndex++;
                        }while(megaApi.getChildNode(cameraUploadNode, photoFinalName) != null);

                        log("photoFinalName: " + photoFinalName + "______" + photoIndex);
                        currentTimeStamp = media.timestamp;
                        megaApi.copyNode(nodeExists, cameraUploadNode, photoFinalName, this);
                        log("CHANGED!!!! MediaFinalName: " + file.getName());
                    }
                }
                else{
                    if(!(Boolean.parseBoolean(prefs.getKeepFileNames()))){
                        //Change the file names as device
                        log("Call Look for Rename Task");
                        final MegaNode existingNode = nodeExists;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                new LookForRenameTask(media,cameraUploadNode).rename(existingNode);
                            }
                        });
                    }
                    else{
                        currentTimeStamp = media.timestamp;
                        dbH.setCamSyncTimeStamp(currentTimeStamp);
                        uploadNext();
                    }

                }
            }
        }
        else{
            uploadNext();
        }
    }

    private class LookForRenameTask{
        Media media;
        String photoFinalName;
        MegaNode uploadNode;

        public LookForRenameTask(Media media, MegaNode uploadNode) {
            this.media = media;
            this.uploadNode = uploadNode;
        }

        protected Boolean rename(MegaNode nodeExists) {

            File file = new File(media.filePath);
            log("RENOMBRAR EL FICHERO: " + media.filePath);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(media.timestamp);
            log("YYYY-MM-DD HH.MM.SS -- " + cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH) + " " + cal.get(Calendar.HOUR_OF_DAY) + "." + cal.get(Calendar.MINUTE) + "." + cal.get(Calendar.SECOND));
            boolean photoAlreadyExists = false;
            ArrayList<MegaNode> nL = megaApi.getChildren(uploadNode, MegaApiJava.ORDER_ALPHABETICAL_ASC);
            for (int i=0;i<nL.size();i++){
                if ((nL.get(i).getName().compareTo(Util.getPhotoSyncName(media.timestamp, media.filePath)) == 0) && (nL.get(i).getSize() == file.length())){
                    photoAlreadyExists = true;
                }
            }

            if (!photoAlreadyExists){
                int photoIndex = 0;
                this.photoFinalName = null;
                do {
                    photoFinalName = Util.getPhotoSyncNameWithIndex(media.timestamp, media.filePath, photoIndex);
                    photoIndex++;
                }while(megaApi.getChildNode(uploadNode, photoFinalName) != null);

                log("photoFinalName: " + photoFinalName + "______" + photoIndex);
                currentTimeStamp = media.timestamp;

                megaApi.renameNode(nodeExists, photoFinalName, cameraUploadsService);
                log("RENAMED!!!! MediaFinalName: " + photoFinalName + "______" + photoIndex);

                return true;
            }
            else{
                currentTimeStamp = media.timestamp;
//				long parentHandle = megaApi.getParentNode(uploadNode).getHandle();
                if(uploadNode.getHandle() == secondaryUploadHandle){
                    log("renameTask: Update SECONDARY Sync TimeStamp, parentHandle= "+uploadNode.getHandle()+" secondaryHandle: "+secondaryUploadHandle);
                    dbH.setSecSyncTimeStamp(currentTimeStamp);
                }
                else{
                    log("renameTask: Update Camera Sync TimeStamp, parentHandle= "+uploadNode.getHandle()+" cameraHandle: "+cameraUploadHandle);
                    dbH.setCamSyncTimeStamp(currentTimeStamp);
                }

                log("Upoad NODE: "+uploadNode.getName());

                File f = new File(media.filePath);
                totalSizeUploaded += f.length();

                uploadNext();
                return false;
            }
        }
    }

    @SuppressLint("NewApi")
    public void uploadNextSDCard(){
        log("uploadNextSDCard()");
    }

    private boolean checkFile(Media media, String path){

        if (media.filePath != null){
            if (path != null){
                if (path.compareTo("") != 0){
                    if (media.filePath.startsWith(path)){
                        Time t = new Time(Time.getCurrentTimezone());
                        t.setToNow();
                        long timeSpent = t.toMillis(true) - media.timestamp;
                        if (timeSpent > ((5 * 60 * 1000)-1)){
                            return true;
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private int shouldRun() {
        log("shouldRun()");

        if (!Util.isOnline(this)){
            log("Not online");
            finish();
            return 1;
        }

        prefs = dbH.getPreferences();
        if (prefs == null){
            log("Not defined, so not enabled");
            finish();
            return 2;
        }
        else{
            if (prefs.getCamSyncEnabled() == null){
                log("Not defined, so not enabled");
                finish();
                return 3;
            }
            else{
                if (!Boolean.parseBoolean(prefs.getCamSyncEnabled())){
                    log("Camera Sync Not enabled");
                    finish();
                    return 4;
                }
                else{
                    localPath = prefs.getCamSyncLocalPath();
                    if (localPath == null){
                        log("Not defined, so not enabled");
                        finish();
                        return 5;
                    }
                    else{
                        if ("".compareTo(localPath) == 0){
                            log("Not defined, so not enabled");
                            finish();
                            return 6;

                        }
                        else{
                            log("Localpath: " + localPath);
                        }
                    }

                    boolean isWifi = Util.isOnWifi(this);
                    if (prefs.getCamSyncWifi() == null){
                        if (!isWifi){
                            log("no wifi...");
                            finish();
                            return 7;
                        }
                    }
                    else{
                        if (Boolean.parseBoolean(prefs.getCamSyncWifi())){
                            if (!isWifi){
                                log("no wifi...");
                                finish();
                                return 8;
                            }
                        }
                    }

                    boolean isCharging = Util.isCharging(this);
                    if (prefs.getCamSyncCharging() == null){
                        if (!isCharging){
                            log("not charging...");
                            finish();
                            return 9;
                        }
                    }
                    else{
                        if (Boolean.parseBoolean(prefs.getCamSyncCharging())){
                            if (!isCharging){
                                log("not charging...");
                                finish();
                                return 10;
                            }
                        }
                    }

                    if (prefs.getCameraFolderExternalSDCard() != null){
                        isExternalSDCard = Boolean.parseBoolean(prefs.getCameraFolderExternalSDCard());
                    }

                    UserCredentials credentials = dbH.getCredentials();
                    if (credentials == null){
                        log("There are not user credentials");
                        finish();
                        return 11;
                    }

                    gSession = credentials.getSession();

                    if (megaApi.getRootNode() == null){
                        log("RootNode = null");

                        running = true;

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (megaApi.getRootNode() == null) {
                                    isLoggingIn = MegaApplication.isLoggingIn();
                                    if (!isLoggingIn) {

                                        isLoggingIn = true;
                                        MegaApplication.setLoggingIn(isLoggingIn);

                                        if (Util.isChatEnabled()) {
                                            log("shouldRun: Chat is ENABLED");
                                            if (megaChatApi == null) {
                                                megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
                                            }

                                            int ret = megaChatApi.getInitState();

                                            if (ret == MegaChatApi.INIT_NOT_DONE || ret == MegaChatApi.INIT_ERROR) {
                                                ret = megaChatApi.init(gSession);
                                                log("shouldRun: result of init ---> " + ret);
                                                chatSettings = dbH.getChatSettings();
                                                if (ret == MegaChatApi.INIT_NO_CACHE) {
                                                    log("shouldRun: condition ret == MegaChatApi.INIT_NO_CACHE");
                                                    megaChatApi.enableGroupChatCalls(true);

                                                } else if (ret == MegaChatApi.INIT_ERROR) {
                                                    log("shouldRun: condition ret == MegaChatApi.INIT_ERROR");
                                                    if (chatSettings == null) {
                                                        log("1 - shouldRun: ERROR----> Switch OFF chat");
                                                        chatSettings = new ChatSettings();
                                                        chatSettings.setEnabled(false + "");
                                                        dbH.setChatSettings(chatSettings);
                                                    } else {
                                                        log("2 - shouldRun: ERROR----> Switch OFF chat");
                                                        dbH.setEnabledChat(false + "");
                                                    }
                                                    megaChatApi.logout(cameraUploadsService);
                                                } else {
                                                    log("shouldRun: Chat correctly initialized");
                                                    megaChatApi.enableGroupChatCalls(true);
                                                }
                                            }
                                        }

                                        megaApi.fastLogin(gSession, cameraUploadsService);
                                    } else {
                                        log("Another login is processing");
                                    }
                                }
                                else{
                                    log("postDelayed RootNode != null");

                                    int r = runLoggedIn();
                                    log("shouldRunAfterLoginDelayed -> " + r);
                                    if (r == 0) {
                                        startCameraUploads();
                                    }
                                }
                            }
                        }, 30000);

                        return LOGIN_IN;
                    }

                    log("RootNode != null");
                    int r = runLoggedIn();
                    return r;
                }
            }
        }
    }

    private int runLoggedIn(){

        if (prefs.getCamSyncHandle() == null){
            log("if (prefs.getCamSyncHandle() == null)");
            cameraUploadHandle = -1;
        }
        else{
            log("if (prefs.getCamSyncHandle() != null)");
            cameraUploadHandle = Long.parseLong(prefs.getCamSyncHandle());
        }

        if (prefs.getSecondaryMediaFolderEnabled() == null){
            log("if (prefs.getSecondaryMediaFolderEnabled() == null)");
            dbH.setSecondaryUploadEnabled(false);
            log("Not defined, so not enabled");
            secondaryEnabled=false;
        }
        else {
            log("if (prefs.getSecondaryMediaFolderEnabled() != null)");
            if (!Boolean.parseBoolean(prefs.getSecondaryMediaFolderEnabled())) {
                log("Not enabled Secondary");
                secondaryEnabled = false;
            } else {
                secondaryEnabled = true;
                localPathSecondary = prefs.getLocalPathSecondaryFolder();
            }
        }

        ArrayList<MegaNode> nl = megaApi.getChildren(megaApi.getRootNode());
        if (cameraUploadHandle == -1){
            log("Find the Camera Uploads folder of the old PhotoSync");
            for (int i=0;i<nl.size();i++){
                if ((CAMERA_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
                    cameraUploadHandle = nl.get(i).getHandle();
                    dbH.setCamSyncHandle(cameraUploadHandle);
                }
                else if((PHOTO_SYNC.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
                    cameraUploadHandle = nl.get(i).getHandle();
                    dbH.setCamSyncHandle(cameraUploadHandle);
                    megaApi.renameNode(nl.get(i), CAMERA_UPLOADS, this);
                }
            }
            
            log("If not Camera Uploads nor Photosync");
            if (cameraUploadHandle == -1){
                log("must create the folder");
                megaApi.createFolder(CAMERA_UPLOADS, megaApi.getRootNode(), this);
                return 13;
            }
        }
        else{
            MegaNode n = megaApi.getNodeByHandle(cameraUploadHandle);
            if(n==null){
                log("Node with cameraUploadHandle is not NULL");
                cameraUploadHandle = -1;
                for (int i=0;i<nl.size();i++){
                    if ((CAMERA_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
                        cameraUploadHandle = nl.get(i).getHandle();
                        dbH.setCamSyncHandle(cameraUploadHandle);
                    }
                    else if((PHOTO_SYNC.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
                        cameraUploadHandle = nl.get(i).getHandle();
                        dbH.setCamSyncHandle(cameraUploadHandle);
                        megaApi.renameNode(nl.get(i), CAMERA_UPLOADS, this);
                    }
                }
                
                if (cameraUploadHandle == -1){
                    log("If not Camera Uploads nor Photosync--- must create the folder");
                    megaApi.createFolder(CAMERA_UPLOADS, megaApi.getRootNode(), this);
                    return 14;
                }
            }
            else{
                log("Sync Folder " + cameraUploadHandle + " Node: "+n.getName());
            }
        }

        if(secondaryEnabled){
            log("the secondary uploads are enabled");
            String temp = prefs.getMegaHandleSecondaryFolder();
            if (temp != null){
                if (temp.compareTo("") != 0){
                    secondaryUploadHandle= Long.parseLong(prefs.getMegaHandleSecondaryFolder());
                    if (secondaryUploadHandle == -1){
                        for (int i=0;i<nl.size();i++){
                            if ((SECONDARY_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
                                secondaryUploadHandle = nl.get(i).getHandle();
                                dbH.setSecondaryFolderHandle(secondaryUploadHandle);
                            }
                        }
                        
                        //If not "Media Uploads"
                        if (secondaryUploadHandle == -1){
                            log("must create the secondary folder");
                            megaApi.createFolder(SECONDARY_UPLOADS, megaApi.getRootNode(), this);
                            return 15;
                        }
                    }
                    else{
                        log("SecondaryUploadHandle: "+secondaryUploadHandle);
                        MegaNode n = megaApi.getNodeByHandle(secondaryUploadHandle);
                        //If ERROR with the handler (the node may no longer exist): Create the folder Media Uploads
                        if(n==null){
                            secondaryUploadHandle=-1;
                            log("The secondary media folder may not longer exists");
                            for (int i=0;i<nl.size();i++){
                                if ((SECONDARY_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
                                    secondaryUploadHandle = nl.get(i).getHandle();
                                    dbH.setSecondaryFolderHandle(secondaryUploadHandle);
                                }
                            }
                            
                            //If not "Media Uploads"
                            if (secondaryUploadHandle == -1){
                                log("must create the folder");
                                megaApi.createFolder(SECONDARY_UPLOADS, megaApi.getRootNode(), this);
                                return 16;
                            }
                        }
                        else{
                            log("Secondary Folder " + secondaryUploadHandle + " Node: "+n.getName());
                            secondaryUploadNode=megaApi.getNodeByHandle(secondaryUploadHandle);
                        }
                    }
                }
                else{
                    //If empty string as SecondaryHandle
                    secondaryUploadHandle=-1;
                    for (int i=0;i<nl.size();i++){
                        if ((SECONDARY_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
                            secondaryUploadHandle = nl.get(i).getHandle();
                            dbH.setSecondaryFolderHandle(secondaryUploadHandle);
                        }
                    }
                    
                    //If not "Media Uploads"
                    if (secondaryUploadHandle == -1){
                        log("must create the folder");
                        megaApi.createFolder(SECONDARY_UPLOADS, megaApi.getRootNode(), this);
                        return 17;
                    }
                }
            }
            else{
                for (int i=0;i<nl.size();i++){
                    if ((SECONDARY_UPLOADS.compareTo(nl.get(i).getName()) == 0) && (nl.get(i).isFolder())){
                        secondaryUploadHandle = nl.get(i).getHandle();
                        dbH.setSecondaryFolderHandle(secondaryUploadHandle);
                    }
                }
                
                //If not "Media Uploads"
                if (secondaryUploadHandle == -1){
                    log("must create the folder");
                    megaApi.createFolder(SECONDARY_UPLOADS, megaApi.getRootNode(), this);
                    return 18;
                }
            }
        }
        else{
            log("Secondary NOT Enabled");
        }

        return 0;
    }

    private void initService(){
        log("initService()");
        
        totalUploaded = -1;
        totalSizeToUpload = 0;
        totalToUpload = 0;
        totalSizeUploaded = 0;
        successCount = 0;

        canceled = false;
        isOverquota = false;
        newFileList = false;

        try{
            app = (MegaApplication) getApplication();
        }
        catch(Exception ex){
            finish();
        }

        megaApi = app.getMegaApi();
        megaChatApi = app.getMegaChatApi();

        if (megaApi == null){
            finish();
            return;
        }

        megaApi.addGlobalListener(this);

        int wifiLockMode = WifiManager.WIFI_MODE_FULL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        lock = wifiManager.createWifiLock(wifiLockMode, "MegaDownloadServiceWifiLock");
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaDownloadServicePowerLock");

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String previousIP = app.getLocalIpAddress();
        String currentIP = Util.getLocalIpAddress();
        if (previousIP == null || (previousIP.length() == 0) || (previousIP.compareTo("127.0.0.1") == 0)) {
            app.setLocalIpAddress(currentIP);
        }
        else if ((currentIP != null) && (currentIP.length() != 0) && (currentIP.compareTo("127.0.0.1") != 0) && (currentIP.compareTo(previousIP) != 0))
        {
            app.setLocalIpAddress(currentIP);
            log("reconnect");
            megaApi.reconnect();
        }
    }

    private void finish(){
        log("finish CameraUploadsService");

        if(running){
            handler.removeCallbacksAndMessages(null);
            running = false;
        }
        cancel();
    }

    private void cancel() {
        if((lock != null) && (lock.isHeld()))
            try{ lock.release(); } catch(Exception ex) {}
        if((wl != null) && (wl.isHeld()))
            try{ wl.release(); } catch(Exception ex) {}

        if(isOverquota){
            showStorageOverquotaNotification();
        }

        canceled = true;
        isForeground = false;
        running = false;
        stopForeground(true);
        if (mNotificationManager != null){
            mNotificationManager.cancel(notificationId);
        }
        jobFinished(globalParams, true);
    }

    private void showStorageOverquotaNotification(){
        log("showStorageOverquotaNotification");

        String contentText = getString(R.string.download_show_info);
        String message = getString(R.string.overquota_alert_title);

        Intent intent = new Intent(this, ManagerActivityLollipop.class);
        intent.setAction(Constants.ACTION_OVERQUOTA_STORAGE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

            mBuilderCompatO
                    .setSmallIcon(R.drawable.ic_stat_camera_sync)
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
                    .setAutoCancel(true).setTicker(contentText)
                    .setContentTitle(message).setContentText(contentText)
                    .setOngoing(false);

            mNotificationManager.notify(Constants.NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompatO.build());
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {

        log("onStopJob");

        // Called by Android when it has to terminate a running service.
        return true;
    }

    public static void log(String message) {
        Util.log("CameraUploadsService", message);
    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {

    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
        log("onUserAlertsUpdate");
    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {

    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {

    }

    @Override
    public void onAccountUpdate(MegaApiJava api) {

    }

    @Override
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {

    }

    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {

    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart: " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        log("onRequestUpdate: " + request.getRequestString());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish: " + request.getRequestString());

        if (request.getType() == MegaRequest.TYPE_LOGIN){
            if (e.getErrorCode() == MegaError.API_OK){
                log("Fast login OK");
                log("Calling fetchNodes from CameraUploadsService");
                megaApi.fetchNodes(this);
            }
            else{
                log("ERROR: " + e.getErrorString());
                isLoggingIn = false;
                MegaApplication.setLoggingIn(isLoggingIn);
                finish();
            }
        }
        else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
            if (e.getErrorCode() == MegaError.API_OK){
                chatSettings = dbH.getChatSettings();
                if(chatSettings!=null) {
                    boolean chatEnabled = Boolean.parseBoolean(chatSettings.getEnabled());
                    if(chatEnabled){
                        log("Chat enabled-->connect");
                        megaChatApi.connectInBackground(this);
                        isLoggingIn = false;
                        MegaApplication.setLoggingIn(isLoggingIn);

                        int r = runLoggedIn();
                        log("shouldRunAfterLogin -> " + r);
                        if (r == 0) {
                            startCameraUploads();
                        }
                    }
                    else{
                        log("Chat NOT enabled - readyToManager");
                        isLoggingIn = false;
                        MegaApplication.setLoggingIn(isLoggingIn);

                        int r = runLoggedIn();
                        log("shouldRunAfterLogin -> " + r);
                        if (r == 0) {
                            startCameraUploads();
                        }
                    }
                }
                else{
                    log("chatSettings NULL - readyToManager");
                    isLoggingIn = false;
                    MegaApplication.setLoggingIn(isLoggingIn);

                    int r = runLoggedIn();
                    log("shouldRunAfterLogin -> " + r);
                    if (r == 0) {
                        startCameraUploads();
                    }
                }
            }
            else{
                log("ERROR: " + e.getErrorString());
                isLoggingIn = false;
                MegaApplication.setLoggingIn(isLoggingIn);
                finish();
            }
        }
        else if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER){
            if (e.getErrorCode() == MegaError.API_OK){
                log("Folder created: "+request.getName());
                String name = request.getName();
                if(name.contains(CAMERA_UPLOADS)){
                    log("CamSync Folder UPDATED DB");
                    dbH.setCamSyncHandle(request.getNodeHandle());
                }
                else if(name.contains(SECONDARY_UPLOADS)){
                    //Update in database
                    log("Secondary Folder UPDATED DB");
                    dbH.setSecondaryFolderHandle(request.getNodeHandle());
                }

                startCameraUploads();
            }
        }
        else if (request.getType() == MegaRequest.TYPE_RENAME || request.getType() == MegaRequest.TYPE_COPY){
            if (e.getErrorCode() == MegaError.API_OK){
                if (megaApi.getNodeByHandle(request.getNodeHandle()).getName().compareTo(CAMERA_UPLOADS) == 0){
                    log("Folder renamed to CAMERA_UPLOADS");
                    startCameraUploads();
                }
                else{
                    MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
                    if(megaApi.getParentNode(node)!=null){
                        long parentHandle = megaApi.getParentNode(node).getHandle();
                        if(parentHandle == secondaryUploadHandle){
                            log("Update SECONDARY Sync TimeStamp");
                            dbH.setSecSyncTimeStamp(currentTimeStamp);
                        }
                        else{
                            log("Update Camera Sync TimeStamp");
                            dbH.setCamSyncTimeStamp(currentTimeStamp);
                        }
                    }

                    totalSizeUploaded += megaApi.getNodeByHandle(request.getNodeHandle()).getSize();
                    if (cameraFilesExternalSDCardQueue != null){
                        if (cameraFilesExternalSDCardQueue.size() > 0){
                            uploadNextSDCard();
                        }
                        else{
                            uploadNext();
                        }
                    }
                    else{
                        uploadNext();
                    }
                }
            }
            else {
                log("Error ("+e.getErrorCode()+"): "+request.getType()+" : "+request.getRequestString());
                if(request.getNodeHandle()!=-1){
                    MegaNode nodeError = megaApi.getNodeByHandle(request.getNodeHandle());
                    if(nodeError!=null){
                        log("Node: "+nodeError.getName());
                    }
                }

                if (e.getErrorCode() == MegaError.API_EOVERQUOTA)
                    isOverquota = true;

                finish();
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestTemporaryError: " + request.getRequestString());
    }

    @Override
    public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
        log("onTransferStart: " + transfer.getFileName());
    }

    @Override
    public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
        if (canceled) {
            log("Transfer cancel: " + transfer.getFileName());

            if((lock != null) && (lock.isHeld()))
                try{ lock.release(); } catch(Exception ex) {}
            if((wl != null) && (wl.isHeld()))
                try{ wl.release(); } catch(Exception ex) {}

            megaApi.cancelTransfer(transfer);
            cancel();
            return;
        }

        if(isOverquota){
            log("After overquota error");
            isOverquota = false;
        }

        final long bytes = transfer.getTransferredBytes();
//		log("Transfer update: " + transfer.getFileName() + "  Bytes: " + bytes);
        updateProgressNotification(totalSizeUploaded + bytes);
    }

    @Override
    public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
        log("onTransferTemporaryError: " + transfer.getFileName());

        if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
            if (e.getValue() != 0)
                log("TRANSFER OVERQUOTA ERROR: " + e.getErrorCode());
            else
                log("STORAGE OVERQUOTA ERROR: " + e.getErrorCode());

            isOverquota = true;

            updateProgressNotification(totalSizeUploaded);
        }
    }

    @Override
    public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e) {
        log("Image sync finished: " + transfer.getFileName() + " size " + transfer.getTransferredBytes());
        log("transfer.getPath:" + transfer.getPath());
        log("transfer.getNodeHandle:" + transfer.getNodeHandle());

        if (canceled) {
            log("Image sync cancelled: " + transfer.getFileName());
            if((lock != null) && (lock.isHeld()))
                try{ lock.release(); } catch(Exception ex) {}
            if((wl != null) && (wl.isHeld()))
                try{ wl.release(); } catch(Exception ex) {}

            if (isExternalSDCard){
                File fileToDelete = new File(transfer.getPath());
                if (fileToDelete != null){
                    if (fileToDelete.exists()){
                        fileToDelete.delete();
                    }
                }
            }

            cancel();
        }
        else{
            if (e.getErrorCode() == MegaError.API_OK) {

                if(isOverquota){
                    log("After overquota error");
                    isOverquota = false;
                }

                log("Image Sync OK: " + transfer.getFileName());
                totalSizeUploaded += transfer.getTransferredBytes();
                log("IMAGESYNCFILE: " + transfer.getPath());

                String tempPath = transfer.getPath();
                if (!isExternalSDCard){
                    if(tempPath.startsWith(localPath)){
                        log("onTransferFinish: Update Camera Sync TimeStamp");
                        dbH.setCamSyncTimeStamp(currentTimeStamp);
                    }
                    else{
                        log("onTransferFinish: Update SECONDARY Sync TimeStamp");
                        dbH.setSecSyncTimeStamp(currentTimeStamp);
                    }
                }

                if (isExternalSDCard){
                    dbH.setCamSyncTimeStamp(currentTimeStamp);
                }

                if(Util.isVideoFile(transfer.getPath())){
                    log("Is video!!!");
                    File previewDir = PreviewUtils.getPreviewFolder(this);
                    File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
                    File thumbDir = ThumbnailUtils.getThumbFolder(this);
                    File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
                    megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
                    megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

                    MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
                    if(node!=null){
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(transfer.getPath());

                        String location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
                        if(location!=null){
                            log("Location: "+location);

                            boolean secondTry = false;
                            try{
                                final int mid = location.length() / 2; //get the middle of the String
                                String[] parts = {location.substring(0, mid),location.substring(mid)};

                                Double lat = Double.parseDouble(parts[0]);
                                Double lon = Double.parseDouble(parts[1]);
                                log("Lat: "+lat); //first part
                                log("Long: "+lon); //second part

                                megaApi.setNodeCoordinates(node, lat, lon, null);
                            }
                            catch (Exception exc){
                                secondTry = true;
                                log("Exception, second try to set GPS coordinates");
                            }

                            if(secondTry){
                                try{
                                    String latString = location.substring(0,7);
                                    String lonString = location.substring(8,17);

                                    Double lat = Double.parseDouble(latString);
                                    Double lon = Double.parseDouble(lonString);
                                    log("Lat2: "+lat); //first part
                                    log("Long2: "+lon); //second part

                                    megaApi.setNodeCoordinates(node, lat, lon, null);
                                }
                                catch (Exception ex){
                                    log("Exception again, no chance to set coordinates of video");
                                }
                            }
                        }
                        else{
                            log("No location info");
                        }
                    }
                }
                else if (MimeTypeList.typeForName(transfer.getPath()).isImage()){
                    log("Is image!!!");

                    File previewDir = PreviewUtils.getPreviewFolder(this);
                    File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
                    File thumbDir = ThumbnailUtils.getThumbFolder(this);
                    File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
                    megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
                    megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

                    MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
                    if(node!=null){
                        try {
                            final ExifInterface exifInterface = new ExifInterface(transfer.getPath());
                            float[] latLong = new float[2];
                            if (exifInterface.getLatLong(latLong)) {
                                log("Latitude: "+latLong[0]+" Longitude: " +latLong[1]);
                                megaApi.setNodeCoordinates(node, latLong[0], latLong[1], null);
                            }

                        } catch (Exception exception) {
                            log("Couldn't read exif info: " + transfer.getPath());
                        }
                    }
                }
                else{
                    log("NOT video or image!");
                }

                if (isExternalSDCard){
                    File fileToDelete = new File(transfer.getPath());
                    if (fileToDelete != null){
                        if (fileToDelete.exists()){
                            fileToDelete.delete();
                        }
                    }
                }

                if (cameraFilesExternalSDCardQueue != null){
                    if (cameraFilesExternalSDCardQueue.size() > 0){
                        uploadNextSDCard();
                    }
                    else{
                        uploadNext();
                    }
                }
                else{
                    uploadNext();
                }
            }
            else{
                log("Image Sync FAIL: " + transfer.getFileName() + "___" + e.getErrorString());

                if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
                    log("OVERQUOTA ERROR: "+e.getErrorCode());
                    isOverquota = true;
                }

                cancel();
            }
        }
    }

    @Override
    public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer){
        return true;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void updateProgressNotification(final long progress) {
        int progressPercent = (int) Math.round((double) progress / totalSizeToUpload
                * 100);
        log(progressPercent + " " + progress + " " + totalSizeToUpload);
        int left = totalToUpload - totalUploaded;
        int current = totalToUpload - left;
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;

        String message = current + " ";
        if (totalToUpload == 1) {
            message += getResources().getQuantityString(
                    R.plurals.general_num_files, 1);
        } else {
            message += getString(R.string.general_x_of_x)
                    + " "
                    + totalToUpload;

            if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB)
            {
                message += " "
                        + getResources().getQuantityString(
                        R.plurals.general_num_files, totalToUpload);
            }
        }

        String status = isOverquota ? getString(R.string.overquota_alert_title) :
                getString(R.string.settings_camera_notif_title);

        Intent intent = null;

        intent = new Intent(this, ManagerActivityLollipop.class);
        intent.setAction(isOverquota ? Constants.ACTION_OVERQUOTA_STORAGE :
                Constants.ACTION_CANCEL_CAM_SYNC);

        String info = Util.getProgressSize(this, progress, totalSizeToUpload);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder mBuilderCompat = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

            mBuilderCompat
                    .setSmallIcon(R.drawable.ic_stat_camera_sync)
                    .setProgress(100, progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setContentTitle(message)
                    .setSubText(info)
                    .setContentText(status)
                    .setOnlyAlertOnce(true);

            notification = mBuilderCompat.build();
        }
        else if (currentapiVersion >= android.os.Build.VERSION_CODES.N) {
            mBuilder
                    .setSmallIcon(R.drawable.ic_stat_camera_sync)
                    .setProgress(100, progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setContentTitle(message)
                    .setSubText(info)
                    .setContentText(status)
                    .setOnlyAlertOnce(true);
            notification = mBuilder.getNotification();
        }
        else if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            mBuilder
                    .setSmallIcon(R.drawable.ic_stat_camera_sync)
                    .setProgress(100, progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setContentTitle(message)
                    .setContentInfo(info)
                    .setContentText(status)
                    .setOnlyAlertOnce(true);
            notification = mBuilder.getNotification();
//					notification = mBuilder.build();
        }
        else
        {
            notification = new Notification(R.drawable.ic_stat_camera_sync, null, 1);
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
            notification.contentIntent = pendingIntent;
            notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_stat_camera_sync);
            notification.contentView.setTextViewText(R.id.status_text, message);
            notification.contentView.setTextViewText(R.id.progress_text, info);
            notification.contentView.setProgressBar(R.id.status_progress, 100, progressPercent, false);
        }

        mNotificationManager.notify(notificationId, notification);
    }
}