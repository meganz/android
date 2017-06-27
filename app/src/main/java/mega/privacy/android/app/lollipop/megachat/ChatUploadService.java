package mega.privacy.android.app.lollipop.megachat;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.widget.RemoteViews;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;

public class ChatUploadService extends Service implements MegaTransferListenerInterface, MegaRequestListenerInterface, MegaChatRequestListenerInterface {

	public static String ACTION_CANCEL = "CANCEL_UPLOAD";
	public static String EXTRA_FILEPATHS = "MEGA_FILE_PATH";
	public static String EXTRA_FOLDERPATH = "MEGA_FOLDER_PATH";
	public static String EXTRA_NAME = "MEGA_FILE_NAME";
	public static String EXTRA_SIZE = "MEGA_SIZE";
	public static String EXTRA_PARENT_HASH = "MEGA_PARENT_HASH";
	public static String EXTRA_CHAT_ID = "CHAT_ID";
	public static String EXTRA_ID_PEND_MSG = "ID_PEND_MSG";

	private int errorCount = 0;

	private boolean isForeground = false;
	private boolean canceled;

	ArrayList<PendingMessage> pendingMessages;

	MegaApplication app;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;

	WifiLock lock;
	WakeLock wl;
	DatabaseHandler dbH = null;

	int transfersCount = 0;

	private Notification.Builder mBuilder;
	private NotificationCompat.Builder mBuilderCompat;
	private NotificationManager mNotificationManager;

	Object syncObject = new Object();

	MegaRequestListenerInterface megaRequestListener;
	MegaTransferListenerInterface megaTransferListener;

	private int notificationId = Constants.NOTIFICATION_UPLOAD;
	private int notificationIdFinal = Constants.NOTIFICATION_UPLOAD_FINAL;

	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		super.onCreate();
		log("onCreate");

		app = (MegaApplication)getApplication();

		megaApi = app.getMegaApi();
		megaChatApi = app.getMegaChatApi();

		megaApi.addTransferListener(this);

		pendingMessages = new ArrayList<>();

		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		isForeground = false;
		canceled = false;

		int wifiLockMode = WifiManager.WIFI_MODE_FULL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		lock = wifiManager.createWifiLock(wifiLockMode, "MegaUploadServiceWifiLock");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaUploadServicePowerLock");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			mBuilder = new Notification.Builder(ChatUploadService.this);
		mBuilderCompat = new NotificationCompat.Builder(ChatUploadService.this);

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public void onDestroy(){
		log("onDestroy");
		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}

		if(megaApi != null)
		{
			megaApi.removeRequestListener(this);
			megaApi.removeTransferListener(this);
		}

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("onStartCommand");

		if(intent == null){
			return START_NOT_STICKY;
		}

		if ((intent.getAction() != null)){
			if (intent.getAction().equals(ACTION_CANCEL)) {
				log("Cancel intent");
				canceled = true;
				megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
				return START_NOT_STICKY;
			}
		}

		onHandleIntent(intent);

