package mega.privacy.android.app;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
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

import me.leolin.shortcutbadger.ShortcutBadger;
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder;
import mega.privacy.android.app.fcm.ContactsAdvancedNotificationBuilder;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.megachat.BadgeIntentService;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.receivers.NetworkStateReceiver;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountSession;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatCallListenerInterface;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatNotificationListenerInterface;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;


public class MegaApplication extends Application implements MegaGlobalListenerInterface, MegaChatRequestListenerInterface, MegaChatNotificationListenerInterface, MegaChatCallListenerInterface, NetworkStateReceiver.NetworkStateReceiverListener {
	final String TAG = "MegaApplication";

	static final public String USER_AGENT = "MEGAAndroid/3.4.0_264";

	DatabaseHandler dbH;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	String localIpAddress = "";
	BackgroundRequestListener requestListener;
	final static public String APP_KEY = "6tioyn8ka5l6hty";
	final static private String APP_SECRET = "hfzgdtrma231qdm";

	MyAccountInfo myAccountInfo;
	boolean esid = false;

	private static boolean activityVisible = false;
	private static boolean isLoggingIn = false;
	private static boolean firstConnect = true;

	private static boolean showInfoChatMessages = false;

	private static boolean showPinScreen = true;

	private static long openChatId = -1;

	private static boolean closedChat = true;

	private static long openCallChatId = -1;

	private static boolean showRichLinkWarning = false;
	private static int counterNotNowRichLinkWarning = -1;
	private static boolean enabledRichLinks = false;

	private static int disableFileVersions = -1;

	private static boolean recentChatVisible = false;
	private static boolean chatNotificationReceived = false;

	private static String urlConfirmationLink = null;

	private static boolean registeredChatListeners = false;

	MegaChatApiAndroid megaChatApi = null;

	private NetworkStateReceiver networkStateReceiver;

