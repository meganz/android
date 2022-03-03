package mega.privacy.android.app;

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
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import mega.privacy.android.app.components.saver.AutoPlayInfo;
import mega.privacy.android.app.components.transferWidget.TransfersManagement;
import mega.privacy.android.app.fragments.offline.OfflineFragment;
import mega.privacy.android.app.lollipop.LoginActivity;
import mega.privacy.android.app.lollipop.ManagerActivity;
import mega.privacy.android.app.notifications.TransferOverQuotaNotification;
import mega.privacy.android.app.objects.SDTransfer;
import mega.privacy.android.app.service.iar.RatingHandlerImpl;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.SDCardOperator;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferData;
import nz.mega.sdk.MegaTransferListenerInterface;

import static mega.privacy.android.app.components.transferWidget.TransfersManagement.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.lollipop.ManagerActivity.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaTransferUtils.getNumPendingDownloadsNonBackground;
import static mega.privacy.android.app.utils.MegaTransferUtils.isBackgroundTransfer;
import static mega.privacy.android.app.utils.MegaTransferUtils.isVoiceClipType;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.SDCardUtils.getSDCardTargetPath;
import static mega.privacy.android.app.utils.SDCardUtils.getSDCardTargetUri;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;

/*
 * Background service to download files
 */
public class DownloadService extends Service implements MegaTransferListenerInterface, MegaRequestListenerInterface {

	// Action to stop download
	public static final String ACTION_CANCEL = "CANCEL_DOWNLOAD";
	public static final String EXTRA_SIZE = "DOCUMENT_SIZE";
	public static final String EXTRA_HASH = "DOCUMENT_HASH";
	public static final String EXTRA_URL = "DOCUMENT_URL";
	public static final String EXTRA_DOWNLOAD_TO_SDCARD = "download_to_sdcard";
	public static final String EXTRA_TARGET_PATH = "target_path";
	public static final String EXTRA_TARGET_URI = "target_uri";
	public static final String EXTRA_PATH = "SAVE_PATH";
	public static final String EXTRA_FOLDER_LINK = "FOLDER_LINK";
	public static final String EXTRA_FROM_MV = "fromMV";
	public static final String EXTRA_CONTACT_ACTIVITY = "CONTACT_ACTIVITY";
	public static final String EXTRA_ZIP_FILE_TO_OPEN = "FILE_TO_OPEN";
	public static final String EXTRA_OPEN_FILE = "OPEN_FILE";
	public static final String EXTRA_CONTENT_URI = "CONTENT_URI";
	public static final String EXTRA_DOWNLOAD_BY_TAP = "EXTRA_DOWNLOAD_BY_TAP";
	public static final String EXTRA_DOWNLOAD_FOR_OFFLINE = "EXTRA_DOWNLOAD_FOR_OFFLINE";

	private static int errorEBloqued = 0;
	private int errorCount = 0;
	private int alreadyDownloaded = 0;

	private boolean isForeground = false;
	private boolean canceled;

	private boolean openFile = true;
	private boolean downloadByTap;
	private String type = "";
	private boolean isOverquota = false;
	private long downloadedBytesToOverquota = 0;
	private MegaNode rootNode;

	MegaApplication app;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	MegaChatApiAndroid megaChatApi;

	ArrayList<Intent> pendingIntents = new ArrayList<Intent>();

	WifiLock lock;
	WakeLock wl;

	File currentFile;
	File currentDir;
	MegaNode currentDocument;

	DatabaseHandler dbH = null;

	int transfersCount = 0;
	Set<Integer> backgroundTransfers = new HashSet<>();

	HashMap<Long, Uri> storeToAdvacedDevices;
	HashMap<Long, Boolean> fromMediaViewers;

	private NotificationCompat.Builder mBuilderCompat;
	private Notification.Builder mBuilder;
	private NotificationManager mNotificationManager;

	MegaNode offlineNode;

	boolean isLoggingIn = false;
	private long lastUpdated;

	private Intent intent;

	/** the receiver and manager for the broadcast to listen to the pause event */
	private BroadcastReceiver pauseBroadcastReceiver;

	private final CompositeDisposable rxSubscriptions = new CompositeDisposable();
	private final Handler uiHandler = new Handler(Looper.getMainLooper());

	// the flag to determine the rating dialog is showed for this download action
	private boolean isRatingShowed;

	private boolean isDownloadForOffline;