		return START_NOT_STICKY;
	}

	protected void onHandleIntent(final Intent intent) {
		log("onHandleIntent");

		ArrayList<String> filePaths = intent.getStringArrayListExtra(EXTRA_FILEPATHS);
		log("Number of files to upload: "+filePaths.size());

		long parentHandle = intent.getLongExtra(EXTRA_PARENT_HASH, 0);

		long chatId = intent.getLongExtra(EXTRA_CHAT_ID, -1);

		long idPendMsg = intent.getLongExtra(EXTRA_ID_PEND_MSG, -1);

		if(chatId!=-1){

			PendingMessage newMessage = new PendingMessage(idPendMsg, chatId, filePaths);
			pendingMessages.add(newMessage);

			for(int i=0; i<filePaths.size();i++){
				File file = new File(filePaths.get(i));

				if (file.isDirectory()) {
//			String nameInMEGA = intent.getStringExtra(EXTRA_NAME);
//			if (nameInMEGA != null){
//				megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGA);
//			}
//			else{
//				megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle));
//			}
				}
				else {
					log("Chat file uploading...");

					if(!wl.isHeld()){
						wl.acquire();
					}

					if(!lock.isHeld()){
						lock.acquire();
					}

					megaApi.startUpload(filePaths.get(i), megaApi.getNodeByHandle(parentHandle));

				}
			}

		}
		else{
			log("Error the chatId is not correct: "+chatId);
		}

	}

	/*
	 * Stop uploading service
	 */
	private void cancel() {
		log("cancel");
		canceled = true;
		isForeground = false;
		stopForeground(true);
		mNotificationManager.cancel(notificationId);
		stopSelf();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/*
	 * No more intents in the queue
	 */
	private void onQueueComplete() {
		log("onQueueComplete");

		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}

		showCompleteNotification();

		int total = megaApi.getNumPendingUploads() + megaApi.getNumPendingDownloads();
		log("onQueueComplete: total of files before reset " + total);
		if(total <= 0){
			log("onQueueComplete: reset total uploads/downloads");
			megaApi.resetTotalUploads();
			megaApi.resetTotalDownloads();
			errorCount = 0;
		}

		log("stopping service!!!!!!!!!!:::::::::::::::!!!!!!!!!!!!");
		isForeground = false;
		stopForeground(true);
		mNotificationManager.cancel(notificationId);
		stopSelf();
		log("after stopSelf");
		String pathSelfie = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.temporalPicDIR;
		File f = new File(pathSelfie);
		//Delete recursively all files and folder
		if (f.exists()) {
			if (f.isDirectory()) {
			    for (File c : f.listFiles())
			      c.delete();
			}
			f.delete();
		}
	}

	/*
	 * Show complete success notification
	 */
	private void showCompleteNotification() {
		log("showCompleteNotification");
		String notificationTitle, size;

		int totalUploads = megaApi.getTotalUploads();
		notificationTitle = getResources().getQuantityString(R.plurals.upload_service_final_notification, totalUploads, totalUploads);

		if (errorCount > 0){
			size = getResources().getQuantityString(R.plurals.upload_service_failed, errorCount, errorCount);
		}
		else{
			String totalBytes = Formatter.formatFileSize(ChatUploadService.this, megaApi.getTotalUploadedBytes());
			size = getString(R.string.general_total_size, totalBytes);
		}

		Intent intent = null;
		intent = new Intent(ChatUploadService.this, ManagerActivityLollipop.class);

		mBuilderCompat
		.setSmallIcon(R.drawable.ic_stat_notify_upload)
		.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
		.setAutoCancel(true).setTicker(notificationTitle)
		.setContentTitle(notificationTitle).setContentText(size)
		.setOngoing(false);

		mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
	}

	@SuppressLint("NewApi")
	private void updateProgressNotification() {

		int pendingTransfers = megaApi.getNumPendingUploads();
		int totalTransfers = megaApi.getTotalUploads();

		long totalSizePendingTransfer = megaApi.getTotalUploadBytes();
		long totalSizeTransferred = megaApi.getTotalUploadedBytes();

		int progressPercent = (int) Math.round((double) totalSizeTransferred / totalSizePendingTransfer * 100);
		log("updateProgressNotification: "+progressPercent);

		String message = "";
		if (totalTransfers == 0){
			message = getString(R.string.download_preparing_files);
		}
		else{
			int inProgress = totalTransfers - pendingTransfers + 1;
			message = getResources().getQuantityString(R.plurals.upload_service_notification, totalTransfers, inProgress, totalTransfers);
		}

		String info = Util.getProgressSize(ChatUploadService.this, totalSizeTransferred, totalSizePendingTransfer);

		Intent intent;
		intent = new Intent(ChatUploadService.this, ManagerActivityLollipop.class);
		intent.setAction(Constants.ACTION_SHOW_TRANSFERS);

		PendingIntent pendingIntent = PendingIntent.getActivity(ChatUploadService.this, 0, intent, 0);
		Notification notification = null;
		int currentapiVersion = Build.VERSION.SDK_INT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			mBuilder
					.setSmallIcon(R.drawable.ic_stat_notify_upload)
					.setProgress(100, progressPercent, false)
					.setContentIntent(pendingIntent)
					.setOngoing(true).setContentTitle(message).setSubText(info)
					.setContentText(getString(R.string.download_touch_to_show))
					.setOnlyAlertOnce(true);
			notification = mBuilder.build();
		}
		else if (currentapiVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)	{

			mBuilder
			.setSmallIcon(R.drawable.ic_stat_notify_upload)
			.setProgress(100, progressPercent, false)
			.setContentIntent(pendingIntent)
			.setOngoing(true).setContentTitle(message).setContentInfo(info)
			.setContentText(getString(R.string.download_touch_to_show))
			.setOnlyAlertOnce(true);
			notification = mBuilder.getNotification();

		}
		else
		{
			notification = new Notification(R.drawable.ic_stat_notify_upload, null, 1);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
			notification.contentIntent = pendingIntent;
			notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_stat_notify_upload);
			notification.contentView.setTextViewText(R.id.status_text, message);
			notification.contentView.setTextViewText(R.id.progress_text, info);
			notification.contentView.setProgressBar(R.id.status_progress, 100, progressPercent, false);
		}
			
			
		if (!isForeground) {
			log("starting foreground!");
			startForeground(notificationId, notification);
			isForeground = true;
		} else {
			mNotificationManager.notify(notificationId, notification);
		}
	}
	
	public static void log(String log) {
		Util.log("ChatUploadService", log);
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("Upload start: " + transfer.getFileName() + "_" + megaApi.getTotalUploads());
        transfersCount++;
		if (!transfer.isFolderTransfer()){
			updateProgressNotification();
		}
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,MegaError error) {
		log("Upload finished: " + transfer.getFileName() + " size " + transfer.getTransferredBytes());
		log("transfer.getPath:" + transfer.getPath());

        transfersCount--;

        if (canceled) {
            log("Upload cancelled: " + transfer.getFileName());

            if((lock != null) && (lock.isHeld()))
                try{ lock.release(); } catch(Exception ex) {}
            if((wl != null) && (wl.isHeld()))
                try{ wl.release(); } catch(Exception ex) {}

            ChatUploadService.this.cancel();
            log("after cancel");
            String pathSelfie = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.temporalPicDIR;
            File f = new File(pathSelfie);
            //Delete recursively all files and folder
            if (f.isDirectory()) {
                for (File c : f.listFiles())
                  c.delete();
            }
            f.delete();

        }
        else{
            if (error.getErrorCode() == MegaError.API_OK) {
                log("Upload OK: " + transfer.getFileName());

                File previewDir = PreviewUtils.getPreviewFolder(this);
                File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
                File thumbDir = ThumbnailUtils.getThumbFolder(this);
                File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle())+".jpg");
                megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
                megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

                if(Util.isVideoFile(transfer.getPath())){
                    log("Is video!!!");
                    ThumbnailUtilsLollipop.createThumbnailVideo(this, transfer.getPath(), megaApi, transfer.getNodeHandle());

                    MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
                    if(node!=null){
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(transfer.getPath());
                        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        if(time!=null){
                            double seconds = Double.parseDouble(time)/1000;
                            log("The original duration is: "+seconds);
                            int secondsAprox = (int) Math.round(seconds);
                            log("The duration aprox is: "+secondsAprox);

                            megaApi.setNodeDuration(node, secondsAprox, null);
                        }
                    }
                }
                else{
                    log("NOT video!");
                }

                //Find the pending message
				for(int i=0; i<pendingMessages.size();i++){
					PendingMessage pendMsg = pendingMessages.get(i);

					ArrayList<String> filePaths = pendMsg.getFilePaths();
					for(int j=0; j<filePaths.size();j++){
						if(filePaths.get(j).equals(transfer.getPath())){
							log("the file to upload FOUND!!");

							if(pendMsg.getFilePaths().size()==1){
								log("Just one file to send in the message");
								if(megaChatApi!=null){
									log("Send the message to the chat");
									MegaNodeList nodeList = MegaNodeList.createInstance();

									MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
									if(node!=null){
										log("Node to send: "+node.getName());
										nodeList.addNode(node);
									}
									megaChatApi.attachNodes(pendMsg.getChatId(), nodeList, this);
								}
							}
							else{
								log("More than one to send in message");

								pendMsg.addNodeHandle(transfer.getNodeHandle());

								if(pendMsg.getNodeHandles().size()==pendMsg.getFilePaths().size()){
									log("All files of the message uploaded! SEND!");
									if(megaChatApi!=null){
										log("Send the message to the chat");
										MegaNodeList nodeList = MegaNodeList.createInstance();

										for(int k=0; k<pendMsg.getNodeHandles().size();k++){
											MegaNode node = megaApi.getNodeByHandle(pendMsg.getNodeHandles().get(k));
											if(node!=null){
												log("Node to send: "+node.getName());
												nodeList.addNode(node);
											}
										}
										megaChatApi.attachNodes(pendMsg.getChatId(), nodeList, this);
									}
								}
								else{
									log("Waiting for more messages...");
								}
							}

							return;
						}
					}

				}
				log("The NOT found in messages");
            }
            else{
                log("Upload Error: " + transfer.getFileName() + "_" + error.getErrorCode() + "___" + error.getErrorString());

                if(!transfer.isFolderTransfer()){
                    errorCount++;
                }

                if(error.getErrorCode() == MegaError.API_EINCOMPLETE){
                    log("API_EINCOMPLETE ERROR: "+error.getErrorCode());
                }
                else if(error.getErrorCode()==MegaError.API_EOVERQUOTA){
                    log("OVERQUOTA ERROR: "+error.getErrorCode());
                    Intent intent;
                    intent = new Intent(this, ManagerActivityLollipop.class);
                    intent.setAction(Constants.ACTION_OVERQUOTA_ALERT);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(intent);

                    Intent tempIntent = null;
                    tempIntent = new Intent(this, ChatUploadService.class);
                    tempIntent.setAction(ChatUploadService.ACTION_CANCEL);
                    startService(tempIntent);
                }
            }

            if (getApplicationContext().getExternalCacheDir() != null){
                File localFile = new File (getApplicationContext().getExternalCacheDir(), transfer.getFileName());
                if (localFile.exists()){
                    log("Delete file!: "+localFile.getAbsolutePath());
                    localFile.delete();
                }
            }
            else{
                File localFile = new File (getApplicationContext().getCacheDir(), transfer.getFileName());
                if (localFile.exists()){
                    log("Delete file!: "+localFile.getAbsolutePath());
                    localFile.delete();
                }
            }

            log("IN Finish: "+transfer.getFileName()+"path? "+transfer.getPath());

        }

	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		if (!transfer.isFolderTransfer()){
			if (canceled) {
				log("Transfer cancel: " + transfer.getFileName());
	
				if((lock != null) && (lock.isHeld()))
					try{ lock.release(); } catch(Exception ex) {}
				if((wl != null) && (wl.isHeld()))
					try{ wl.release(); } catch(Exception ex) {}
				
				megaApi.cancelTransfer(transfer);
				ChatUploadService.this.cancel();
				log("after cancel");
				return;
			}
			
			if (transfer.getPath() != null){
				File f = new File(transfer.getPath());
				if (f.isDirectory()){
					transfer.getTotalBytes();				
				}
			}

			updateProgressNotification();
		}
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		log(transfer.getPath() + "\nDownload Temporary Error: " + e.getErrorString() + "__" + e.getErrorCode());

		if(e.getErrorCode() == MegaError.API_EOVERQUOTA) {
			log("API_EOVERQUOTA error!!");

			Intent intent = null;
			intent = new Intent(this, ManagerActivityLollipop.class);
			intent.setAction(Constants.ACTION_OVERQUOTA_TRANSFER);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getName());
		if (request.getType() == MegaRequest.TYPE_COPY){
			updateProgressNotification();
		}
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("UPLOAD: onRequestFinish "+request.getRequestString());
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate: " + request.getName());
	}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer)
	{
		return true;
	}

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		if(request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE){
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("File sent correctly");
				MegaNodeList nodeList = request.getMegaNodeList();

				//Find the pending message
				for(int i=0; i<pendingMessages.size();i++){
					PendingMessage pendMsg = pendingMessages.get(i);

					//Check node handles - if match add to DB the karere temp id of the message

					ArrayList<Long> nodeHandles = pendMsg.getNodeHandles();
					if(nodeHandles.size()==1){
						log("Just one file to send in the message");
						MegaNode node = nodeList.get(0);
						if(node.getHandle()==nodeHandles.get(0)){
							log("The message MATCH!!");
							long tempId = request.getMegaChatMessage().getTempId();
							log("The tempId of the message is: "+tempId);
							dbH.updatePendingMessage(pendMsg.getId(), tempId+"");
						}
					}
					else{
						log("More than one to send in message");


					}

				}


			}
			else{
				log("File NOT sent: "+e.getErrorCode());
			}
		}

		if (megaApi.getNumPendingUploads() == 0 && transfersCount==0){
			onQueueComplete();
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}
}
