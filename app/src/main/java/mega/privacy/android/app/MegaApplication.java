package mega.privacy.android.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDexApplication;
import android.support.text.emoji.EmojiCompat;
import android.support.text.emoji.FontRequestEmojiCompatConfig;
import android.support.text.emoji.bundled.BundledEmojiCompatConfig;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.provider.FontRequest;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import org.webrtc.AndroidVideoTrackSourceObserver;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.ContextUtils;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import me.leolin.shortcutbadger.ShortcutBadger;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.TwitterEmojiProvider;
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder;
import mega.privacy.android.app.fcm.ContactsAdvancedNotificationBuilder;
import mega.privacy.android.app.fcm.IncomingCallService;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.megachat.BadgeIntentService;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.receivers.NetworkStateReceiver;
import mega.privacy.android.app.utils.CacheFolderManager;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TimeUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountSession;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatCallListenerInterface;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatNotificationListenerInterface;
import nz.mega.sdk.MegaChatPresenceConfig;
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
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static android.provider.Settings.System.DEFAULT_RINGTONE_URI;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Util.toCDATA;
import static mega.privacy.android.app.utils.JobUtil.scheduleCameraUploadJob;


public class MegaApplication extends MultiDexApplication implements MegaGlobalListenerInterface, MegaChatRequestListenerInterface, MegaChatNotificationListenerInterface, MegaChatCallListenerInterface, NetworkStateReceiver.NetworkStateReceiverListener, MegaChatListenerInterface {
	final String TAG = "MegaApplication";

	final private static int INITIAL_SOUND_LEVEL = 10;
	static final public String USER_AGENT = "MEGAAndroid/3.6.4_249";

	DatabaseHandler dbH;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	String localIpAddress = "";
	BackgroundRequestListener requestListener;
	final static public String APP_KEY = "6tioyn8ka5l6hty";
	final static private String APP_SECRET = "hfzgdtrma231qdm";

	MyAccountInfo myAccountInfo;
	boolean esid = false;

	private int storageState = MegaApiJava.STORAGE_STATE_GREEN; //Default value

	private static boolean activityVisible = false;
	private static boolean isLoggingIn = false;
	private static boolean firstConnect = true;

	private static final boolean USE_BUNDLED_EMOJI = false;

	private static boolean showInfoChatMessages = false;

	private static boolean showPinScreen = true;

	private static long openChatId = -1;

	private static boolean closedChat = true;
	private static HashMap<Long, Boolean> hashMapSpeaker = new HashMap<>();
	private static HashMap<Long, Boolean> hashMapCallLayout = new HashMap<>();

	private static long openCallChatId = -1;

	private static boolean showRichLinkWarning = false;
	private static int counterNotNowRichLinkWarning = -1;
	private static boolean enabledRichLinks = false;

	private static boolean enabledGeoLocation = false;

	private static int disableFileVersions = -1;

	private static boolean recentChatVisible = false;
	private static boolean chatNotificationReceived = false;

	private static String urlConfirmationLink = null;

	private static boolean registeredChatListeners = false;

	MegaChatApiAndroid megaChatApi = null;

	private NetworkStateReceiver networkStateReceiver;
	private BroadcastReceiver logoutReceiver;

