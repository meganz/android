package mega.privacy.android.app.lollipop.megachat;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
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
import mega.privacy.android.app.interfaces.MyChatFilesExisitListener;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.DBUtil;
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

import static mega.privacy.android.app.utils.Constants.CHAT_FOLDER;

import static mega.privacy.android.app.utils.CacheFolderManager.buildVoiceClipFile;
import static mega.privacy.android.app.utils.CacheFolderManager.isFileAvailable;

public class ChatUploadService extends Service implements MegaTransferListenerInterface, MegaRequestListenerInterface, MegaChatRequestListenerInterface, MyChatFilesExisitListener<Intent> {

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

	private int notificationId = Constants.NOTIFICATION_CHAT_UPLOAD;
	private String notificationChannelId = Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID;
	private String notificationChannelName = Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_NAME;

	//Intent being stored when My Chat Files folder does not exist
	private Intent preservedIntent;

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
		isOverquota = 0;

		mapVideoDownsampling = new HashMap();
		mapProgressTransfers = new HashMap();

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

        if (megaChatApi != null){
            megaChatApi.saveCurrentState();
        }

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

	Bitmap.CompressFormat getCompressFormat(String name) {
		String[] s = name.split("\\.");
		String ext;
		if (s != null && s.length > 1) {
			ext = s[s.length-1];
			switch (ext) {
				case "jpeg" :
				case "jpg":{
					return Bitmap.CompressFormat.JPEG;
				}
				case "png": {
					return Bitmap.CompressFormat.PNG;
				}
				case "webp":{
					return Bitmap.CompressFormat.WEBP;
				}
				default: {
					return Bitmap.CompressFormat.JPEG;
				}
			}
		}
		return Bitmap.CompressFormat.JPEG;
	}

	protected void onHandleIntent(final Intent intent) {
		log("onHandleIntent");
		if (ChatUtil.existsMyChatFiles(intent, megaApi, this, this)) {
			log(Constants.CHAT_FOLDER + " already exists");
			parentNode = megaApi.getNodeByPath("/" + Constants.CHAT_FOLDER);
			handleIntentIfFolderExist(intent);
		} else {
			log(Constants.CHAT_FOLDER + " does not exist, create the folder then upload files");
		}
	}

