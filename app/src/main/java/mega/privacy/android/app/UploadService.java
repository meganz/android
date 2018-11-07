package mega.privacy.android.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.format.Formatter;
import android.widget.RemoteViews;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;

/*
 * Service to Upload files
 */
public class UploadService extends Service implements MegaTransferListenerInterface, MegaRequestListenerInterface {

	public static String ACTION_CANCEL = "CANCEL_UPLOAD";
	public static String EXTRA_FILEPATH = "MEGA_FILE_PATH";
	public static String EXTRA_FOLDERPATH = "MEGA_FOLDER_PATH";
	public static String EXTRA_NAME = "MEGA_FILE_NAME";
	public static String EXTRA_NAME_EDITED = "MEGA_FILE_NAME_EDITED";
	public static String EXTRA_SIZE = "MEGA_SIZE";
	public static String EXTRA_PARENT_HASH = "MEGA_PARENT_HASH";

	public static final int CHECK_FILE_TO_UPLOAD_UPLOAD = 1000;
	public static final int CHECK_FILE_TO_UPLOAD_COPY = 1001;
	public static final int CHECK_FILE_TO_UPLOAD_OVERWRITE = 1002;
	public static final int CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER = 1003;

	private int errorCount = 0;
	private int copiedCount = 0;

	private boolean isForeground = false;
	private boolean canceled;

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
	private String notificationChannelId = Constants.NOTIFICATION_CHANNEL_UPLOAD_ID;
	private String notificationChannelName = Constants.NOTIFICATION_CHANNEL_UPLOAD_NAME;

	private HashMap<String, String> transfersCopy;
	HashMap<Integer, MegaTransfer> mapProgressTransfers;
	int totalUploadsCompleted = 0;
	int totalUploads = 0;

	//0 - not overquota, not pre-overquota
	//1 - overquota
	//2 - pre-overquota
	int isOverquota = 0;

	@SuppressLint("NewApi")
	@Override
	public void onCreate() {
		super.onCreate();
		log("onCreate");

		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		megaChatApi = app.getMegaChatApi();

		transfersCopy = new HashMap<String, String>();
		mapProgressTransfers = new HashMap();

		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		isForeground = false;
		canceled = false;
		isOverquota = 0;

		int wifiLockMode = WifiManager.WIFI_MODE_FULL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		lock = wifiManager.createWifiLock(wifiLockMode, "MegaUploadServiceWifiLock");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaUploadServicePowerLock");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			mBuilder = new Notification.Builder(UploadService.this);
		mBuilderCompat = new NotificationCompat.Builder(UploadService.this);

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public void onDestroy(){
		log("****onDestroy");
		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}

		if(megaApi != null)
		{
			megaApi.removeRequestListener(this);
		}

        if (megaChatApi != null){
            megaChatApi.saveCurrentState();
        }

        totalUploads = 0;
		totalUploadsCompleted = 0;

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("onStartCommand");
		canceled = false;

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

		isOverquota = 0;

		onHandleIntent(intent);

		return START_NOT_STICKY;
	}