	@Override
	public void networkAvailable() {
		log("Net available: Broadcast to ManagerActivity");
		Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE);
		intent.putExtra("actionType", Constants.GO_ONLINE);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
	}

	@Override
	public void networkUnavailable() {
		log("Net unavailable: Broadcast to ManagerActivity");
		Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE);
		intent.putExtra("actionType", Constants.GO_OFFLINE);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
	}

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

			if (request.getType() == MegaRequest.TYPE_LOGOUT){
				if (e.getErrorCode() == MegaError.API_ESID){
					log("TYPE_LOGOUT:API_ESID");
					myAccountInfo = new MyAccountInfo(getApplicationContext());

					esid = true;

					if(!Util.isChatEnabled()){
						log("Chat is not enable - proceed to show login");
						if(activityVisible){
							launchExternalLogout();
						}
					}

					AccountController.localLogoutApp(getApplicationContext());
				}
			}
			else if(request.getType() == MegaRequest.TYPE_LOGIN){
				log("BackgroundRequestListener:onRequestFinish:TYPE_LOGIN");
				if (e.getErrorCode() == MegaError.API_OK){
					askForFullAccountInfo();
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
			else if (request.getType() == MegaRequest.TYPE_GET_PRICING){
				if (e.getErrorCode() == MegaError.API_OK) {
					MegaPricing p = request.getPricing();

					dbH.setPricingTimestamp();

					if(myAccountInfo!=null){
						myAccountInfo.setProductAccounts(p);
						myAccountInfo.setPricing(p);
					}

					Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
					intent.putExtra("actionType", Constants.UPDATE_GET_PRICING);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
				}
				else{
					log("Error TYPE_GET_PRICING: "+e.getErrorCode());
				}
			}
			else if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_METHODS){
				log ("payment methods request");
				if(myAccountInfo!=null){
					myAccountInfo.setGetPaymentMethodsBoolean(true);
				}

				if (e.getErrorCode() == MegaError.API_OK){
					dbH.setPaymentMethodsTimeStamp();
					if(myAccountInfo!=null){
						myAccountInfo.setPaymentBitSet(Util.convertToBitSet(request.getNumber()));
					}

					Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
					intent.putExtra("actionType", Constants.UPDATE_PAYMENT_METHODS);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
				}
			}
			else if(request.getType() == MegaRequest.TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS){
				if (e.getErrorCode() == MegaError.API_OK){
					if(myAccountInfo!=null){
						myAccountInfo.setNumberOfSubscriptions(request.getNumber());
						log("NUMBER OF SUBS: " + myAccountInfo.getNumberOfSubscriptions());
					}

					Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
					intent.putExtra("actionType", Constants.UPDATE_CREDIT_CARD_SUBSCRIPTION);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
				}
			}
			else if (request.getType() == MegaRequest.TYPE_ACCOUNT_DETAILS){
				log ("account_details request");
				if (e.getErrorCode() == MegaError.API_OK){

					dbH.setAccountDetailsTimeStamp();

					if(myAccountInfo!=null && request.getMegaAccountDetails()!=null){
						myAccountInfo.setAccountInfo(request.getMegaAccountDetails());
						myAccountInfo.setAccountDetails();

						MegaAccountSession megaAccountSession = request.getMegaAccountDetails().getSession(0);

						if(megaAccountSession!=null){
							log("getMegaAccountSESSION not Null");
							dbH.setExtendedAccountDetailsTimestamp();
							long mostRecentSession = megaAccountSession.getMostRecentUsage();

							String date = TimeChatUtils.formatDateAndTime(mostRecentSession, TimeChatUtils.DATE_LONG_FORMAT);

							myAccountInfo.setLastSessionFormattedDate(date);
							myAccountInfo.setCreateSessionTimeStamp(megaAccountSession.getCreationTimestamp());
						}

						log("onRequest TYPE_ACCOUNT_DETAILS: "+myAccountInfo.getUsedPerc());
					}

					Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
					intent.putExtra("actionType", Constants.UPDATE_ACCOUNT_DETAILS);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
				}
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			log("BackgroundRequestListener: onRequestTemporaryError: " + request.getRequestString());
		}
		
	}

	public void launchExternalLogout(){
		log("launchExternalLogout");
		Intent loginIntent = new Intent(this, LoginActivityLollipop.class);
		loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(loginIntent);
	}

	private final int interval = 3000;
	private Handler keepAliveHandler = new Handler();
	int backgroundStatus = -1;

	private Runnable keepAliveRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				if (activityVisible) {
					log("KEEPALIVE: " + System.currentTimeMillis());
					if (megaChatApi != null) {
						backgroundStatus = megaChatApi.getBackgroundStatus();
						log("backgroundStatus_activityVisible: " + backgroundStatus);
						if (backgroundStatus != -1){
							if (backgroundStatus != 0){
								megaChatApi.setBackgroundStatus(false);
							}
						}
					}

				} else {
					log("KEEPALIVEAWAY: " + System.currentTimeMillis());
					if (megaChatApi != null) {
						backgroundStatus = megaChatApi.getBackgroundStatus();
						log("backgroundStatus_!activityVisible: " + backgroundStatus);
						if (backgroundStatus != -1){
							if (backgroundStatus != 1){
								megaChatApi.setBackgroundStatus(true);
							}
						}
					}
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

		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		megaApi = getMegaApi();
		megaApiFolder = getMegaApiFolder();
		megaChatApi = getMegaChatApi();

		Util.setContext(getApplicationContext());
		boolean fileLoggerSDK = false;
		boolean staging = false;
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

				if (attrs.getStaging() != null){
					try{
						staging = Boolean.parseBoolean(attrs.getStaging());
					} catch (Exception e){ staging = false;}
				}
			}
			else {
				fileLoggerSDK = false;
				staging = false;
			}
		}

		if (staging){
			megaApi.changeApiUrl("https://staging.api.mega.co.nz/");
		}
		else{
			megaApi.changeApiUrl("https://g.api.mega.co.nz/");
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

		//init logger only when pre-request are all ready
		MegaApiAndroid.addLoggerObject(new AndroidLogger(AndroidLogger.LOG_FILE_NAME, Util.getFileLoggerSDK()));
		MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);

		MegaChatApiAndroid.setLoggerObject(new AndroidChatLogger(AndroidChatLogger.LOG_FILE_NAME, Util.getFileLoggerKarere()));
		MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX);

		boolean useHttpsOnly = false;
		if (dbH != null) {
			useHttpsOnly = Boolean.parseBoolean(dbH.getUseHttpsOnly());
			log("Value of useHttpsOnly: "+useHttpsOnly);
			megaApi.useHttpsOnly(useHttpsOnly);
		}

		myAccountInfo = new MyAccountInfo(this);

		if (dbH != null) {
			dbH.resetExtendedAccountDetailsTimestamp();
		}

		networkStateReceiver = new NetworkStateReceiver();
		networkStateReceiver.addListener(this);
		this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