	/*A/V Calls*/
	private AudioManager audioManager;
	private MediaPlayer thePlayer;
	private Ringtone ringtone = null;
	private Vibrator vibrator = null;
	private Timer ringerTimer = null;

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
				if (e.getErrorCode() == MegaError.API_EINCOMPLETE){
					if (request.getParamType() == MegaError.API_ESSL) {
						log("SSL verification failed");
						Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED);
						LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
					}
				}
				else if (e.getErrorCode() == MegaError.API_ESID){
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
			else if(request.getType() == MegaRequest.TYPE_FETCH_NODES){
				log("BackgroundRequestListener:onRequestFinish:TYPE_FETCH_NODES");
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
						myAccountInfo.setAccountDetails(request.getNumDetails());

						boolean sessions = (request.getNumDetails() & myAccountInfo.hasSessionsDetails) != 0;
						if (sessions) {
							MegaAccountSession megaAccountSession = request.getMegaAccountDetails().getSession(0);

							if(megaAccountSession!=null){
								log("getMegaAccountSESSION not Null");
								dbH.setExtendedAccountDetailsTimestamp();
								long mostRecentSession = megaAccountSession.getMostRecentUsage();

								String date = TimeUtils.formatDateAndTime(getApplicationContext(),mostRecentSession, TimeUtils.DATE_LONG_FORMAT);

								myAccountInfo.setLastSessionFormattedDate(date);
								myAccountInfo.setCreateSessionTimeStamp(megaAccountSession.getCreationTimestamp());
							}
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
						if (backgroundStatus != -1 && backgroundStatus != 0) {
							MegaHandleList callRingIn = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RING_IN);
							if (callRingIn == null || callRingIn.size() <= 0) {
								megaChatApi.setBackgroundStatus(false);
							}
						}
					}

				} else {
					log("KEEPALIVEAWAY: " + System.currentTimeMillis());
					if (megaChatApi != null) {
						backgroundStatus = megaChatApi.getBackgroundStatus();
						log("backgroundStatus_!activityVisible: " + backgroundStatus);
						if (backgroundStatus != -1 && backgroundStatus != 1) {
							megaChatApi.setBackgroundStatus(true);
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
        scheduleCameraUploadJob(getApplicationContext());

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

		MegaApiAndroid.addLoggerObject(new AndroidLogger(AndroidLogger.LOG_FILE_NAME, fileLoggerSDK));
		MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);

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

		MegaChatApiAndroid.setLoggerObject(new AndroidChatLogger(AndroidChatLogger.LOG_FILE_NAME, fileLoggerKarere));
		MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX);

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

		myAccountInfo = new MyAccountInfo(this);

		if (dbH != null) {
			dbH.resetExtendedAccountDetailsTimestamp();
		}

		networkStateReceiver = new NetworkStateReceiver();
		networkStateReceiver.addListener(this);
		this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

		logoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context,Intent intent) {
                if (intent != null) {
                    if (intent.getAction() == Constants.ACTION_LOG_OUT) {
                        storageState = MegaApiJava.STORAGE_STATE_GREEN; //Default value
                    }
                }
            }
        };
		LocalBroadcastManager.getInstance(this).registerReceiver(logoutReceiver, new IntentFilter(Constants.ACTION_LOG_OUT));
		EmojiManager.install(new TwitterEmojiProvider());

		final EmojiCompat.Config config;
		if (USE_BUNDLED_EMOJI) {
			log("use Bundle emoji");
			// Use the bundled font for EmojiCompat
			config = new BundledEmojiCompatConfig(getApplicationContext());
		} else {
			log("use downloadable font for EmojiCompat");
			// Use a downloadable font for EmojiCompat
			final FontRequest fontRequest = new FontRequest(
					"com.google.android.gms.fonts",
					"com.google.android.gms",
					"Noto Color Emoji Compat",
					R.array.com_google_android_gms_fonts_certs);
			config = new FontRequestEmojiCompatConfig(getApplicationContext(), fontRequest)
					.setReplaceAll(false)
					.registerInitCallback(new EmojiCompat.InitCallback() {
						@Override
						public  void onInitialized() {
							log("EmojiCompat initialized");
						}
						@Override
						public  void onFailed(@Nullable Throwable throwable) {
							log("EmojiCompat initialization failed");
						}
					});
		}
		EmojiCompat.init(config);
		// clear the cache files stored in the external cache folder.
        clearPublicCache(this);

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
		log("askForAccountDetails");
		if (dbH != null) {
			dbH.resetAccountDetailsTimeStamp();
		}
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
		log("createCameraCapturer");
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
		log("stopVideoCapture");

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
		log("startVideoCapture");

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

	static public void startVideoCaptureWithParameters(int videoWidth, int videoHeight, int videoFps, long nativeAndroidVideoTrackSource, SurfaceTextureHelper surfaceTextureHelper) {
		log("startVideoCaptureWithParameters");

		// Settings
		boolean useCamera2 = false;
		boolean captureToTexture = true;

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

			megaApiFolder.retrySSLerrors(true);

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
				megaChatApi.addChatListener(this);
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
				megaChatApi.removeChatListener(this);
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

			megaApi.retrySSLerrors(true);

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

	public static void setOpenCallChatId(long value) {
	    log("setOpenCallChatId: "+value);
		openCallChatId = value;
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
	public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
		log("onUserAlertsUpdate");
		updateAppBadge();
	}

	@Override
	public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> updatedNodes) {
		log("onNodesUpdate");
		if (updatedNodes != null) {
			log("updatedNodes: " + updatedNodes.size());

			for (int i = 0; i < updatedNodes.size(); i++) {
				MegaNode n = updatedNodes.get(i);
				if (n.isInShare() && n.hasChanged(MegaNode.CHANGE_TYPE_INSHARE)) {
					log("updatedNodes name: " + n.getName() + " isInshared: " + n.isInShare() + " getchanges: " + n.getChanges() + " haschanged(TYPE_INSHARE): " + n.hasChanged(MegaNode.CHANGE_TYPE_INSHARE));

					showSharedFolderNotification(n);
				}
			}
		}
		else{
			log("Updated nodes is NULL");
		}
	}

	public void showSharedFolderNotification(MegaNode n) {
		log("showSharedFolderNotification");

		try {
			ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
			String name = "";
			for (int j = 0; j < sharesIncoming.size(); j++) {
				MegaShare mS = sharesIncoming.get(j);
				if (mS.getNodeHandle() == n.getHandle()) {
					MegaUser user = megaApi.getContact(mS.getUser());
					if (user != null) {
						MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));

						if (contactDB != null) {
							if (!contactDB.getName().equals("")) {
								name = contactDB.getName() + " " + contactDB.getLastName();

							} else {
								name = user.getEmail();

							}
						} else {
							log("The contactDB is null: ");
							name = user.getEmail();

						}
					} else {
						name = user.getEmail();
					}
				}
			}

			String source = "<b>" + n.getName() + "</b> " + getString(R.string.incoming_folder_notification) + " " + toCDATA(name);
			Spanned notificationContent;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				notificationContent = Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
			} else {
				notificationContent = Html.fromHtml(source);
			}

			int notificationId = Constants.NOTIFICATION_PUSH_CLOUD_DRIVE;
			String notificationChannelId = Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_ID;
			String notificationChannelName = Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_NAME;

			Intent intent = new Intent(this, ManagerActivityLollipop.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.setAction(Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
					PendingIntent.FLAG_ONE_SHOT);

			Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_HIGH);
				channel.setShowBadge(true);
				NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.createNotificationChannel(channel);

				NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(this, notificationChannelId);
				notificationBuilderO
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setContentTitle(getString(R.string.title_incoming_folder_notification))
						.setContentText(notificationContent)
						.setStyle(new NotificationCompat.BigTextStyle()
								.bigText(notificationContent))
						.setAutoCancel(true)
						.setSound(defaultSoundUri)
						.setContentIntent(pendingIntent)
						.setColor(ContextCompat.getColor(this, R.color.mega));

				Drawable d = getResources().getDrawable(R.drawable.ic_folder_incoming, getTheme());
				notificationBuilderO.setLargeIcon(((BitmapDrawable) d).getBitmap());

				notificationManager.notify(notificationId, notificationBuilderO.build());
			}
			else {
				NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setContentTitle(getString(R.string.title_incoming_folder_notification))
						.setContentText(notificationContent)
						.setStyle(new NotificationCompat.BigTextStyle()
								.bigText(notificationContent))
						.setAutoCancel(true)
						.setSound(defaultSoundUri)
						.setContentIntent(pendingIntent);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					notificationBuilder.setColor(ContextCompat.getColor(this, R.color.mega));
				}

				Drawable d;

				if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					d = getResources().getDrawable(R.drawable.ic_folder_incoming, getTheme());
				} else {
					d = ContextCompat.getDrawable(this, R.drawable.ic_folder_incoming);
				}

				notificationBuilder.setLargeIcon(((BitmapDrawable) d).getBitmap());


				if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
					//API 25 = Android 7.1
					notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
				} else {
					notificationBuilder.setPriority(NotificationManager.IMPORTANCE_HIGH);
				}

				NotificationManager notificationManager =
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

				notificationManager.notify(notificationId, notificationBuilder.build());
			}
		} catch (Exception e) {
			log("Exception: " + e.toString());
		}

