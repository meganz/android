package mega.privacy.android.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
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
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Formatter;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.OfflineFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SettingsFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.SDCardOperator;
import mega.privacy.android.app.utils.TL;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;

/*
 * Background service to download files
 */
public class DownloadService extends Service implements MegaTransferListenerInterface, MegaRequestListenerInterface, MegaChatRequestListenerInterface {

	// Action to stop download
	public static String ACTION_CANCEL = "CANCEL_DOWNLOAD";
	public static String EXTRA_SIZE = "DOCUMENT_SIZE";
	public static String EXTRA_HASH = "DOCUMENT_HASH";
	public static String EXTRA_URL = "DOCUMENT_URL";
	public static String EXTRA_DOWNLOAD_TO_SDCARD = "download_to_sdcard";
	public static String EXTRA_TARGET_ROOT = "download_root";
	public static String EXTRA_PATH = "SAVE_PATH";
	public static String EXTRA_FOLDER_LINK = "FOLDER_LINK";
	public static String EXTRA_CONTACT_ACTIVITY = "CONTACT_ACTIVITY";
	public static String EXTRA_ZIP_FILE_TO_OPEN = "FILE_TO_OPEN";
	public static String EXTRA_OPEN_FILE = "OPEN_FILE";
	public static String EXTRA_CONTENT_URI = "CONTENT_URI";
	public static String EXTRA_SERIALIZE_STRING = "SERIALIZE_STRING";

	public static String DB_FILE = "0";
	public static String DB_FOLDER = "1";

	private int errorCount = 0;
	private int alreadyDownloaded = 0;

	private boolean isForeground = false;
	private boolean canceled;

	private String pathFileToOpen;

	private boolean openFile = true;

	private boolean isOverquota = false;
	private long downloadedBytesToOverquota = 0;

	MegaApplication app;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	MegaChatApiAndroid megaChatApi;
	ChatSettings chatSettings;

	ArrayList<Intent> pendingIntents = new ArrayList<Intent>();

	WifiLock lock;
	WakeLock wl;

	File currentFile;
	File currentDir;
	private Map<Long,String> targetPaths = new HashMap<>();
	MegaNode currentDocument;

	DatabaseHandler dbH = null;

	int transfersCount = 0;

	HashMap<Long, Uri> storeToAdvacedDevices;
	HashMap<Long, Boolean> fromMediaViewers;

	private int notificationId = Constants.NOTIFICATION_DOWNLOAD;
	private int notificationIdFinal = Constants.NOTIFICATION_DOWNLOAD_FINAL;
	private String notificationChannelId = Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID;
	private String notificationChannelName = Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME;
	private NotificationCompat.Builder mBuilderCompat;
	private Notification.Builder mBuilder;
	private NotificationManager mNotificationManager;

	MegaNode offlineNode;