//		initializeGA();
		
//		new MegaTest(getMegaApi()).start();
	}

	public void askForFullAccountInfo(){
		log("askForFullAccountInfo");
		megaApi.getPaymentMethods(null);
		megaApi.getAccountDetails(null);
		megaApi.getPricing(null);
		megaApi.creditCardQuerySubscriptions(null);
	}

	public void askForPaymentMethods(){
		log("askForPaymentMethods");
		megaApi.getPaymentMethods(null);
	}

	public void askForPricing(){

		megaApi.getPricing(null);
	}

	public void askForAccountDetails(){

		megaApi.getAccountDetails(null);
	}

	public void askForCCSubscriptions(){

		megaApi.creditCardQuerySubscriptions(null);
	}

	public void askForExtendedAccountDetails(){
		log("askForExtendedAccountDetails");
		if (dbH != null) {
			dbH.resetExtendedAccountDetailsTimestamp();
		}
		megaApi.getExtendedAccountDetails(true,false, false, null);
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
			}
		}

		if(megaChatApi!=null) {
			if (!registeredChatListeners) {
				log("Add listeners of megaChatApi");
				megaChatApi.addChatRequestListener(this);
				megaChatApi.addChatNotificationListener(this);
				megaChatApi.addChatCallListener(this);
				registeredChatListeners = true;
			}
		}

		return megaChatApi;
	}

	public void disableMegaChatApi(){
		try {
			if (megaChatApi != null) {
				megaChatApi.removeChatRequestListener(this);
				megaChatApi.removeChatNotificationListener(this);
				megaChatApi.removeChatCallListener(this);
				registeredChatListeners = false;
			}
		}
		catch (Exception e){}
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

			megaApi.addGlobalListener(this);

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

	public static boolean isShowInfoChatMessages() {
		return showInfoChatMessages;
	}

	public static void setShowInfoChatMessages(boolean showInfoChatMessages) {
		MegaApplication.showInfoChatMessages = showInfoChatMessages;
	}

	public static void activityResumed() {
		log("activityResumed()");
		activityVisible = true;
	}

	public static void activityPaused() {
		log("activityPaused()");
		activityVisible = false;
	}

	public static boolean isShowPinScreen() {
		return showPinScreen;
	}

	public static void setShowPinScreen(boolean showPinScreen) {
		MegaApplication.showPinScreen = showPinScreen;
	}

	public static String getUrlConfirmationLink() {
		return urlConfirmationLink;
	}

	public static void setUrlConfirmationLink(String urlConfirmationLink) {
		MegaApplication.urlConfirmationLink = urlConfirmationLink;
	}

	public static boolean isLoggingIn() {
		return isLoggingIn;
	}

	public static void setLoggingIn(boolean loggingIn) {
		isLoggingIn = loggingIn;
	}

	public static void setOpenChatId(long openChatId){
		MegaApplication.openChatId = openChatId;
	}

	public static long getOpenCallChatId() {
		return openCallChatId;
	}

	public static void setOpenCallChatId(long openCallChatId) {
		MegaApplication.openCallChatId = openCallChatId;
	}

	public static boolean isRecentChatVisible() {
		if(activityVisible){
			return recentChatVisible;
		}
		else{
			return false;
		}
	}

	public static void setRecentChatVisible(boolean recentChatVisible) {
		log("setRecentChatVisible: "+recentChatVisible);
		MegaApplication.recentChatVisible = recentChatVisible;
	}

	public static boolean isChatNotificationReceived() {
		return chatNotificationReceived;
	}

	public static void setChatNotificationReceived(boolean chatNotificationReceived) {
		MegaApplication.chatNotificationReceived = chatNotificationReceived;
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
	public void onAccountUpdate(MegaApiJava api) {
		log("onAccountUpdate");

		megaApi.getPaymentMethods(null);
		megaApi.getAccountDetails(null);
		megaApi.getPricing(null);
		megaApi.creditCardQuerySubscriptions(null);
		dbH.resetExtendedAccountDetailsTimestamp();
	}

	@Override
	public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {
		log("onContactRequestUpdate");

		if(requests!=null){
			for (int i = 0; i < requests.size(); i++) {
				MegaContactRequest cr = requests.get(i);
				if (cr != null) {
					if ((cr.getStatus() == MegaContactRequest.STATUS_UNRESOLVED) && (!cr.isOutgoing())) {

						ContactsAdvancedNotificationBuilder notificationBuilder;
						notificationBuilder =  ContactsAdvancedNotificationBuilder.newInstance(this, megaApi);

						notificationBuilder.removeAllIncomingContactNotifications();
						notificationBuilder.showIncomingContactRequestNotification();

						log("IPC: " + cr.getSourceEmail() + " cr.isOutgoing: " + cr.isOutgoing() + " cr.getStatus: " + cr.getStatus());
					}
					else if ((cr.getStatus() == MegaContactRequest.STATUS_ACCEPTED) && (cr.isOutgoing())) {

						ContactsAdvancedNotificationBuilder notificationBuilder;
						notificationBuilder =  ContactsAdvancedNotificationBuilder.newInstance(this, megaApi);

						notificationBuilder.showAcceptanceContactRequestNotification(cr.getTargetEmail());

						log("ACCEPT OPR: " + cr.getSourceEmail() + " cr.isOutgoing: " + cr.isOutgoing() + " cr.getStatus: " + cr.getStatus());
					}
				}
			}
		}
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
		log("onRequestStart (CHAT): " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {
	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		log("onRequestFinish (CHAT): " + request.getRequestString() + "_"+e.getErrorCode());
		if (request.getType() == MegaChatRequest.TYPE_SET_BACKGROUND_STATUS){
			log("SET_BACKGROUND_STATUS: " + request.getFlag());
		}
		else if (request.getType() == MegaChatRequest.TYPE_LOGOUT) {
			log("CHAT_TYPE_LOGOUT: " + e.getErrorCode() + "__" + e.getErrorString());

			try{
				if (megaChatApi != null){
					megaChatApi.removeChatRequestListener(this);
					megaChatApi.removeChatNotificationListener(this);
					megaChatApi.removeChatCallListener(this);
					registeredChatListeners = false;
				}
			}
			catch (Exception exc){}

			try{
				ShortcutBadger.applyCount(getApplicationContext(), 0);

				startService(new Intent(getApplicationContext(), BadgeIntentService.class).putExtra("badgeCount", 0));
			}
			catch (Exception exc){
                log("EXCEPTION removing badge indicator");
            }

			if(megaApi!=null){
				int loggedState = megaApi.isLoggedIn();
				log("Login status on "+loggedState);
				if(loggedState==0){
					AccountController aC = new AccountController(this);
					aC.logoutConfirmed(this);

					if(activityVisible){
						if(getUrlConfirmationLink()!=null){
							log("Launch intent to confirmation account screen");
							Intent confirmIntent = new Intent(this, LoginActivityLollipop.class);
							confirmIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
							confirmIntent.putExtra(Constants.EXTRA_CONFIRMATION, getUrlConfirmationLink());
							confirmIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							confirmIntent.setAction(Constants.ACTION_CONFIRM);
							setUrlConfirmationLink(null);
							startActivity(confirmIntent);
						}
						else{
							log("Launch intent to login activity");
							Intent tourIntent = new Intent(this, LoginActivityLollipop.class);
							tourIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
							this.startActivity(tourIntent);
						}
					}
					else{
						log("No activity visible on logging out chat");
						if(getUrlConfirmationLink()!=null){
							log("Show confirmation account screen");
							Intent confirmIntent = new Intent(this, LoginActivityLollipop.class);
							confirmIntent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
							confirmIntent.putExtra(Constants.EXTRA_CONFIRMATION, getUrlConfirmationLink());
							confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
							confirmIntent.setAction(Constants.ACTION_CONFIRM);
							setUrlConfirmationLink(null);
							startActivity(confirmIntent);
						}
					}
				}
				else{
					log("Disable chat finish logout");
				}
			}
			else{

				AccountController aC = new AccountController(this);
				aC.logoutConfirmed(this);

				if(activityVisible){
					log("Launch intent to login screen");
					Intent tourIntent = new Intent(this, LoginActivityLollipop.class);
					tourIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					this.startActivity(tourIntent);
				}
			}
		}
		else if (request.getType() == MegaChatRequest.TYPE_PUSH_RECEIVED) {
			log("TYPE_PUSH_RECEIVED: " + e.getErrorCode() + "__" + e.getErrorString());

			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				log("OK:TYPE_PUSH_RECEIVED");
				chatNotificationReceived = true;

				ChatAdvancedNotificationBuilder notificationBuilder;
				notificationBuilder =  ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);

				notificationBuilder.removeAllChatNotifications();
				notificationBuilder.generateChatNotification(request);
			}
			else{
				log("Error TYPE_PUSH_RECEIVED: "+e.getErrorString());
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		log("onRequestTemporaryError (CHAT): "+e.getErrorString());
	}

	@Override
	public void onEvent(MegaApiJava api, MegaEvent event) {

	}

	@Override
	public void onChatNotification(MegaChatApiJava api, long chatid, MegaChatMessage msg) {
		log("onChatNotification");

		int unread = megaChatApi.getUnreadChats();
		//Add Android version check if needed
		if (unread == 0) {
			//Remove badge indicator - no unread chats
			ShortcutBadger.applyCount(getApplicationContext(), 0);
			//Xiaomi support
			startService(new Intent(getApplicationContext(), BadgeIntentService.class).putExtra("badgeCount", 0));
		} else {
			//Show badge with indicator = unread
			ShortcutBadger.applyCount(getApplicationContext(), Math.abs(unread));
			//Xiaomi support
			startService(new Intent(getApplicationContext(), BadgeIntentService.class).putExtra("badgeCount", unread));
		}

		if(MegaApplication.getOpenChatId() == chatid){
			log("Do not update/show notification - opened chat");
			return;
		}

		if(isRecentChatVisible()){
			log("Do not show notification - recent chats shown");
			return;
		}

		if(activityVisible){

			try{
				if(msg!=null){

					NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					mNotificationManager.cancel(Constants.NOTIFICATION_GENERAL_PUSH_CHAT);

					if(msg.getStatus()==MegaChatMessage.STATUS_NOT_SEEN){
						if(msg.getType()==MegaChatMessage.TYPE_NORMAL||msg.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT||msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT||msg.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT){
							if(msg.isDeleted()){
								log("Message deleted");
//								updateChatNotification(chatid, msg);

								megaChatApi.pushReceived(false);
							}
							else if(msg.isEdited()){
								log("Message edited");
//								updateChatNotification(chatid, msg);
								megaChatApi.pushReceived(false);
							}
							else{
								log("New normal message");
//								showChatNotification(chatid, msg);
								megaChatApi.pushReceived(true);
							}
						}
						else if(msg.getType()==MegaChatMessage.TYPE_TRUNCATE){
							log("New TRUNCATE message");
//							showChatNotification(chatid, msg);
							megaChatApi.pushReceived(false);
						}
					}
					else{
						log("Message SEEN");
//						removeChatSeenNotification(chatid, msg);
						megaChatApi.pushReceived(false);
					}
				}
			}
			catch (Exception e){
				log("EXCEPTION when showing chat notification");
			}
		}
		else{
			log("Do not notify chat messages: app in background");
		}
	}

//	public void updateChatNotification(long chatid, MegaChatMessage msg){
//		ChatAdvancedNotificationBuilder notificationBuilder;
//		notificationBuilder =  ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);
//
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//			notificationBuilder.updateNotification(chatid, msg);
//		}
//		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//
//			NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//			StatusBarNotification[] notifs = mNotificationManager.getActiveNotifications();
//			boolean shown=false;
//			for(int i = 0; i< notifs.length; i++){
//				if(notifs[i].getId()==Constants.NOTIFICATION_PRE_N_CHAT){
//					shown = true;
//					break;
//				}
//			}
//			if(shown){
//				notificationBuilder.sendBundledNotificationIPC(null, null, chatid, msg);
//			}
//		}
//		else{
//			notificationBuilder.sendBundledNotificationIPC(null, null, chatid, msg);
//		}
//	}
//
//	public void removeChatSeenNotification(long chatid, MegaChatMessage msg){
//		ChatAdvancedNotificationBuilder notificationBuilder;
//		notificationBuilder =  ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);
//
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//			notificationBuilder.removeSeenNotification(chatid, msg);
//		}
//		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//
//			NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//			StatusBarNotification[] notifs = mNotificationManager.getActiveNotifications();
//			boolean shown=false;
//			for(int i = 0; i< notifs.length; i++){
//				if(notifs[i].getId()==Constants.NOTIFICATION_PRE_N_CHAT){
//					shown = true;
//					break;
//				}
//			}
//			if(shown){
//				notificationBuilder.sendBundledNotificationIPC(null, null, chatid, msg);
//			}
//		}
//		else{
//			notificationBuilder.sendBundledNotificationIPC(null, null, chatid, msg);
//		}
//	}

	@Override
	public void onChatCallUpdate(MegaChatApiJava api, MegaChatCall call) {
		log("onChatCallUpdate");

		if (call.getStatus() == MegaChatCall.CALL_STATUS_DESTROYED) {
			log("Call destroyed: "+call.getTermCode());
		}

		if (call.getStatus() >= MegaChatCall.CALL_STATUS_IN_PROGRESS) {
			clearIncomingCallNotification(call.getId());
		}

		MegaHandleList handleList = megaChatApi.getChatCalls();
		if(handleList!=null) {

			long numberOfCalls = handleList.size();
			log("Number of calls in progress: " + numberOfCalls);
			if (numberOfCalls == 1) {

				if (call.getStatus() <= MegaChatCall.CALL_STATUS_IN_PROGRESS) {

					long chatId = handleList.get(0);

					if(openCallChatId!=chatId){
						MegaChatCall callToLaunch = megaChatApi.getChatCall(chatId);
						if (callToLaunch != null) {
							if (callToLaunch.getStatus() <= MegaChatCall.CALL_STATUS_IN_PROGRESS) {
								launchCallActivity(callToLaunch);
							} else {
								log("Launch not in correct status");
							}
						}
					}
					else{
						log("Call already opened");
					}
				}
			} else if (numberOfCalls > 1) {
				log("MORE than one call in progress: " + numberOfCalls);
				checkQueuedCalls();

			} else {
				log("No calls in progress");
			}
		}

		//Show missed call if time out ringing (for incoming calls)
		if(call.getStatus()==MegaChatCall.CALL_STATUS_DESTROYED){
			try{
				if(((call.getTermCode()==MegaChatCall.TERM_CODE_ANSWER_TIMEOUT || call.getTermCode()==MegaChatCall.TERM_CODE_CALL_REQ_CANCEL) && !(call.isIgnored()))){
					log("onChatCallUpdate:TERM_CODE_ANSWER_TIMEOUT");
					if(call.isLocalTermCode()==false){
						log("onChatCallUpdate:localTermCodeNotLocal");
						try{
							ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);
							notificationBuilder.showMissedCallNotification(call);
						}
						catch(Exception e){
							log("EXCEPTION when showing missed call notification: "+e.getMessage());
						}
					}
				}
			}
			catch(Exception e){
				log("EXCEPTION when showing missed call notification: "+e.getMessage());
			}
		}
	}

	public void checkQueuedCalls(){
		log("checkQueuedCalls");

		try{
			ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);
			notificationBuilder.checkQueuedCalls();
		}
		catch (Exception e){
			log("EXCEPTION: "+e.getMessage());
		}
	}

	public void launchCallActivity(MegaChatCall call){
		log("launchCallActivity: "+call.getStatus());
		MegaApplication.setShowPinScreen(false);

		Intent i = new Intent(this, ChatCallActivity.class);
		i.putExtra("chatHandle", call.getChatid());
		i.putExtra("callId", call.getId());
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		startActivity(i);

		MegaChatRoom chatRoom = megaChatApi.getChatRoom(call.getChatid());
		log("Launch call: "+chatRoom.getTitle());

	}

	public void clearIncomingCallNotification(long chatCallId) {
		log("clearIncomingCallNotification:chatID: "+chatCallId);

		try{
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			String notificationCallId = MegaApiJava.userHandleToBase64(chatCallId);
			int notificationId = (notificationCallId).hashCode();

			notificationManager.cancel(notificationId);
		}
		catch(Exception e){
			log("clearIncomingCallNotification:EXCEPTION");
		}
	}

	public static boolean isShowRichLinkWarning() {
		return showRichLinkWarning;
	}

	public static void setShowRichLinkWarning(boolean showRichLinkWarning) {
		MegaApplication.showRichLinkWarning = showRichLinkWarning;
	}

	public static int getCounterNotNowRichLinkWarning() {
		return counterNotNowRichLinkWarning;
	}

	public static void setCounterNotNowRichLinkWarning(int counterNotNowRichLinkWarning) {
		MegaApplication.counterNotNowRichLinkWarning = counterNotNowRichLinkWarning;
	}

	public static boolean isEnabledRichLinks() {
		return enabledRichLinks;
	}

	public static void setEnabledRichLinks(boolean enabledRichLinks) {
		MegaApplication.enabledRichLinks = enabledRichLinks;
	}

	public static int isDisableFileVersions() {
		return disableFileVersions;
	}

	public static void setDisableFileVersions(boolean disableFileVersions) {
		if(disableFileVersions){
			MegaApplication.disableFileVersions = 1;
		}
		else{
			MegaApplication.disableFileVersions = 0;
		}
	}

	public boolean isEsid() {
		return esid;
	}

	public void setEsid(boolean esid) {
		this.esid = esid;
	}

	public static boolean isClosedChat() {
		return closedChat;
	}

	public static void setClosedChat(boolean closedChat) {
		MegaApplication.closedChat = closedChat;
	}

	public MyAccountInfo getMyAccountInfo() {
		return myAccountInfo;
	}
}
