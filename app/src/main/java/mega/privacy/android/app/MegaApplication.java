package mega.privacy.android.app;

//import com.google.android.gms.analytics.GoogleAnalytics;
//import com.google.android.gms.analytics.Logger.LogLevel;
//import com.google.android.gms.analytics.Tracker;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.util.Log;

import org.webrtc.AndroidVideoTrackSourceObserver;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.ContextUtils;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;

import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUser;


public class MegaApplication extends Application implements MegaListenerInterface, MegaChatRequestListenerInterface {
	final String TAG = "MegaApplication";
	static final String USER_AGENT = "MEGAAndroid/3.3_178";

	DatabaseHandler dbH;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	String localIpAddress = "";
	BackgroundRequestListener requestListener;
	final static private String APP_KEY = "6tioyn8ka5l6hty";
	final static private String APP_SECRET = "hfzgdtrma231qdm";


	MegaChatApiAndroid megaChatApi = null;

//	static final String GA_PROPERTY_ID = "UA-59254318-1";
//	
//	/**
//	 * Enum used to identify the tracker that needs to be used for tracking.
//	 *
//	 * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
//	 * storing them all in Application object helps ensure that they are created only once per
//	 * application instance.
//	 */
//	public enum TrackerName {
//	  APP_TRACKER/*, // Tracker used only in this app.
//	  GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
//	  ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
//	  */
//	}
//
//	HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();
	
