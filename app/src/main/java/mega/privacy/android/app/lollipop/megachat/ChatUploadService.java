package mega.privacy.android.app.lollipop.megachat;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.widget.RemoteViews;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.VideoDownsampling;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
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

import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED;
import static mega.privacy.android.app.constants.BroadcastConstants.FILE_EXPLORER_CHAT_UPLOAD;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.PreviewUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;


public class ChatUploadService extends Service implements MegaTransferListenerInterface, MegaRequestListenerInterface, MegaChatRequestListenerInterface {

	static final float DOWNSCALE_IMAGES_PX = 2000000f;

	public static String ACTION_CANCEL = "CANCEL_UPLOAD";
	public static String EXTRA_SIZE = "MEGA_SIZE";
	public static String EXTRA_CHAT_ID = "CHAT_ID";
	public static String EXTRA_ID_PEND_MSG = "ID_PEND_MSG";
	public static String EXTRA_NAME_EDITED = "MEGA_FILE_NAME_EDITED";
	public static String EXTRA_COMES_FROM_FILE_EXPLORER = "COMES_FROM_FILE_EXPLORER";
	public static String EXTRA_ATTACH_FILES = "ATTACH_FILES";
	public static String EXTRA_ATTACH_CHAT_IDS = "ATTACH_CHAT_IDS";
	public static String EXTRA_UPLOAD_FILES_FINGERPRINTS = "UPLOAD_FILES_FINGERPRINTS";
	public static String EXTRA_PEND_MSG_IDS = "PEND_MSG_IDS";
	public static final String EXTRA_PARENT_NODE = "EXTRA_PARENT_NODE";

	private boolean isForeground = false;
	private boolean canceled;

	boolean sendOriginalAttachments=false;

	//0 - not overquota, not pre-overquota
	//1 - overquota
	//2 - pre-overquota
	int isOverquota = 0;

	ArrayList<PendingMessageSingle> pendingMessages;
	HashMap<String, Integer> mapVideoDownsampling;
	HashMap<Integer, MegaTransfer> mapProgressTransfers;

	MegaApplication app;
	MegaApiAndroid megaApi;
	MegaChatApiAndroid megaChatApi;
	int requestSent = 0;

	WifiLock lock;
	WakeLock wl;
	DatabaseHandler dbH = null;

	int transfersCount = 0;
	int numberVideosPending = 0;
	int totalVideos = 0;
	int totalUploadsCompleted = 0;
	int totalUploads = 0;
	private String type = "";

	MegaNode parentNode;

	VideoDownsampling videoDownsampling;

	private Notification.Builder mBuilder;
	private NotificationCompat.Builder mBuilderCompat;
	private NotificationManager mNotificationManager;

	Object syncObject = new Object();

	MegaRequestListenerInterface megaRequestListener;
	MegaTransferListenerInterface megaTransferListener;

	private int notificationId = NOTIFICATION_CHAT_UPLOAD;
	private String notificationChannelId = NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID;
	private String notificationChannelName = NOTIFICATION_CHANNEL_CHAT_UPLOAD_NAME;

	private static boolean fileExplorerUpload;
	private static long snackbarChatHandle = MEGACHAT_INVALID_HANDLE;

	/** the receiver and manager for the broadcast to listen to the pause event */
	private BroadcastReceiver pauseBroadcastReceiver;
	private LocalBroadcastManager pauseBroadcastManager = LocalBroadcastManager.getInstance(this);

	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		super.onCreate();
		logDebug("onCreate");

		app = (MegaApplication)getApplication();

		megaApi = app.getMegaApi();
		megaChatApi = app.getMegaChatApi();
		megaApi.addTransferListener(this);
		pendingMessages = new ArrayList<>();

		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		isForeground = false;
		canceled = false;
		isOverquota = 0;

		mapVideoDownsampling = new HashMap<>();
		mapProgressTransfers = new HashMap<>();

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