//        try{
//            String source = "Tap to get more info";
//            Spanned notificationContent;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//                notificationContent = Html.fromHtml(source,Html.FROM_HTML_MODE_LEGACY);
//            } else {
//                notificationContent = Html.fromHtml(source);
//            }
//
//            int notificationId = Constants.NOTIFICATION_PUSH_CLOUD_DRIVE;
//
//            Intent intent = new Intent(this, ManagerActivityLollipop.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            intent.setAction(Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION);
//            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
//                    PendingIntent.FLAG_ONE_SHOT);
//
//            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
//                    .setSmallIcon(R.drawable.ic_stat_notify_download)
//                    .setContentTitle(getString(R.string.title_incoming_folder_notification))
//                    .setContentText(notificationContent)
//                    .setStyle(new NotificationCompat.BigTextStyle()
//                            .bigText(notificationContent))
//                    .setAutoCancel(true)
//                    .setSound(defaultSoundUri)
//                    .setColor(ContextCompat.getColor(this,R.color.mega))
//                    .setContentIntent(pendingIntent);
//
//            Drawable d;
//
//            if(android.os.Build.VERSION.SDK_INT >=  Build.VERSION_CODES.LOLLIPOP){
//                d = getResources().getDrawable(R.drawable.ic_folder_incoming, getTheme());
//            } else {
//                d = getResources().getDrawable(R.drawable.ic_folder_incoming);
//            }
//
//            notificationBuilder.setLargeIcon(((BitmapDrawable)d).getBitmap());
//
//            NotificationManager notificationManager =
//                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//            notificationManager.notify(notificationId, notificationBuilder.build());
//        }
//        catch(Exception e){
//            log("Exception when showing shared folder notification: "+e.getMessage());
//        }
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

		updateAppBadge();

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
					megaChatApi.removeChatListener(this);
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
		log("onEvent: " + event.getText());

		if (event.getType() == MegaEvent.EVENT_STORAGE) {
			log("Storage status changed");
			int state = (int) event.getNumber();
			if (state == MegaApiJava.STORAGE_STATE_CHANGE) {
				api.getAccountDetails(null);
			}
			else {
				storageState = state;
				Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
				intent.setAction(Constants.ACTION_STORAGE_STATE_CHANGED);
				intent.putExtra("state", state);
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
			}
		}
	}


	@Override
	public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {

	}

	@Override
	public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {

	}

	@Override
	public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userhandle, int status, boolean inProgress) {

	}

	@Override
	public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {
		if(config.isPending()==false){
			log("Launch local broadcast");
			Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE);
			LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
		}
	}

	@Override
	public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {

	}

	@Override
	public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {

	}


	public void updateAppBadge(){
		log("updateAppBadge");

		int totalHistoric = 0;
		int totalIpc = 0;
		if(megaApi!=null && megaApi.getRootNode()!=null){
			totalHistoric = megaApi.getNumUnreadUserAlerts();
			ArrayList<MegaContactRequest> requests = megaApi.getIncomingContactRequests();
			if(requests!=null) {
				totalIpc = requests.size();
			}
		}

		int chatUnread = 0;
		if(Util.isChatEnabled() && megaChatApi != null) {
			chatUnread = megaChatApi.getUnreadChats();
		}

		int totalNotifications = totalHistoric + totalIpc + chatUnread;
		//Add Android version check if needed
		if (totalNotifications == 0) {
			//Remove badge indicator - no unread chats
			ShortcutBadger.applyCount(getApplicationContext(), 0);
			//Xiaomi support
			startService(new Intent(getApplicationContext(), BadgeIntentService.class).putExtra("badgeCount", 0));
		} else {
			//Show badge with indicator = unread
			ShortcutBadger.applyCount(getApplicationContext(), Math.abs(totalNotifications));
			//Xiaomi support
			startService(new Intent(getApplicationContext(), BadgeIntentService.class).putExtra("badgeCount", totalNotifications));
		}
	}

	@Override
	public void onChatNotification(MegaChatApiJava api, long chatid, MegaChatMessage msg) {
		log("onChatNotification");

		updateAppBadge();

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
		log("onChatCallUpdate: callId "+call+", call.getStatus " + call.getStatus());
		stopService(new Intent(this, IncomingCallService.class));

		if (call.getStatus() >= MegaChatCall.CALL_STATUS_IN_PROGRESS) {
			clearIncomingCallNotification(call.getId());
		}

		if (call.getStatus() == MegaChatCall.CALL_STATUS_JOINING || call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS || call.getStatus() == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION || call.getStatus() == MegaChatCall.CALL_STATUS_DESTROYED) {
			stopAudioSignals();
		}


		if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {

			int callStatus = call.getStatus();
			switch (callStatus) {
				case MegaChatCall.CALL_STATUS_REQUEST_SENT:
				case MegaChatCall.CALL_STATUS_RING_IN:
				case MegaChatCall.CALL_STATUS_JOINING:
				case MegaChatCall.CALL_STATUS_IN_PROGRESS: {
					audioManagerStatus(call);

					if (megaChatApi != null) {
						MegaHandleList listAllCalls = megaChatApi.getChatCalls();
						if (listAllCalls != null) {
							if (listAllCalls.size() == 1) {
								log("onChatCallUpdate:One call");
								long chatId = listAllCalls.get(0);

								if ( openCallChatId != chatId) {
									MegaChatCall callToLaunch = megaChatApi.getChatCall(chatId);
									if (callToLaunch != null) {
										if (callToLaunch.getStatus() <= MegaChatCall.CALL_STATUS_IN_PROGRESS) {
											log("onChatCallUpdate:One call: open call");
											launchCallActivity(callToLaunch);
										} else {
											log("Launch not in correct status");
										}
									}
								} else {
									log("onChatCallUpdate:One call: call already opened");
								}

							} else if (listAllCalls.size() > 1) {
								log("onChatCallUpdate:Several calls = " + listAllCalls.size());

								if (call.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
									log("onChatCallUpdate:Several calls - REQUEST_SENT");
									MegaHandleList handleListRequestSent = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_REQUEST_SENT);
									if ((handleListRequestSent != null) && (handleListRequestSent.size() > 0)) {
										for (int i = 0; i < handleListRequestSent.size(); i++) {
											if (openCallChatId != handleListRequestSent.get(i)) {
												MegaChatCall callToLaunch = megaChatApi.getChatCall(handleListRequestSent.get(i));
												if (callToLaunch != null) {
													log("onChatCallUpdate:Several calls - REQUEST_SENT: open call");
													launchCallActivity(callToLaunch);
													break;
												}
											} else {
												log("onChatCallUpdate:Several calls - REQUEST_SENT: call already opened");
											}
										}
									}
								} else if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
									log("onChatCallUpdate:Several calls - RING_IN");
									if ((megaChatApi != null) && (mega.privacy.android.app.utils.ChatUtil.participatingInACall(megaChatApi))) {
										log("onChatCallUpdate:Several calls - RING_IN: show notification");
										checkQueuedCalls();
									} else {
										log("onChatCallUpdate:Several calls - RING_IN: NOT participating in a call");
										MegaHandleList handleListRingIn = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RING_IN);
										if ((handleListRingIn != null) && (handleListRingIn.size() > 0)) {
											for (int i = 0; i < handleListRingIn.size(); i++) {
												if (openCallChatId != handleListRingIn.get(i)) {
													MegaChatCall callToLaunch = megaChatApi.getChatCall(handleListRingIn.get(i));
													if (callToLaunch != null) {
														log("onChatCallUpdate:Several calls - RING_IN: open call");
														launchCallActivity(callToLaunch);
														break;
													}
												} else {
													log("onChatCallUpdate:Several calls - RING_IN: call already opened");
												}
											}
										}
									}
								} else if (call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
									log("onChatCallUpdate:Several calls - IN_PROGRESS");

									MegaHandleList handleListInProg = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS);
									if ((handleListInProg != null) && (handleListInProg.size() > 0)) {
										for (int i = 0; i < handleListInProg.size(); i++) {
											if (openCallChatId != handleListInProg.get(i)) {
												MegaChatCall callToLaunch = megaChatApi.getChatCall(handleListInProg.get(i));
												if (callToLaunch != null) {
													log("onChatCallUpdate:Several calls - IN_PROGRESS: open call");
													launchCallActivity(callToLaunch);
													break;
												}
											} else {
												log("onChatCallUpdate:Several calls - IN_PROGRESS: call already opened");
											}
										}
									}
								} else {
									log("onChatCallUpdate:Several calls: show notification");
									checkQueuedCalls();
								}

							} else {
								log("No calls in progress");
							}
						}
					}
					break;
				}

				case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION: {
					log("onChatCallUpdate:STATUS: CALL_STATUS_TERMINATING_USER_PARTICIPATION");
					hashMapSpeaker.remove(call.getChatid());
					break;
				}

				case MegaChatCall.CALL_STATUS_DESTROYED: {
					log("onChatCallUpdate:STATUS: DESTROYED");
					hashMapSpeaker.remove(call.getChatid());

					//Show missed call if time out ringing (for incoming calls)
					try {
						if (((call.getTermCode() == MegaChatCall.TERM_CODE_ANSWER_TIMEOUT || call.getTermCode() == MegaChatCall.TERM_CODE_CALL_REQ_CANCEL) && !(call.isIgnored()))) {
							log("onChatCallUpdate:TERM_CODE_ANSWER_TIMEOUT");
							if (call.isLocalTermCode() == false) {
								log("onChatCallUpdate:localTermCodeNotLocal");
								try {
									ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);
									notificationBuilder.showMissedCallNotification(call);
								} catch (Exception e) {
									log("EXCEPTION when showing missed call notification: " + e.getMessage());
								}
							}
						}
					} catch (Exception e) {
						log("EXCEPTION when showing missed call notification: " + e.getMessage());
					}

					//Register a call from Mega in the phone