	class BackgroundRequestListener implements MegaRequestListenerInterface
	{

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			log("BackgroundRequestListener:onRequestStart: " + request.getRequestString());
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			log("BackgroundRequestListener:onRequestUpdate: " + request.getRequestString());
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			log("BackgroundRequestListener:onRequestFinish: " + request.getRequestString() + "____" + e.getErrorCode() + "___" + request.getParamType());
			if (e.getErrorCode() == MegaError.API_ESID){
				if (request.getType() == MegaRequest.TYPE_LOGOUT){
					log("type_logout");
					AccountController.logout(getApplicationContext(), getMegaApi(), getMegaChatApi(), false);
				}
			}
			else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
				if (e.getErrorCode() == MegaError.API_OK){
					if (megaApi != null){
						log("BackgroundRequestListener:onRequestFinish: enableTransferResumption ");
						log("BackgroundRequestListener:onRequestFinish: enableTransferResumption - Session: " + megaApi.dumpSession());
//						megaApi.enableTransferResumption();
					}
				}
			}
			else if(request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
				if (e.getErrorCode() == MegaError.API_OK){

					if(request.getParamType()==MegaApiJava.USER_ATTR_FIRSTNAME||request.getParamType()==MegaApiJava.USER_ATTR_LASTNAME){
						log("BackgroundRequestListener:onRequestFinish: Name: "+request.getText());
						if (megaApi != null){
							if(request.getEmail()!=null){
								log("BackgroundRequestListener:onRequestFinish: Email: "+request.getEmail());
								MegaUser user = megaApi.getContact(request.getEmail());
								if (user != null) {
									log("BackgroundRequestListener:onRequestFinish: User handle: "+user.getHandle());
									log("Visibility: "+user.getVisibility()); //If user visibity == MegaUser.VISIBILITY_UNKNOW then, non contact
									if(user.getVisibility()!=MegaUser.VISIBILITY_VISIBLE){
										log("BackgroundRequestListener:onRequestFinish: Non-contact");
										if(request.getParamType()==MegaApiJava.USER_ATTR_FIRSTNAME){
											dbH.setNonContactEmail(request.getEmail(), user.getHandle()+"");
											dbH.setNonContactFirstName(request.getText(), user.getHandle()+"");
										}
										else if(request.getParamType()==MegaApiJava.USER_ATTR_LASTNAME){
											dbH.setNonContactLastName(request.getText(), user.getHandle()+"");
										}
									}
									else{
										log("BackgroundRequestListener:onRequestFinish: The user is or was CONTACT: "+user.getEmail());
									}
								}
								else{
									log("BackgroundRequestListener:onRequestFinish: User is NULL");
								}
							}
						}
					}
				}
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			log("BackgroundRequestListener: onRequestTemporaryError: " + request.getRequestString());
		}
		
	}

	private final int interval = 3000;
	private Handler keepAliveHandler = new Handler();

	private Runnable keepAliveRunnable = new Runnable() {
		@Override
		public void run() {
			try {

				if (activityVisible) {
					log("SEND KEEPALIVE");
					if (megaChatApi != null) {
						megaChatApi.setBackgroundStatus(false);
					}

				} else {
					log("SEND KEEPALIVEAWAY");
					if (megaChatApi != null) {
						megaChatApi.setBackgroundStatus(true);
					}
				}

				if (activityVisible) {
					log("Handler KEEPALIVE: " + System.currentTimeMillis());
				} else {
					log("Handler KEEPALIVEAWAY: " + System.currentTimeMillis());
				}
				keepAliveHandler.postAtTime(keepAliveRunnable, System.currentTimeMillis() + interval);
				keepAliveHandler.postDelayed(keepAliveRunnable, interval);
			}
			catch (Exception exc){
				log("Exception in keepAliveRunnable");
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();

		keepAliveHandler.postAtTime(keepAliveRunnable, System.currentTimeMillis()+interval);
		keepAliveHandler.postDelayed(keepAliveRunnable, interval);

		MegaApiAndroid.addLoggerObject(new AndroidLogger());
		MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);

		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		megaApi = getMegaApi();
		megaApiFolder = getMegaApiFolder();
		megaChatApi = getMegaChatApi();

		MegaChatApiAndroid.setLoggerObject(new AndroidChatLogger());
		MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX);

		Util.setContext(getApplicationContext());
		boolean fileLoggerSDK = false;
		if (dbH != null) {
			MegaAttributes attrs = dbH.getAttributes();
			if (attrs != null) {
				if (attrs.getFileLoggerSDK() != null) {
					try {
						fileLoggerSDK = Boolean.parseBoolean(attrs.getFileLoggerSDK());
					} catch (Exception e) {
						fileLoggerSDK = false;
					}
				} else {
					fileLoggerSDK = false;
				}
			} else {
				fileLoggerSDK = false;
			}
		}

		if (Util.DEBUG){
			MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);
		}
		else {
			Util.setFileLoggerSDK(fileLoggerSDK);
			if (fileLoggerSDK) {
				MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);
			} else {
				MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL);
			}
		}

		boolean fileLoggerKarere = false;
		if (dbH != null) {
			MegaAttributes attrs = dbH.getAttributes();
			if (attrs != null) {
				if (attrs.getFileLoggerKarere() != null) {
					try {
						fileLoggerKarere = Boolean.parseBoolean(attrs.getFileLoggerKarere());
					} catch (Exception e) {
						fileLoggerKarere = false;
					}
				} else {
					fileLoggerKarere = false;
				}
			} else {
				fileLoggerKarere = false;
			}
		}

		if (Util.DEBUG){
			MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX);
		}
		else {
			Util.setFileLoggerKarere(fileLoggerKarere);
			if (fileLoggerKarere) {
				MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX);
			} else {
				MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_ERROR);
			}
		}

		boolean useHttpsOnly = false;
		if (dbH != null) {
			useHttpsOnly = Boolean.parseBoolean(dbH.getUseHttpsOnly());
			log("Value of useHttpsOnly: "+useHttpsOnly);
			megaApi.useHttpsOnly(useHttpsOnly);
		}

//		initializeGA();
		