	protected void onHandleIntent(final Intent intent) {
		log("onHandleIntent");

		final File file = new File(intent.getStringExtra(EXTRA_FILEPATH));

		if(file!=null){
			log("File to manage: "+file.getAbsolutePath());
		}

		long parentHandle = intent.getLongExtra(EXTRA_PARENT_HASH, 0);
		String nameInMEGA = intent.getStringExtra(EXTRA_NAME);
		String nameInMEGAEdited = intent.getStringExtra(EXTRA_NAME_EDITED);

		if (file.isDirectory()) {
			totalUploads++;

			if (nameInMEGA != null){
				megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGA, this);
			}
			else{
				megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), this);
			}
		}
		else {
			if (nameInMEGAEdited != null){
				switch (checkFileToUploadRenamed(file, parentHandle, nameInMEGAEdited)) {
					case CHECK_FILE_TO_UPLOAD_UPLOAD: {
						log("CHECK_FILE_TO_UPLOAD_UPLOAD");

						if (!wl.isHeld()) {
							wl.acquire();
						}
						if (!lock.isHeld()) {
							lock.acquire();
						}

						totalUploads++;

						megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGAEdited, this);
						break;
					}
					case CHECK_FILE_TO_UPLOAD_COPY: {
						log("CHECK_FILE_TO_UPLOAD_COPY");
						copiedCount++;
						break;
					}
					case CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER: {
						log("CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER");
						String sShow = nameInMEGAEdited + " " + getString(R.string.general_already_uploaded);
						//					Toast.makeText(getApplicationContext(), sShow,Toast.LENGTH_SHORT).show();

						Intent i = new Intent(this, ManagerActivityLollipop.class);
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						i.setAction(Constants.SHOW_REPEATED_UPLOAD);
						i.putExtra("MESSAGE", sShow);
						startActivity(i);
						log("Return - file already uploaded");
						return;

					}
				}
			}
			else {
				switch (checkFileToUpload(file, parentHandle)) {
					case CHECK_FILE_TO_UPLOAD_UPLOAD: {
						log("CHECK_FILE_TO_UPLOAD_UPLOAD");

						if (!wl.isHeld()) {
							wl.acquire();
						}
						if (!lock.isHeld()) {
							lock.acquire();
						}

						totalUploads++;

						if (nameInMEGA != null) {
							megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGA, this);
						} else {
							megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), this);
						}
						break;
					}
					case CHECK_FILE_TO_UPLOAD_COPY: {
						log("CHECK_FILE_TO_UPLOAD_COPY");
						copiedCount++;
						break;
					}
					case CHECK_FILE_TO_UPLOAD_OVERWRITE: {
						log("CHECK_FILE_TO_UPLOAD_OVERWRITE");
						MegaNode nodeExistsInFolder = megaApi.getNodeByPath(file.getName(), megaApi.getNodeByHandle(parentHandle));
						megaApi.remove(nodeExistsInFolder);

						if (!wl.isHeld()) {
							wl.acquire();
						}
						if (!lock.isHeld()) {
							lock.acquire();
						}

						totalUploads++;

						if (nameInMEGA != null) {
							megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), nameInMEGA, this);
						} else {
							megaApi.startUpload(file.getAbsolutePath(), megaApi.getNodeByHandle(parentHandle), this);
						}
						break;

					}
					case CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER: {
						log("CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER");
						String sShow = file.getName() + " " + getString(R.string.general_already_uploaded);
						//					Toast.makeText(getApplicationContext(), sShow,Toast.LENGTH_SHORT).show();

						Intent i = new Intent(this, ManagerActivityLollipop.class);
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						i.setAction(Constants.SHOW_REPEATED_UPLOAD);
						i.putExtra("MESSAGE", sShow);
						startActivity(i);
						log("Return - file already uploaded");
						return;
					}
				}
			}
		}
	}

	int checkFileToUploadRenamed (File file, long parentHandle, String nameInMEGAEdited) {
		MegaNode nodeEditedExistsInFolder = megaApi.getNodeByPath(nameInMEGAEdited, megaApi.getNodeByHandle(parentHandle));
		if (nodeEditedExistsInFolder == null){
			String localFingerPrint = megaApi.getFingerprint(file.getAbsolutePath());
			MegaNode nodeExists = megaApi.getNodeByFingerprint(localFingerPrint);
			if (nodeExists == null){
				return CHECK_FILE_TO_UPLOAD_UPLOAD;
			}
			else if (nodeExists.getName().equals(nameInMEGAEdited)){
				transfersCopy.put(localFingerPrint, nameInMEGAEdited);
				megaApi.copyNode(nodeExists, megaApi.getNodeByHandle(parentHandle), this);
				return CHECK_FILE_TO_UPLOAD_COPY;
			}
			else {
				return CHECK_FILE_TO_UPLOAD_UPLOAD;
			}
		}
		else{
			if (file.length() == nodeEditedExistsInFolder.getSize()){
				return CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER;
			}
			else{
				return CHECK_FILE_TO_UPLOAD_UPLOAD;
			}
		}
	}

	int checkFileToUpload(File file, long parentHandle){

		MegaNode nodeExistsInFolder = megaApi.getNodeByPath(file.getName(), megaApi.getNodeByHandle(parentHandle));
		if (nodeExistsInFolder == null){
			String localFingerPrint = megaApi.getFingerprint(file.getAbsolutePath());
			MegaNode nodeExists = megaApi.getNodeByFingerprint(localFingerPrint);
			if (nodeExists == null){
				return CHECK_FILE_TO_UPLOAD_UPLOAD;
			}
			else{
				transfersCopy.put(localFingerPrint, file.getName());
				megaApi.copyNode(nodeExists, megaApi.getNodeByHandle(parentHandle), this);
				return CHECK_FILE_TO_UPLOAD_COPY;
			}
		}
		else{
			if (file.length() == nodeExistsInFolder.getSize()){
				return CHECK_FILE_TO_UPLOAD_SAME_FILE_IN_FOLDER;
			}
			else{
				return CHECK_FILE_TO_UPLOAD_UPLOAD;
				//return CHECK_FILE_TO_UPLOAD_OVERWRITE;
			}
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
		log("****onQueueComplete");

		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}

		if(isOverquota!=0){
			showStorageOverquotaNotification();
		}
		else{
			showCompleteNotification();
		}

		if(megaApi.getNumPendingUploads() <= 0){
			log("onQueueComplete: reset total uploads");
			megaApi.resetTotalUploads();
		}

		errorCount = 0;
		copiedCount = 0;

		totalUploads = 0;

		totalUploadsCompleted = 0;

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
				if(f.list().length<=0){
					f.delete();
				}
			}
		}
	}

	/*
	 * Show complete success notification
	 */
	private void showCompleteNotification() {
		log("****showCompleteNotification");

		if(isOverquota==0){
			String notificationTitle, size;

			notificationTitle = getResources().getQuantityString(R.plurals.upload_service_final_notification, totalUploadsCompleted, totalUploadsCompleted);

			if(copiedCount>0 && errorCount>0){
				String copiedString = getResources().getQuantityString(R.plurals.copied_service_upload, copiedCount, copiedCount);;
				String errorString = getResources().getQuantityString(R.plurals.upload_service_failed, errorCount, errorCount);
				size = copiedString+", "+errorString;
			}
			else if(copiedCount>0){
				size = getResources().getQuantityString(R.plurals.copied_service_upload, copiedCount, copiedCount);
			}
			else if(errorCount>0){
				size = getResources().getQuantityString(R.plurals.upload_service_failed, errorCount, errorCount);
			}
			else{
				Collection<MegaTransfer> transfers= mapProgressTransfers.values();
				long transferredBytes = 0;
				for (Iterator iterator = transfers.iterator(); iterator.hasNext();) {
					MegaTransfer currentTransfer = (MegaTransfer) iterator.next();
					transferredBytes = transferredBytes + currentTransfer.getTransferredBytes();
				}

				String totalBytes = Formatter.formatFileSize(UploadService.this, transferredBytes);
				size = getString(R.string.general_total_size, totalBytes);
			}

			Intent intent = null;
			intent = new Intent(UploadService.this, ManagerActivityLollipop.class);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
				channel.setShowBadge(true);
				channel.setSound(null, null);
				mNotificationManager.createNotificationChannel(channel);

				NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

				mBuilderCompatO
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
						.setAutoCancel(true).setTicker(notificationTitle)
						.setContentTitle(notificationTitle).setContentText(size)
						.setOngoing(false);

				mNotificationManager.notify(notificationIdFinal, mBuilderCompatO.build());
			}
			else {
				mBuilderCompat
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
						.setAutoCancel(true).setTicker(notificationTitle)
						.setContentTitle(notificationTitle).setContentText(size)
						.setOngoing(false);

				mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
			}
		}
	}

	@SuppressLint("NewApi")
	private void updateProgressNotification() {

		if(isOverquota==0){
			long progressPercent = 0;

			Collection<MegaTransfer> transfers= mapProgressTransfers.values();

			long total = 0;
			long inProgress = 0;

			for (Iterator iterator = transfers.iterator(); iterator.hasNext();) {
				MegaTransfer currentTransfer = (MegaTransfer) iterator.next();
				if(currentTransfer.getState()==MegaTransfer.STATE_COMPLETED){
					total = total + currentTransfer.getTotalBytes();
					inProgress = inProgress + currentTransfer.getTotalBytes();
				}
				else{
					total = total + currentTransfer.getTotalBytes();
					inProgress = inProgress + currentTransfer.getTransferredBytes();
				}
			}

			long inProgressTemp = 0;
			if(total>0){
				inProgressTemp = inProgress *100;
				progressPercent = inProgressTemp/total;
			}

			String message = "";
			if (inProgress == 0){
				message = getString(R.string.download_preparing_files);
			}
			else{
				int filesProgress = totalUploadsCompleted+1;
				message = getResources().getQuantityString(R.plurals.upload_service_notification, totalUploads, filesProgress, totalUploads);
			}

			log("****updateProgressNotification: "+ progressPercent+" "+message);

			String info = Util.getProgressSize(UploadService.this, inProgress, total);

			Intent intent;
			intent = new Intent(UploadService.this, ManagerActivityLollipop.class);
			intent.setAction(Constants.ACTION_SHOW_TRANSFERS);

			PendingIntent pendingIntent = PendingIntent.getActivity(UploadService.this, 0, intent, 0);
			Notification notification = null;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
				channel.setShowBadge(true);
				channel.setSound(null, null);
				mNotificationManager.createNotificationChannel(channel);

				NotificationCompat.Builder mBuilderCompat = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

				mBuilderCompat
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setColor(ContextCompat.getColor(this, R.color.mega))
						.setProgress(100, (int)progressPercent, false)
						.setContentIntent(pendingIntent)
						.setOngoing(true).setContentTitle(message).setSubText(info)
						.setContentText(getString(R.string.download_touch_to_show))
						.setOnlyAlertOnce(true);

				notification = mBuilderCompat.build();
			}
			else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				mBuilder
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setColor(ContextCompat.getColor(this, R.color.mega))
						.setProgress(100, (int)progressPercent, false)
						.setContentIntent(pendingIntent)
						.setOngoing(true).setContentTitle(message).setSubText(info)
						.setContentText(getString(R.string.download_touch_to_show))
						.setOnlyAlertOnce(true);
				notification = mBuilder.build();
			}
			else if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)	{

				mBuilder
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setProgress(100, (int)progressPercent, false)
						.setContentIntent(pendingIntent)
						.setOngoing(true).setContentTitle(message).setContentInfo(info)
						.setContentText(getString(R.string.download_touch_to_show))
						.setOnlyAlertOnce(true);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
					mBuilder.setColor(ContextCompat.getColor(this,R.color.mega));
				}

				notification = mBuilder.getNotification();
			}
			else
			{
				notification = new Notification(R.drawable.ic_stat_notify, null, 1);
				notification.flags |= Notification.FLAG_ONGOING_EVENT;
				notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
				notification.contentIntent = pendingIntent;
				notification.contentView.setImageViewResource(R.id.status_icon, R.drawable.ic_stat_notify);
				notification.contentView.setTextViewText(R.id.status_text, message);
				notification.contentView.setTextViewText(R.id.progress_text, info);
				notification.contentView.setProgressBar(R.id.status_progress, 100, (int)progressPercent, false);
			}

			if (!isForeground) {
				log("starting foreground");
				try {
					startForeground(notificationId, notification);
					isForeground = true;
				}
				catch (Exception e){
					log("startforeground exception: " + e.getMessage());
					isForeground = false;
				}
			} else {
				mNotificationManager.notify(notificationId, notification);
			}
		}
	}

	public static void log(String log) {
		Util.log("UploadService", log);
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("Upload start: " + transfer.getFileName());
		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
			String appData = transfer.getAppData();

			if(appData!=null){
				return;
			}

			transfersCount++;
			mapProgressTransfers.put(transfer.getTag(), transfer);
			if (!transfer.isFolderTransfer()){
				updateProgressNotification();
			}
		}
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError error) {
		log("****onTransferFinish: " + transfer.getFileName() + " size " + transfer.getTransferredBytes());
		log("transfer.getPath:" + transfer.getPath());
		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {

			String appData = transfer.getAppData();

			if(appData!=null){
				return;
			}

			transfersCount--;
			totalUploadsCompleted++;

			mapProgressTransfers.put(transfer.getTag(), transfer);

			if (!transfer.isFolderTransfer()) {

				if (transfer.getState() == MegaTransfer.STATE_COMPLETED) {
					String size = Util.getSizeString(transfer.getTotalBytes());
					AndroidCompletedTransfer completedTransfer = new AndroidCompletedTransfer(transfer.getFileName(), transfer.getType(), transfer.getState(), size, transfer.getNodeHandle() + "");
					dbH.setCompletedTransfer(completedTransfer);
				}
			}

			if (canceled) {
				log("Upload canceled: " + transfer.getFileName());

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

				UploadService.this.cancel();
				log("after cancel");
				String pathSelfie = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.temporalPicDIR;
				File f = new File(pathSelfie);
				//Delete recursively all files and folder
				if (f.isDirectory()) {
					if(f.list().length<=0){
						f.delete();
					}
				}

			} else {
				if (error.getErrorCode() == MegaError.API_OK) {
					log("Upload OK: " + transfer.getFileName());

					if (Util.isVideoFile(transfer.getPath())) {
						log("Is video!!!");

						File previewDir = PreviewUtils.getPreviewFolder(this);
						File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						File thumbDir = ThumbnailUtils.getThumbFolder(this);
						File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
						megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

						MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());

						if (node != null) {
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
								catch (Exception e){
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
									catch (Exception e){
										log("Exception again, no chance to set coordinates of video");
									}
								}
							}
							else{
								log("No location info");
							}
						}
					} else if (MimeTypeList.typeForName(transfer.getPath()).isImage()) {
						log("Is image!!!");

						File previewDir = PreviewUtils.getPreviewFolder(this);
						File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						File thumbDir = ThumbnailUtils.getThumbFolder(this);
						File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
						megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

						MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
						if (node != null) {
							try {
								final ExifInterface exifInterface = new ExifInterface(transfer.getPath());
								float[] latLong = new float[2];
								if (exifInterface.getLatLong(latLong)) {
									log("Latitude: " + latLong[0] + " Longitude: " + latLong[1]);
									megaApi.setNodeCoordinates(node, latLong[0], latLong[1], null);
								}

							} catch (Exception e) {
								log("Couldn't read exif info: " + transfer.getPath());
							}
						}
					} else if (MimeTypeList.typeForName(transfer.getPath()).isPdf()) {
						log("Is pdf!!!");

						try {
							ThumbnailUtilsLollipop.createThumbnailPdf(this, transfer.getPath(), megaApi, transfer.getNodeHandle());
						} catch(Exception e) {
							log("Pdf thumbnail could not be created");
						}

						int pageNumber = 0;
						FileOutputStream out = null;

						try {
						PdfiumCore pdfiumCore = new PdfiumCore(this);
						MegaNode pdfNode = megaApi.getNodeByHandle(transfer.getNodeHandle());

						if (pdfNode == null){
							log("pdf is NULL");
							return;
						}

						File previewDir = PreviewUtils.getPreviewFolder(this);
						File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
						File file = new File(transfer.getPath());

							PdfDocument pdfDocument = pdfiumCore.newDocument(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
							pdfiumCore.openPage(pdfDocument, pageNumber);
							int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
							int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);
							Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
							pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height);
							Bitmap resizedBitmap = PreviewUtils.resizeBitmapUpload(bmp, width, height);
							out = new FileOutputStream(preview);
							boolean result = resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
							if(result){
								log("Compress OK!");
								megaApi.setPreview(pdfNode, preview.getAbsolutePath());
							}
							else{
								log("Not Compress");
							}
							pdfiumCore.closeDocument(pdfDocument);
						} catch(Exception e) {
							log("Pdf preview could not be created");
						} finally {
							try {
								if (out != null)
									out.close();
							} catch (Exception e) {

							}
						}

					} else {
						log("NOT video, image or pdf!");
					}
				} else {
					log("Upload Error: " + transfer.getFileName() + "_" + error.getErrorCode() + "___" + error.getErrorString());

					if (!transfer.isFolderTransfer()) {
						errorCount++;
					}
				}

				String qrFileName = megaApi.getMyEmail() + "QRcode.jpg";

				if (getApplicationContext().getExternalCacheDir() != null) {
					File localFile = new File(getApplicationContext().getExternalCacheDir(), transfer.getFileName());
					if (localFile.exists() && !localFile.getName().equals(qrFileName)) {
						log("Delete file!: " + localFile.getAbsolutePath());
						localFile.delete();
					}
				} else {
					File localFile = new File(getApplicationContext().getCacheDir(), transfer.getFileName());
					if (localFile.exists() && !localFile.getName().equals(qrFileName)) {
						log("Delete file!: " + localFile.getAbsolutePath());
						localFile.delete();
					}
				}

				if (isOverquota!=0) {
					megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this);
				}

				log("IN Finish: " + transfer.getFileName() + "path? " + transfer.getPath());
				String pathSelfie = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.temporalPicDIR;
				if (transfer.getPath() != null) {
					if (transfer.getPath().startsWith(pathSelfie)) {
						File f = new File(transfer.getPath());
						f.delete();
					}
				} else {
					log("transfer.getPath() is NULL");
				}

				int total = totalUploadsCompleted + copiedCount;
				if (total==totalUploads && transfersCount == 0) {
					onQueueComplete();
				}
				else{
					updateProgressNotification();
				}
			}
		}
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		log("****onTransferUpdate");
		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD){
			String appData = transfer.getAppData();

			if(appData!=null){
				return;
			}

			if (!transfer.isFolderTransfer()){
				if (canceled) {
					log("Transfer cancel: " + transfer.getFileName());

					if((lock != null) && (lock.isHeld()))
						try{ lock.release(); } catch(Exception ex) {}
					if((wl != null) && (wl.isHeld()))
						try{ wl.release(); } catch(Exception ex) {}

					megaApi.cancelTransfer(transfer);
					UploadService.this.cancel();
					log("after cancel");
					return;
				}

				if(isOverquota!=0){
					log("after overquota alert");
					return;
				}

				mapProgressTransfers.put(transfer.getTag(), transfer);

				updateProgressNotification();
			}
		}
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		log("onTransferTemporaryError: " + e.getErrorString() + "__" + e.getErrorCode());

		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
			switch (e.getErrorCode())
			{
				case MegaError.API_EOVERQUOTA:
				case MegaError.API_EGOINGOVERQUOTA:
					if (e.getErrorCode() == MegaError.API_EOVERQUOTA) {
						isOverquota = 1;
					}
					else if (e.getErrorCode() == MegaError.API_EGOINGOVERQUOTA) {
						isOverquota = 2;
					}

					if (e.getValue() != 0) {
						log("TRANSFER OVERQUOTA ERROR: " + e.getErrorCode());
					}
					else {
						log("STORAGE OVERQUOTA ERROR: " + e.getErrorCode());
						showStorageOverquotaNotification();
					}
					break;
			}
		}
	}

	private void showStorageOverquotaNotification(){
		log("showStorageOverquotaNotification");

		String contentText = getString(R.string.download_show_info);
		String message = getString(R.string.overquota_alert_title);

		Intent intent = new Intent(this, ManagerActivityLollipop.class);
		if(isOverquota==1){
			intent.setAction(Constants.ACTION_OVERQUOTA_STORAGE);
		}
		else{
			intent.setAction(Constants.ACTION_PRE_OVERQUOTA_STORAGE);
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

			mNotificationManager.notify(Constants.NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompatO.build());
		}
		else {
			mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
					.setAutoCancel(true).setTicker(contentText)
					.setContentTitle(message).setContentText(contentText)
					.setOngoing(false);

			mNotificationManager.notify(Constants.NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompat.build());
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
		if (request.getType() == MegaRequest.TYPE_COPY){
			log("TYPE_COPY finished");
			if (e.getErrorCode() == MegaError.API_OK){
				MegaNode n = megaApi.getNodeByHandle(request.getNodeHandle());
				if (n != null){
					String currentNodeName = n.getName();
					String megaFingerPrint = megaApi.getFingerprint(n);
					log("copy node");
					String nameInMega = transfersCopy.get(megaFingerPrint);
					if (nameInMega != null){
						if (nameInMega.compareTo(currentNodeName) != 0){
							megaApi.renameNode(n, nameInMega);
						}
					}
					transfersCopy.remove(megaFingerPrint);
					int total = totalUploadsCompleted + copiedCount;
					if (total==totalUploads){
						onQueueComplete();
					}
				}
				else{
					log("ERROR - node is NULL");
//					Intent tempIntent = null;
//					tempIntent = new Intent(this, UploadService.class);
//					tempIntent.setAction(UploadService.ACTION_CANCEL);
//					startService(tempIntent);
				}
			}
			else if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
				log("OVERQUOTA ERROR: "+e.getErrorCode());
				isOverquota = 1;

				onQueueComplete();
			}
			else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
				log("OVERQUOTA ERROR: "+e.getErrorCode());
				isOverquota = 2;

				onQueueComplete();
			}
			else{
				log("ERROR: "+e.getErrorCode());
//				Intent tempIntent = null;
//				tempIntent = new Intent(this, UploadService.class);
//				tempIntent.setAction(UploadService.ACTION_CANCEL);
//				startService(tempIntent);
			}
		}
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
}