//					MegaChatRoom chatRoom = megaChatApi.getChatRoom(call.getChatid());
//					if(chatRoom.isGroup()){
//						//Group call ended
//					}else{
//						//Individual call ended
//						try {
//							if (call.getTermCode() == MegaChatCall.TERM_CODE_ANSWER_TIMEOUT) {
//								//Unanswered call
//								if (call.isOutgoing()) {
//									try {
//										//I'm calling and the contact doesn't answer
//										ContentValues values = new ContentValues();
//										values.put(CallLog.Calls.NUMBER, chatRoom.getPeerFullname(0));
//										values.put(CallLog.Calls.DATE, System.currentTimeMillis());
//										values.put(CallLog.Calls.DURATION, 0);
//										values.put(CallLog.Calls.TYPE, CallLog.Calls.OUTGOING_TYPE);
//										values.put(CallLog.Calls.NEW, 1);
//
//										if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//											return;
//										}
//										this.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
//									} catch (Exception e) {
//										log("EXCEPTION:TERM_CODE_ANSWER_TIMEOUT:call.isOutgoing " + e.getMessage());
//									}
//								}else if(call.isIncoming()){
//									try {
//										//I'm receiving a call and I don't answer
//										ContentValues values = new ContentValues();
//										values.put(CallLog.Calls.NUMBER, chatRoom.getPeerFullname(0));
//										values.put(CallLog.Calls.DATE, System.currentTimeMillis());
//										values.put(CallLog.Calls.DURATION, 0);
//										values.put(CallLog.Calls.TYPE, CallLog.Calls.MISSED_TYPE);
//										values.put(CallLog.Calls.NEW, 1);
//
//										if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//											return;
//										}
//										this.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
//									} catch (Exception e) {
//										log("EXCEPTION:TERM_CODE_ANSWER_TIMEOUT:call.isIncoming " + e.getMessage());
//									}
//								}
//							}else if (call.getTermCode() == MegaChatCall.TERM_CODE_CALL_REJECT) {
//								//Rejected call
//								if (call.isOutgoing()) {
//									try {
//										//I'm calling and the user rejects the call
//										ContentValues values = new ContentValues();
//										values.put(CallLog.Calls.NUMBER, chatRoom.getPeerFullname(0));
//		                                values.put(CallLog.Calls.DATE, System.currentTimeMillis());
//										values.put(CallLog.Calls.DURATION, 0);
//										values.put(CallLog.Calls.TYPE, CallLog.Calls.OUTGOING_TYPE);
//										values.put(CallLog.Calls.NEW, 1);
//
//										if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//											return;
//										}
//										this.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
//									} catch (Exception e) {
//										log("EXCEPTION:TERM_CODE_CALL_REJECT:call.isOutgoing " + e.getMessage());
//									}
//								}else if(call.isIncoming()){
//									try {
//										//I'm receiving a call and I reject it
//										ContentValues values = new ContentValues();
//										values.put(CallLog.Calls.NUMBER, chatRoom.getPeerFullname(0));
//										values.put(CallLog.Calls.DATE, System.currentTimeMillis());
//										values.put(CallLog.Calls.DURATION, 0);
//										values.put(CallLog.Calls.TYPE, CallLog.Calls.REJECTED_TYPE);
//										values.put(CallLog.Calls.NEW, 1);
//
//										if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//											return;
//										}
//										this.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
//									} catch (Exception e) {
//										log("EXCEPTION:TERM_CODE_CALL_REJECT:call.isIncoming " + e.getMessage());
//									}
//								}
//							}else if (call.getTermCode() == MegaChatCall.TERM_CODE_USER_HANGUP) {
//								//Call answered and hung
//								if (call.isOutgoing()) {
//									try {
//										//I'm calling and the user answers it
//										ContentValues values = new ContentValues();
//										values.put(CallLog.Calls.NUMBER, chatRoom.getPeerFullname(0));
//										values.put(CallLog.Calls.DATE, System.currentTimeMillis());
//										values.put(CallLog.Calls.DURATION, call.getDuration());
//										values.put(CallLog.Calls.TYPE, CallLog.Calls.OUTGOING_TYPE);
//										values.put(CallLog.Calls.NEW, 1);
//
//										if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//											return;
//										}
//										this.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
//									} catch (Exception e) {
//										log("EXCEPTION:TERM_CODE_USER_HANGUP:call.isOutgoing " + e.getMessage());
//									}
//								}else if(call.isIncoming()){
//									try {
//										//I'm receiving a call and I answer it
//										ContentValues values = new ContentValues();
//		                                values.put(CallLog.Calls.NUMBER, chatRoom.getPeerFullname(0));
//										values.put(CallLog.Calls.DATE, System.currentTimeMillis());
//										values.put(CallLog.Calls.DURATION, call.getDuration());
//										values.put(CallLog.Calls.TYPE, CallLog.Calls.INCOMING_TYPE);
//										values.put(CallLog.Calls.NEW, 1);
//
//										if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//											return;
//										}
//										this.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
//									} catch (Exception e) {
//										log("EXCEPTION:TERM_CODE_USER_HANGUP:call.isIncoming " + e.getMessage());
//									}
//								}
//							}
//						} catch (Exception e) {
//							log("EXCEPTION:register call on device " + e.getMessage());
//						}
//					}

					break;
				}

				default:
					break;
			}
		}
	}

	public void audioManagerStatus(MegaChatCall call) {
		int callStatus = call.getStatus();
		if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
			log("audioManagerStatus:REQUEST_SENT");
			outgoingCallSound();
			setAudioManagerValues(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_VIBRATE);
		} else if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
			log("audioManagerStatus:RING_IN");
			updateRingingStatus();
		}
	}

	private void outgoingCallSound() {
		if (thePlayer != null && thePlayer.isPlaying()) return;
		log("outgoingCallSound");
		checkAudioManager();
		if (audioManager == null) return;
		if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT || audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE || audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, INITIAL_SOUND_LEVEL, 0);
		} else {
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), 0);
		}

		thePlayer = MediaPlayer.create(getApplicationContext(), R.raw.outgoing_voice_video_call);
		thePlayer.setLooping(true);
		thePlayer.start();
	}

	public void checkAudioManager() {
		if (audioManager != null) return;
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	}

	public void setAudioManagerValues(int streamType, int direction, int flags) {
		log("setAudioManagerValues");
		checkAudioManager();
		if (audioManager == null) return;
		audioManager.adjustStreamVolume(streamType, direction, flags);
		if (streamType == AudioManager.STREAM_RING) {
			updateRingingStatus();
		}
	}

	private void updateRingingStatus() {
		log("updateRingingStatus");
		checkAudioManager();
		if (audioManager == null) return;

		incomingCallSound();
		if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
			stopIncomingCallVibration();
			return;
		}
		if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
			startIncomingCallVibration();
			return;
		}
		if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
			return;
		}
		startIncomingCallVibration();
	}


	private void incomingCallSound() {
		log("incomingCallSound");

		stopRingtone();
		ringtone = RingtoneManager.getRingtone(this, DEFAULT_RINGTONE_URI);

		cancelRingerTimer();
		ringerTimer = new Timer();

		MyRingerTask myRingerTask = new MyRingerTask();
		ringerTimer.schedule(myRingerTask, 0, 500);
	}

	private void stopIncomingCallVibration() {
		if (vibrator == null || !vibrator.hasVibrator()) return;
		log("stopIncomingCallVibration");
		cancelVibrator();
	}

	private void startIncomingCallVibration() {
		if (vibrator != null) return;
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		if (vibrator == null || !vibrator.hasVibrator()) return;
		log("startIncomingCallVibration");
		long[] pattern = {0, 1000, 500, 500, 1000};
		vibrator.vibrate(pattern, 0);
	}

	private void stopAudioSignals() {
		log("stopAudioSignals");
		stopThePlayer();
		stopRingtone();
		cancelRingerTimer();
		cancelVibrator();
	}

	private void stopThePlayer() {
		try {
			if (thePlayer != null) {
				thePlayer.stop();
				thePlayer.reset();
				thePlayer.release();
				thePlayer = null;
			}
		} catch (Exception e) {
			log("Exception stopping player");
		}
	}

	private void stopRingtone() {
		try {
			if (ringtone != null) {
				ringtone.stop();
				ringtone = null;
			}
		} catch (Exception e) {
			log("Exception stopping ringtone");

		}
	}

	private void cancelRingerTimer() {
		try {
			if (ringerTimer != null) {
				ringerTimer.cancel();
				ringerTimer = null;
			}
		} catch (Exception e) {
			log("Exception canceling ringing time");

		}
	}

	private void cancelVibrator() {
		try {
			if (vibrator != null && vibrator.hasVibrator()) {
				vibrator.cancel();
				vibrator = null;
			}
		} catch (Exception e) {
			log("Exception canceling vibrator");
		}
	}

	private class MyRingerTask extends TimerTask {
		@Override
		public void run() {
			if (ringtone != null && !ringtone.isPlaying()) {
				ringtone.play();
			}
		}
	}

	public void checkQueuedCalls() {
		log("checkQueuedCalls");
		try {
			stopService(new Intent(this, IncomingCallService.class));
			ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);
			notificationBuilder.checkQueuedCalls();
		} catch (Exception e) {
			log("EXCEPTION: " + e.getMessage());
		}
	}

	public void launchCallActivity(MegaChatCall call) {
		log("launchCallActivity: " + call.getStatus());
		MegaApplication.setShowPinScreen(false);
//		MegaApplication.setOpenCallChatId(call.getChatid());
		Intent i = new Intent(this, ChatCallActivity.class);
		i.putExtra(Constants.CHAT_ID, call.getChatid());
		i.putExtra(Constants.CALL_ID, call.getId());
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);

		MegaChatRoom chatRoom = megaChatApi.getChatRoom(call.getChatid());
		log("Launch call: " + chatRoom.getTitle());
		if (call.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT || call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
			setCallLayoutStatus(call.getChatid(), true);
		}
	}

	public void clearIncomingCallNotification(long chatCallId) {
		log("clearIncomingCallNotification:chatID: " + chatCallId);

		try {
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			String notificationCallId = MegaApiJava.userHandleToBase64(chatCallId);
			int notificationId = (notificationCallId).hashCode();

			notificationManager.cancel(notificationId);
		} catch (Exception e) {
			log("clearIncomingCallNotification:EXCEPTION");
		}
	}

	public static boolean isShowRichLinkWarning() {
		return showRichLinkWarning;
	}

	public static void setShowRichLinkWarning(boolean showRichLinkWarning) {
		MegaApplication.showRichLinkWarning = showRichLinkWarning;
	}

	public static boolean isEnabledGeoLocation() {
		return enabledGeoLocation;
	}

	public static void setEnabledGeoLocation(boolean enabledGeoLocation) {
		MegaApplication.enabledGeoLocation = enabledGeoLocation;
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

	public static boolean getSpeakerStatus(long chatId) {
		boolean entryExists = hashMapSpeaker.containsKey(chatId);
		if (entryExists) {
			return hashMapSpeaker.get(chatId);
		}
		setSpeakerStatus(chatId, false);
		return false;
	}

	public static void setSpeakerStatus(long chatId, boolean speakerStatus) {
		hashMapSpeaker.put(chatId, speakerStatus);
	}

	public static boolean getCallLayoutStatus(long chatId) {
		boolean entryExists = hashMapCallLayout.containsKey(chatId);
		if (entryExists) {
			return hashMapCallLayout.get(chatId);
		}
		setCallLayoutStatus(chatId, false);
		return false;
	}

	public static void setCallLayoutStatus(long chatId, boolean callLayoutStatus) {
		hashMapCallLayout.put(chatId, callLayoutStatus);
	}

	public int getStorageState() {
	    return storageState;
	}

    public void setStorageState(int state) {
	    this.storageState = state;
	}

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        super.unregisterReceiver(receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logoutReceiver);
    }
}