//		new MegaTest(getMegaApi()).start();
	}	
	

	static private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
		final String[] deviceNames = enumerator.getDeviceNames();

		// First, try to find front facing camera
		for (String deviceName : deviceNames) {
			if (enumerator.isFrontFacing(deviceName)) {
				VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

				if (videoCapturer != null) {
					return videoCapturer;
				}
			}
		}

		// Front facing camera not found, try something else
		for (String deviceName : deviceNames) {
			if (!enumerator.isFrontFacing(deviceName)) {
				VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

				if (videoCapturer != null) {
					return videoCapturer;
				}
			}
		}

		return null;
	}

	static VideoCapturer videoCapturer = null;

	static public void stopVideoCapture() {
		if (videoCapturer != null) {
			try {
				videoCapturer.stopCapture();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			videoCapturer = null;
		}
	}

	static public void startVideoCapture(long nativeAndroidVideoTrackSource, SurfaceTextureHelper surfaceTextureHelper) {
		// Settings
		boolean useCamera2 = false;
		boolean captureToTexture = true;
		int videoWidth = 480;
		int videoHeight = 320;
		int videoFps = 15;

		stopVideoCapture();
		Context context = ContextUtils.getApplicationContext();
		if (Camera2Enumerator.isSupported(context) && useCamera2) {
			videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
		} else {
			videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture));
		}

		if (videoCapturer == null) {
			log("Unable to create video capturer");
			return;
		}

		// Link the capturer with the surfaceTextureHelper and the native video source
		VideoCapturer.CapturerObserver capturerObserver = new AndroidVideoTrackSourceObserver(nativeAndroidVideoTrackSource);
		videoCapturer.initialize(surfaceTextureHelper, context, capturerObserver);

		// Start the capture!
		videoCapturer.startCapture(videoWidth, videoHeight, videoFps);
	}