	private void handleIntentIfFolderExist(Intent intent) {
		ArrayList<PendingMessageSingle> pendingMessageSingles = new ArrayList<>();
		if (intent.getBooleanExtra(EXTRA_COMES_FROM_FILE_EXPLORER, false)) {
			HashMap<String, String> fileFingerprints = (HashMap<String, String>) intent.getSerializableExtra(EXTRA_UPLOAD_FILES_FINGERPRINTS);
			long[] idPendMsgs = intent.getLongArrayExtra(EXTRA_PEND_MSG_IDS);
			long[] attachFiles = intent.getLongArrayExtra(EXTRA_ATTACH_FILES);
			long[] idChats = intent.getLongArrayExtra(EXTRA_ATTACH_CHAT_IDS);

			if (attachFiles!=null && attachFiles.length>0 && idChats!=null && idChats.length>0) {
				for (int i=0; i<attachFiles.length; i++) {
					for (int j=0; j<idChats.length; j++) {
						requestSent++;
						megaChatApi.attachNode(idChats[j], attachFiles[i], this);
					}
				}
			}

			if (idPendMsgs!=null && idPendMsgs.length>0 && fileFingerprints!=null && !fileFingerprints.isEmpty()) {
				for (Map.Entry<String, String> entry : fileFingerprints.entrySet()) {
					if (entry != null) {
						String fingerprint = entry.getKey();
						String path = entry.getValue();

						if (fingerprint == null || path == null) {
							log("Error, fingerprint: "+ fingerprint+" path: "+path);
							continue;
						}

						totalUploads++;

						if(!wl.isHeld()){
							wl.acquire();
						}

						if(!lock.isHeld()){
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
									}
								}
							}
							initUpload(pendingMessageSingles, null);
						}
					}
				}
			}
			else {
				long chatId = intent.getLongExtra(EXTRA_CHAT_ID, -1);
				type = intent.getStringExtra(Constants.EXTRA_TRANSFER_TYPE);
				long idPendMsg = intent.getLongExtra(EXTRA_ID_PEND_MSG, -1);
				PendingMessageSingle pendingMsg = null;
				if(idPendMsg!=-1){
					pendingMsg = dbH.findPendingMessageById(idPendMsg);
				}

			if (pendingMsg!=null) {
				sendOriginalAttachments = DBUtil.isSendOriginalAttachments(this);
				log("sendOriginalAttachments is "+sendOriginalAttachments);

				if(chatId!=-1){
					log("The chat id is: "+chatId);

						if((type==null)||(!type.equals(Constants.EXTRA_VOICE_CLIP))){
							totalUploads++;
						}

					if(!wl.isHeld()){
						wl.acquire();
					}

						if(!lock.isHeld()){
							lock.acquire();
						}
						pendingMessageSingles.clear();
						pendingMessageSingles.add(pendingMsg);
						initUpload(pendingMessageSingles, type);
					}
				}
				else{
					log("Error the chatId is not correct: "+chatId);
				}
			}
		}

	void initUpload (ArrayList<PendingMessageSingle> pendingMsgs, String type) {
		log("initUpload");

		PendingMessageSingle pendingMsg = pendingMsgs.get(0);
		File file = new File(pendingMsg.getFilePath());

		ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		boolean isWIFI = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
		boolean isData = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();

		if(MimeTypeList.typeForName(file.getName()).isImage() && !MimeTypeList.typeForName(file.getName()).isGIF() && isData){
			log("DATA connection is Image");
			BitmapFactory.Options options = new BitmapFactory.Options();
			Bitmap fileBitmap = BitmapFactory.decodeFile(file.getPath(), options);
			if (fileBitmap != null) {
				log("DATA connection file decoded");
				float width = options.outWidth;
				float height = options.outHeight;
				float totalPixels = width * height;
				float division = DOWNSCALE_IMAGES_PX/totalPixels;
				float factor = (float) Math.min(Math.sqrt(division), 1);
				if (factor < 1) {
					width *= factor;
					height *= factor;
					log("DATA connection factor<1 totalPixels: "+totalPixels+" width: "+width+ " height: "+height+" DOWNSCALE_IMAGES_PX/totalPixels: "+division+" Math.sqrt(DOWNSCALE_IMAGES_PX/totalPixels): "+Math.sqrt(division));
					Bitmap scaleBitmap = Bitmap.createScaledBitmap(fileBitmap, (int)width, (int)height, false);
					if (scaleBitmap != null) {
						log("DATA connection scaled Bitmap != null");
						File defaultDownloadLocation = null;
						if (Environment.getExternalStorageDirectory() != null) {
							defaultDownloadLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.chatTempDIR + "/");
						}
						else{
							defaultDownloadLocation = getFilesDir();
						}
						defaultDownloadLocation.mkdirs();
						File outFile = new File(defaultDownloadLocation.getAbsolutePath(), file.getName());
						if (outFile != null) {
							log("DATA connection new file != null");
							FileOutputStream fOut;
							try {
								fOut = new FileOutputStream(outFile);
								scaleBitmap.compress(getCompressFormat(file.getName()), 100, fOut);
								fOut.flush();
								fOut.close();
								log("DATA connection file compressed");
								String fingerprint = megaApi.getFingerprint(outFile.getAbsolutePath());
								for (PendingMessageSingle pendMsg : pendingMsgs) {
									if (fingerprint != null) {
										pendMsg.setFingerprint(fingerprint);
									}
									pendingMessages.add(pendMsg);
								}
								megaApi.startUploadWithTopPriority(outFile.getAbsolutePath(), parentNode, Constants.UPLOAD_APP_DATA_CHAT+">"+pendingMsg.getId(), false);
								log("DATA connection file uploading");
							} catch (Exception e){
								for (PendingMessageSingle pendMsg : pendingMsgs) {
									pendingMessages.add(pendMsg);
								}
								megaApi.startUploadWithTopPriority(pendingMsg.getFilePath(), parentNode, Constants.UPLOAD_APP_DATA_CHAT+">"+pendingMsg.getId(), false);
								log("DATA connection Exception compressing: "+ e.getMessage());
							}
							fileBitmap.recycle();
							scaleBitmap.recycle();
						}
						else {
							fileBitmap.recycle();
							scaleBitmap.recycle();
							for (PendingMessageSingle pendMsg : pendingMsgs) {
								pendingMessages.add(pendMsg);
							}

							megaApi.startUploadWithTopPriority(pendingMsg.getFilePath(), parentNode, Constants.UPLOAD_APP_DATA_CHAT+">"+pendingMsg.getId(), false);
							log("DATA connection new file NULL");
						}
					}
					else {
						fileBitmap.recycle();
						for (PendingMessageSingle pendMsg : pendingMsgs) {
							pendingMessages.add(pendMsg);
						}

						megaApi.startUploadWithTopPriority(pendingMsg.getFilePath(), parentNode, Constants.UPLOAD_APP_DATA_CHAT+">"+pendingMsg.getId(), false);
						log("DATA connection scaled Bitmap NULL");
					}
				}
				else {
					fileBitmap.recycle();
					for (PendingMessageSingle pendMsg : pendingMsgs) {
						pendingMessages.add(pendMsg);
					}

					megaApi.startUploadWithTopPriority(pendingMsg.getFilePath(), parentNode, Constants.UPLOAD_APP_DATA_CHAT+">"+pendingMsg.getId(), false);
					log("DATA connection factor >= 1 totalPixels: "+totalPixels+" width: "+width+ " height: "+height+" DOWNSCALE_IMAGES_PX/totalPixels: "+DOWNSCALE_IMAGES_PX/totalPixels+" Math.sqrt(DOWNSCALE_IMAGES_PX/totalPixels): "+Math.sqrt(DOWNSCALE_IMAGES_PX/totalPixels));
				}
			}
			else {
				for (PendingMessageSingle pendMsg : pendingMsgs) {
					pendingMessages.add(pendMsg);
				}

				megaApi.startUploadWithTopPriority(pendingMsg.getFilePath(), parentNode, Constants.UPLOAD_APP_DATA_CHAT+">"+pendingMsg.getId(), false);
				log("DATA connection file NULL");
			}
		}
		else if(MimeTypeList.typeForName(file.getName()).isMp4Video() && (!sendOriginalAttachments)){
			log("DATA connection is Mp4Video");

			try {
				totalVideos++;
				numberVideosPending++;
				File defaultDownloadLocation = null;
				if (Environment.getExternalStorageDirectory() != null){
					defaultDownloadLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.chatTempDIR + "/");
				}
				else{
					defaultDownloadLocation = getFilesDir();
				}

				defaultDownloadLocation.mkdirs();

				File outFile = new File(defaultDownloadLocation.getAbsolutePath(), file.getName());
				int index = 0;
				if(outFile!=null){
					while(outFile.exists()){
						if(index>0){
							outFile = new File(defaultDownloadLocation.getAbsolutePath(), file.getName());
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

						outFile = new File(defaultDownloadLocation.getAbsolutePath(), fileName);
					}
				}

				outFile.createNewFile();

				if(outFile==null){
					numberVideosPending--;
					totalVideos--;
					for (PendingMessageSingle pendMsg : pendingMsgs) {
						pendingMessages.add(pendMsg);
					}

					megaApi.startUploadWithTopPriority(pendingMsg.getFilePath(), parentNode, Constants.UPLOAD_APP_DATA_CHAT+">"+pendingMsg.getId(), false);
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

				megaApi.startUploadWithTopPriority(pendingMsg.getFilePath(), parentNode, Constants.UPLOAD_APP_DATA_CHAT+">"+pendingMsg.getId(), false);
				log("EXCEPTION: Video cannot be downsampled");
			}
		}
		else{
			for (PendingMessageSingle pendMsg : pendingMsgs) {
				pendingMessages.add(pendMsg);
			}
			String data = Constants.UPLOAD_APP_DATA_CHAT+">"+pendingMsg.getId();
			if((type!=null)&&(type.equals(Constants.EXTRA_VOICE_CLIP))){
				data = Constants.EXTRA_VOICE_CLIP+"-"+data;
			}
			megaApi.startUploadWithTopPriority(pendingMsg.getFilePath(), parentNode, data, false);
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
		//Review when is called

		if((lock != null) && (lock.isHeld()))
			try{ lock.release(); } catch(Exception ex) {}
		if((wl != null) && (wl.isHeld()))
			try{ wl.release(); } catch(Exception ex) {}

		if(isOverquota!=0){
			showStorageOverquotaNotification();
		}

		log("Reset figures of chatUploadService");
		numberVideosPending=0;
		totalVideos=0;
		totalUploads = 0;
		totalUploadsCompleted = 0;

		if(megaApi.getNumPendingUploads()<=0){
			megaApi.resetTotalUploads();
		}

		log("stopping service!!!!!!!!!!:::::::::::::::!!!!!!!!!!!!");
		isForeground = false;
		stopForeground(true);
		mNotificationManager.cancel(notificationId);
		stopSelf();
		log("after stopSelf");

		try{
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
		catch (Exception e){
			log("EXCEPTION: pathSelfie not deleted");
		}

		try{
			String pathVideoDownsampling = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.chatTempDIR;
			File fVideo = new File(pathVideoDownsampling);
			//Delete recursively all files and folder
			if (fVideo.exists()) {
				if (fVideo.isDirectory()) {
					if(fVideo.list().length<=0){
						fVideo.delete();
					}
				}

			}
		}
		catch (Exception e){
			log("EXCEPTION: pathVideoDownsampling not deleted");
		}

		try{
			File f = getExternalFilesDir(null);
//			File f = new File(pathSelfie);
			//Delete recursively all files and folder
			if (f.exists()) {
				if (f.isDirectory()) {
					if(f.list().length<=0){
						f.delete();
					}
				}
			}
		}
		catch (Exception e){
			log("EXCEPTION: pathSelfie not deleted");
		}
	}

	public void updateProgressDownsampling(int percentage, String key){
		mapVideoDownsampling.put(key, percentage);
		updateProgressNotification();
	}

	public void finishDownsampling(String returnedFile, boolean success, long idPendingMessage){
		log("finishDownsampling");
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
						log("Found the downFile");
					}
				}
				else{
					log("Error message could not been downsampled");
				}
			}
			if(downFile!=null){
				mapVideoDownsampling.put(downFile.getAbsolutePath(), 100);
			}
		}

		if(downFile!=null){
			megaApi.startUploadWithTopPriority(downFile.getPath(), parentNode, Constants.UPLOAD_APP_DATA_CHAT+">"+idPendingMessage, false);
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
				intent.setAction(Constants.ACTION_OVERQUOTA_STORAGE);
				break;
			case 2:
				intent.setAction(Constants.ACTION_PRE_OVERQUOTA_STORAGE);
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

	@SuppressLint("NewApi")
	private void updateProgressNotification() {
		log("updatePpogressNotification");
        long progressPercent = 0;
        Collection<MegaTransfer> transfers= mapProgressTransfers.values();

        if(sendOriginalAttachments){
            long total = 0;
            long inProgress = 0;

            for (Iterator iterator = transfers.iterator(); iterator.hasNext();) {
                MegaTransfer currentTransfer = (MegaTransfer) iterator.next();
                if(!currentTransfer.getAppData().contains(Constants.EXTRA_VOICE_CLIP)){
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

					if(!currentTransfer.getAppData().contains(Constants.EXTRA_VOICE_CLIP)){
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

					if(!currentTransfer.getAppData().contains(Constants.EXTRA_VOICE_CLIP)){
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

        log("updateProgressNotification: progress: "+progressPercent);

        String message = "";
        if (isOverquota != 0){
            message = getString(R.string.overquota_alert_title);
        }
        else if(totalUploadsCompleted==totalUploads){
            message = getResources().getQuantityString(R.plurals.upload_service_notification, totalUploads, totalUploadsCompleted, totalUploads);
        }
        else{
            int inProgress = totalUploadsCompleted+1;
            message = getResources().getQuantityString(R.plurals.upload_service_notification, totalUploads, inProgress, totalUploads);
        }

        Intent intent;
        intent = new Intent(ChatUploadService.this, ManagerActivityLollipop.class);
		switch (isOverquota) {
			case 0:
			default:
				intent.setAction(Constants.ACTION_SHOW_TRANSFERS);
				break;
			case 1:
				intent.setAction(Constants.ACTION_OVERQUOTA_STORAGE);
				break;
			case 2:
				intent.setAction(Constants.ACTION_PRE_OVERQUOTA_STORAGE);
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

	public static void log(String log) {
		Util.log("ChatUploadService", log);
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {

		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
			log("onTransferStart: " + transfer.getPath());

			String appData = transfer.getAppData();

			if(appData==null) return;

			if(appData.contains(Constants.UPLOAD_APP_DATA_CHAT)){
				log("This is a chat upload: "+ appData);
				if(!appData.contains(Constants.EXTRA_VOICE_CLIP)) {
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
				if (!transfer.isFolderTransfer() && !appData.contains(Constants.EXTRA_VOICE_CLIP)){
					updateProgressNotification();
				}
			}
		}
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {

		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
			log("onTransferUpdate: " + transfer.getPath());

			String appData = transfer.getAppData();

			if(appData!=null && appData.contains(Constants.UPLOAD_APP_DATA_CHAT)){
				if(transfer.isStreamingTransfer()){
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
						ChatUploadService.this.cancel();
						log("after cancel");
						return;
					}

					if(isOverquota!=0){
						log("After overquota error");
						isOverquota = 0;
					}
					mapProgressTransfers.put(transfer.getTag(), transfer);

					if(!appData.contains(Constants.EXTRA_VOICE_CLIP)) {
						updateProgressNotification();
					}

				}
			}
		}
	}



	@Override
	public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
		log(transfer.getPath() + "\nUpload Temporary Error: " + e.getErrorString() + "__" + e.getErrorCode());
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
						log("TRANSFER OVERQUOTA ERROR: " + e.getErrorCode());
					}else {
						log("STORAGE OVERQUOTA ERROR: " + e.getErrorCode());
						if(transfer.getAppData().contains(Constants.EXTRA_VOICE_CLIP)){
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

		if(transfer.getType()==MegaTransfer.TYPE_UPLOAD) {
			log("onTransferFinish: " + transfer.getPath());
			String appData = transfer.getAppData();

			if(appData!=null && appData.contains(Constants.UPLOAD_APP_DATA_CHAT)){
				if(transfer.isStreamingTransfer()){
					return;
				}
				if(!appData.contains(Constants.EXTRA_VOICE_CLIP)) {
					transfersCount--;
					totalUploadsCompleted++;
				}
				mapProgressTransfers.put(transfer.getTag(), transfer);

				if (canceled) {
					log("Upload cancelled: " + transfer.getFileName());

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
					log("after cancel");
					if(appData.contains(Constants.EXTRA_VOICE_CLIP)) {
						File localFile = buildVoiceClipFile(this, transfer.getFileName());
						if (isFileAvailable(localFile) && !localFile.getName().equals(transfer.getFileName())) {
							localFile.delete();
						}
					}else {
						String pathSelfie = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.temporalPicDIR;
						File f = new File(pathSelfie);
						//Delete recursively all files and folder
						if (f.exists()) {
							if (f.isDirectory()) {
								if (f.list().length <= 0) {
									f.delete();
								}
							}
						}
						f.delete();
					}

				}
				else{
					if (error.getErrorCode() == MegaError.API_OK) {
						log("Upload OK: " + transfer.getFileName());

						if(Util.isVideoFile(transfer.getPath())){
							log("Is video!!!");

							File previewDir = PreviewUtils.getPreviewFolder(this);
							File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
							File thumbDir = ThumbnailUtils.getThumbFolder(this);
							File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
							megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());
							megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

							attachNodes(transfer);
						}
						else if (MimeTypeList.typeForName(transfer.getPath()).isImage()){
							log("Is image!!!");

							File previewDir = PreviewUtils.getPreviewFolder(this);
							File preview = new File(previewDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
							megaApi.createPreview(transfer.getPath(), preview.getAbsolutePath());

							File thumbDir = ThumbnailUtils.getThumbFolder(this);
							File thumb = new File(thumbDir, MegaApiAndroid.handleToBase64(transfer.getNodeHandle()) + ".jpg");
							megaApi.createThumbnail(transfer.getPath(), thumb.getAbsolutePath());

							attachNodes(transfer);
						}
						else if (MimeTypeList.typeForName(transfer.getPath()).isPdf()) {
							log("Is pdf!!!");

							try{
								ThumbnailUtilsLollipop.createThumbnailPdf(this, transfer.getPath(), megaApi, transfer.getNodeHandle());
							}
							catch(Exception e){
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
									File oldPreview = new File(previewDir, transfer.getFileName()+".jpg");
									if (oldPreview.exists()){
										oldPreview.delete();
									}
								}
								else{
									log("Not Compress");
								}
								//Attach node one the request finish
								requestSent++;
								megaApi.setPreview(pdfNode, preview.getAbsolutePath(), this);

								pdfiumCore.closeDocument(pdfDocument);

								updatePdfAttachStatus(transfer);

							} catch(Exception e) {
								log("Pdf preview could not be created");
								attachNodes(transfer);
							} finally {
								try {
									if (out != null)
										out.close();
								} catch (Exception e) {
								}
							}
						}else if(ChatUtil.isVoiceClip(transfer.getPath())){
							log("is Voice clip");
							attachVoiceClips(transfer);
						}
						else{
							log("NOT video, image or pdf!");
							attachNodes(transfer);
						}
					}
					else{
						log("Upload Error: " + transfer.getFileName() + "_" + error.getErrorCode() + "___" + error.getErrorString());

						if(error.getErrorCode() == MegaError.API_EEXIST){
							log("Transfer API_EEXIST: "+transfer.getNodeHandle());
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

					log("IN Finish: "+transfer.getFileName()+" path: "+transfer.getPath());
					String pathSelfie = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.temporalPicDIR;
					if (transfer.getPath() != null) {
						if (transfer.getPath().startsWith(pathSelfie)) {
							File f = new File(transfer.getPath());
							f.delete();
						}
					} else {
						log("transfer.getPath() is NULL");
					}
				}

				if (totalUploadsCompleted==totalUploads && transfersCount==0 && numberVideosPending<=0 && requestSent<=0){
					onQueueComplete();
				}
				else{
					if(!appData.contains(Constants.EXTRA_VOICE_CLIP)) {
						updateProgressNotification();
					}
				}
			}
		}
	}

	public void attachNodes(MegaTransfer transfer){
		log("attachNodes()");
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
			log("attach");

			requestSent++;
			pendMsg.setNodeHandle(transfer.getNodeHandle());
			pendMsg.setState(PendingMessageSingle.STATE_ATTACHING);
			megaChatApi.attachNode(pendMsg.getChatId(), transfer.getNodeHandle(), this);

			if(Util.isVideoFile(transfer.getPath())){
				String pathDownsampled = pendMsg.getVideoDownSampled();
				if(transfer.getPath().equals(pathDownsampled)){
					//Delete the local temp video file
					File f = new File(transfer.getPath());

					if (f.exists()) {
						boolean deleted = f.delete();
						if(!deleted){
							log("ERROR: Local file not deleted!");
						}
					}
				}
			}

		}
	}

	public void attachVoiceClips(MegaTransfer transfer){
		log("attachVoiceClips()");
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
		log("updatePdfAttachStatus");
		//Find the pending message
		for(int i=0; i<pendingMessages.size();i++){
			PendingMessageSingle pendMsg = pendingMessages.get(i);

			if(pendMsg.getFilePath().equals(transfer.getPath())){
				if(pendMsg.getNodeHandle()==-1){
					log("Set node handle to the pdf file: "+transfer.getNodeHandle());
					pendMsg.setNodeHandle(transfer.getNodeHandle());
				}
				else{
					log("updatePdfAttachStatus: set node handle error");
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
		log("attachPdfNode: nodeHandle: "+nodeHandle);
		//Find the pending message
		for(int i=0; i<pendingMessages.size();i++){
			PendingMessageSingle pendMsg = pendingMessages.get(i);

			if(pendMsg.getNodeHandle()==nodeHandle){
				if(megaChatApi!=null){
					log("Send node: "+nodeHandle+ " to chat: "+pendMsg.getChatId());
					requestSent++;
					MegaNode nodePdf = megaApi.getNodeByHandle(nodeHandle);
					if(nodePdf.hasPreview()){
						log("The pdf node has preview");
					}
					megaChatApi.attachNode(pendMsg.getChatId(), nodeHandle, this);
				}
			}
			else{
				log("PDF attach error");
			}
		}
	}



	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getName());
		if (request.getType() == MegaRequest.TYPE_COPY){
			updateProgressNotification();
		}
		else if (request.getType() == MegaRequest.TYPE_SET_ATTR_FILE) {
			log("TYPE_SET_ATTR_FILE");
		}
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("UPLOAD: onRequestFinish "+request.getRequestString());

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

		if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER && CHAT_FOLDER.equals(request.getName())) {
			if (e.getErrorCode() == MegaError.API_OK) {
				log("Create folder successfully, continue on pending chat upload");
				handleStoredData();
			} else {
				//cannot create chat folder
				log("Chat folder NOT exists and cannot be created --> STOP service");
			    isForeground = false;
			    stopForeground(true);
			    mNotificationManager.cancel(notificationId);
			    stopSelf();
			    log("after stopSelf");
			}
		}

		if (e.getErrorCode()==MegaError.API_OK) {
			log("onRequestFinish OK");
		}
		else {
			log("onRequestFinish:ERROR: "+e.getErrorCode());

			if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
				log("OVERQUOTA ERROR: "+e.getErrorCode());
				isOverquota = 1;
			}
			else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
				log("PRE-OVERQUOTA ERROR: "+e.getErrorCode());
				isOverquota = 2;
			}
			onQueueComplete();
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
				log("Attachment sent correctly");
				MegaNodeList nodeList = request.getMegaNodeList();

				//Find the pending message
				for(int i=0; i<pendingMessages.size();i++){
					PendingMessageSingle pendMsg = pendingMessages.get(i);

					//Check node handles - if match add to DB the karere temp id of the message
					long nodeHandle = pendMsg.getNodeHandle();
					MegaNode node = nodeList.get(0);
					if(node.getHandle()==nodeHandle){
						log("The message MATCH!!");
						long tempId = request.getMegaChatMessage().getTempId();
						log("The tempId of the message is: "+tempId);
						dbH.updatePendingMessageOnAttach(pendMsg.getId(), tempId+"", PendingMessageSingle.STATE_SENT);
						pendingMessages.remove(i);
						break;
					}
				}
			}
			else{
				log("Attachment not correctly sent: "+e.getErrorCode()+" "+ e.getErrorString());
				MegaNodeList nodeList = request.getMegaNodeList();

				//Find the pending message
				for(int i=0; i<pendingMessages.size();i++){
					PendingMessageSingle pendMsg = pendingMessages.get(i);
					//Check node handles - if match add to DB the karere temp id of the message
					long nodeHandle = pendMsg.getNodeHandle();
					MegaNode node = nodeList.get(0);
					if(node.getHandle()==nodeHandle){
						log("The message MATCH!!");
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
		log("launchErrorToChat");

		//Find the pending message
		for(int i=0; i<pendingMessages.size();i++) {
			PendingMessageSingle pendMsg = pendingMessages.get(i);
			if(pendMsg.getId() == id){
				long openChatId = MegaApplication.getOpenChatId();
				if(pendMsg.getChatId()==openChatId){
					log("Error update activity");
					Intent intent;
					intent = new Intent(this, ChatActivityLollipop.class);
					intent.setAction(Constants.ACTION_UPDATE_ATTACHMENT);
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

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
				mBuilderCompat.setColor(ContextCompat.getColor(this,R.color.mega));
			}

			mNotificationManager.notify(Constants.NOTIFICATION_STORAGE_OVERQUOTA, mBuilderCompat.build());
		}
	}

	@Override
	public void storedUnhandledData(Intent preservedData) {
		preservedIntent = preservedData;
	}

	@Override
	public void handleStoredData() {
		log("Create folder successfully, continue on pending chat upload");
		if (parentNode == null) {
			parentNode = megaApi.getNodeByPath("/"+Constants.CHAT_FOLDER);
		}
		handleIntentIfFolderExist(preservedIntent);
		preservedIntent = null;
	}
}