    /**
     * Contains the info of a node that to be opened in-app.
     */
    private AutoPlayInfo autoPlayInfo;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(){
		super.onCreate();
		logDebug("onCreate");

		app = MegaApplication.getInstance();
		megaApi = app.getMegaApi();
		megaApi.addTransferListener(this);
		megaApi.addRequestListener(this);
		megaApiFolder = app.getMegaApiFolder();
		megaChatApi = app.getMegaChatApi();

		isForeground = false;
		canceled = false;

		storeToAdvacedDevices = new HashMap<Long, Uri>();
		fromMediaViewers = new HashMap<>();

		int wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		WifiManager wifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		lock = wifiManager.createWifiLock(wifiLockMode, "MegaDownloadServiceWifiLock");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaDownloadServicePowerLock");
		mBuilder = new Notification.Builder(DownloadService.this);
		mBuilderCompat = new NotificationCompat.Builder(getApplicationContext());
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		startForeground();

		rootNode = megaApi.getRootNode();

		// delay 1 second to refresh the pause notification to prevent update is missed
		pauseBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				new Handler().postDelayed(() -> {
					updateProgressNotification();
				}, 1000);
			}
		};

		registerReceiver(pauseBroadcastReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION));

	}

	private void startForeground() {
		if (getNumPendingDownloadsNonBackground(megaApi) <= 0) {
			return;
		}

		try {
			startForeground(NOTIFICATION_DOWNLOAD, createInitialServiceNotification(NOTIFICATION_CHANNEL_DOWNLOAD_ID,
					NOTIFICATION_CHANNEL_DOWNLOAD_NAME, mNotificationManager,
					new NotificationCompat.Builder(DownloadService.this, NOTIFICATION_CHANNEL_DOWNLOAD_ID),
					mBuilder));
			isForeground = true;
		} catch (Exception e) {
			logWarning("Error starting foreground.", e);
			isForeground = false;
		}
	}

	private void stopForeground() {
		isForeground = false;
		stopForeground(true);
		mNotificationManager.cancel(NOTIFICATION_DOWNLOAD);
		stopSelf();
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

		rootNode = null;
		// remove all the generated folders in cache folder on SD card.
        File[] fs = getExternalCacheDirs();
        if (fs.length > 1 && fs[1] != null) {
            purgeDirectory(fs[1]);
        }

        unregisterReceiver(pauseBroadcastReceiver);
		rxSubscriptions.clear();
		stopForeground();

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		logDebug("onStartCommand");
		canceled = false;

		if(intent == null){
			logWarning("intent==null");
			return START_NOT_STICKY;
		}

		if (intent.getAction() != null && intent.getAction().equals(ACTION_CANCEL)){
			logDebug("Cancel intent");
			canceled = true;
			megaApi.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD);
			return START_NOT_STICKY;
		}

		rxSubscriptions.add(Single.just(intent)
			.observeOn(Schedulers.single())
			.subscribe(this::onHandleIntent,
					throwable -> logError("onHandleIntent onError", throwable)));
		return START_NOT_STICKY;
	}

	protected void onHandleIntent(final Intent intent) {
		logDebug("onHandleIntent");
		this.intent = intent;

		if (intent.getAction() != null && intent.getAction().equals(ACTION_RESTART_SERVICE)) {
			MegaTransferData transferData = megaApi.getTransferData(null);
			if (transferData == null) {
				stopForeground();
				return;
			}

			int uploadsInProgress = transferData.getNumDownloads();

			for (int i = 0; i < uploadsInProgress; i++) {
				MegaTransfer transfer = megaApi.getTransferByTag(transferData.getDownloadTag(i));
				if (transfer == null) {
					continue;
				}

				if (!isVoiceClipType(transfer) && !isBackgroundTransfer(transfer)) {
					MegaApplication.getTransfersManagement().checkIfTransferIsPaused(transfer);
					transfersCount++;
				}
			}

			if (transfersCount > 0) {
				updateProgressNotification();
			} else {
				stopForeground();
			}

			launchTransferUpdateIntent(MegaTransfer.TYPE_DOWNLOAD);
			return;
		}

        long hash = intent.getLongExtra(EXTRA_HASH, -1);
        String url = intent.getStringExtra(EXTRA_URL);
        isDownloadForOffline = intent.getBooleanExtra(EXTRA_DOWNLOAD_FOR_OFFLINE, false);
        boolean isFolderLink = intent.getBooleanExtra(EXTRA_FOLDER_LINK, false);
        openFile = intent.getBooleanExtra(EXTRA_OPEN_FILE, true);
        downloadByTap = intent.getBooleanExtra(EXTRA_DOWNLOAD_BY_TAP, false);
		type = intent.getStringExtra(EXTRA_TRANSFER_TYPE);

		Uri contentUri = null;
        if(intent.getStringExtra(EXTRA_CONTENT_URI)!=null){
            contentUri = Uri.parse(intent.getStringExtra(EXTRA_CONTENT_URI));
        }

        boolean highPriority = intent.getBooleanExtra(HIGH_PRIORITY_TRANSFER, false);
        boolean fromMV = intent.getBooleanExtra(EXTRA_FROM_MV, false);
		logDebug("fromMV: " + fromMV);

		megaApi = app.getMegaApi();

		UserCredentials credentials = dbH.getCredentials();

		if (credentials != null) {

			String gSession = credentials.getSession();
			if (rootNode == null) {
				rootNode = megaApi.getRootNode();
				isLoggingIn = MegaApplication.isLoggingIn();
				if (!isLoggingIn) {
					isLoggingIn = true;
					MegaApplication.setLoggingIn(isLoggingIn);

					ChatUtil.initMegaChatApi(gSession);

					pendingIntents.add(intent);
					if (type == null || (!type.contains(APP_DATA_VOICE_CLIP) && !type.contains(APP_DATA_BACKGROUND_TRANSFER))) {
						updateProgressNotification();
					}

					megaApi.fastLogin(gSession);
					return;
				}
				else{
					logWarning("Another login is processing");
				}
				pendingIntents.add(intent);
				return;
			}
		}

		String serialize = intent.getStringExtra(EXTRA_SERIALIZE_STRING);

		if(serialize != null){
			logDebug("serializeString: " + serialize);
			currentDocument = MegaNode.unserialize(serialize);
			if(currentDocument != null){
				hash = currentDocument.getHandle();
				logDebug("hash after unserialize: " + hash);
			}
			else{
				logWarning("Node is NULL after unserialize");
			}
		} else if (isFolderLink) {
			currentDocument = megaApiFolder.getNodeByHandle(hash);
		} else {
			currentDocument = megaApi.getNodeByHandle(hash);
		}

        if(url != null){
			logDebug("Public node");
            currentDir = new File(intent.getStringExtra(EXTRA_PATH));
            if (currentDir != null){
                currentDir.mkdirs();
            }
            megaApi.getPublicNode(url);
            return;
        }

		if((currentDocument == null) && (url == null)){
			logWarning("Node not found");
			return;
		}

		fromMediaViewers.put(currentDocument.getHandle(), fromMV);

        currentDir = getDir(currentDocument, intent);
        currentDir.mkdirs();
		if (currentDir.isDirectory()) {
			currentFile = new File(currentDir, megaApi.escapeFsIncompatible(currentDocument.getName(), currentDir.getAbsolutePath() + SEPARATOR));
		} else {
			currentFile = currentDir;
		}

		String appData = getSDCardAppData(intent);

		if(!checkCurrentFile(currentDocument)){
			logDebug("checkCurrentFile == false");

			alreadyDownloaded++;
            if (getNumPendingDownloadsNonBackground(megaApi) == 0){
                onQueueComplete(currentDocument.getHandle());
            }

            return;
        }

        if(!wl.isHeld()){
            wl.acquire();
        }
        if(!lock.isHeld()){
            lock.acquire();
        }

        if (contentUri != null || currentDir.isDirectory()) {
			if (contentUri != null) {
				//To download to Advanced Devices
				currentDir = new File(intent.getStringExtra(EXTRA_PATH));
				currentDir.mkdirs();

				if (!currentDir.isDirectory()) {
					logWarning("currentDir is not a directory");
				}

				storeToAdvacedDevices.put(currentDocument.getHandle(), contentUri);
			} else if (currentFile.exists()) {
				//Check the fingerprint
				String localFingerprint = megaApi.getFingerprint(currentFile.getAbsolutePath());
				String megaFingerprint = megaApi.getFingerprint(currentDocument);

				if (!isTextEmpty(localFingerprint)
						&& !isTextEmpty(megaFingerprint)
						&& localFingerprint.equals(megaFingerprint)) {
					logDebug("Delete the old version");
					currentFile.delete();
				}
			}

			if (currentDir.getAbsolutePath().contains(OFFLINE_DIR)) {
//			Save for offline: do not open when finishes
				openFile = false;
			}

			if (isFolderLink) {
				currentDocument = megaApiFolder.authorizeNode(currentDocument);
			}

			if (TransfersManagement.isOnTransferOverQuota()) {
				checkTransferOverQuota(false);
			}

			logDebug("CurrentDocument is not null");
			if (highPriority) {
			    // Download to SD card from chat.
                if (!isTextEmpty(appData)) {
                    megaApi.startDownloadWithTopPriority(currentDocument, currentDir.getAbsolutePath() + "/", appData);
                } else {
                    String data = type != null && type.contains(APP_DATA_VOICE_CLIP) ? APP_DATA_VOICE_CLIP : "";
                    megaApi.startDownloadWithTopPriority(currentDocument, currentDir.getAbsolutePath() + "/", data);
                }
			} else if (!isTextEmpty(appData)) {
				megaApi.startDownloadWithData(currentDocument, currentDir.getAbsolutePath() + "/", appData);
			} else {
				megaApi.startDownload(currentDocument, currentDir.getAbsolutePath() + "/");
			}
		} else {
			logWarning("currentDir is not a directory");
		}
    }

	/**
	 * Checks if the download of the current Intent corresponds to a SD card download.
	 * If so, stores the SD card paths on an app data String.
	 * If not, do nothing.
	 *
	 * @param intent Current Intent.
	 * @return The app data String.
	 */
	private String getSDCardAppData(Intent intent) {
		if (intent == null
				|| !intent.getBooleanExtra(EXTRA_DOWNLOAD_TO_SDCARD, false)) {
			return null;
		}

		String sDCardAppData = APP_DATA_SD_CARD;

		String targetPath = intent.getStringExtra(EXTRA_TARGET_PATH);
		if (!isTextEmpty(targetPath)) {
			sDCardAppData += APP_DATA_INDICATOR + targetPath;
		}

		String targetUri = intent.getStringExtra(EXTRA_TARGET_URI);
		if (!isTextEmpty(targetUri)) {
			sDCardAppData += APP_DATA_INDICATOR + targetUri;
		}

		return sDCardAppData;
	}

	private void onQueueComplete(long handle) {
		logDebug("onQueueComplete");

		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}

        showCompleteNotification(handle);
		stopForeground();
		rootNode = null;
		int pendingDownloads = getNumPendingDownloadsNonBackground(megaApi);
		logDebug("onQueueComplete: total of files before reset " + pendingDownloads);
		if(pendingDownloads <= 0){
			logDebug("onQueueComplete: reset total downloads");
			// When download a single file by tapping it, and auto play is enabled.
			int totalDownloads = megaApi.getTotalDownloads() - backgroundTransfers.size();
			if (totalDownloads == 1 && Boolean.parseBoolean(dbH.getAutoPlayEnabled()) && autoPlayInfo != null && downloadByTap) {
                sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED)
                        .putExtra(TRANSFER_TYPE, DOWNLOAD_TRANSFER_OPEN)
                        .putExtra(NODE_NAME, autoPlayInfo.getNodeName())
                        .putExtra(NODE_HANDLE, autoPlayInfo.getNodeHandle())
                        .putExtra(NUMBER_FILES, 1)
                        .putExtra(NODE_LOCAL_PATH, autoPlayInfo.getLocalPath()));
            } else if (totalDownloads > 0) {
            	Intent intent = new Intent(BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED)
						.putExtra(TRANSFER_TYPE, DOWNLOAD_TRANSFER)
						.putExtra(NUMBER_FILES, totalDownloads);
            	if (isDownloadForOffline) {
            		intent.putExtra(OFFLINE_AVAILABLE, true);
				}
				sendBroadcast(intent);
			}
			sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE));

			megaApi.resetTotalDownloads();
			backgroundTransfers.clear();
			errorEBloqued = 0;
			errorCount = 0;
			alreadyDownloaded = 0;
		}
	}

	private void sendTakenDownAlert() {
	    if (errorEBloqued <= 0) return;

		Intent intent = new Intent(BROADCAST_ACTION_INTENT_TAKEN_DOWN_FILES);
		intent.putExtra(NUMBER_FILES, errorEBloqued);
		sendBroadcast(intent);
	}

	private File getDir(MegaNode document, Intent intent) {
		boolean toDownloads = (intent.hasExtra(EXTRA_PATH) == false);
		File destDir;
		if (toDownloads) {
			destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		} else {
			destDir = new File(intent.getStringExtra(EXTRA_PATH));
		}
		return destDir;
	}

	boolean checkCurrentFile(MegaNode document)	{
		logDebug("checkCurrentFile");
		if (currentFile.exists()
				&& document.getSize() == currentFile.length()
				&& isFileDownloadedLatest(currentFile, document)) {

			currentFile.setReadable(true, false);

			return false;
		}

		if(document.getSize() > ((long)1024*1024*1024*4)) {
			logDebug("Show size alert: " + document.getSize());
			uiHandler.post(() -> Toast.makeText(getApplicationContext(),
					getString(R.string.error_file_size_greater_than_4gb),
					Toast.LENGTH_LONG).show());
		}

		return true;
	}

	/*
	 * Show download success notification
	 */
	private void showCompleteNotification(long handle) {
		logDebug("showCompleteNotification");
		String notificationTitle, size;

        int totalDownloads = megaApi.getTotalDownloads() - backgroundTransfers.size();

		if(alreadyDownloaded>0 && errorCount>0){
			int totalNumber = totalDownloads + errorCount + alreadyDownloaded;
			notificationTitle = getResources().getQuantityString(R.plurals.download_service_final_notification_with_details, totalNumber, totalDownloads, totalNumber);

			String copiedString = getResources().getQuantityString(R.plurals.already_downloaded_service, alreadyDownloaded, alreadyDownloaded);;
			String errorString = getResources().getQuantityString(R.plurals.upload_service_failed, errorCount, errorCount);
			size = copiedString+", "+errorString;
		}
		else if(alreadyDownloaded>0){
			int totalNumber = totalDownloads + alreadyDownloaded;
			notificationTitle = getResources().getQuantityString(R.plurals.download_service_final_notification_with_details, totalNumber, totalDownloads, totalNumber);

			size = getResources().getQuantityString(R.plurals.already_downloaded_service, alreadyDownloaded, alreadyDownloaded);
		}
		else if(errorCount>0){
            sendTakenDownAlert();
			int totalNumber = totalDownloads + errorCount;
			notificationTitle = getResources().getQuantityString(R.plurals.download_service_final_notification_with_details, totalNumber, totalDownloads, totalNumber);

			size = getResources().getQuantityString(R.plurals.download_service_failed, errorCount, errorCount);
		}
		else{
			notificationTitle = getResources().getQuantityString(R.plurals.download_service_final_notification, totalDownloads, totalDownloads);
			String totalBytes = getSizeString(megaApi.getTotalDownloadedBytes());
			size = getString(R.string.general_total_size, totalBytes);
		}

		Intent intent = new Intent(getApplicationContext(), ManagerActivity.class);
		intent.setAction(ACTION_SHOW_TRANSFERS);
		intent.putExtra(TRANSFERS_TAB, COMPLETED_TAB);

		PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

		if (totalDownloads != 1) {
			logDebug("Show notification");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
				channel.setShowBadge(true);
				channel.setSound(null, null);
				mNotificationManager.createNotificationChannel(channel);

				NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

				mBuilderCompatO
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
						.setContentIntent(pendingIntent)
						.setAutoCancel(true).setTicker(notificationTitle)
						.setContentTitle(notificationTitle).setContentText(size)
						.setOngoing(false);

				mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
			}
			else {
				mBuilderCompat
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
						.setContentIntent(pendingIntent)
						.setAutoCancel(true).setTicker(notificationTitle)
						.setContentTitle(notificationTitle).setContentText(size)
						.setOngoing(false);

				mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
			}
		}
		else
		{
			try {
                boolean autoPlayEnabled = Boolean.parseBoolean(dbH.getAutoPlayEnabled());
                if (openFile && autoPlayEnabled) {
                    String fileLocalPath;
                    String path = getLocalFile(megaApi.getNodeByHandle(handle));
                    if(path != null ) {
                        fileLocalPath = path;
                    } else {
                        fileLocalPath = currentFile.getAbsolutePath();
                    }

                    autoPlayInfo = new AutoPlayInfo(currentDocument.getName(), currentDocument.getHandle(), fileLocalPath, true);

					logDebug("Both openFile and autoPlayEnabled are true");
					boolean fromMV = false;
					if (fromMediaViewers.containsKey(handle)){
						Boolean result = fromMediaViewers.get(handle);
						fromMV = result != null && result;
					}

					if (MimeTypeList.typeForName(currentFile.getName()).isPdf()){
						logDebug("Pdf file");

						if(fromMV) {
							logDebug("Show notification");
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
								NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
								channel.setShowBadge(true);
								channel.setSound(null, null);
								mNotificationManager.createNotificationChannel(channel);

								NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

								mBuilderCompatO
										.setSmallIcon(R.drawable.ic_stat_notify)
										.setContentIntent(pendingIntent)
										.setAutoCancel(true).setTicker(notificationTitle)
										.setContentTitle(notificationTitle).setContentText(size)
										.setOngoing(false);

								mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
							}
							else {
								mBuilderCompat
										.setSmallIcon(R.drawable.ic_stat_notify)
										.setContentIntent(pendingIntent)
										.setAutoCancel(true).setTicker(notificationTitle)
										.setContentTitle(notificationTitle).setContentText(size)
										.setOngoing(false);

								mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
							}
						}
					}
					else if (MimeTypeList.typeForName(currentFile.getName()).isVideoReproducible() || MimeTypeList.typeForName(currentFile.getName()).isAudio()) {
						logDebug("Video/Audio file");
						if (fromMV) {
							logDebug("Show notification");
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
								NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
								channel.setShowBadge(true);
								channel.setSound(null, null);
								mNotificationManager.createNotificationChannel(channel);

								NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

								mBuilderCompatO
										.setSmallIcon(R.drawable.ic_stat_notify)
										.setContentIntent(pendingIntent)
										.setAutoCancel(true).setTicker(notificationTitle)
										.setContentTitle(notificationTitle).setContentText(size)
										.setOngoing(false);

								mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
							}
							else {
								mBuilderCompat
										.setSmallIcon(R.drawable.ic_stat_notify)
										.setContentIntent(pendingIntent)
										.setAutoCancel(true).setTicker(notificationTitle)
										.setContentTitle(notificationTitle).setContentText(size)
										.setOngoing(false);

								mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
							}
						}
					} else if (MimeTypeList.typeForName(currentFile.getName()).isImage()) {
						logDebug("Download is IMAGE");
						if (fromMV) {
							logDebug("Show notification");
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
								NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
								channel.setShowBadge(true);
								channel.setSound(null, null);
								mNotificationManager.createNotificationChannel(channel);

								NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

								mBuilderCompatO
										.setSmallIcon(R.drawable.ic_stat_notify)
										.setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
										.setContentIntent(pendingIntent)
										.setAutoCancel(true).setTicker(notificationTitle)
										.setContentTitle(notificationTitle).setContentText(size)
										.setOngoing(false);

								mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
							}
							else {
								mBuilderCompat
										.setSmallIcon(R.drawable.ic_stat_notify)
										.setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
										.setContentIntent(pendingIntent)
										.setAutoCancel(true).setTicker(notificationTitle)
										.setContentTitle(notificationTitle).setContentText(size)
										.setOngoing(false);

								mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
							}
						}

					} else {
						logDebug("Show notification");
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
							channel.setShowBadge(true);
							channel.setSound(null, null);
							mNotificationManager.createNotificationChannel(channel);

							NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

							mBuilderCompatO
									.setSmallIcon(R.drawable.ic_stat_notify)
									.setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
									.setContentIntent(pendingIntent)
									.setAutoCancel(true).setTicker(notificationTitle)
									.setContentTitle(notificationTitle).setContentText(size)
									.setOngoing(false);

							mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
						}
						else {
							mBuilderCompat
									.setSmallIcon(R.drawable.ic_stat_notify)
									.setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
									.setContentIntent(pendingIntent)
									.setAutoCancel(true).setTicker(notificationTitle)
									.setContentTitle(notificationTitle).setContentText(size)
									.setOngoing(false);

							mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
						}
					}
				} else {
					openFile = true; //Set the openFile to the default

					logDebug("Show notification");
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
						channel.setShowBadge(true);
						channel.setSound(null, null);
						mNotificationManager.createNotificationChannel(channel);

						NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

						mBuilderCompatO
								.setSmallIcon(R.drawable.ic_stat_notify)
								.setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
								.setContentIntent(pendingIntent)
								.setAutoCancel(true).setTicker(notificationTitle)
								.setContentTitle(notificationTitle).setContentText(size)
								.setOngoing(false);

						mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
					}
					else {
						mBuilderCompat
								.setSmallIcon(R.drawable.ic_stat_notify)
								.setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
								.setContentIntent(pendingIntent)
								.setAutoCancel(true).setTicker(notificationTitle)
								.setContentTitle(notificationTitle).setContentText(size)
								.setOngoing(false);

						mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
					}
				}
			}
			catch (Exception e){
				openFile = true; //Set the openFile to the default
				logError("Exception", e);

				logDebug("Show notification");
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
					channel.setShowBadge(true);
					channel.setSound(null, null);
					mNotificationManager.createNotificationChannel(channel);

					NotificationCompat.Builder mBuilderCompatO = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

					mBuilderCompatO
							.setSmallIcon(R.drawable.ic_stat_notify)
							.setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
							.setContentIntent(pendingIntent)
							.setAutoCancel(true).setTicker(notificationTitle)
							.setContentTitle(notificationTitle).setContentText(size)
							.setOngoing(false);

					mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompatO.build());
				}
				else {
					mBuilderCompat
							.setSmallIcon(R.drawable.ic_stat_notify)
							.setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
							.setContentIntent(pendingIntent)
							.setAutoCancel(true).setTicker(notificationTitle)
							.setContentTitle(notificationTitle).setContentText(size)
							.setOngoing(false);

					mNotificationManager.notify(NOTIFICATION_DOWNLOAD_FINAL, mBuilderCompat.build());
				}
			}
		}
	}


	/*
	 * Update notification download progress
	 */
	@SuppressLint("NewApi")
	private void updateProgressNotification() {
		int pendingTransfers = getNumPendingDownloadsNonBackground(megaApi);
        int totalTransfers = megaApi.getTotalDownloads() - backgroundTransfers.size();

        long totalSizePendingTransfer = megaApi.getTotalDownloadBytes();
        long totalSizeTransferred = megaApi.getTotalDownloadedBytes();

		boolean update;

		if(isOverquota){
			logDebug("Overquota flag! is TRUE");
			if(downloadedBytesToOverquota<=totalSizeTransferred){
				update = false;
			}
			else{
				update = true;
				logDebug("Change overquota flag");
				isOverquota = false;
			}
		}
		else{
			logDebug("NOT overquota flag");
			update = true;
		}

		if(update){
			/* refresh UI every 1 seconds to avoid too much workload on main thread
			 * while in paused status, the update should not be avoided*/
			if(!isOverquota) {
				long now = System.currentTimeMillis();
				if (now - lastUpdated > ONTRANSFERUPDATE_REFRESH_MILLIS || megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD) ) {
					lastUpdated = now;
				} else {
					return;
				}
			}
			int progressPercent = (int) Math.round((double) totalSizeTransferred / totalSizePendingTransfer * 100);
			logDebug("Progress: " + progressPercent + "%");

			showRating(totalSizePendingTransfer, megaApi.getCurrentDownloadSpeed());

			String message = "";
			if (totalTransfers == 0){
				message = getString(R.string.download_preparing_files);
			}
			else{
				int inProgress = pendingTransfers == 0 ? totalTransfers
						: totalTransfers - pendingTransfers + 1;

				if (megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD)) {
					message = StringResourcesUtils.getString(R.string.download_service_notification_paused, inProgress, totalTransfers);
				} else {
					message = StringResourcesUtils.getString(R.string.download_service_notification, inProgress, totalTransfers);
				}
			}

			Intent intent;
			PendingIntent pendingIntent;

			String info = getProgressSize(DownloadService.this, totalSizeTransferred, totalSizePendingTransfer);

			Notification notification = null;

			String contentText = "";

			if(dbH.getCredentials()==null){
				contentText = getString(R.string.download_touch_to_cancel);
				intent = new Intent(DownloadService.this, LoginActivity.class);
				intent.setAction(ACTION_CANCEL_DOWNLOAD);
				pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			}
			else{
				contentText = getString(R.string.download_touch_to_show);
				intent = new Intent(DownloadService.this, ManagerActivity.class);
				intent.setAction(ACTION_SHOW_TRANSFERS);
				intent.putExtra(TRANSFERS_TAB, PENDING_TAB);
				pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD_ID, NOTIFICATION_CHANNEL_DOWNLOAD_NAME, NotificationManager.IMPORTANCE_DEFAULT);
				channel.setShowBadge(true);
				channel.setSound(null, null);
				mNotificationManager.createNotificationChannel(channel);

				NotificationCompat.Builder mBuilderCompat = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_DOWNLOAD_ID);

				mBuilderCompat
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setColor(ContextCompat.getColor(this,R.color.red_600_red_300))
						.setProgress(100, progressPercent, false)
						.setContentIntent(pendingIntent)
						.setOngoing(true).setContentTitle(message).setSubText(info)
						.setContentText(contentText)
						.setOnlyAlertOnce(true);

				notification = mBuilderCompat.build();
			}
			else {
				mBuilder
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setColor(ContextCompat.getColor(this,R.color.red_600_red_300))
						.setProgress(100, progressPercent, false)
						.setContentIntent(pendingIntent)
						.setOngoing(true).setContentTitle(message).setContentInfo(info)
						.setContentText(contentText)
						.setOnlyAlertOnce(true);

				notification = mBuilder.build();
			}

			if (!isForeground) {
				logDebug("Starting foreground!");
				try {
					startForeground(NOTIFICATION_DOWNLOAD, notification);
					isForeground = true;
				}
				catch (Exception e){
					isForeground = false;
				}
			} else {
				mNotificationManager.notify(NOTIFICATION_DOWNLOAD, notification);
			}
		}
	}

	/**
	 * Determine if should show the rating page to users
	 *
	 * @param total the total size of uploading file
	 * @param currentDownloadSpeed current downloading speed
	 */
	private void showRating(long total, int currentDownloadSpeed) {
		if (!isRatingShowed) {
			new RatingHandlerImpl(this)
					.showRatingBaseOnSpeedAndSize(total, currentDownloadSpeed, () -> isRatingShowed = true);
		}
	}

	private void cancel() {
		logDebug("cancel");
		canceled = true;
		stopForeground();
		rootNode = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void
	onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		rxSubscriptions.add(Single.just(transfer)
				.observeOn(Schedulers.single())
				.subscribe(this::doOnTransferStart,
						throwable -> logError("doOnTransferStart onError", throwable)));
	}

	private void doOnTransferStart(MegaTransfer transfer) {
		logDebug("Download start: " + transfer.getNodeHandle() + ", totalDownloads: " + megaApi.getTotalDownloads());

		if (transfer.isStreamingTransfer() || isVoiceClipType(transfer)) return;
		if (isBackgroundTransfer(transfer)) {
			backgroundTransfers.add(transfer.getTag());
			return;
		}

		if (transfer.getType() == MegaTransfer.TYPE_DOWNLOAD) {
			String appData = transfer.getAppData();

			if (!isTextEmpty(appData) && appData.contains(APP_DATA_SD_CARD)) {
				dbH.addSDTransfer(new SDTransfer(
						transfer.getTag(),
						transfer.getFileName(),
						getSizeString(transfer.getTotalBytes()),
						Long.toString(transfer.getNodeHandle()),
						transfer.getPath(),
						appData));
			}

			launchTransferUpdateIntent(MegaTransfer.TYPE_DOWNLOAD);
			transfersCount++;
			updateProgressNotification();
		}
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError error) {
 		rxSubscriptions.add(Single.just(true)
				.observeOn(Schedulers.single())
				.subscribe(ignored -> doOnTransferFinish(transfer, error),
						throwable -> logError("doOnTransferFinish onError", throwable)));
	}

	private void doOnTransferFinish(MegaTransfer transfer, MegaError error) {
		logDebug("Node handle: " + transfer.getNodeHandle() + ", Type = " + transfer.getType());

		if (transfer.isStreamingTransfer()) {
			return;
		}

		if (error.getErrorCode() == MegaError.API_EBUSINESSPASTDUE) {
			sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED));
		}

		if(transfer.getType()==MegaTransfer.TYPE_DOWNLOAD){
			boolean isVoiceClip = isVoiceClipType(transfer);
			boolean isBackgroundTransfer = isBackgroundTransfer(transfer);

			if(!isVoiceClip && !isBackgroundTransfer) transfersCount--;

			String path = transfer.getPath();
			String targetPath = getSDCardTargetPath(transfer.getAppData());

			if (!transfer.isFolderTransfer()) {
				if (!isVoiceClip && !isBackgroundTransfer) {
					AndroidCompletedTransfer completedTransfer = new AndroidCompletedTransfer(transfer, error);
					if (!isTextEmpty(targetPath)) {
						completedTransfer.setPath(targetPath);
					}

					addCompletedTransfer(completedTransfer);
				}

				launchTransferUpdateIntent(MegaTransfer.TYPE_DOWNLOAD);
				if (transfer.getState() == MegaTransfer.STATE_FAILED) {
					MegaApplication.getTransfersManagement().setFailedTransfers(true);
				}

				if (!isVoiceClip && !isBackgroundTransfer) {
					updateProgressNotification();
				}
			}

            if (canceled) {
				if((lock != null) && (lock.isHeld()))
					try{ lock.release(); } catch(Exception ex) {}
				if((wl != null) && (wl.isHeld()))
					try{ wl.release(); } catch(Exception ex) {}

				logDebug("Download canceled: " + transfer.getNodeHandle());

				if (isVoiceClip) {
					resultTransfersVoiceClip(transfer.getNodeHandle(), ERROR_VOICE_CLIP_TRANSFER);
					File localFile = buildVoiceClipFile(this, transfer.getFileName());
					if (isFileAvailable(localFile)) {
						logDebug("Delete own voiceclip : exists");
						localFile.delete();
					}
				} else {
					File file = new File(transfer.getPath());
					file.delete();
				}
				DownloadService.this.cancel();

			}
			else{
				if (error.getErrorCode() == MegaError.API_OK) {
					logDebug("Download OK - Node handle: " + transfer.getNodeHandle());

					if(isVoiceClip) {
						resultTransfersVoiceClip(transfer.getNodeHandle(), SUCCESSFUL_VOICE_CLIP_TRANSFER);
					}

                    //need to move downloaded file to a location on sd card.
                    if (targetPath != null) {
						File source = new File(path);

						try {
							SDCardOperator sdCardOperator = new SDCardOperator(this);
							sdCardOperator.moveDownloadedFileToDestinationPath(source, targetPath,
									getSDCardTargetUri(transfer.getAppData()), transfer.getTag());
						} catch (Exception e) {
							logError("Error moving file to the sd card path", e);
						}
                    }
					//To update thumbnails for videos
					if(isVideoFile(transfer.getPath())){
						logDebug("Is video!!!");
						MegaNode videoNode = megaApi.getNodeByHandle(transfer.getNodeHandle());
						if (videoNode != null){
							if(!videoNode.hasThumbnail()){
                                logDebug("The video has not thumb");
								ThumbnailUtils.createThumbnailVideo(this, path, megaApi, transfer.getNodeHandle());
							}
						}
						else{
							logWarning("videoNode is NULL");
						}
					}
					else{
						logDebug("NOT video!");
					}

					if (!isTextEmpty(path)) {
						sendBroadcastToUpdateGallery(this, new File(path));
					}

					if(storeToAdvacedDevices.containsKey(transfer.getNodeHandle())){
						logDebug("Now copy the file to the SD Card");
						openFile=false;
						Uri tranfersUri = storeToAdvacedDevices.get(transfer.getNodeHandle());
						MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
						alterDocument(tranfersUri, node.getName());
					}

					if(!isTextEmpty(path) && path.contains(OFFLINE_DIR)){
						logDebug("It is Offline file");
						dbH = DatabaseHandler.getDbHandler(getApplicationContext());
						offlineNode = megaApi.getNodeByHandle(transfer.getNodeHandle());

						if(offlineNode!=null){
							saveOffline(this, megaApi, dbH, offlineNode, transfer.getPath());
						}
						else{
							saveOfflineChatFile(dbH, transfer);
						}

						refreshOfflineFragment();
						refreshSettingsFragment();
					}
				}
				else
				{
					logError("Download ERROR: " + transfer.getNodeHandle());
					if(isVoiceClip){
						resultTransfersVoiceClip(transfer.getNodeHandle(), ERROR_VOICE_CLIP_TRANSFER);
						File localFile = buildVoiceClipFile(this, transfer.getFileName());
						if (isFileAvailable(localFile)) {
							logDebug("Delete own voice clip : exists");
							localFile.delete();
						}
					}else{
						if (error.getErrorCode() == MegaError.API_EBLOCKED) {
							errorEBloqued++;
						}

						if(!transfer.isFolderTransfer()){
							errorCount++;
						}

						if (!isTextEmpty(transfer.getPath())) {
							File file = new File(transfer.getPath());
							file.delete();
						}
					}
				}
			}

			if(isVoiceClip || isBackgroundTransfer) return;

			if (getNumPendingDownloadsNonBackground(megaApi) == 0 && transfersCount==0){
				onQueueComplete(transfer.getNodeHandle());
			}
		}
	}

	private void resultTransfersVoiceClip(long nodeHandle, int result){
		logDebug("nodeHandle =  " + nodeHandle + ", the result is " + result);
		Intent intent = new Intent(BROADCAST_ACTION_INTENT_VOICE_CLIP_DOWNLOADED);
		intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle);
		intent.putExtra(EXTRA_RESULT_TRANSFER, result);
		sendBroadcast(intent);
	}

	private void alterDocument(Uri uri, String fileName) {
		logDebug("alterUri");
	    try {

	    	File tempFolder = getCacheFolder(getApplicationContext(), TEMPORAL_FOLDER);
	    	if (!isFileAvailable(tempFolder)) return;

	    	String sourceLocation = tempFolder.getAbsolutePath() + File.separator +fileName;

	        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
	        FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

	    	InputStream in = new FileInputStream(sourceLocation);

	        // Copy the bits from instream to outstream
	        byte[] buf = new byte[1024];
	        int len;
	        while ((len = in.read(buf)) > 0) {
	        	fileOutputStream.write(buf, 0, len);
	        }
	        in.close();

	        // Let the document provider know you're done by closing the stream.
	        fileOutputStream.close();
	        pfd.close();

	        File deleteTemp = new File(sourceLocation);
	        deleteTemp.delete();
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		rxSubscriptions.add(Single.just(transfer)
				.observeOn(Schedulers.single())
				.subscribe(this::doOnTransferUpdate,
						throwable -> logError("doOnTransferUpdate onError", throwable)));
	}

	private void doOnTransferUpdate(MegaTransfer transfer) {
		if(transfer.getType()==MegaTransfer.TYPE_DOWNLOAD){
			launchTransferUpdateIntent(MegaTransfer.TYPE_DOWNLOAD);
			if (canceled) {
				logDebug("Transfer cancel: " + transfer.getNodeHandle());

				if((lock != null) && (lock.isHeld()))
					try{ lock.release(); } catch(Exception ex) {}
				if((wl != null) && (wl.isHeld()))
					try{ wl.release(); } catch(Exception ex) {}

				megaApi.cancelTransfer(transfer);
				DownloadService.this.cancel();
				return;
			}
			if(transfer.isStreamingTransfer() || isVoiceClipType(transfer)) return;
			if (isBackgroundTransfer(transfer)) {
				backgroundTransfers.add(transfer.getTag());
				return;
			}

			if(!transfer.isFolderTransfer()){
				sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE));
				updateProgressNotification();
			}

			if (!TransfersManagement.isOnTransferOverQuota() && MegaApplication.getTransfersManagement().hasNotToBeShowDueToTransferOverQuota()) {
				MegaApplication.getTransfersManagement().setHasNotToBeShowDueToTransferOverQuota(false);
			}
		}
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		rxSubscriptions.add(Single.just(true)
				.observeOn(Schedulers.single())
				.subscribe(ignored -> doOnTransferTemporaryError(transfer, e),
						throwable -> logError("doOnTransferTemporaryError onError", throwable)));
	}

	private void doOnTransferTemporaryError(MegaTransfer transfer, MegaError e) {
		logWarning("Download Temporary Error - Node Handle: " + transfer.getNodeHandle() +
				"\nError: " + e.getErrorCode() + " " + e.getErrorString());

		if (transfer.isStreamingTransfer() || isBackgroundTransfer(transfer)) {
			return;
		}

		if(transfer.getType()==MegaTransfer.TYPE_DOWNLOAD){
			if(e.getErrorCode() == MegaError.API_EOVERQUOTA) {
				if (e.getValue() != 0) {
					logWarning("TRANSFER OVERQUOTA ERROR: " + e.getErrorCode());
					checkTransferOverQuota(true);

					downloadedBytesToOverquota = megaApi.getTotalDownloadedBytes();
					isOverquota = true;
				}
			}
		}
	}

	/**
	 * Checks if should show transfer over quota warning.
	 * If so, sends a broadcast to show it in the current view.
	 *
	 * @param isCurrentOverQuota	true if the overquota is currently received, false otherwise
	 */
	private void checkTransferOverQuota(boolean isCurrentOverQuota) {
		TransfersManagement transfersManagement = MegaApplication.getTransfersManagement();

		if (app.isActivityVisible()) {
			if (transfersManagement.shouldShowTransferOverQuotaWarning()) {
				transfersManagement.setCurrentTransferOverQuota(isCurrentOverQuota);
				transfersManagement.setTransferOverQuotaTimestamp();
				sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE).setAction(ACTION_TRANSFER_OVER_QUOTA));
			}
		} else if (!transfersManagement.isTransferOverQuotaNotificationShown()){
			transfersManagement.setTransferOverQuotaNotificationShown(true);
			isForeground = false;
			stopForeground(true);
			mNotificationManager.cancel(NOTIFICATION_DOWNLOAD);
			new TransferOverQuotaNotification().show();
		}
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		logDebug("onRequestFinish");

		if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS){
			logDebug("TYPE_CANCEL_TRANSFERS finished");
			if (e.getErrorCode() == MegaError.API_OK){
				cancel();
			}

		}
		else if (request.getType() == MegaRequest.TYPE_LOGIN){
			if (e.getErrorCode() == MegaError.API_OK){
				logDebug("Logged in. Setting account auth token for folder links.");
				megaApiFolder.setAccountAuth(megaApi.getAccountAuth());
				logDebug("Fast login OK, Calling fetchNodes from CameraSyncService");
				megaApi.fetchNodes();

                // Get cookies settings after login.
                MegaApplication.getInstance().checkEnabledCookies();
			}
			else{
				logError("ERROR: " + e.getErrorString());
				isLoggingIn = false;
				MegaApplication.setLoggingIn(isLoggingIn);
			}
		}
		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
			if (e.getErrorCode() == MegaError.API_OK){
				isLoggingIn = false;
				MegaApplication.setLoggingIn(isLoggingIn);

				for (int i=0;i<pendingIntents.size();i++){
					onHandleIntent(pendingIntents.get(i));
				}
				pendingIntents.clear();
			}
			else{
				logError("ERROR: " + e.getErrorString());
				isLoggingIn = false;
				MegaApplication.setLoggingIn(isLoggingIn);
			}
		} else {
			logDebug("Public node received");

			if (e.getErrorCode() != MegaError.API_OK) {
				logError("Public node error");
				return;
			}

			MegaNode node = request.getPublicMegaNode();
			if (node == null) {
				logError("Public node is null");
				return;
			}

			if (currentDir == null) {
				logError("currentDir is null");
				return;
			}

			if (currentDir.isDirectory()) {
				currentFile = new File(currentDir, megaApi.escapeFsIncompatible(node.getName(), currentDir.getAbsolutePath() + SEPARATOR));
			} else {
				currentFile = currentDir;
			}

			String appData = getSDCardAppData(intent);

			logDebug("Public node download launched");
			if (!wl.isHeld()) wl.acquire();
			if (!lock.isHeld()) lock.acquire();
			if (currentDir.isDirectory()) {
				logDebug("To downloadPublic(dir)");
				if (!isTextEmpty(appData)) {
					megaApi.startDownloadWithData(node, currentDir.getAbsolutePath() + "/", appData);
				} else {
					megaApi.startDownload(node, currentDir.getAbsolutePath() + "/");
				}
			}
		}
	}


	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		logWarning("Node handle: " + request.getNodeHandle());
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		logDebug("onRequestUpdate");
	}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer)
	{
		return true;
	}

	private void refreshOfflineFragment(){
		sendBroadcast(new Intent(OfflineFragment.REFRESH_OFFLINE_FILE_LIST));
	}

	private void refreshSettingsFragment() {
		Intent intent = new Intent(BROADCAST_ACTION_INTENT_SETTINGS_UPDATED);
		intent.setAction(ACTION_REFRESH_CLEAR_OFFLINE_SETTING);
		sendBroadcast(intent);
	}
}