//	private void initializeGA(){
//		// Set the log level to verbose.
//		GoogleAnalytics.getInstance(this).getLogger().setLogLevel(LogLevel.VERBOSE);
//	}
	
	public MegaApiAndroid getMegaApiFolder(){
		if (megaApiFolder == null){
			PackageManager m = getPackageManager();
			String s = getPackageName();
			PackageInfo p;
			String path = null;
			try
			{
				p = m.getPackageInfo(s, 0);
				path = p.applicationInfo.dataDir + "/";
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
			
			Log.d(TAG, "Database path: " + path);
			megaApiFolder = new MegaApiAndroid(MegaApplication.APP_KEY, 
					MegaApplication.USER_AGENT, path);
			
			megaApiFolder.setDownloadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
			megaApiFolder.setUploadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
		}
		
		return megaApiFolder;
	}

	public MegaChatApiAndroid getMegaChatApi(){
		if (megaChatApi == null){
			if (megaApi == null){
				getMegaApi();
			}
			else{
				megaChatApi = new MegaChatApiAndroid(megaApi);
				megaChatApi.addChatRequestListener(this);
			}
		}

		return megaChatApi;
	}

	public void disableMegaChatApi(){
		try {
			if (megaChatApi != null) {
				megaChatApi.removeChatRequestListener(this);
			}
		}
		catch (Exception e){}

		megaChatApi = null;
	}

	public void enableChat(){
		log("enableChat");
		if(Util.isChatEnabled()){
			megaChatApi = getMegaChatApi();
		}
	}
	
	public MegaApiAndroid getMegaApi()
	{
		if(megaApi == null)
		{
			log("MEGAAPI = null");
			PackageManager m = getPackageManager();
			String s = getPackageName();
			PackageInfo p;
			String path = null;
			try
			{
				p = m.getPackageInfo(s, 0);
				path = p.applicationInfo.dataDir + "/";
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
			
			Log.d(TAG, "Database path: " + path);
			megaApi = new MegaApiAndroid(MegaApplication.APP_KEY, 
					MegaApplication.USER_AGENT, path);
			
			megaApi.setDownloadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
			megaApi.setUploadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
			
			requestListener = new BackgroundRequestListener();
			log("ADD REQUESTLISTENER");
			megaApi.addRequestListener(requestListener);
			megaApi.addListener(this);

//			DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
//			if (dbH.getCredentials() != null){
//				megaChatApi = new MegaChatApiAndroid(megaApi, true);
//			}
//			else{
//				megaChatApi = new MegaChatApiAndroid(megaApi, false);
//			}

			if(Util.isChatEnabled()){
				megaChatApi = getMegaChatApi();
			}

			String language = Locale.getDefault().toString();
			boolean languageString = megaApi.setLanguage(language);
			log("Result: "+languageString+" Language: "+language);
			if(languageString==false){
				language = Locale.getDefault().getLanguage();
				languageString = megaApi.setLanguage(language);
				log("2--Result: "+languageString+" Language: "+language);
			}
		}
		
		return megaApi;
	}

	public static boolean isActivityVisible() {
		log("isActivityVisible() => " + activityVisible);
		return activityVisible;
	}

	public static void setFirstConnect(boolean firstConnect){
		MegaApplication.firstConnect = firstConnect;
	}

	public static boolean isFirstConnect(){
		return firstConnect;
	}

	public static void activityResumed() {
		log("activityResumed()");
		activityVisible = true;
	}

	public static void activityPaused() {
		log("activityPaused()");
		activityVisible = false;
	}

	private static boolean activityVisible = false;
	private static boolean isLoggingIn = false;
	private static boolean firstConnect = true;

	private static long openChatId = -1;

	public static boolean isLoggingIn() {
		return isLoggingIn;
	}

	public static void setLoggingIn(boolean loggingIn) {
		isLoggingIn = loggingIn;
	}

	public static void setOpenChatId(long openChatId){
		MegaApplication.openChatId = openChatId;
	}
	
	
//	synchronized Tracker getTracker(TrackerName trackerId) {
//		if (!mTrackers.containsKey(trackerId)) {
//
//			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
//			Tracker t = null;
//			if (trackerId == TrackerName.APP_TRACKER){
//				t = analytics.newTracker(GA_PROPERTY_ID);
//			}
////			Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(PROPERTY_ID)
////					: (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(R.xml.global_tracker)
////							: analytics.newTracker(R.xml.ecommerce_tracker);
//					mTrackers.put(trackerId, t);
//					
//		}
//	
//		return mTrackers.get(trackerId);
//	}


	public static long getOpenChatId() {
		return openChatId;
	}

	public String getLocalIpAddress(){
		return localIpAddress;
	}
	
	public void setLocalIpAddress(String ip){
		localIpAddress = ip;
	}
	
	public static void log(String message) {
		Util.log("MegaApplication", message);
	}



	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestFinish: " + request.getRequestString());
		if (request.getType() == MegaRequest.TYPE_LOGOUT){
			log("type_logout: " + e.getErrorCode() + "__" + request.getParamType());
			if (e.getErrorCode() == MegaError.API_ESID){
				log("calling ManagerActivity.logout");
				AccountController.logout(getApplicationContext(), getMegaApi(), getMegaChatApi(), false);
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {
		log("onUsersUpdate");
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> updatedNodes) {
		log("onNodesUpdate");
	}

	@Override
	public void onReloadNeeded(MegaApiJava api) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onTransferFinish(MegaApiJava api, MegaTransfer transfer,
			MegaError e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onTransferTemporaryError(MegaApiJava api,
			MegaTransfer transfer, MegaError e) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onTransferData(MegaApiJava api, MegaTransfer transfer,
			byte[] buffer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onAccountUpdate(MegaApiJava api) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {
		log("onContactRequestUpdate");

	}

	public void sendSignalPresenceActivity(){
		log("sendSignalPresenceActivity");
		if(Util.isChatEnabled()){
			if (megaChatApi != null){
				if(megaChatApi.isSignalActivityRequired()){
					megaChatApi.signalPresenceActivity();
				}
			}
		}
	}

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
		log("onRequestStart: Chat");
	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {
		log("onRequestUpdate: Chat");
	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		log("onRequestFinish: Chat " + request.getRequestString());
		if (request.getType() == MegaChatRequest.TYPE_SET_BACKGROUND_STATUS){
			log("SET_BACKGROUND_STATUS: " + request.getFlag());
		}
		if (request.getType() == MegaChatRequest.TYPE_LOGOUT) {
			log("CHAT_TYPE_LOGOUT: " + e.getErrorCode() + "__" + e.getErrorString());
			try{
				if (megaChatApi != null){
					megaChatApi.removeChatRequestListener(this);
				}
			}
			catch (Exception exc){}

			megaChatApi = null;
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		log("onRequestTemporaryError: Chat");
	}
}