	boolean isLoggingIn = false;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(){
		super.onCreate();
		log("onCreate");

		app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		megaApiFolder = app.getMegaApiFolder();
		megaChatApi = app.getMegaChatApi();

		isForeground = false;
		canceled = false;

		storeToAdvacedDevices = new HashMap<Long, Uri>();
		fromMediaViewers = new HashMap<>();

		int wifiLockMode = WifiManager.WIFI_MODE_FULL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF;
        }

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		WifiManager wifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		lock = wifiManager.createWifiLock(wifiLockMode, "MegaDownloadServiceWifiLock");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaDownloadServicePowerLock");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			mBuilder = new Notification.Builder(DownloadService.this);
		}
		mBuilderCompat = new NotificationCompat.Builder(getApplicationContext());

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

		if (megaChatApi != null){
			megaChatApi.saveCurrentState();
		}

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		log("onStartCommand");
		canceled = false;

		if(intent == null){
			log("intent==null");
			return START_NOT_STICKY;
		}

		if (intent.getAction() != null){
			if (intent.getAction().equals(ACTION_CANCEL)){
				log("Cancel intent");
				canceled = true;
				megaApi.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD, this);
				megaApiFolder.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD, this);
				return START_NOT_STICKY;
			}
		}

		onHandleIntent(intent);
		return START_NOT_STICKY;
	}

    protected void onHandleIntent(final Intent intent) {
        log("onHandleIntent");

        long hash = intent.getLongExtra(EXTRA_HASH, -1);
        String url = intent.getStringExtra(EXTRA_URL);
        boolean isFolderLink = intent.getBooleanExtra(EXTRA_FOLDER_LINK, false);
        openFile = intent.getBooleanExtra(EXTRA_OPEN_FILE, true);
        if(intent.getBooleanExtra(EXTRA_DOWNLOAD_TO_SDCARD, false)) {
            targetPaths.put(hash, intent.getStringExtra(EXTRA_TARGET_ROOT));
        }
		Uri contentUri = null;
        if(intent.getStringExtra(EXTRA_CONTENT_URI)!=null){
            contentUri = Uri.parse(intent.getStringExtra(EXTRA_CONTENT_URI));
        }

        boolean highPriority = intent.getBooleanExtra(Constants.HIGH_PRIORITY_TRANSFER, false);

        boolean fromMV = intent.getBooleanExtra("fromMV", false);
        log("fromMV: "+fromMV);

		megaApi = app.getMegaApi();

		UserCredentials credentials = dbH.getCredentials();

		if (credentials != null) {

			String gSession = credentials.getSession();

			if (megaApi.getRootNode() == null) {
				isLoggingIn = MegaApplication.isLoggingIn();
				if (!isLoggingIn) {
					isLoggingIn = true;
					MegaApplication.setLoggingIn(isLoggingIn);

					if (Util.isChatEnabled()) {
						if (megaChatApi == null) {
							megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
						}

						int ret = megaChatApi.getInitState();

						if(ret==MegaChatApi.INIT_NOT_DONE||ret==MegaChatApi.INIT_ERROR){
							ret = megaChatApi.init(gSession);
							log("result of init ---> " + ret);
							chatSettings = dbH.getChatSettings();
							if (ret == MegaChatApi.INIT_NO_CACHE) {
								log("condition ret == MegaChatApi.INIT_NO_CACHE");
								megaChatApi.enableGroupChatCalls(true);
							} else if (ret == MegaChatApi.INIT_ERROR) {
								log("condition ret == MegaChatApi.INIT_ERROR");
								if (chatSettings == null) {
									log("ERROR----> Switch OFF chat");
									chatSettings = new ChatSettings();
									chatSettings.setEnabled(false+"");
									dbH.setChatSettings(chatSettings);
								} else {
									log("ERROR----> Switch OFF chat");
									dbH.setEnabledChat(false + "");
								}
								megaChatApi.logout(this);
							} else {
								log("Chat correctly initialized");
								megaChatApi.enableGroupChatCalls(true);
							}
						}
					}

					pendingIntents.add(intent);
					updateProgressNotification();
					megaApi.fastLogin(gSession, this);
					return;
				}
				else{
					log("Another login is processing");
				}
				pendingIntents.add(intent);
				return;
			}
		}

		String serialize = intent.getStringExtra(EXTRA_SERIALIZE_STRING);

		if(serialize!=null){
			log("serializeString: "+serialize);
			currentDocument = MegaNode.unserialize(serialize);
			if(currentDocument != null){
				hash = currentDocument.getHandle();
				log("hash after unserialize: "+hash);
			}
			else{
				log("Node is NULL after unserialize");
			}
		}
		else{
			if (isFolderLink){
				currentDocument = megaApiFolder.getNodeByHandle(hash);
			}
			else{
				currentDocument = megaApi.getNodeByHandle(hash);
			}
		}

        if(intent.getStringExtra(EXTRA_ZIP_FILE_TO_OPEN)!=null){
            pathFileToOpen = intent.getStringExtra(EXTRA_ZIP_FILE_TO_OPEN);
        }
        else{
            pathFileToOpen=null;
        }

        if(url != null){
            log("Public node");
            currentDir = new File(intent.getStringExtra(EXTRA_PATH));
            if (currentDir != null){
                currentDir.mkdirs();
            }
            megaApi.getPublicNode(url, this);
            return;
        }

		if((currentDocument == null) && (url == null)){
			log("Node not found");
			return;
		}

		fromMediaViewers.put(currentDocument.getHandle(), fromMV);

        currentDir = getDir(currentDocument, intent);
        currentDir.mkdirs();
        if (currentDir.isDirectory()){
            log("currentDir is Directory");
            currentFile = new File(currentDir, megaApi.escapeFsIncompatible(currentDocument.getName()));
        }
        else{
            log("currentDir is File");
            currentFile = currentDir;
        }

        log("dir: " + currentDir.getAbsolutePath() + " file: " + currentDocument.getName() + "  Size: " + currentDocument.getSize());
        if(!checkCurrentFile(currentDocument)){
            log("checkCurrentFile == false");

			alreadyDownloaded++;
            if ((megaApi.getNumPendingDownloads() == 0) && (megaApiFolder.getNumPendingDownloads() == 0)){
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

        if(contentUri!=null){
			log("contentUri is NOT null");
            //To download to Advanced Devices
            log("Download to advanced devices checked");
            currentDir = new File(intent.getStringExtra(EXTRA_PATH));
            currentDir.mkdirs();

            if (currentDir.isDirectory()){
                log("To download(dir): " + currentDir.getAbsolutePath() + "/");
            }
            else{
                log("currentDir is not a directory");
            }
            storeToAdvacedDevices.put(currentDocument.getHandle(), contentUri);

			if (currentDir.getAbsolutePath().contains(Util.offlineDIR)){
				log("currentDir contains offlineDIR");
				openFile = false;
			}
			else {
				log("currentDir is NOT on offlineDIR: openFile->"+openFile);
			}

			if (isFolderLink){
				if (dbH.getCredentials() == null) {
					megaApiFolder.startDownload(currentDocument, currentDir.getAbsolutePath() + "/", this);
					log("getCredentials null");
					return;
				}

				log("Folder link node");
				MegaNode currentDocumentAuth = megaApiFolder.authorizeNode(currentDocument);
				if (currentDocumentAuth == null){
					log("CurrentDocumentAuth is null");
					megaApiFolder.startDownload(currentDocument, currentDir.getAbsolutePath() + "/", this);
					return;
				}
				else{
					log("CurrentDocumentAuth is not null");
					currentDocument = megaApiFolder.authorizeNode(currentDocument);
				}
			}

			log("CurrentDocument is not null");

			if(highPriority){
				megaApi.startDownloadWithTopPriority(currentDocument, currentDir.getAbsolutePath() + "/", "", this);
			}
			else{
				megaApi.startDownload(currentDocument, currentDir.getAbsolutePath() + "/", this);
			}
        }
        else{
			log("contentUri NULL");
            if (currentDir.isDirectory()){
                log("To download(dir): " + currentDir.getAbsolutePath() + "/");

                if(currentFile.exists()){
                    log("The file already exists!");
                    //Check the fingerprint
                    String localFingerprint = megaApi.getFingerprint(currentFile.getAbsolutePath());
                    String megaFingerprint = megaApi.getFingerprint(currentDocument);

                    if((localFingerprint!=null) && (!localFingerprint.isEmpty()) && (megaFingerprint!=null) && (!megaFingerprint.isEmpty()))
                    {
                        if(localFingerprint.compareTo(megaFingerprint)!=0)
                        {
                            log("Delete the old version");
                            currentFile.delete();
                        }
                    }
                }

                if (currentDocument.isFolder()){
                    log("IS FOLDER_:_");
                }
                else{
                    log("IS FILE_:_");
                }

				if (currentDir.getAbsolutePath().contains(Util.offlineDIR)){
                	log("currentDir contains offlineDIR");
					openFile = false;
				}
				else {
					log("currentDir is NOT on offlineDIR: openFile->"+openFile);
				}

                if (isFolderLink){

                    log("Folder link node");
                    MegaNode currentDocumentAuth = megaApiFolder.authorizeNode(currentDocument);
                    if (currentDocumentAuth == null){
                        log("CurrentDocumentAuth is null");
                        megaApiFolder.startDownload(currentDocument, currentDir.getAbsolutePath() + "/", this);
                        return;
                    }
                    else{
						log("CurrentDocumentAuth is not null");
						currentDocument = megaApiFolder.authorizeNode(currentDocument);
					}
                }

                log("CurrentDocument is not null");
				if(highPriority){
					megaApi.startDownloadWithTopPriority(currentDocument, currentDir.getAbsolutePath() + "/", "", this);
				}
				else{
					megaApi.startDownload(currentDocument, currentDir.getAbsolutePath() + "/", this);
				}

            }
            else{
                log("currentDir is not a directory");

            }
        }
    }

	private void onQueueComplete(long handle) {
		log("onQueueComplete");

		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}

        showCompleteNotification(handle);

		isForeground = false;
		stopForeground(true);
		mNotificationManager.cancel(notificationId);
		stopSelf();

		int total = megaApi.getNumPendingDownloads() + megaApiFolder.getNumPendingDownloads();
		log("onQueueComplete: total of files before reset " + total);
		if(total <= 0){
			log("onQueueComplete: reset total downloads");
			megaApi.resetTotalDownloads();
			megaApiFolder.resetTotalDownloads();
			errorCount = 0;
			alreadyDownloaded = 0;
		}
	}

	private File getDir(MegaNode document, Intent intent) {
		log("getDir");
		boolean toDownloads = (intent.hasExtra(EXTRA_PATH) == false);
		File destDir;
		if (toDownloads) {
			destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		} else {
			destDir = new File(intent.getStringExtra(EXTRA_PATH));
		}
		log("save to: " + destDir.getAbsolutePath());
		return destDir;
	}

	boolean checkCurrentFile(MegaNode document)	{
		log("checkCurrentFile");
		if(currentFile.exists() && (document.getSize() == currentFile.length())){

			currentFile.setReadable(true, false);
//			Toast.makeText(getApplicationContext(), document.getName() + " " +  getString(R.string.general_already_downloaded), Toast.LENGTH_SHORT).show();

			return false;
		}

		if(document.getSize() > ((long)1024*1024*1024*4))
		{
			log("show size alert: " + document.getSize());
	    	Toast.makeText(getApplicationContext(), getString(R.string.error_file_size_greater_than_4gb),
	    			Toast.LENGTH_LONG).show();
	    	Toast.makeText(getApplicationContext(), getString(R.string.error_file_size_greater_than_4gb),
	    			Toast.LENGTH_LONG).show();
	    	Toast.makeText(getApplicationContext(), getString(R.string.error_file_size_greater_than_4gb),
	    			Toast.LENGTH_LONG).show();
		}
		return true;
	}

	/*
	 * Show download success notification
	 */
	private void showCompleteNotification(long handle) {
		log("showCompleteNotification");
		String notificationTitle, size;

        int totalDownloads = megaApi.getTotalDownloads() + megaApiFolder.getTotalDownloads();

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
			int totalNumber = totalDownloads + errorCount;
			notificationTitle = getResources().getQuantityString(R.plurals.download_service_final_notification_with_details, totalNumber, totalDownloads, totalNumber);

			size = getResources().getQuantityString(R.plurals.download_service_failed, errorCount, errorCount);
		}
		else{
			notificationTitle = getResources().getQuantityString(R.plurals.download_service_final_notification, totalDownloads, totalDownloads);
			String totalBytes = Formatter.formatFileSize(DownloadService.this, megaApi.getTotalDownloadedBytes()+megaApiFolder.getTotalDownloadedBytes());
			size = getString(R.string.general_total_size, totalBytes);
		}

		Intent intent = null;
		if(totalDownloads != 1)
		{
			intent = new Intent(getApplicationContext(), ManagerActivityLollipop.class);

			log("Show notification 1");
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

				mBuilderCompatO.setColor(ContextCompat.getColor(this, R.color.mega));

				mNotificationManager.notify(notificationIdFinal, mBuilderCompatO.build());
			}
			else {
				mBuilderCompat
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
						.setAutoCancel(true).setTicker(notificationTitle)
						.setContentTitle(notificationTitle).setContentText(size)
						.setOngoing(false);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					mBuilderCompat.setColor(ContextCompat.getColor(this, R.color.mega));
				}

				mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
			}
		}
		else
		{
			try {
                boolean autoPlayEnabled = Boolean.parseBoolean(dbH.getAutoPlayEnabled());
                if (openFile && autoPlayEnabled) {
                    log("both openFile and autoPlayEnabled are true");
					Boolean externalFile;
					if (!currentFile.getAbsolutePath().contains(Environment.getExternalStorageDirectory().getPath())){
						externalFile = true;
					}
					else {
						externalFile = false;
					}

					boolean fromMV = false;
					if (fromMediaViewers.containsKey(handle)){
						fromMV = fromMediaViewers.get(handle);
					}

					if (MimeTypeList.typeForName(currentFile.getName()).isZip()){
						log("Download success of zip file!");

						if(pathFileToOpen!=null){
							Intent intentZip;
							intentZip = new Intent(this, ZipBrowserActivityLollipop.class);
							intentZip.setAction(ZipBrowserActivityLollipop.ACTION_OPEN_ZIP_FILE);
							intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_ZIP_FILE_TO_OPEN, pathFileToOpen);
							intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, currentFile.getAbsolutePath());
							intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_HANDLE_ZIP, currentDocument.getHandle());


							if(intentZip!=null){
								intentZip.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intentZip);
							}
						}
						else{
							Intent intentZip = null;

							intentZip = new Intent(this, ManagerActivityLollipop.class);
							intentZip.setAction(Constants.ACTION_EXPLORE_ZIP);
							intentZip.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							intentZip.putExtra(Constants.EXTRA_PATH_ZIP, currentFile.getAbsolutePath());

							startActivity(intentZip);
						}

						log("Lanzo intent al manager.....");
					}
					else if (MimeTypeList.typeForName(currentFile.getName()).isPdf()){
						log("Pdf file");

						if (!fromMV) {
							Intent pdfIntent = new Intent(this, PdfViewerActivityLollipop.class);

							pdfIntent.putExtra("HANDLE", handle);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !externalFile) {
								pdfIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
							} else {
								pdfIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
							}
							pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
							pdfIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							pdfIntent.putExtra("fromDownloadService", true);
							pdfIntent.putExtra("inside", true);
							pdfIntent.putExtra("isUrl", false);
							startActivity(pdfIntent);
						}
						else {
							log("Show notification 2");
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
					else if (MimeTypeList.typeForName(currentFile.getName()).isVideoReproducible() || MimeTypeList.typeForName(currentFile.getName()).isAudio()) {
						log("Video/Audio file");

						if (!fromMV) {
							Intent mediaIntent;
							boolean internalIntent;
							boolean opusFile = false;
							if (MimeTypeList.typeForName(currentFile.getName()).isVideoNotSupported() || MimeTypeList.typeForName(currentFile.getName()).isAudioNotSupported()) {
								mediaIntent = new Intent(Intent.ACTION_VIEW);
								internalIntent = false;
								String[] s = currentFile.getName().split("\\.");
								if (s != null && s.length > 1 && s[s.length - 1].equals("opus")) {
									opusFile = true;
								}
							} else {
								internalIntent = true;
								mediaIntent = new Intent(this, AudioVideoPlayerLollipop.class);
							}

							mediaIntent.putExtra("isPlayList", false);
							mediaIntent.putExtra("HANDLE", handle);
							mediaIntent.putExtra("fromDownloadService", true);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !externalFile) {
								mediaIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
							} else {
								mediaIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
							}
							if (opusFile) {
								mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
							}
							mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
							mediaIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

							if (internalIntent) {
								startActivity(mediaIntent);
							}
							else {
								if (MegaApiUtils.isIntentAvailable(this, mediaIntent)) {
									startActivity(mediaIntent);
								}
								else {
									Intent intentShare = new Intent(Intent.ACTION_SEND);
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !externalFile) {
										intentShare.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
									} else {
										intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
									}
									intentShare.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
									if (MegaApiUtils.isIntentAvailable(this, mediaIntent)) {
										startActivity(intentShare);
									}
								}
							}
						}
						else {
							log("Show notification 2");
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
					else if (MimeTypeList.typeForName(currentFile.getName()).isDocument()) {
						log("Download is document");

						Intent viewIntent = new Intent(Intent.ACTION_VIEW);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
							viewIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						} else {
							viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						}
						viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

						if (MegaApiUtils.isIntentAvailable(this, viewIntent))
							startActivity(viewIntent);
						else {
							viewIntent.setAction(Intent.ACTION_GET_CONTENT);

							if (MegaApiUtils.isIntentAvailable(this, viewIntent))
								startActivity(viewIntent);
							else {
								Intent intentShare = new Intent(Intent.ACTION_SEND);
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
									intentShare.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
								} else {
									intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
								}
								intentShare.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
								startActivity(intentShare);
							}
						}
					} else if (MimeTypeList.typeForName(currentFile.getName()).isImage()) {
						log("Download is IMAGE");
						if (!fromMV){
							Intent viewIntent = new Intent(Intent.ACTION_VIEW);
							//					viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
								viewIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
							} else {
								viewIntent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
							}
							viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

							if (MegaApiUtils.isIntentAvailable(this, viewIntent))
								startActivity(viewIntent);
							else {
								Intent intentShare = new Intent(Intent.ACTION_SEND);
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
									intentShare.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
								} else {
									intentShare.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
								}
								intentShare.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
								startActivity(intentShare);
							}
						}
						else {
							log("Show notification 2");
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

								mBuilderCompatO.setColor(ContextCompat.getColor(this, R.color.mega));

								mNotificationManager.notify(notificationIdFinal, mBuilderCompatO.build());
							}
							else {
								mBuilderCompat
										.setSmallIcon(R.drawable.ic_stat_notify)
										.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
										.setAutoCancel(true).setTicker(notificationTitle)
										.setContentTitle(notificationTitle).setContentText(size)
										.setOngoing(false);

								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
									mBuilderCompat.setColor(ContextCompat.getColor(this, R.color.mega));
								}

								mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
							}
						}

					}
					else if (MimeTypeList.typeForName(currentFile.getName()).isURL()) {
						log("Is URL file");
						InputStream instream = null;

						try {
							// open the file for reading
							instream = new FileInputStream(currentFile.getAbsolutePath());

							// if file the available for reading
							if (instream != null) {
								// prepare the file for reading
								InputStreamReader inputreader = new InputStreamReader(instream);
								BufferedReader buffreader = new BufferedReader(inputreader);

								String line1 = buffreader.readLine();
								if(line1!=null){
									String line2= buffreader.readLine();

									String url = line2.replace("URL=","");

									log("Is URL - launch browser intent");
									Intent i = new Intent(Intent.ACTION_VIEW);
									i.setData(Uri.parse(url));
									startActivity(i);
								}
								else{
									log("Not expected format: Exception on processing url file");
									intent = new Intent(Intent.ACTION_VIEW);
									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
										intent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), "text/plain");
									} else {
										intent.setDataAndType(Uri.fromFile(currentFile), "text/plain");
									}
									intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

									if (MegaApiUtils.isIntentAvailable(this, intent)){
										startActivity(intent);
									}
									else{
										log("No app to url file as text: show notification");
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

											mBuilderCompatO.setColor(ContextCompat.getColor(this, R.color.mega));

											mNotificationManager.notify(notificationIdFinal, mBuilderCompatO.build());
										}
										else {
											mBuilderCompat
													.setSmallIcon(R.drawable.ic_stat_notify)
													.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
													.setAutoCancel(true).setTicker(notificationTitle)
													.setContentTitle(notificationTitle).setContentText(size)
													.setOngoing(false);

											if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
												mBuilderCompat.setColor(ContextCompat.getColor(this, R.color.mega));
											}

											mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
										}
									}
								}
							}
						} catch (Exception ex) {

							intent = new Intent(Intent.ACTION_VIEW);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
								intent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), "text/plain");
							} else {
								intent.setDataAndType(Uri.fromFile(currentFile), "text/plain");
							}
							intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

							if (MegaApiUtils.isIntentAvailable(this, intent)){
								startActivity(intent);
							}
							else{
								log("Exception on processing url file: show notification");
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

									mBuilderCompatO.setColor(ContextCompat.getColor(this, R.color.mega));

									mNotificationManager.notify(notificationIdFinal, mBuilderCompatO.build());
								}
								else {
									mBuilderCompat
											.setSmallIcon(R.drawable.ic_stat_notify)
											.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
											.setAutoCancel(true).setTicker(notificationTitle)
											.setContentTitle(notificationTitle).setContentText(size)
											.setOngoing(false);

									if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
										mBuilderCompat.setColor(ContextCompat.getColor(this, R.color.mega));
									}

									mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
								}
							}

						} finally {
							// close the file.
							instream.close();
						}

					}else {
						log("Download is OTHER FILE");
						intent = new Intent(Intent.ACTION_VIEW);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
							intent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						} else {
							intent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
						}
						intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

						if (MegaApiUtils.isIntentAvailable(this, intent))
							startActivity(intent);
						else {
							log("Not intent available for ACTION_VIEW");
							intent.setAction(Intent.ACTION_GET_CONTENT);

							if (MegaApiUtils.isIntentAvailable(this, intent))
								startActivity(intent);
							else {
								log("Not intent available for ACTION_GET_CONTENT");
								intent.setAction(Intent.ACTION_SEND);
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
									intent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
								} else {
									intent.setDataAndType(Uri.fromFile(currentFile), MimeTypeList.typeForName(currentFile.getName()).getType());
								}
								intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
								startActivity(intent);
							}
						}

						log("Show notification 2");
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

							mBuilderCompatO.setColor(ContextCompat.getColor(this, R.color.mega));

							mNotificationManager.notify(notificationIdFinal, mBuilderCompatO.build());
						}
						else {
							mBuilderCompat
									.setSmallIcon(R.drawable.ic_stat_notify)
									.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
									.setAutoCancel(true).setTicker(notificationTitle)
									.setContentTitle(notificationTitle).setContentText(size)
									.setOngoing(false);

							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
								mBuilderCompat.setColor(ContextCompat.getColor(this, R.color.mega));
							}

							mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
						}
					}
				} else {
					openFile = true; //Set the openFile to the default

					intent = new Intent(getApplicationContext(), ManagerActivityLollipop.class);

					log("Show notification 3");
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

						mBuilderCompatO.setColor(ContextCompat.getColor(this, R.color.mega));

						mNotificationManager.notify(notificationIdFinal, mBuilderCompatO.build());
					}
					else {
						mBuilderCompat
								.setSmallIcon(R.drawable.ic_stat_notify)
								.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
								.setAutoCancel(true).setTicker(notificationTitle)
								.setContentTitle(notificationTitle).setContentText(size)
								.setOngoing(false);

						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
							mBuilderCompat.setColor(ContextCompat.getColor(this, R.color.mega));
						}

						mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
					}
				}
			}
			catch (Exception e){
				openFile = true; //Set the openFile to the default
				log("Exception: " + e.getMessage());
				intent = new Intent(getApplicationContext(), ManagerActivityLollipop.class);

				log("Show notification 4");
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

					mBuilderCompatO.setColor(ContextCompat.getColor(this, R.color.mega));

					mNotificationManager.notify(notificationIdFinal, mBuilderCompatO.build());
				}
				else {
					mBuilderCompat
							.setSmallIcon(R.drawable.ic_stat_notify)
							.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, intent, 0))
							.setAutoCancel(true).setTicker(notificationTitle)
							.setContentTitle(notificationTitle).setContentText(size)
							.setOngoing(false);

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						mBuilderCompat.setColor(ContextCompat.getColor(this, R.color.mega));
					}

					mNotificationManager.notify(notificationIdFinal, mBuilderCompat.build());
				}
			}
		}
	}


	/*
	 * Update notification download progress
	 */
	@SuppressLint("NewApi")
	private void updateProgressNotification() {

		int pendingTransfers = megaApi.getNumPendingDownloads() + megaApiFolder.getNumPendingDownloads();
        int totalTransfers = megaApi.getTotalDownloads() + megaApiFolder.getTotalDownloads();

        long totalSizePendingTransfer = megaApi.getTotalDownloadBytes() + megaApiFolder.getTotalDownloadBytes();
        long totalSizeTransferred = megaApi.getTotalDownloadedBytes() + megaApiFolder.getTotalDownloadedBytes();

		boolean update;

		if(isOverquota){
			log("Overquota flag! is TRUE");
			if(downloadedBytesToOverquota<=totalSizeTransferred){
				update = false;
			}
			else{
				update = true;
				log("Change overquota flag");
				isOverquota = false;
			}
		}
		else{
			log("NOT overquota flag");
			update = true;
		}

		if(update){
			int progressPercent = (int) Math.round((double) totalSizeTransferred / totalSizePendingTransfer * 100);
			log("updateProgressNotification: "+progressPercent);

			String message = "";
			if (totalTransfers == 0){
				message = getString(R.string.download_preparing_files);
			}
			else{
				int inProgress = totalTransfers - pendingTransfers + 1;
				message = getResources().getQuantityString(R.plurals.download_service_notification, totalTransfers, inProgress, totalTransfers);
			}

			Intent intent;
			PendingIntent pendingIntent;

			String info = Util.getProgressSize(DownloadService.this, totalSizeTransferred, totalSizePendingTransfer);

			Notification notification = null;

			String contentText = "";

			if(dbH.getCredentials()==null){
				contentText = getString(R.string.download_touch_to_cancel);
				intent = new Intent(DownloadService.this, LoginActivityLollipop.class);
				intent.setAction(Constants.ACTION_CANCEL_DOWNLOAD);
				pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, 0);
			}
			else{
				contentText = getString(R.string.download_touch_to_show);
				intent = new Intent(DownloadService.this, ManagerActivityLollipop.class);
				intent.setAction(Constants.ACTION_SHOW_TRANSFERS);
				pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, 0);
			}

			int currentapiVersion = android.os.Build.VERSION.SDK_INT;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
				channel.setShowBadge(true);
				channel.setSound(null, null);
				mNotificationManager.createNotificationChannel(channel);

				NotificationCompat.Builder mBuilderCompat = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

				mBuilderCompat
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setColor(ContextCompat.getColor(this,R.color.mega))
						.setProgress(100, progressPercent, false)
						.setContentIntent(pendingIntent)
						.setOngoing(true).setContentTitle(message).setSubText(info)
						.setContentText(contentText)
						.setOnlyAlertOnce(true);

				notification = mBuilderCompat.build();
			}
			else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				mBuilder
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setColor(ContextCompat.getColor(this,R.color.mega))
						.setProgress(100, progressPercent, false)
						.setContentIntent(pendingIntent)
						.setOngoing(true).setContentTitle(message).setSubText(info)
						.setContentText(contentText)
						.setOnlyAlertOnce(true);
				notification = mBuilder.build();
			}
			else if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			{
				mBuilder
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setProgress(100, progressPercent, false)
						.setContentIntent(pendingIntent)
						.setOngoing(true).setContentTitle(message).setContentInfo(info)
						.setContentText(contentText)
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
				notification.contentView.setProgressBar(R.id.status_progress, 100, progressPercent, false);
			}

			if (!isForeground) {
				log("starting foreground!");
				try {
					startForeground(notificationId, notification);
					isForeground = true;
				}
				catch (Exception e){
					isForeground = false;
				}
			} else {
				mNotificationManager.notify(notificationId, notification);
			}
		}
	}

	private void showTransferOverquotaNotification(){
		log("showTransferOverquotaNotification");

		long totalSizePendingTransfer = megaApi.getTotalDownloadBytes() + megaApiFolder.getTotalDownloadBytes();
		long totalSizeTransferred = megaApi.getTotalDownloadedBytes() + megaApiFolder.getTotalDownloadedBytes();

		int progressPercent = (int) Math.round((double) totalSizeTransferred / totalSizePendingTransfer * 100);
		log("updateProgressNotification: "+progressPercent);

		Intent intent;
		PendingIntent pendingIntent;

		String info = Util.getProgressSize(DownloadService.this, totalSizeTransferred, totalSizePendingTransfer);

		Notification notification = null;

		String contentText = getString(R.string.download_show_info);
		String message = getString(R.string.title_depleted_transfer_overquota);

		if(megaApi.isLoggedIn()==0 || dbH.getCredentials()==null){
			dbH.clearEphemeral();
			intent = new Intent(DownloadService.this, LoginActivityLollipop.class);
			intent.setAction(Constants.ACTION_OVERQUOTA_TRANSFER);
			pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, 0);
		}
		else{
			intent = new Intent(DownloadService.this, ManagerActivityLollipop.class);
			intent.setAction(Constants.ACTION_OVERQUOTA_TRANSFER);
			pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, intent, 0);
		}

		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_DEFAULT);
			channel.setShowBadge(true);
			channel.setSound(null, null);
			mNotificationManager.createNotificationChannel(channel);

			NotificationCompat.Builder mBuilderCompat = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

			mBuilderCompat
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setColor(ContextCompat.getColor(this,R.color.mega))
					.setProgress(100, progressPercent, false)
					.setContentIntent(pendingIntent)
					.setOngoing(true).setContentTitle(message).setSubText(info)
					.setContentText(contentText)
					.setOnlyAlertOnce(true);

			notification = mBuilderCompat.build();
		}
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			mBuilder
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setColor(ContextCompat.getColor(this,R.color.mega))
					.setProgress(100, progressPercent, false)
					.setContentIntent(pendingIntent)
					.setOngoing(true).setContentTitle(message).setSubText(info)
					.setContentText(contentText)
					.setOnlyAlertOnce(true);

			notification = mBuilder.build();
		}
		else if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		{
			mBuilder
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setProgress(100, progressPercent, false)
					.setContentIntent(pendingIntent)
					.setOngoing(true).setContentTitle(message).setContentInfo(info)
					.setContentText(contentText)
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
			notification.contentView.setProgressBar(R.id.status_progress, 100, progressPercent, false);
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

	@Override
	public void
	onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		log("Download start: " + transfer.getFileName() + "_" + megaApi.getTotalDownloads() + "_" + megaApiFolder.getTotalDownloads());

		if(transfer.getType()==MegaTransfer.TYPE_DOWNLOAD){
			transfersCount++;

			updateProgressNotification();
		}
	}

	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError error) {
		log("onTransferFinish: " + transfer.getFileName());

		if(transfer.getType()==MegaTransfer.TYPE_DOWNLOAD){
			transfersCount--;

			if(!transfer.isFolderTransfer()){
				if(transfer.getState()==MegaTransfer.STATE_COMPLETED){
					String size = Util.getSizeString(transfer.getTotalBytes());
					AndroidCompletedTransfer completedTransfer = new AndroidCompletedTransfer(transfer.getFileName(), transfer.getType(), transfer.getState(), size, transfer.getNodeHandle()+"");
					dbH.setCompletedTransfer(completedTransfer);
				}

				updateProgressNotification();
			}

            String path = transfer.getPath();
            if (canceled) {
				if((lock != null) && (lock.isHeld()))
					try{ lock.release(); } catch(Exception ex) {}
				if((wl != null) && (wl.isHeld()))
					try{ wl.release(); } catch(Exception ex) {}

				log("Download cancelled: " + transfer.getFileName());
				File file = new File(path);
				file.delete();
				DownloadService.this.cancel();
			}
			else{
				if (error.getErrorCode() == MegaError.API_OK) {
					log("Download OK: " + transfer.getFileName());
					log("DOWNLOADFILE: " + path);
                    String targetPath = targetPaths.get(transfer.getNodeHandle());
                    if (targetPath != null) {
                        try {
                            SDCardOperator sdCardOperator = new SDCardOperator(this);
                            sdCardOperator.initDocumentFileRoot(dbH.getPreferences());
                            File source = new File(path);
                            path = sdCardOperator.move(targetPath,source);
                            TL.log(this, "@#@", "new path is: " + path);
                            File newFile = new File(path);
                            if(newFile.exists() && newFile.length() == source.length()) {
                                source.delete();

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            log(e.getMessage());
                        }
                    }
                    //To update thumbnails for videos
                    if(Util.isVideoFile(path)){
						log("Is video!!!");
						MegaNode videoNode = megaApi.getNodeByHandle(transfer.getNodeHandle());
						if (videoNode != null){
							if(!videoNode.hasThumbnail()){
								log("The video has not thumb");
								ThumbnailUtilsLollipop.createThumbnailVideo(this, path, megaApi, transfer.getNodeHandle());
							}
						}
						else{
							log("videoNode is NULL");
						}
					}
					else{
						log("NOT video!");
					}

					File resultFile = new File(path);
					File treeParent = resultFile.getParentFile();
					while(treeParent != null)
					{
						treeParent.setReadable(true, false);
						treeParent.setExecutable(true, false);
						treeParent = treeParent.getParentFile();
					}
					resultFile.setReadable(true, false);
					resultFile.setExecutable(true, false);

					String filePath = path;
					File f = new File(filePath);
					try {
						Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
						Uri finishedContentUri;
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
							finishedContentUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", f);
						} else {
							finishedContentUri = Uri.fromFile(f);
						}
						mediaScanIntent.setData(finishedContentUri);
						mediaScanIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						this.sendBroadcast(mediaScanIntent);
					}
					catch (Exception e){}

					try {
						MediaScannerConnection.scanFile(getApplicationContext(), new String[]{
								f.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
							@Override
							public void onScanCompleted(String path, Uri uri) {
								log("File was scanned successfully");
							}
						});
					}
					catch (Exception e){}

					if(storeToAdvacedDevices.containsKey(transfer.getNodeHandle())){
						log("Now copy the file to the SD Card");
						openFile=false;
						Uri tranfersUri = storeToAdvacedDevices.get(transfer.getNodeHandle());
						MegaNode node = megaApi.getNodeByHandle(transfer.getNodeHandle());
						alterDocument(tranfersUri, node.getName());
					}

					if(path.contains(Util.offlineDIR)){
						log("YESSSS it is Offline file");
						dbH = DatabaseHandler.getDbHandler(getApplicationContext());
						offlineNode = megaApi.getNodeByHandle(transfer.getNodeHandle());

						if(offlineNode!=null){
							saveOffline(offlineNode, path);
						}
						else{
							saveOfflineChatFile(transfer);
						}

						refreshOfflineFragment();
						refreshSettingsFragment();
					}
				}
				else
				{
					log("Download Error: " + transfer.getFileName() + "_" + error.getErrorCode() + "___" + error.getErrorString());

					if(!transfer.isFolderTransfer()){
						errorCount++;
					}

					if(error.getErrorCode() == MegaError.API_EINCOMPLETE){
						File file = new File(path);
						file.delete();
					}
					else{
						File file = new File(path);
						file.delete();
					}
				}
			}

			if ((megaApi.getNumPendingDownloads() == 0) && (transfersCount==0) && (megaApiFolder.getNumPendingDownloads() == 0)){
				onQueueComplete(transfer.getNodeHandle());
			}
		}
	}

	private void alterDocument(Uri uri, String fileName) {
		log("alterUri");
	    try {

	    	String sourceLocation = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.advancesDevicesDIR + "/"+fileName;

	    	log("Gonna copy: "+sourceLocation);

	        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
	        FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());

	    	InputStream in = new FileInputStream(sourceLocation);
//
//	        OutputStream out = new FileOutputStream(targetLocation);
//
	        // Copy the bits from instream to outstream
	        byte[] buf = new byte[1024];
	        int len;
	        while ((len = in.read(buf)) > 0) {
	        	fileOutputStream.write(buf, 0, len);
	        }
	        in.close();
//	        out.close();


//	        fileOutputStream.write(("Overwritten by MyCloud at " + System.currentTimeMillis() + "\n").getBytes());
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

	public void saveOffline (MegaNode node, String path){
		log("saveOffline");

		File destination = null;
		if (Environment.getExternalStorageDirectory() != null){
			destination = new File(path);
		}
		else{
			destination = getFilesDir();
		}

		destination.mkdirs();

		log("saveOffline: "+ destination.getAbsolutePath());
		log("Handle to save for offline : "+node.getHandle());

		Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
		if (node.getType() == MegaNode.TYPE_FOLDER) {
			log("saveOffline:isFolder");
			getDlList(dlFiles, node, new File(destination, new String(node.getName())));
		} else {
			log("saveOffline:isFile");
			dlFiles.put(node, destination.getAbsolutePath());
		}

		ArrayList<MegaNode> nodesToDB = new ArrayList<MegaNode>();

		for (MegaNode document : dlFiles.keySet()) {
			nodesToDB.add(document);
		}

        String inboxPath = Util.offlineDIR+"/in/";
        if(path.contains(inboxPath)){
            insertDB(nodesToDB, true);
        }
        else{
            insertDB(nodesToDB, false);
        }
	}

	public void saveOfflineChatFile (MegaTransfer transfer){
		log("saveOfflineChatFile: "+transfer.getNodeHandle()+ " " + transfer.getFileName());

		MegaOffline mOffInsert = new MegaOffline(Long.toString(transfer.getNodeHandle()), "/", transfer.getFileName(),-1, DB_FILE, 0, "-1");
		long checkInsert=dbH.setOfflineFile(mOffInsert);
		log("Test insert Chat File: "+checkInsert);

	}

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

	private void insertDB (ArrayList<MegaNode> nodesToDB, boolean fromInbox){
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

					path = MegaApiUtils.createStringTree(nodeToInsert, this);
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
							insertParentDB(parentNode, fromInbox);
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

//				if(megaApi.getParentNode(nodeToInsert).getType() != MegaNode.TYPE_ROOT){

				parentNode = megaApi.getParentNode(nodeToInsert);
				log("ParentNode: "+parentNode.getName());

				path = MegaApiUtils.createStringTree(nodeToInsert, this);
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
						insertIncomingParentDB(parentNode);
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
//				}
//				else{
//					path="/";
//
//					if(nodeToInsert.isFile()){
//						MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(),-1, DB_FILE, true);
//						long checkInsert=dbH.setOfflineFile(mOffInsert);
//						log("Test insert C: "+checkInsert);
//					}
//					else{
//						MegaOffline mOffInsert = new MegaOffline(Long.toString(nodeToInsert.getHandle()), path, nodeToInsert.getName(), -1, DB_FOLDER, true);
//						long checkInsert=dbH.setOfflineFile(mOffInsert);
//						log("Test insert D: "+checkInsert);
//					}
//				}
			}
		}
	}

	//Insert for incoming

	private void insertIncomingParentDB (MegaNode parentNode){
		log("insertIncomingParentDB: Check SaveOffline: "+parentNode.getName());

		MegaOffline mOffParentParent = null;
		String path=MegaApiUtils.createStringTree(parentNode, this);
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
				insertIncomingParentDB(megaApi.getParentNode(parentNode));
				//Insert the parent node
				mOffParentParent = dbH.findByHandle(megaApi.getParentNode(parentNode).getHandle());
				if(mOffParentParent==null){
					insertIncomingParentDB(megaApi.getParentNode(parentNode));

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
//		else{
//			log("---------------PARENT NODE ROOT------");
//			if(parentNode.isFile()){
//				MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(),-1, DB_FILE, false);
//				long checkInsert=dbH.setOfflineFile(mOffInsert);
//				log("Test insert I: "+checkInsert);
//			}
//			else{
//				MegaOffline mOffInsert = new MegaOffline(Long.toString(parentNode.getHandle()), path, parentNode.getName(), -1, DB_FOLDER, false);
//				long checkInsert=dbH.setOfflineFile(mOffInsert);
//				log("Test insert J: "+checkInsert);
//			}
//		}
	}

	private void insertParentDB (MegaNode parentNode, boolean fromInbox){
		log("insertParentDB: Check SaveOffline: "+parentNode.getName());

		MegaOffline mOffParentParent = null;
		String path=MegaApiUtils.createStringTree(parentNode, this);
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
				log("En algun momento!!!");
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
				insertParentDB(megaApi.getParentNode(parentNode), fromInbox);
				//Insert the parent node
				mOffParentParent = dbH.findByHandle(megaApi.getParentNode(parentNode).getHandle());
				if(mOffParentParent==null){
					log("call again");
					insertParentDB(megaApi.getParentNode(parentNode), fromInbox);
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

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		if(transfer.getType()==MegaTransfer.TYPE_DOWNLOAD){
			if (canceled) {
				log("Transfer cancel: " + transfer.getFileName());

				if((lock != null) && (lock.isHeld()))
					try{ lock.release(); } catch(Exception ex) {}
				if((wl != null) && (wl.isHeld()))
					try{ wl.release(); } catch(Exception ex) {}

				megaApi.cancelTransfer(transfer);
				DownloadService.this.cancel();
				return;
			}
			if(!transfer.isFolderTransfer()){
				updateProgressNotification();
			}
		}
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		log(transfer.getPath() + "\nDownload Temporary Error: " + e.getErrorString() + "__" + e.getErrorCode());

		if(transfer.getType()==MegaTransfer.TYPE_DOWNLOAD){
			if(e.getErrorCode() == MegaError.API_EOVERQUOTA) {
				if (e.getValue() != 0) {
					log("TRANSFER OVERQUOTA ERROR: " + e.getErrorCode());

					UserCredentials credentials = dbH.getCredentials();
					if(credentials!=null){
						log("Credentials is NOT null");
					}

					downloadedBytesToOverquota = megaApi.getTotalDownloadedBytes() + megaApiFolder.getTotalDownloadedBytes();
					isOverquota = true;
					log("downloaded bytes to reach overquota: "+downloadedBytesToOverquota);

					showTransferOverquotaNotification();
				}
			}
		}
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestFinish");

		if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS){
			log("TYPE_PAUSE_TRANSFERS finished");
			if (e.getErrorCode() == MegaError.API_OK){
				cancel();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFERS){
			log("TYPE_CANCEL_TRANSFERS finished");
			if (e.getErrorCode() == MegaError.API_OK){
				cancel();
			}

		}
		else if (request.getType() == MegaRequest.TYPE_LOGIN){
			if (e.getErrorCode() == MegaError.API_OK){
				log("Fast login OK");
				log("Calling fetchNodes from CameraSyncService");
				megaApi.fetchNodes(this);
			}
			else{
				log("ERROR: " + e.getErrorString());
				isLoggingIn = false;
				MegaApplication.setLoggingIn(isLoggingIn);
//				finish();
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
					}
					else{
						log("Chat NOT enabled - readyToManager");
						isLoggingIn = false;
						MegaApplication.setLoggingIn(isLoggingIn);
					}
				}
				else{
					log("chatSettings NULL - readyToManager");
					isLoggingIn = false;
					MegaApplication.setLoggingIn(isLoggingIn);
				}

				for (int i=0;i<pendingIntents.size();i++){
					onHandleIntent(pendingIntents.get(i));
				}
				pendingIntents.clear();
			}
			else{
				log("ERROR: " + e.getErrorString());
				isLoggingIn = false;
				MegaApplication.setLoggingIn(isLoggingIn);
//				finish();
			}
		}
		else{
			log("Public node received");
			if (e.getErrorCode() != MegaError.API_OK) {
				log("Public node error");
				return;
			}
			else {
				MegaNode node = request.getPublicMegaNode();

				if(node!=null){
					if (currentDir.isDirectory()){
						currentFile = new File(currentDir, megaApi.escapeFsIncompatible(node.getName()));
						log("node.getName(): " + node.getName());

					}
					else{
						currentFile = currentDir;
						log("CURREN");
					}

					log("Public node download launched");
					if(!wl.isHeld()) wl.acquire();
					if(!lock.isHeld()) lock.acquire();
					if (currentDir.isDirectory()){
						log("To downloadPublic(dir): " + currentDir.getAbsolutePath() + "/");
						megaApi.startDownload(node, currentDir.getAbsolutePath() + "/", this);
					}
				}
			}
		}
	}


	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getName());
	}

	public static void log(String log){
		Util.log("DownloadService", log);
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		log("onRequestUpdate");
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
		if (request.getType() == MegaChatRequest.TYPE_CONNECT){

			isLoggingIn = false;
			MegaApplication.setLoggingIn(isLoggingIn);

			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("Connected to chat!");
			}
			else{
				log("EEEERRRRROR WHEN CONNECTING " + e.getErrorString());
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

	}

	private void refreshOfflineFragment(){
		Intent intent = new Intent(OfflineFragmentLollipop.REFRESH_OFFLINE_FILE_LIST);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
	}

	private void refreshSettingsFragment() {
		Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_SETTINGS_UPDATED);
		intent.setAction(SettingsFragmentLollipop.ACTION_REFRESH_CLEAR_OFFLINE_SETTING);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
	}
}