		// delay 1 second to refresh the pause notification to prevent update is missed
		pauseBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				new Handler().postDelayed(() -> {
					updateProgressNotification();
				}, 1000);
			}
		};
		pauseBroadcastManager.registerReceiver(pauseBroadcastReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION));
	}

	@Override
	public void onDestroy(){
		logDebug("onDestroy");
		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}

		if(megaApi != null)
		{
			megaApi.removeRequestListener(this);
			megaApi.removeTransferListener(this);
		}

        if (megaChatApi != null){
            megaChatApi.saveCurrentState();
        }

		pauseBroadcastManager.unregisterReceiver(pauseBroadcastReceiver);
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logDebug("Flags: " + flags + ", Start ID: " + startId);

		canceled = false;

		if(intent == null){
			return START_NOT_STICKY;
		}

		if ((intent.getAction() != null)){
			if (intent.getAction().equals(ACTION_CANCEL)) {
				logDebug("Cancel intent");
				canceled = true;
				megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
				return START_NOT_STICKY;
			}
		}

		isOverquota = 0;

		onHandleIntent(intent);

		return START_NOT_STICKY;
	}

	@SuppressWarnings("unchecked")
	protected void onHandleIntent(final Intent intent) {
		if (intent == null) return;

		ArrayList<PendingMessageSingle> pendingMessageSingles = new ArrayList<>();
		parentNode = MegaNode.unserialize(intent.getStringExtra(EXTRA_PARENT_NODE));

		if (intent.getBooleanExtra(EXTRA_COMES_FROM_FILE_EXPLORER, false)) {
			fileExplorerUpload = true;
			HashMap<String, String> fileFingerprints = (HashMap<String, String>) intent.getSerializableExtra(EXTRA_UPLOAD_FILES_FINGERPRINTS);
			long[] idPendMsgs = intent.getLongArrayExtra(EXTRA_PEND_MSG_IDS);
			long[] attachFiles = intent.getLongArrayExtra(EXTRA_ATTACH_FILES);
			long[] idChats = intent.getLongArrayExtra(EXTRA_ATTACH_CHAT_IDS);

			boolean onlyOneChat = true;

			if (attachFiles != null && attachFiles.length > 0 && idChats != null && idChats.length > 0) {
				for (int i = 0; i < attachFiles.length; i++) {
					for (int j = 0; j < idChats.length; j++) {
						requestSent++;
						megaChatApi.attachNode(idChats[j], attachFiles[i], this);
					}
				}

				if (idChats.length == 1) {
					snackbarChatHandle = idChats[0];
				} else {
					onlyOneChat = false;
				}
			}

			if (idPendMsgs != null && idPendMsgs.length > 0 && fileFingerprints != null && !fileFingerprints.isEmpty()) {
				for (Map.Entry<String, String> entry : fileFingerprints.entrySet()) {
					if (entry != null) {
						String fingerprint = entry.getKey();
						String path = entry.getValue();

						if (fingerprint == null || path == null) {
							logError("Error: Fingerprint: " + fingerprint + ", Path: " + path);
							continue;
						}

						totalUploads++;

						if (!wl.isHeld()) {
							wl.acquire();
						}

						if (!lock.isHeld()) {
							lock.acquire();
						}
						pendingMessageSingles.clear();
						for (int i = 0; i < idPendMsgs.length; i++) {
							PendingMessageSingle pendingMsg = null;
							if (idPendMsgs[i] != -1) {
								pendingMsg = dbH.findPendingMessageById(idPendMsgs[i]);
//									One transfer for file --> onTransferFinish() attach to all selected chats
								if (pendingMsg != null && pendingMsg.getChatId() != -1 && path.equals(pendingMsg.getFilePath()) && fingerprint.equals(pendingMsg.getFingerprint())) {
									pendingMessageSingles.add(pendingMsg);
									if (onlyOneChat) {
										if (snackbarChatHandle == MEGACHAT_INVALID_HANDLE) {
											snackbarChatHandle = pendingMsg.getChatId();
										} else if (snackbarChatHandle != pendingMsg.getChatId()) {
											onlyOneChat = false;
										}
									}
								}
							}
						}
						initUpload(pendingMessageSingles, null);
					}
				}
			}
		} else {
			long chatId = intent.getLongExtra(EXTRA_CHAT_ID, -1);
			type = intent.getStringExtra(EXTRA_TRANSFER_TYPE);
			long idPendMsg = intent.getLongExtra(EXTRA_ID_PEND_MSG, -1);
			PendingMessageSingle pendingMsg = null;
			if (idPendMsg != -1) {
				pendingMsg = dbH.findPendingMessageById(idPendMsg);
			}

			if (pendingMsg != null) {
				sendOriginalAttachments = isSendOriginalAttachments();
				logDebug("sendOriginalAttachments is " + sendOriginalAttachments);

				if (chatId != -1) {
					logDebug("The chat ID is: " + chatId);

					if ((type == null) || (!type.equals(EXTRA_VOICE_CLIP))) {
						totalUploads++;
					}

					if (!wl.isHeld()) {
						wl.acquire();
					}

					if (!lock.isHeld()) {
						lock.acquire();
					}
					pendingMessageSingles.clear();
					pendingMessageSingles.add(pendingMsg);
					initUpload(pendingMessageSingles, type);
				}
			} else {
				logError("Error the chatId is not correct: " + chatId);
			}
		}
	}

	void initUpload (ArrayList<PendingMessageSingle> pendingMsgs, String type) {
		logDebug("initUpload");

		PendingMessageSingle pendingMsg = pendingMsgs.get(0);
		File file = new File(pendingMsg.getFilePath());
		boolean isData = ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();

		if(MimeTypeList.typeForName(file.getName()).isImage() && !MimeTypeList.typeForName(file.getName()).isGIF() && isData){
			String uploadPath;
			File compressedFile = checkImageBeforeUpload(file);

			if (isFileAvailable(compressedFile)) {
				String fingerprint = megaApi.getFingerprint(compressedFile.getAbsolutePath());
				for (PendingMessageSingle pendMsg : pendingMsgs) {
					if (fingerprint != null) {
						pendMsg.setFingerprint(fingerprint);
					}
					pendingMessages.add(pendMsg);
				}

				uploadPath = compressedFile.getAbsolutePath();
			} else {
				for (PendingMessageSingle pendMsg : pendingMsgs) {
					pendingMessages.add(pendMsg);
				}

				uploadPath = pendingMsg.getFilePath();
			}

			megaApi.startUploadWithTopPriority(uploadPath, parentNode, UPLOAD_APP_DATA_CHAT + ">" + pendingMsg.getId(), false);
		} else if(MimeTypeList.typeForName(file.getName()).isMp4Video() && (!sendOriginalAttachments)){
			logDebug("DATA connection is Mp4Video");

			try {
				totalVideos++;
				numberVideosPending++;
				File chatTempFolder = getCacheFolder(getApplicationContext(), CHAT_TEMPORAL_FOLDER);
				File outFile = buildChatTempFile(getApplicationContext(), file.getName());
				int index = 0;
				if(outFile!=null){
					while(outFile.exists()){
						if(index>0){
							outFile = new File(chatTempFolder.getAbsolutePath(), file.getName());
						}

						index++;
						String outFilePath = outFile.getAbsolutePath();
						String[] splitByDot = outFilePath.split("\\.");
						String ext="";
						if(splitByDot!=null && splitByDot.length>1)
							ext = splitByDot[splitByDot.length-1];
						String fileName = outFilePath.substring(outFilePath.lastIndexOf(File.separator)+1, outFilePath.length());
						if(ext.length()>0)
							fileName=fileName.replace("."+ext, "_"+index+".mp4");
						else
							fileName=fileName.concat("_"+index+".mp4");

						outFile = new File(chatTempFolder.getAbsolutePath(), fileName);
					}
				}

				outFile.createNewFile();

				if(outFile==null){
					numberVideosPending--;
					totalVideos--;
					for (PendingMessageSingle pendMsg : pendingMsgs) {
						pendingMessages.add(pendMsg);
					}

					megaApi.startUploadWithTopPriority(pendingMsg.getFilePath(), parentNode, UPLOAD_APP_DATA_CHAT+">"+pendingMsg.getId(), false);
				}
				else{
					for (PendingMessageSingle pendMsg : pendingMsgs) {
						pendMsg.setVideoDownSampled(outFile.getAbsolutePath());
						pendingMessages.add(pendMsg);
					}
					mapVideoDownsampling.put(outFile.getAbsolutePath(), 0);
					if(videoDownsampling==null){
						videoDownsampling = new VideoDownsampling(this);
					}
					videoDownsampling.changeResolution(file, outFile.getAbsolutePath(), pendingMsg.getId());
				}

			} catch (Throwable throwable) {
				for (PendingMessageSingle pendMsg : pendingMsgs) {
					pendingMessages.add(pendMsg);
				}

				megaApi.startUploadWithTopPriority(pendingMsg.getFilePath(), parentNode, UPLOAD_APP_DATA_CHAT+">"+pendingMsg.getId(), false);
				logError("EXCEPTION: Video cannot be downsampled", throwable);
			}
		}
		else{
			for (PendingMessageSingle pendMsg : pendingMsgs) {
				pendingMessages.add(pendMsg);
			}
			String data = UPLOAD_APP_DATA_CHAT+">"+pendingMsg.getId();
			if((type!=null)&&(type.equals(EXTRA_VOICE_CLIP))){
				data = EXTRA_VOICE_CLIP+"-"+data;
			}
			megaApi.startUploadWithTopPriority(pendingMsg.getFilePath(), parentNode, data, false);
		}
	}

	/*
	 * Stop uploading service
	 */
	private void cancel() {
		logDebug("cancel");
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
		logDebug("onQueueComplete");
		//Review when is called

		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}

		if(isOverquota!=0){
			showStorageOverquotaNotification();
		}

		logDebug("Reset figures of chatUploadService");
		numberVideosPending=0;
		totalVideos=0;
		totalUploads = 0;
		totalUploadsCompleted = 0;

		if(megaApi.getNumPendingUploads()<=0){
			megaApi.resetTotalUploads();
		}

		if (fileExplorerUpload) {
			fileExplorerUpload = false;
			LocalBroadcastManager.getInstance(this).sendBroadcast(
					new Intent(BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED)
							.putExtra(FILE_EXPLORER_CHAT_UPLOAD, true)
							.putExtra(CHAT_ID, snackbarChatHandle));
			snackbarChatHandle = MEGACHAT_INVALID_HANDLE;
		}

		logDebug("Stopping service!!");
		isForeground = false;
		stopForeground(true);
		mNotificationManager.cancel(notificationId);
		stopSelf();
		logDebug("After stopSelf");

		try{
			deleteCacheFolderIfEmpty(getApplicationContext(), TEMPORAL_FOLDER);
		}
		catch (Exception e){
			logError("EXCEPTION: pathSelfie not deleted", e);
		}

		try{
			deleteCacheFolderIfEmpty(getApplicationContext(), CHAT_TEMPORAL_FOLDER);
		}
		catch (Exception e){
			logError("EXCEPTION: pathVideoDownsampling not deleted", e);
		}
	}

	public void updateProgressDownsampling(int percentage, String key){
		mapVideoDownsampling.put(key, percentage);
		updateProgressNotification();
	}

	public void finishDownsampling(String returnedFile, boolean success, long idPendingMessage){
		logDebug("success: " + success + ", idPendingMessage: " + idPendingMessage);
		numberVideosPending--;

		File downFile = null;

		if(success){
			mapVideoDownsampling.put(returnedFile, 100);
			downFile = new File(returnedFile);

			for(int i=0; i<pendingMessages.size();i++){
				PendingMessageSingle pendMsg = pendingMessages.get(i);
				if(pendMsg.getVideoDownSampled()!=null && pendMsg.getVideoDownSampled().equals(returnedFile)){
					String fingerPrint = megaApi.getFingerprint(returnedFile);
					if (fingerPrint != null) {
						pendMsg.setFingerprint(fingerPrint);
					}
				}
			}
		}
		else{
			mapVideoDownsampling.remove(returnedFile);

			for(int i=0; i<pendingMessages.size();i++){
				PendingMessageSingle pendMsg = pendingMessages.get(i);

				if(pendMsg.getVideoDownSampled()!=null){
					if(pendMsg.getVideoDownSampled().equals(returnedFile)){
						pendMsg.setVideoDownSampled(null);

						downFile = new File(pendMsg.getFilePath());
						logDebug("Found the downFile");
					}
				}
				else{
					logError("Error message could not been downsampled");
				}
			}
			if(downFile!=null){
				mapVideoDownsampling.put(downFile.getAbsolutePath(), 100);
			}
		}

		if(downFile!=null){
			megaApi.startUploadWithTopPriority(downFile.getPath(), parentNode, UPLOAD_APP_DATA_CHAT+">"+idPendingMessage, false);
		}
	}

	private void showOverquotaNotification(){
		String message = "";
		if (isOverquota != 0){
			message = getString(R.string.overquota_alert_title);
		}

		Intent intent;
		intent = new Intent(ChatUploadService.this, ManagerActivityLollipop.class);

		switch (isOverquota) {
			case 1:
				intent.setAction(ACTION_OVERQUOTA_STORAGE);
				break;
			case 2:
				intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
				break;
			default:break;
		}
		PendingIntent pendingIntent = PendingIntent.getActivity(ChatUploadService.this, 0, intent, 0);
		Notification notification = null;
		int currentapiVersion = Build.VERSION.SDK_INT;


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
			channel.setShowBadge(true);
			channel.setSound(null, null);
			mNotificationManager.createNotificationChannel(channel);

			NotificationCompat.Builder mBuilderCompat = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

			mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setContentIntent(pendingIntent)
					.setOngoing(true).setContentTitle(message)
					.setOnlyAlertOnce(true)
					.setAutoCancel(true)
					.setColor(ContextCompat.getColor(this,R.color.mega));

			notification = mBuilderCompat.build();

		}else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			mBuilder
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setContentIntent(pendingIntent)
					.setOngoing(true).setContentTitle(message)
					.setAutoCancel(true)
					.setOnlyAlertOnce(true);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
				mBuilder.setColor(ContextCompat.getColor(this,R.color.mega));
			}
			notification = mBuilder.build();

		}else if (currentapiVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)	{
			mBuilder
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setContentIntent(pendingIntent)
					.setOngoing(true).setContentTitle(message)
					.setAutoCancel(true)
					.setContentText(getString(R.string.chat_upload_title_notification))
					.setOnlyAlertOnce(true);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
				mBuilder.setColor(ContextCompat.getColor(this,R.color.mega));
			}
			notification = mBuilder.getNotification();

		}else{
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
			notification.contentIntent = pendingIntent;
			notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_stat_notify);
			notification.contentView.setTextViewText(R.id.status_text, message);
		}

		if (!isForeground) {
			logDebug("Starting foreground");
			try {
				startForeground(notificationId, notification);
				isForeground = true;
			}
			catch (Exception e){
				logError("startForeground EXCEPTION", e);
				isForeground = false;
			}
		} else {
			mNotificationManager.notify(notificationId, notification);
		}
	}

	@SuppressLint("NewApi")
	private void updateProgressNotification() {
		logDebug("updatePpogressNotification");
        long progressPercent = 0;
        Collection<MegaTransfer> transfers= mapProgressTransfers.values();

        if(sendOriginalAttachments){
            long total = 0;
            long inProgress = 0;

            for (Iterator iterator = transfers.iterator(); iterator.hasNext();) {
                MegaTransfer currentTransfer = (MegaTransfer) iterator.next();
                if(!currentTransfer.getAppData().contains(EXTRA_VOICE_CLIP)){
					if(currentTransfer.getState()==MegaTransfer.STATE_COMPLETED){
						total = total + currentTransfer.getTotalBytes();
						inProgress = inProgress + currentTransfer.getTotalBytes();
					}
					else{
						total = total + currentTransfer.getTotalBytes();
						inProgress = inProgress + currentTransfer.getTransferredBytes();
					}
				}

            }

            long inProgressTemp = 0;
            if(total>0){
                inProgressTemp = inProgress *100;
                progressPercent = inProgressTemp/total;
            }
        }
        else{

			if(totalVideos>0){
                for (Iterator iterator = transfers.iterator(); iterator.hasNext();) {
                    MegaTransfer currentTransfer = (MegaTransfer) iterator.next();

					if(!currentTransfer.getAppData().contains(EXTRA_VOICE_CLIP)){
						long individualInProgress = currentTransfer.getTransferredBytes();
						long individualTotalBytes = currentTransfer.getTotalBytes();
						long individualProgressPercent = 0;

						if(currentTransfer.getState()==MegaTransfer.STATE_COMPLETED){
							if(MimeTypeList.typeForName(currentTransfer.getFileName()).isMp4Video()){
								individualProgressPercent = 50;
							}
							else{
								individualProgressPercent = 100;
							}
						}
						else{
							if(MimeTypeList.typeForName(currentTransfer.getFileName()).isMp4Video()){
								individualProgressPercent = individualInProgress*50 / individualTotalBytes;
							}
							else{
								individualProgressPercent = individualInProgress*100 / individualTotalBytes;
							}
						}
						progressPercent = progressPercent + individualProgressPercent/totalUploads;
					}
                }

                Collection<Integer> values= mapVideoDownsampling.values();
                int simplePercentage = 50/totalUploads;
                for (Iterator iterator2 = values.iterator(); iterator2.hasNext();) {
                    Integer value = (Integer) iterator2.next();
                    int downsamplingPercent = simplePercentage*value/100;
                    progressPercent = progressPercent + downsamplingPercent;
                }
            }
            else{
				long total = 0;
                long inProgress = 0;

                for (Iterator iterator = transfers.iterator(); iterator.hasNext();) {
                    MegaTransfer currentTransfer = (MegaTransfer) iterator.next();

					if(!currentTransfer.getAppData().contains(EXTRA_VOICE_CLIP)){
						total = total + currentTransfer.getTotalBytes();
						inProgress = inProgress + currentTransfer.getTransferredBytes();
					}
                }
                inProgress = inProgress *100;
                if(total<=0){
                    progressPercent = 0;
                }
                else{
                    progressPercent = inProgress/total;
                }
            }
        }

		logDebug("Progress: " + progressPercent);

        String message = "";
        if (isOverquota != 0){
            message = getString(R.string.overquota_alert_title);
        } else if (totalUploadsCompleted == totalUploads) {
			if (megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
				message = getResources().getQuantityString(R.plurals.upload_service_paused_notification, totalUploads, totalUploadsCompleted, totalUploads);
			} else {
				message = getResources().getQuantityString(R.plurals.upload_service_notification, totalUploads, totalUploadsCompleted, totalUploads);
			}
		} else {
			int inProgress = totalUploadsCompleted + 1;
			if (megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
				message = getResources().getQuantityString(R.plurals.upload_service_paused_notification, totalUploads, inProgress, totalUploads);
			} else {
				message = getResources().getQuantityString(R.plurals.upload_service_notification, totalUploads, inProgress, totalUploads);
			}
		}

        Intent intent;
        intent = new Intent(ChatUploadService.this, ManagerActivityLollipop.class);
		switch (isOverquota) {
			case 0:
			default:
				intent.setAction(ACTION_SHOW_TRANSFERS);
				break;
			case 1:
				intent.setAction(ACTION_OVERQUOTA_STORAGE);
				break;
			case 2:
				intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
				break;
		}

		String actionString = isOverquota == 0 ? getString(R.string.chat_upload_title_notification) :
				getString(R.string.general_show_info);

        PendingIntent pendingIntent = PendingIntent.getActivity(ChatUploadService.this, 0, intent, 0);
        Notification notification = null;
        int currentapiVersion = Build.VERSION.SDK_INT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            mNotificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder mBuilderCompat = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

            mBuilderCompat
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setProgress(100, (int)progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).setContentTitle(message)
                    .setContentText(actionString)
                    .setOnlyAlertOnce(true)
                    .setColor(ContextCompat.getColor(this,R.color.mega));

            notification = mBuilderCompat.build();
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mBuilder
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setProgress(100, (int)progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).setContentTitle(message)
                    .setContentText(actionString)
                    .setOnlyAlertOnce(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                mBuilder.setColor(ContextCompat.getColor(this,R.color.mega));
            }

            notification = mBuilder.build();
        }
        else if (currentapiVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)	{

            mBuilder
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setProgress(100, (int)progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).setContentTitle(message)
                    .setContentText(getString(R.string.chat_upload_title_notification))
                    .setOnlyAlertOnce(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                mBuilder.setColor(ContextCompat.getColor(this,R.color.mega));
            }

            notification = mBuilder.getNotification();

        }
        else
        {
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
            notification.contentIntent = pendingIntent;
            notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_stat_notify);
            notification.contentView.setTextViewText(R.id.status_text, message);
            notification.contentView.setProgressBar(R.id.status_progress, 100, (int)progressPercent, false);
        }

        if (!isForeground) {
			logDebug("Starting foreground");
            try {
                startForeground(notificationId, notification);
                isForeground = true;
            }
            catch (Exception e){
				logError("startForeground EXCEPTION", e);
                isForeground = false;
            }
        } else {
            mNotificationManager.notify(notificationId, notification);
        }
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {

		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
			logDebug("onTransferStart: " + transfer.getNodeHandle());

			String appData = transfer.getAppData();

			if(appData==null) return;

			if(appData.contains(UPLOAD_APP_DATA_CHAT)){
				logDebug("This is a chat upload: " + appData);
				if(!appData.contains(EXTRA_VOICE_CLIP)) {
					transfersCount++;
				}

				if(transfer.isStreamingTransfer()){
					return;
				}

				String[] parts = appData.split(">");
				int last = parts.length-1;
				String idFound = parts[last];

				int id = Integer.parseInt(idFound);
				//Update status and tag on db
				dbH.updatePendingMessageOnTransferStart(id, transfer.getTag());
				mapProgressTransfers.put(transfer.getTag(), transfer);
				if (!transfer.isFolderTransfer() && !appData.contains(EXTRA_VOICE_CLIP)){
					updateProgressNotification();
				}
			}
		}
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {

		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
			logDebug("onTransferUpdate: " + transfer.getNodeHandle());

			String appData = transfer.getAppData();

			if(appData!=null && appData.contains(UPLOAD_APP_DATA_CHAT)){
				if(transfer.isStreamingTransfer()){
					return;
				}

				if (!transfer.isFolderTransfer()){
					if (canceled) {
						logWarning("Transfer cancel: " + transfer.getNodeHandle());

						if((lock != null) && (lock.isHeld()))
							try{ lock.release(); } catch(Exception ex) {}
						if((wl != null) && (wl.isHeld()))
							try{ wl.release(); } catch(Exception ex) {}

						megaApi.cancelTransfer(transfer);
						ChatUploadService.this.cancel();
						logDebug("After cancel");
						return;
					}

					if(isOverquota!=0){
						logWarning("After overquota error");
						isOverquota = 0;
					}
					mapProgressTransfers.put(transfer.getTag(), transfer);

					if(!appData.contains(EXTRA_VOICE_CLIP)) {
						updateProgressNotification();
					}

				}
			}
		}
	}



	@Override
	public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		logWarning(transfer.getNodeHandle() + "\nUpload Temporary Error: " + e.getErrorString() + "__" + e.getErrorCode());
		if((transfer.getType()==MegaTransfer.TYPE_UPLOAD)) {
			switch (e.getErrorCode())
			{
				case MegaError.API_EOVERQUOTA:
				case MegaError.API_EGOINGOVERQUOTA:
					if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
						isOverquota = 1;
					}else if (e.getErrorCode() == MegaError.API_EGOINGOVERQUOTA) {
						isOverquota = 2;
					}

					if (e.getValue() != 0) {
						logWarning("TRANSFER OVERQUOTA ERROR: " + e.getErrorCode());
					}else {
						logWarning("STORAGE OVERQUOTA ERROR: " + e.getErrorCode());
						if(transfer.getAppData().contains(EXTRA_VOICE_CLIP)){
							showOverquotaNotification();
							break;
						}

						updateProgressNotification();

					}

					break;
			}
		}
	}


	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,MegaError error) {

		if (error.getErrorCode() == MegaError.API_EBUSINESSPASTDUE) {
			LocalBroadcastManager.getInstance(getApplicationContext())
					.sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED));
		}

		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
			logDebug("onTransferFinish: " + transfer.getNodeHandle());
			String appData = transfer.getAppData();

			if(appData!=null && appData.contains(UPLOAD_APP_DATA_CHAT)){
				if(transfer.isStreamingTransfer()){
					return;
				}
				if(!appData.contains(EXTRA_VOICE_CLIP)) {
					transfersCount--;
					totalUploadsCompleted++;
				}
				mapProgressTransfers.put(transfer.getTag(), transfer);

				if (canceled) {
					logWarning("Upload cancelled: " + transfer.getNodeHandle());

					if ((lock != null) && (lock.isHeld()))
						try {
							lock.release();
						} catch (Exception ex) {
						}
					if ((wl != null) && (wl.isHeld()))
						try {
							wl.release();
						} catch (Exception ex) {
						}

					ChatUploadService.this.cancel();
					logDebug("After cancel");

					if(appData.contains(EXTRA_VOICE_CLIP)) {
						File localFile = buildVoiceClipFile(this, transfer.getFileName());
						if (isFileAvailable(localFile) && !localFile.getName().equals(transfer.getFileName())) {
							localFile.delete();
						}
					}else {
						//Delete recursively all files and folder-??????
						deleteCacheFolderIfEmpty(getApplicationContext(), TEMPORAL_FOLDER);
					}
				}
				else{
					if (error.getErrorCode() == MegaError.API_OK) {
						logDebug("Upload OK: " + transfer.getNodeHandle());

						if(isVideoFile(transfer.getPath())){
							logDebug("Is video!!!");

							File previewDir = getPreviewFolder(this);
							File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
							File thumbDir = getThumbFolder(this);
							File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
							megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
							megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

							attachNodes(transfer);
						}
						else if (MimeTypeList.typeForName(transfer.getPath()).isImage()){
							logDebug("Is image!!!");

							File previewDir = getPreviewFolder(this);
							File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
							megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

							File thumbDir = getThumbFolder(this);
							File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
							megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());

							attachNodes(transfer);
						}
						else if (MimeTypeList.typeForName(transfer.getPath()).isPdf()) {
							logDebug("Is pdf!!!");

							try{
								ThumbnailUtilsLollipop.createThumbnailPdf(this, transfer.getPath(), megaApi, transfer.getNodeHandle());
							}
							catch(Exception e){
								logError("Pdf thumbnail could not be created", e);
							}

							int pageNumber = 0;
							FileOutputStream out = null;

							try {

								PdfiumCore pdfiumCore = new PdfiumCore(this);
								MegaNode pdfNode = megaApi.getNodeByHandle(transfer.getNodeHandle());

								if (pdfNode == null){
									logError("pdf is NULL");
									return;
								}

								File previewDir = getPreviewFolder(this);
								File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
								File file = new File(transfer.getPath());

								PdfDocument pdfDocument = pdfiumCore.newDocument(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
								pdfiumCore.openPage(pdfDocument, pageNumber);
								int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
								int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);
								Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
								pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
								Bitmap resizedBitmap = resizeBitmapUpload(bmp, width, height);
								out = new FileOutputStream(preview);
								boolean result = resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
								if(result){
									logDebug("Compress OK!");
									File oldPreview = new File(previewDir, transfer.getFileName()+".jpg");
									if (oldPreview.exists()){
										oldPreview.delete();
									}
								}
								else{
									logDebug("Not Compress");
								}
								//Attach node one the request finish
								requestSent++;
								megaApi.setPreview(pdfNode, preview.getAbsolutePath(), this);

								pdfiumCore.closeDocument(pdfDocument);

								updatePdfAttachStatus(transfer);

							} catch(Exception e) {
								logError("Pdf preview could not be created", e);
								attachNodes(transfer);
							} finally {
								try {
									if (out != null)
										out.close();
								} catch (Exception e) {
								}
							}
						}else if(isVoiceClip(transfer.getPath())){
							logDebug("Is voice clip");
							attachVoiceClips(transfer);
						}
						else{
							logDebug("NOT video, image or pdf!");
							attachNodes(transfer);
						}
					}
					else{
						logError("Upload Error: " + transfer.getNodeHandle() + "_" + error.getErrorCode() + "___" + error.getErrorString());

						if(error.getErrorCode() == MegaError.API_EEXIST){
							logWarning("Transfer API_EEXIST: " + transfer.getNodeHandle());
						}
						else{
							if (error.getErrorCode() == MegaError.API_EOVERQUOTA) {
								isOverquota = 1;
							}
							else if (error.getErrorCode() == MegaError.API_EGOINGOVERQUOTA) {
								isOverquota = 2;
							}

							String[] parts = appData.split(">");
							int last = parts.length-1;
							String idFound = parts[last];

							int id = Integer.parseInt(idFound);
							//Update status and tag on db
							dbH.updatePendingMessageOnTransferFinish(id, "-1", PendingMessageSingle.STATE_ERROR_UPLOADING);
							launchErrorToChat(id);

							if (totalUploadsCompleted==totalUploads && transfersCount==0 && numberVideosPending<=0 && requestSent<=0){
								onQueueComplete();
								return;
							}
						}
					}
					File tempPic = getCacheFolder(getApplicationContext(), TEMPORAL_FOLDER);
					logDebug("IN Finish: " + transfer.getNodeHandle());
					if (isFileAvailable(tempPic) && transfer.getPath() != null) {
						if (transfer.getPath().startsWith(tempPic.getAbsolutePath())) {
							File f = new File(transfer.getPath());
							f.delete();
						}
					} else {
						logError("transfer.getPath() is NULL or temporal folder unavailable");
					}
				}

				if (totalUploadsCompleted==totalUploads && transfersCount==0 && numberVideosPending<=0 && requestSent<=0){
					onQueueComplete();
				}
				else{
					if(!appData.contains(EXTRA_VOICE_CLIP)) {
						updateProgressNotification();
					}
				}
			}
		}
	}

	public void attachNodes(MegaTransfer transfer){
		logDebug("attachNodes()");
		//Find the pending message
		String appData = transfer.getAppData();
		String[] parts = appData.split(">");
		int last = parts.length-1;
		String idFound = parts[last];

		int id = Integer.parseInt(idFound);
		//Update status and nodeHandle on db
		dbH.updatePendingMessageOnTransferFinish(id, transfer.getNodeHandle()+"", PendingMessageSingle.STATE_ATTACHING);

		String fingerprint = megaApi.getFingerprint(transfer.getPath());
		if (fingerprint != null) {
			for(int i=0; i<pendingMessages.size();i++) {
				PendingMessageSingle pendMsg = pendingMessages.get(i);
				if (pendMsg.getId() == id || pendMsg.getFingerprint().equals(fingerprint)) {
					attach(pendMsg, transfer);
				}
			}
		}
		else {
			for(int i=0; i<pendingMessages.size();i++) {
				PendingMessageSingle pendMsg = pendingMessages.get(i);
				if (pendMsg.getId() == id) {
					attach(pendMsg, transfer);
				}
			}
		}

	}

	public void attach (PendingMessageSingle pendMsg, MegaTransfer transfer) {
		if (megaChatApi != null) {
			logDebug("attach");

			requestSent++;
			pendMsg.setNodeHandle(transfer.getNodeHandle());
			pendMsg.setState(PendingMessageSingle.STATE_ATTACHING);
			megaChatApi.attachNode(pendMsg.getChatId(), transfer.getNodeHandle(), this);

			if(isVideoFile(transfer.getPath())){
				String pathDownsampled = pendMsg.getVideoDownSampled();
				if(transfer.getPath().equals(pathDownsampled)){
					//Delete the local temp video file
					File f = new File(transfer.getPath());

					if (f.exists()) {
						boolean deleted = f.delete();
						if(!deleted){
							logError("ERROR: Local file not deleted!");
						}
					}
				}
			}

		}
	}

	public void attachVoiceClips(MegaTransfer transfer){
		logDebug("attachVoiceClips()");
		//Find the pending message
		String appData = transfer.getAppData();
		String[] parts = appData.split(">");
		int last = parts.length-1;
		String idFound = parts[last];

		int id = Integer.parseInt(idFound);
		//Update status and nodeHandle on db
		dbH.updatePendingMessageOnTransferFinish(id, transfer.getNodeHandle()+"", PendingMessageSingle.STATE_ATTACHING);

		for(int i=0; i<pendingMessages.size();i++) {
			PendingMessageSingle pendMsg = pendingMessages.get(i);
			if (pendMsg.getId() == id) {
				pendMsg.setNodeHandle(transfer.getNodeHandle());
				pendMsg.setState(PendingMessageSingle.STATE_ATTACHING);
				megaChatApi.attachVoiceMessage(pendMsg.getChatId(), transfer.getNodeHandle(), this);
			}
		}
	}



	public void updatePdfAttachStatus(MegaTransfer transfer){
		logDebug("updatePdfAttachStatus");
		//Find the pending message
		for(int i=0; i<pendingMessages.size();i++){
			PendingMessageSingle pendMsg = pendingMessages.get(i);

			if(pendMsg.getFilePath().equals(transfer.getPath())){
				if(pendMsg.getNodeHandle()==-1){
					logDebug("Set node handle to the pdf file: " + transfer.getNodeHandle());
					pendMsg.setNodeHandle(transfer.getNodeHandle());
				}
				else{
					logError("Set node handle error");
				}
			}
		}

		//Upadate node handle in db
		String appData = transfer.getAppData();
		String[] parts = appData.split(">");
		int last = parts.length-1;
		String idFound = parts[last];

		int id = Integer.parseInt(idFound);
		//Update status and nodeHandle on db
		dbH.updatePendingMessageOnTransferFinish(id, transfer.getNodeHandle()+"", PendingMessageSingle.STATE_ATTACHING);
	}

	public void attachPdfNode(long nodeHandle){
		logDebug("Node Handle: " + nodeHandle);
		//Find the pending message
		for(int i=0; i<pendingMessages.size();i++){
			PendingMessageSingle pendMsg = pendingMessages.get(i);

			if(pendMsg.getNodeHandle()==nodeHandle){
				if(megaChatApi!=null){
					logDebug("Send node: " + nodeHandle + " to chat: " + pendMsg.getChatId());
					requestSent++;
					MegaNode nodePdf = megaApi.getNodeByHandle(nodeHandle);
					if(nodePdf.hasPreview()){
						logDebug("The pdf node has preview");
					}
					megaChatApi.attachNode(pendMsg.getChatId(), nodeHandle, this);
				}
			}
			else{
				logError("PDF attach error");
			}
		}
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart: " + request.getName());
		if (request.getType() == MegaRequest.TYPE_COPY){
			updateProgressNotification();
		}
		else if (request.getType() == MegaRequest.TYPE_SET_ATTR_FILE) {
			logDebug("TYPE_SET_ATTR_FILE");
		}
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		logDebug("UPLOAD: onRequestFinish "+request.getRequestString());

		//Send the file without preview if the set attribute fails
		if(request.getType() == MegaRequest.TYPE_SET_ATTR_FILE && request.getParamType()==MegaApiJava.ATTR_TYPE_PREVIEW){
			requestSent--;
			long handle = request.getNodeHandle();
			MegaNode node = megaApi.getNodeByHandle(handle);
			if(node!=null){
				String nodeName = node.getName();
				if(MimeTypeList.typeForName(nodeName).isPdf()){
					attachPdfNode(handle);
				}
			}
		}

		if (e.getErrorCode()==MegaError.API_OK) {
			logDebug("onRequestFinish OK");
		}
		else {
			logError("onRequestFinish:ERROR: " + e.getErrorCode());

			if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
				logWarning("OVERQUOTA ERROR: "+e.getErrorCode());
				isOverquota = 1;
			}
			else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
				logWarning("PRE-OVERQUOTA ERROR: "+e.getErrorCode());
				isOverquota = 2;
			}
			onQueueComplete();
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("onRequestTemporaryError: " + request.getName());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestUpdate: " + request.getName());
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
            requestSent--;
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				logDebug("Attachment sent correctly");
				MegaNodeList nodeList = request.getMegaNodeList();

				//Find the pending message
				for(int i=0; i<pendingMessages.size();i++){
					PendingMessageSingle pendMsg = pendingMessages.get(i);

					//Check node handles - if match add to DB the karere temp id of the message
					long nodeHandle = pendMsg.getNodeHandle();
					MegaNode node = nodeList.get(0);
					if(node.getHandle()==nodeHandle){
						logDebug("The message MATCH!!");
						long tempId = request.getMegaChatMessage().getTempId();
						logDebug("The tempId of the message is: " + tempId);
						dbH.updatePendingMessageOnAttach(pendMsg.getId(), tempId+"", PendingMessageSingle.STATE_SENT);
						pendingMessages.remove(i);
						break;
					}
				}
			}
			else{
				logWarning("Attachment not correctly sent: " + e.getErrorCode()+" " + e.getErrorString());
				MegaNodeList nodeList = request.getMegaNodeList();

				//Find the pending message
				for(int i=0; i<pendingMessages.size();i++){
					PendingMessageSingle pendMsg = pendingMessages.get(i);
					//Check node handles - if match add to DB the karere temp id of the message
					long nodeHandle = pendMsg.getNodeHandle();
					MegaNode node = nodeList.get(0);
					if(node.getHandle()==nodeHandle){
						logDebug("The message MATCH!!");
						dbH.updatePendingMessageOnAttach(pendMsg.getId(), -1+"", PendingMessageSingle.STATE_ERROR_ATTACHING);

						launchErrorToChat(pendMsg.getId());
						break;
					}
				}
			}
		}

		if (totalUploadsCompleted==totalUploads && transfersCount==0 && numberVideosPending<=0 && requestSent<=0){
			onQueueComplete();
		}
	}

	public void launchErrorToChat(long id){
		logDebug("ID: " + id);

		//Find the pending message
		for(int i=0; i<pendingMessages.size();i++) {
			PendingMessageSingle pendMsg = pendingMessages.get(i);
			if(pendMsg.getId() == id){
				long openChatId = MegaApplication.getOpenChatId();
				if(pendMsg.getChatId()==openChatId){
					logWarning("Error update activity");
					Intent intent;
					intent = new Intent(this, ChatActivityLollipop.class);
					intent.setAction(ACTION_UPDATE_ATTACHMENT);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.putExtra("ID_MSG", pendMsg.getId());
					intent.putExtra("IS_OVERQUOTA", isOverquota);
					startActivity(intent);
				}
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}

	private void showStorageOverquotaNotification(){
		logDebug("showStorageOverquotaNotification");

		String contentText = getString(R.string.download_show_info);
		String message = getString(R.string.overquota_alert_title);

		Intent intent = new Intent(this, ManagerActivityLollipop.class);

		if(isOverquota==1){
			intent.setAction(ACTION_OVERQUOTA_STORAGE);
		}
		else{
			intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
			channel.setShowBadge(true);
			channel.setSound(null, null);
			mNotificationManager.createNotificationChannel(channel);

			NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

			mBuilderCompatO
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
					.setAutoCancel(true).setTicker(contentText)
					.setContentTitle(message).setContentText(contentText)
					.setOngoing(false);

			mNotificationManager.notify(NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompatO.build());
		}
		else {
			mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
					.setAutoCancel(true).setTicker(contentText)
					.setContentTitle(message).setContentText(contentText)
					.setOngoing(false);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
				mBuilderCompat.setColor(ContextCompat.getColor(this,R.color.mega));
			}

			mNotificationManager.notify(NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompat.build());
		}
	}
}
