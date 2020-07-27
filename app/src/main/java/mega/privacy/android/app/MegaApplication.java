package mega.privacy.android.app;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.multidex.MultiDexApplication;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;
import androidx.emoji.bundled.BundledEmojiCompatConfig;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.provider.FontRequest;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import org.webrtc.ContextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import me.leolin.shortcutbadger.ShortcutBadger;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiManagerShortcodes;
import mega.privacy.android.app.components.twemoji.TwitterEmojiProvider;
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder;
import mega.privacy.android.app.fcm.IncomingCallService;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.listeners.GlobalListener;
import mega.privacy.android.app.listeners.CallListener;
import mega.privacy.android.app.fcm.KeepAliveService;
import mega.privacy.android.app.listeners.GlobalTransferListener;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.megachat.BadgeIntentService;
import mega.privacy.android.app.lollipop.megachat.calls.ChatAudioManager;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.receivers.NetworkStateReceiver;
import nz.mega.sdk.MegaAccountSession;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
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
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.IncomingCallNotification.*;
import static mega.privacy.android.app.utils.JobUtil.*;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static nz.mega.sdk.MegaApiJava.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class MegaApplication extends MultiDexApplication implements Application.ActivityLifecycleCallbacks, MegaChatRequestListenerInterface, MegaChatNotificationListenerInterface, NetworkStateReceiver.NetworkStateReceiverListener, MegaChatListenerInterface {

	final String TAG = "MegaApplication";

	static final public String USER_AGENT = "MEGAAndroid/3.7.7_318";

	DatabaseHandler dbH;
	MegaApiAndroid megaApi;
	MegaApiAndroid megaApiFolder;
	String localIpAddress = "";
	BackgroundRequestListener requestListener;
	final static public String APP_KEY = "6tioyn8ka5l6hty";
	final static private String APP_SECRET = "hfzgdtrma231qdm";

	MyAccountInfo myAccountInfo;
	boolean esid = false;

	private int storageState = MegaApiJava.STORAGE_STATE_UNKNOWN; //Default value

	// The current App Activity
	private Activity currentActivity = null;

	// Attributes to detect if app changes between background and foreground
	// Keep the count of number of Activities in the started state
	private int activityReferences = 0;
	// Flag to indicate if the current Activity is going through configuration change like orientation switch
	private boolean isActivityChangingConfigurations = false;

	private static boolean isLoggingIn = false;
	private static boolean firstConnect = true;

	private static final boolean USE_BUNDLED_EMOJI = false;

	private static boolean showInfoChatMessages = false;

	private static boolean showPinScreen = true;

	private static long openChatId = -1;

	private static boolean closedChat = true;
	private static HashMap<Long, Boolean> hashMapVideo = new HashMap<>();
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

	private static boolean isVerifySMSShowed = false;

    private static boolean isBlockedDueToWeakAccount = false;
	private static boolean isWebOpenDueToEmailVerification = false;
	private static boolean isLoggingRunning = false;

	MegaChatApiAndroid megaChatApi = null;

	private NetworkStateReceiver networkStateReceiver;
	private BroadcastReceiver logoutReceiver;
	private ChatAudioManager chatAudioManager = null;
    private AppRTCAudioManager rtcAudioManager = null;

    private static MegaApplication singleApplicationInstance;

	private PowerManager.WakeLock wakeLock;

	private CallListener callListener = new CallListener();

    @Override
	public void networkAvailable() {
		logDebug("Net available: Broadcast to ManagerActivity");
		Intent intent = new Intent(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE);
		intent.putExtra("actionType", GO_ONLINE);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
	}

	@Override
	public void networkUnavailable() {
		logDebug("Net unavailable: Broadcast to ManagerActivity");
		Intent intent = new Intent(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE);
		intent.putExtra("actionType", GO_OFFLINE);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
	}

	public static void smsVerifyShowed(boolean isShowed) {
	    isVerifySMSShowed = isShowed;
    }

	@Override
	public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

	}

	@Override
	public void onActivityStarted(@NonNull Activity activity) {
		currentActivity = activity;
    	if (++activityReferences == 1 && !isActivityChangingConfigurations) {
			logInfo("App enters foreground");
			if (storageState == STORAGE_STATE_PAYWALL) {
				showOverDiskQuotaPaywallWarning();
			}
		}
	}

	@Override
	public void onActivityResumed(@NonNull Activity activity) {
		if (!activity.equals(currentActivity)) {
			currentActivity = activity;
		}
	}

	@Override
	public void onActivityPaused(@NonNull Activity activity) {
    	if (activity.equals(currentActivity)) {
    		currentActivity = null;
		}
	}

	@Override
	public void onActivityStopped(@NonNull Activity activity) {
		isActivityChangingConfigurations = activity.isChangingConfigurations();
		if (--activityReferences == 0 && !isActivityChangingConfigurations) {
			logInfo("App enters background");
		}
	}

	@Override
	public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

	}

	@Override
	public void onActivityDestroyed(@NonNull Activity activity) {

	}

	class BackgroundRequestListener implements MegaRequestListenerInterface
	{

		@Override
		public void onRequestStart(MegaApiJava api, MegaRequest request) {
			logDebug("BackgroundRequestListener:onRequestStart: " + request.getRequestString());
		}

		@Override
		public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
			logDebug("BackgroundRequestListener:onRequestUpdate: " + request.getRequestString());
		}

		@Override
		public void onRequestFinish(MegaApiJava api, MegaRequest request,
				MegaError e) {
			logDebug("BackgroundRequestListener:onRequestFinish: " + request.getRequestString() + "____" + e.getErrorCode() + "___" + request.getParamType());

			if (e.getErrorCode() == MegaError.API_EPAYWALL) {
				showOverDiskQuotaPaywallWarning();
				return;
			}

			if (e.getErrorCode() == MegaError.API_EBUSINESSPASTDUE) {
				LocalBroadcastManager.getInstance(getApplicationContext())
						.sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED));
				return;
			}

			if (request.getType() == MegaRequest.TYPE_LOGOUT){
				logDebug("Logout finished: " + e.getErrorString() + "(" + e.getErrorCode() +")");
				if (e.getErrorCode() == MegaError.API_OK) {
					logDebug("END logout sdk request - wait chat logout");
				} else if (e.getErrorCode() == MegaError.API_EINCOMPLETE) {
					if (request.getParamType() == MegaError.API_ESSL) {
						logWarning("SSL verification failed");
						Intent intent = new Intent(BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED);
						LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
					}
				} else if (e.getErrorCode() == MegaError.API_ESID) {
					logWarning("TYPE_LOGOUT:API_ESID");
					myAccountInfo = new MyAccountInfo();

					esid = true;

					AccountController.localLogoutApp(getApplicationContext());
				} else if (e.getErrorCode() == MegaError.API_EBLOCKED) {
					api.localLogout();
					megaChatApi.logout();
				}
			}
			else if(request.getType() == MegaRequest.TYPE_FETCH_NODES){
				logDebug("TYPE_FETCH_NODES");
				if (e.getErrorCode() == MegaError.API_OK){
					askForFullAccountInfo();
					GetAttrUserListener listener = new GetAttrUserListener(getApplicationContext(), true);
					if (dbH != null && dbH.getMyChatFilesFolderHandle() == INVALID_HANDLE) {
						megaApi.getMyChatFilesFolder(listener);
					}
					//Ask for MU and CU folder when App in init state
                    megaApi.getUserAttribute(USER_ATTR_CAMERA_UPLOADS_FOLDER,listener);
				}
			}
			else if(request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
				if (e.getErrorCode() == MegaError.API_OK) {
					if (request.getParamType() == MegaApiJava.USER_ATTR_FIRSTNAME || request.getParamType() == MegaApiJava.USER_ATTR_LASTNAME) {
						if (megaApi != null && request.getEmail() != null) {
							MegaUser user = megaApi.getContact(request.getEmail());
							if (user != null) {
								logDebug("User handle: " + user.getHandle());
								logDebug("Visibility: " + user.getVisibility()); //If user visibity == MegaUser.VISIBILITY_UNKNOW then, non contact
								if (user.getVisibility() != MegaUser.VISIBILITY_VISIBLE) {
									logDebug("Non-contact");
									if (request.getParamType() == MegaApiJava.USER_ATTR_FIRSTNAME) {
										dbH.setNonContactEmail(request.getEmail(), user.getHandle() + "");
										dbH.setNonContactFirstName(request.getText(), user.getHandle() + "");
									} else if (request.getParamType() == MegaApiJava.USER_ATTR_LASTNAME) {
										dbH.setNonContactLastName(request.getText(), user.getHandle() + "");
									}
								} else {
									logDebug("The user is or was CONTACT:");
								}
							} else {
								logWarning("User is NULL");
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

					Intent intent = new Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
					intent.putExtra("actionType", UPDATE_GET_PRICING);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
				}
				else{
					logError("Error TYPE_GET_PRICING: " + e.getErrorCode());
				}
			}
			else if (request.getType() == MegaRequest.TYPE_GET_PAYMENT_METHODS){
				logDebug("Payment methods request");
				if(myAccountInfo!=null){
					myAccountInfo.setGetPaymentMethodsBoolean(true);
				}

				if (e.getErrorCode() == MegaError.API_OK){
					dbH.setPaymentMethodsTimeStamp();
					if(myAccountInfo!=null){
						myAccountInfo.setPaymentBitSet(convertToBitSet(request.getNumber()));
					}

					Intent intent = new Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
					intent.putExtra("actionType", UPDATE_PAYMENT_METHODS);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
				}
			}
			else if(request.getType() == MegaRequest.TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS){
				if (e.getErrorCode() == MegaError.API_OK){
					if(myAccountInfo!=null){
						myAccountInfo.setNumberOfSubscriptions(request.getNumber());
						logDebug("NUMBER OF SUBS: " + myAccountInfo.getNumberOfSubscriptions());
					}

					Intent intent = new Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
					intent.putExtra("actionType", UPDATE_CREDIT_CARD_SUBSCRIPTION);
					LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
				}
			}
			else if (request.getType() == MegaRequest.TYPE_ACCOUNT_DETAILS){
				logDebug ("Account details request");
				if (e.getErrorCode() == MegaError.API_OK){

					boolean storage = (request.getNumDetails() & myAccountInfo.hasStorageDetails) != 0;
					if (storage) {
						dbH.setAccountDetailsTimeStamp();
					}

					if(myAccountInfo!=null && request.getMegaAccountDetails()!=null){
						myAccountInfo.setAccountInfo(request.getMegaAccountDetails());
						myAccountInfo.setAccountDetails(request.getNumDetails());

						boolean sessions = (request.getNumDetails() & myAccountInfo.hasSessionsDetails) != 0;
						if (sessions) {
							MegaAccountSession megaAccountSession = request.getMegaAccountDetails().getSession(0);

							if(megaAccountSession!=null){
								logDebug("getMegaAccountSESSION not Null");
								dbH.setExtendedAccountDetailsTimestamp();
								long mostRecentSession = megaAccountSession.getMostRecentUsage();

								String date = formatDateAndTime(getApplicationContext(),mostRecentSession, DATE_LONG_FORMAT);

								myAccountInfo.setLastSessionFormattedDate(date);
								myAccountInfo.setCreateSessionTimeStamp(megaAccountSession.getCreationTimestamp());
							}
						}

						logDebug("onRequest TYPE_ACCOUNT_DETAILS: " + myAccountInfo.getUsedPerc());
					}

					sendBroadcastUpdateAccountDetails();
				}
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			logDebug("BackgroundRequestListener: onRequestTemporaryError: " + request.getRequestString());
		}
		
	}

	private void sendBroadcastUpdateAccountDetails() {
		Intent intent = new Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
		intent.putExtra("actionType", UPDATE_ACCOUNT_DETAILS);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
	}

	private final int interval = 3000;
	private Handler keepAliveHandler = new Handler();
	int backgroundStatus = -1;

	private Runnable keepAliveRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				if (isActivityVisible()) {
					logDebug("KEEPALIVE: " + System.currentTimeMillis());
					if (megaChatApi != null) {
						backgroundStatus = megaChatApi.getBackgroundStatus();
						logDebug("backgroundStatus_activityVisible: " + backgroundStatus);
						if (backgroundStatus != -1 && backgroundStatus != 0) {
							MegaHandleList callRingIn = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RING_IN);
							if (callRingIn == null || callRingIn.size() <= 0) {
								megaChatApi.setBackgroundStatus(false);
							}
						}
					}

				} else {
					logDebug("KEEPALIVEAWAY: " + System.currentTimeMillis());
					if (megaChatApi != null) {
						backgroundStatus = megaChatApi.getBackgroundStatus();
						logDebug("backgroundStatus_!activityVisible: " + backgroundStatus);
						if (backgroundStatus != -1 && backgroundStatus != 1) {
							megaChatApi.setBackgroundStatus(true);
						}
					}
				}

				keepAliveHandler.postAtTime(keepAliveRunnable, System.currentTimeMillis() + interval);
				keepAliveHandler.postDelayed(keepAliveRunnable, interval);
			}
			catch (Exception exc) {
				logError("Exception in keepAliveRunnable", exc);
			}
		}
	};

	public void handleUncaughtException(Thread thread, Throwable e) {
		logFatal("UNCAUGHT EXCEPTION", e);
		e.printStackTrace();
	}

	private BroadcastReceiver chatCallUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null || intent.getAction() == null)
				return;

			long chatId = intent.getLongExtra(UPDATE_CHAT_CALL_ID, MEGACHAT_INVALID_HANDLE);
			long callId = intent.getLongExtra(UPDATE_CALL_ID, MEGACHAT_INVALID_HANDLE);
			if (chatId == MEGACHAT_INVALID_HANDLE || callId == MEGACHAT_INVALID_HANDLE) {
				logError("Error. Chat id " + chatId + ", Call id "+callId);
				return;
			}

			if (intent.getAction().equals(ACTION_UPDATE_CALL)) {
				stopService(new Intent(getInstance(), IncomingCallService.class));
			}

			if (intent.getAction().equals(ACTION_CALL_STATUS_UPDATE)) {
				int callStatus = intent.getIntExtra(UPDATE_CALL_STATUS, -1);
				switch (callStatus) {
					case MegaChatCall.CALL_STATUS_REQUEST_SENT:
					case MegaChatCall.CALL_STATUS_RING_IN:
					case MegaChatCall.CALL_STATUS_JOINING:
					case MegaChatCall.CALL_STATUS_IN_PROGRESS:
					case MegaChatCall.CALL_STATUS_RECONNECTING:
						logDebug("Call status is "+callStatusToString(callStatus));
						MegaHandleList listAllCalls = megaChatApi.getChatCalls();
						if (listAllCalls == null || listAllCalls.size() == 0){
							logError("Calls not found");
							return;
						}

						if (callStatus == MegaChatCall.CALL_STATUS_RING_IN || callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                            createChatAudioManager();
							setAudioManagerValues(callStatus);
						}

						if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS
								|| callStatus == MegaChatCall.CALL_STATUS_JOINING
								|| callStatus == MegaChatCall.CALL_STATUS_RECONNECTING) {
							removeChatAudioManager();
							clearIncomingCallNotification(callId);
						}

						if (listAllCalls.size() == 1) {
							checkOneCall(listAllCalls.get(0));
						} else {
							checkSeveralCall(listAllCalls, callStatus);
						}
						break;
					case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
						removeValues(chatId);
						break;
					case MegaChatCall.CALL_STATUS_DESTROYED:
						int termCode = intent.getIntExtra(UPDATE_CALL_TERM_CODE, -1);
						boolean isIgnored = intent.getBooleanExtra(UPDATE_CALL_IGNORE, false);
						boolean isLocalTermCode = intent.getBooleanExtra(UPDATE_CALL_LOCAL_TERM_CODE, false);
						checkCallDestroyed(chatId, callId, termCode, isIgnored, isLocalTermCode);
						break;
				}
			}
		}
	};

	public static MegaApplication getInstance() {
		return singleApplicationInstance;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// Setup handler for uncaught exceptions.
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable e) {
				handleUncaughtException(thread, e);
			}
		});

		registerActivityLifecycleCallbacks(this);

		isVerifySMSShowed = false;
		singleApplicationInstance = this;

		keepAliveHandler.postAtTime(keepAliveRunnable, System.currentTimeMillis()+interval);
		keepAliveHandler.postDelayed(keepAliveRunnable, interval);
		dbH = DatabaseHandler.getDbHandler(getApplicationContext());

		initLoggerSDK();
		initLoggerKarere();

		checkAppUpgrade();

		megaApi = getMegaApi();
		megaApiFolder = getMegaApiFolder();
		megaChatApi = getMegaChatApi();
        scheduleCameraUploadJob(getApplicationContext());
        storageState = dbH.getStorageState();

		boolean staging = false;
		if (dbH != null) {
			MegaAttributes attrs = dbH.getAttributes();
			if (attrs != null && attrs.getStaging() != null) {
				staging = Boolean.parseBoolean(attrs.getStaging());
			}
		}

		if (staging) {
			megaApi.changeApiUrl("https://staging.api.mega.co.nz/");
			megaApiFolder.changeApiUrl("https://staging.api.mega.co.nz/");
		}

		boolean useHttpsOnly = false;
		if (dbH != null) {
			useHttpsOnly = Boolean.parseBoolean(dbH.getUseHttpsOnly());
			logDebug("Value of useHttpsOnly: " + useHttpsOnly);
			megaApi.useHttpsOnly(useHttpsOnly);
		}

		myAccountInfo = new MyAccountInfo();

		if (dbH != null) {
			dbH.resetExtendedAccountDetailsTimestamp();
		}

		networkStateReceiver = new NetworkStateReceiver();
		networkStateReceiver.addListener(this);
		this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

		IntentFilter filter = new IntentFilter(BROADCAST_ACTION_INTENT_CALL_UPDATE);
		filter.addAction(ACTION_CALL_STATUS_UPDATE);
		filter.addAction(ACTION_UPDATE_CALL);
		LocalBroadcastManager.getInstance(this).registerReceiver(chatCallUpdateReceiver, filter);

		logoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context,Intent intent) {
                if (intent != null) {
                    if (intent.getAction() == ACTION_LOG_OUT) {
                        storageState = MegaApiJava.STORAGE_STATE_UNKNOWN; //Default value
                    }
                }
            }
        };
		LocalBroadcastManager.getInstance(this).registerReceiver(logoutReceiver, new IntentFilter(ACTION_LOG_OUT));
		EmojiManager.install(new TwitterEmojiProvider());

		EmojiManagerShortcodes.initEmojiData(getApplicationContext());
		EmojiManager.install(new TwitterEmojiProvider());
		final EmojiCompat.Config config;
		if (USE_BUNDLED_EMOJI) {
			logDebug("Use Bundle emoji");
			// Use the bundled font for EmojiCompat
			config = new BundledEmojiCompatConfig(getApplicationContext());
		} else {
			logDebug("Use downloadable font for EmojiCompat");
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
						public void onInitialized() {
							logDebug("EmojiCompat initialized");
						}
						@Override
						public void onFailed(@Nullable Throwable throwable) {
							logWarning("EmojiCompat initialization failed");
						}
					});
		}
		EmojiCompat.init(config);
		// clear the cache files stored in the external cache folder.
        clearPublicCache(this);

		ContextUtils.initialize(getApplicationContext());
	}


	public void askForFullAccountInfo(){
		logDebug("askForFullAccountInfo");
		megaApi.getPaymentMethods(null);

		if (storageState == MegaApiAndroid.STORAGE_STATE_UNKNOWN) {
			megaApi.getAccountDetails();
		} else {
			megaApi.getSpecificAccountDetails(false, true, true);
		}

		megaApi.getPricing(null);
		megaApi.creditCardQuerySubscriptions(null);
	}

	public void askForPaymentMethods(){
		logDebug("askForPaymentMethods");
		megaApi.getPaymentMethods(null);
	}

	public void askForPricing(){

		megaApi.getPricing(null);
	}

	public void askForAccountDetails(){
		logDebug("askForAccountDetails");
		if (dbH != null) {
			dbH.resetAccountDetailsTimeStamp();
		}
		megaApi.getAccountDetails(null);
	}

	public void askForCCSubscriptions(){

		megaApi.creditCardQuerySubscriptions(null);
	}

	public void askForExtendedAccountDetails(){
		logDebug("askForExtendedAccountDetails");
		if (dbH != null) {
			dbH.resetExtendedAccountDetailsTimestamp();
		}
		megaApi.getExtendedAccountDetails(true,false, false, null);
	}

	public void refreshAccountInfo(){
		//Check if the call is recently
		if(callToAccountDetails() || myAccountInfo.getUsedFormatted().trim().length() <= 0) {
			logDebug("megaApi.getAccountDetails SEND");
			askForAccountDetails();
		}

		if(callToExtendedAccountDetails()){
			logDebug("megaApi.getExtendedAccountDetails SEND");
			askForExtendedAccountDetails();
		}

		if(callToPaymentMethods()){
			logDebug("megaApi.getPaymentMethods SEND");
			askForPaymentMethods();
		}
	}
	
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
				logDebug("Add listeners of megaChatApi");
				megaChatApi.addChatRequestListener(this);
				megaChatApi.addChatNotificationListener(this);
				megaChatApi.addChatListener(this);
				megaChatApi.addChatCallListener(callListener);
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
				megaChatApi.removeChatListener(this);
				megaChatApi.removeChatCallListener(callListener);
				registeredChatListeners = false;
			}
		}
		catch (Exception e){}
	}

	public MegaApiAndroid getMegaApi()
	{
		if(megaApi == null)
		{
			logDebug("MEGAAPI = null");
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
			logDebug("ADD REQUESTLISTENER");
			megaApi.addRequestListener(requestListener);
			megaApi.addTransferListener(new GlobalTransferListener());

			megaApi.addGlobalListener(new GlobalListener());
			megaChatApi = getMegaChatApi();

			String language = Locale.getDefault().toString();
			boolean languageString = megaApi.setLanguage(language);
			logDebug("Result: " + languageString + " Language: " + language);
			if(languageString==false){
				language = Locale.getDefault().getLanguage();
				languageString = megaApi.setLanguage(language);
				logDebug("Result: " + languageString + " Language: " + language);
			}
		}
		
		return megaApi;
	}

	public DatabaseHandler getDbH() {
		if (dbH == null) {
			DatabaseHandler.getDbHandler(getApplicationContext());
		}

		return dbH;
	}

	public boolean isActivityVisible() {
		logDebug("Activity visible? => " + (currentActivity != null));
		return getCurrentActivity() != null;
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
        logDebug("New open call chat ID: " + value);
        openCallChatId = value;
	}

	public boolean isRecentChatVisible() {
		if(isActivityVisible()){
			return recentChatVisible;
		}
		else{
			return false;
		}
	}

	public static void setRecentChatVisible(boolean recentChatVisible) {
		logDebug("setRecentChatVisible: " + recentChatVisible);
		MegaApplication.recentChatVisible = recentChatVisible;
	}

	public static boolean isChatNotificationReceived() {
		return chatNotificationReceived;
	}

    public static void setChatNotificationReceived(boolean chatNotificationReceived) {
        MegaApplication.chatNotificationReceived = chatNotificationReceived;
    }

	public static long getOpenChatId() {
		return openChatId;
	}

	public String getLocalIpAddress(){
		return localIpAddress;
	}
	
	public void setLocalIpAddress(String ip){
		localIpAddress = ip;
	}

	public void showSharedFolderNotification(MegaNode n) {
		logDebug("showSharedFolderNotification");

		try {
			ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
			String name = "";
			for (int j = 0; j < sharesIncoming.size(); j++) {
				MegaShare mS = sharesIncoming.get(j);
				if (mS.getNodeHandle() == n.getHandle()) {
					MegaUser user = megaApi.getContact(mS.getUser());

					name = getMegaUserNameDB(user);
					if(name == null) name = "";
				}
			}

			String source = "<b>" + n.getName() + "</b> " + getString(R.string.incoming_folder_notification) + " " + toCDATA(name);
			Spanned notificationContent;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
				notificationContent = Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
			} else {
				notificationContent = Html.fromHtml(source);
			}

			int notificationId = NOTIFICATION_PUSH_CLOUD_DRIVE;
			String notificationChannelId = NOTIFICATION_CHANNEL_CLOUDDRIVE_ID;
			String notificationChannelName = NOTIFICATION_CHANNEL_CLOUDDRIVE_NAME;

			Intent intent = new Intent(this, ManagerActivityLollipop.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.setAction(ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
					PendingIntent.FLAG_ONE_SHOT);

			Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            String notificationTitle;
            if(n.hasChanged(MegaNode.CHANGE_TYPE_INSHARE) && !n.hasChanged(MegaNode.CHANGE_TYPE_NEW)){
                notificationTitle = getString(R.string.context_permissions_changed);
            }else{
                notificationTitle = getString(R.string.title_incoming_folder_notification);
            }
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_HIGH);
				channel.setShowBadge(true);
				NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManager.createNotificationChannel(channel);

				NotificationCompat.Builder notificationBuilderO = new NotificationCompat.Builder(this, notificationChannelId);
				notificationBuilderO
						.setSmallIcon(R.drawable.ic_stat_notify)
						.setContentTitle(notificationTitle)
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
						.setContentTitle(notificationTitle)
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
			logError("Exception", e);
		}
	}

	public void sendSignalPresenceActivity(){
		logDebug("sendSignalPresenceActivity");
		if (megaChatApi != null) {
			if (megaChatApi.isSignalActivityRequired()) {
				megaChatApi.signalPresenceActivity();
			}
		}
	}

	@Override
	public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
		logDebug("onRequestStart (CHAT): " + request.getRequestString());
	}

	@Override
	public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {
	}

	@Override
	public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		logDebug("onRequestFinish (CHAT): " + request.getRequestString() + "_"+e.getErrorCode());
		if (request.getType() == MegaChatRequest.TYPE_SET_BACKGROUND_STATUS){
			logDebug("SET_BACKGROUND_STATUS: " + request.getFlag());
		}
		else if (request.getType() == MegaChatRequest.TYPE_LOGOUT) {
			logDebug("CHAT_TYPE_LOGOUT: " + e.getErrorCode() + "__" + e.getErrorString());

			try{
				if (megaChatApi != null){
					megaChatApi.removeChatRequestListener(this);
					megaChatApi.removeChatNotificationListener(this);
					megaChatApi.removeChatListener(this);
					megaChatApi.removeChatCallListener(callListener);
					registeredChatListeners = false;
				}
			}
			catch (Exception exc){}

			try{
				ShortcutBadger.applyCount(getApplicationContext(), 0);

				startService(new Intent(getApplicationContext(), BadgeIntentService.class).putExtra("badgeCount", 0));
			}
			catch (Exception exc){
				logError("EXCEPTION removing badge indicator", exc);
            }

			if(megaApi!=null){
				int loggedState = megaApi.isLoggedIn();
				logDebug("Login status on " + loggedState);
				if(loggedState==0){
					AccountController aC = new AccountController(this);
					aC.logoutConfirmed(this);

					if (isIsLoggingRunning()) {
						logDebug("Already in Login Activity, not necessary to launch it again");
						return;
					}

					Intent loginIntent = new Intent(this, LoginActivityLollipop.class);

					if (getUrlConfirmationLink() != null) {
						loginIntent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
						loginIntent.putExtra(EXTRA_CONFIRMATION, getUrlConfirmationLink());
						if (isActivityVisible()) {
							loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
						} else {
							loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
						}
						loginIntent.setAction(ACTION_CONFIRM);
						setUrlConfirmationLink(null);
					} else if (isActivityVisible()) {
						loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					} else {
						loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					}

					startActivity(loginIntent);
				}
				else{
					logDebug("Disable chat finish logout");
				}
			}
			else{

				AccountController aC = new AccountController(this);
				aC.logoutConfirmed(this);

				if(isActivityVisible()){
					logDebug("Launch intent to login screen");
					Intent tourIntent = new Intent(this, LoginActivityLollipop.class);
					tourIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					this.startActivity(tourIntent);
				}
			}
		}
		else if (request.getType() == MegaChatRequest.TYPE_PUSH_RECEIVED) {
			logDebug("TYPE_PUSH_RECEIVED: " + e.getErrorCode() + "__" + e.getErrorString());
			stopService(new Intent(this, KeepAliveService.class));
			if(e.getErrorCode()==MegaChatError.ERROR_OK){
				logDebug("OK:TYPE_PUSH_RECEIVED");
				chatNotificationReceived = true;
				ChatAdvancedNotificationBuilder notificationBuilder;
				notificationBuilder =  ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);
				notificationBuilder.generateChatNotification(request);
			}
			else{
				logError("Error TYPE_PUSH_RECEIVED: " + e.getErrorString());
			}
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		logWarning("onRequestTemporaryError (CHAT): "+e.getErrorString());
	}

	public void updateBusinessStatus() {
		myAccountInfo.setBusinessStatusReceived(true);
		int status = megaApi.getBusinessStatus();
		if (status == BUSINESS_STATUS_EXPIRED
				|| (megaApi.isMasterBusinessAccount() && status == BUSINESS_STATUS_GRACE_PERIOD)){
			myAccountInfo.setShouldShowBusinessAlert(true);
		}
		sendBroadcastUpdateAccountDetails();
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
			logDebug("Launch local broadcast");
			Intent intent = new Intent(BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE);
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
		logDebug("updateAppBadge");

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
		if (megaChatApi != null) {
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
		logDebug("onChatNotification");

		updateAppBadge();

		if(MegaApplication.getOpenChatId() == chatid){
			logDebug("Do not update/show notification - opened chat");
			return;
		}

		if(isRecentChatVisible()){
			logDebug("Do not show notification - recent chats shown");
			return;
		}

		if(isActivityVisible()){

			try{
				if(msg!=null){

					NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
					mNotificationManager.cancel(NOTIFICATION_GENERAL_PUSH_CHAT);

					if(msg.getStatus()==MegaChatMessage.STATUS_NOT_SEEN){
						if(msg.getType()==MegaChatMessage.TYPE_NORMAL||msg.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT||msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT||msg.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT){
							if(msg.isDeleted()){
								logDebug("Message deleted");

								megaChatApi.pushReceived(false);
							}
							else if(msg.isEdited()){
								logDebug("Message edited");
								megaChatApi.pushReceived(false);
							}
							else{
								logDebug("New normal message");
								megaChatApi.pushReceived(true);
							}
						}
						else if(msg.getType()==MegaChatMessage.TYPE_TRUNCATE){
							logDebug("New TRUNCATE message");
							megaChatApi.pushReceived(false);
						}
					}
					else{
						logDebug("Message SEEN");
						megaChatApi.pushReceived(false);
					}
				}
			}
			catch (Exception e){
				logError("EXCEPTION when showing chat notification", e);
			}
		}
		else{
			logDebug("Do not notify chat messages: app in background");
		}
	}

	private void checkOneCall(long chatId) {
		logDebug("One call : Chat Id = " + chatId + ", openCall Chat Id = " + openCallChatId);
		if (openCallChatId == chatId) {
			logDebug("The call is already opened");
			return;
		}
		MegaChatCall callToLaunch = megaChatApi.getChatCall(chatId);
		if (callToLaunch == null || callToLaunch.getStatus() > MegaChatCall.CALL_STATUS_IN_PROGRESS){
			logWarning("Launch not in correct status");
			return;
		}
		logDebug("Open the call");
		if (shouldNotify(this) && !isActivityVisible()) {
			PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
			if (pm != null) {
				wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ":MegaIncomingCallPowerLock");
			}
			if (!wakeLock.isHeld()) {
				wakeLock.acquire(10 * 1000);
			}
			toIncomingCall(this, callToLaunch, megaChatApi);
		} else {
			launchCallActivity(callToLaunch);
		}
	}

	private void checkSeveralCall(MegaHandleList listAllCalls, int callStatus) {
		logDebug("Several calls = " + listAllCalls.size() + "- Current call Status: " + callStatusToString(callStatus));
		if (callStatus == MegaChatCall.CALL_STATUS_RECONNECTING || (callStatus == MegaChatCall.CALL_STATUS_RING_IN && participatingInACall())) {
			logDebug("Several calls: show notification");
			checkQueuedCalls();
			return;
		}

		MegaHandleList handleList = megaChatApi.getChatCalls(callStatus);
		if (handleList == null || handleList.size() == 0) return;

		for (int i = 0; i < handleList.size(); i++) {
			if (openCallChatId != handleList.get(i)) {
				MegaChatCall callToLaunch = megaChatApi.getChatCall(handleList.get(i));
				if (callToLaunch != null) {
					logDebug("Open call");
					launchCallActivity(callToLaunch);
					break;
				}
			} else {
				logDebug("The call is already opened");
			}
		}
	}

	private void removeValues(long chatId) {
		removeStatusVideoAndSpeaker(chatId);
		removeChatAudioManager();
        removeRTCAudioManager();
	}

	private void checkCallDestroyed(long chatId, long callId, int termCode, boolean isIgnored, boolean isLocalTermCode) {
		removeValues(chatId);

		if (shouldNotify(this)) {
			toSystemSettingNotification(this);
		}
		cancelIncomingCallNotification(this);
		if (wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
		}

		clearIncomingCallNotification(callId);
		//Show missed call if time out ringing (for incoming calls)
		try {

			if ((termCode == MegaChatCall.TERM_CODE_ANSWER_TIMEOUT || termCode == MegaChatCall.TERM_CODE_CALL_REQ_CANCEL) && !isIgnored) {
				logDebug("TERM_CODE_ANSWER_TIMEOUT");
				if (!isLocalTermCode) {
					logDebug("localTermCodeNotLocal");
					try {
						ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);
						notificationBuilder.showMissedCallNotification(chatId, callId);
					} catch (Exception e) {
						logError("EXCEPTION when showing missed call notification", e);
					}
				}
			}
		} catch (Exception e) {
			logError("EXCEPTION when showing missed call notification", e);
		}
	}

	private void removeStatusVideoAndSpeaker(long chatId){
		hashMapSpeaker.remove(chatId);
		hashMapVideo.remove(chatId);
	}

    /**
     * Create or update the AppRTCAudioManager for the in progress call.
     *
     * @param isSpeakerOn the speaker status.
     */
    public void createRTCAudioManager(boolean isSpeakerOn) {
        if (rtcAudioManager != null) {
            logDebug("Updating RTC Audio Manager values...");
            rtcAudioManager.updateSpeakerStatus(isSpeakerOn);
            return;
        }

        logDebug("Creating RTC Audio Manager...");
        rtcAudioManager = AppRTCAudioManager.create(this, isSpeakerOn);
        startProximitySensor();
        rtcAudioManager.setOnProximitySensorListener(isNear -> {
            Intent intent = new Intent(BROADCAST_ACTION_INTENT_PROXIMITY_SENSOR);
            intent.putExtra(UPDATE_PROXIMITY_SENSOR_STATUS, isNear);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        });
    }

    /**
     * Remove the AppRTCAudioManager.
     */
    public void removeRTCAudioManager() {
        if (rtcAudioManager == null)
            return;

        try {
            logDebug("Removing RTC Audio Manager...");
            unregisterProximitySensor();
            rtcAudioManager.stop();
            rtcAudioManager = null;
        } catch (Exception e) {
            logError("Exception stopping speaker audio manager", e);
        }
    }

    /**
     * Activate the proximity sensor.
     */
    public void startProximitySensor() {
        if (rtcAudioManager != null) {
            logDebug("Starting proximity sensor...");
            rtcAudioManager.startProximitySensor();
        }
    }

    /**
     * Deactivates the proximity sensor
     */
    public void unregisterProximitySensor() {
        if (rtcAudioManager != null) {
            logDebug("Stopping proximity sensor...");
            rtcAudioManager.unregisterProximitySensor();
        }
    }

    /**
     * Create the ChatAudioManager for the incoming and outgoing call.
     */
	public void createChatAudioManager() {
		if (chatAudioManager != null)
			return;

        logDebug("Creating Chat Audio Manager...");
		chatAudioManager = ChatAudioManager.create(getApplicationContext());
	}

    /**
     * Remove the ChatAudioManager.
     */
	public void removeChatAudioManager() {
		if (chatAudioManager == null) {
			return;
		}

        logDebug("Removing Chat Audio Manager...");
		chatAudioManager.stopAudioSignals();
		chatAudioManager = null;
	}

    /**
     * Update the values of the ChatAudioManager depending on the call status.
     *
     * @param callStatus The current call status.
     */
	public void setAudioManagerValues(int callStatus){
        if (chatAudioManager != null) {
			MegaHandleList listCallsRequest = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_REQUEST_SENT);
			MegaHandleList listCallsRing = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_RING_IN);
			chatAudioManager.setAudioManagerValues(callStatus, listCallsRequest, listCallsRing);
		}
	}

	public void checkQueuedCalls() {
		logDebug("checkQueuedCalls");
		try {
			stopService(new Intent(this, IncomingCallService.class));
			ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);
			notificationBuilder.checkQueuedCalls();
		} catch (Exception e) {
			logError("EXCEPTION", e);
		}
	}

	public void launchCallActivity(MegaChatCall call) {
		logDebug("Show the call " + callStatusToString(call.getStatus()) + " screen.");
		MegaApplication.setShowPinScreen(false);
		Intent i = new Intent(this, ChatCallActivity.class);
		i.putExtra(CHAT_ID, call.getChatid());
		i.putExtra(CALL_ID, call.getId());
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);

		MegaChatRoom chatRoom = megaChatApi.getChatRoom(call.getChatid());
		logDebug("Launch call: " + getTitleChat(chatRoom));
		if (call.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT || call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
			setCallLayoutStatus(call.getChatid(), true);
		}
	}

	public void clearIncomingCallNotification(long chatCallId) {
		logDebug("Chat ID: " + chatCallId);

		try {
			NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			String notificationCallId = MegaApiJava.userHandleToBase64(chatCallId);
			int notificationId = (notificationCallId).hashCode();

			notificationManager.cancel(notificationId);
		} catch (Exception e) {
			logError("EXCEPTION", e);
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

	public static boolean getVideoStatus(long chatId) {
		boolean entryExists = hashMapVideo.containsKey(chatId);
		if (entryExists) {
			return hashMapVideo.get(chatId);
		}
		setVideoStatus(chatId, false);
		return false;
	}

	public static void setVideoStatus(long chatId, boolean videoStatus) {
		hashMapVideo.put(chatId, videoStatus);
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

    public static boolean isVerifySMSShowed() {
		return isVerifySMSShowed;
	}

	public static void setIsBlockedDueToWeakAccount(boolean isBlockedDueToWeakAccount) {
		MegaApplication.isBlockedDueToWeakAccount = isBlockedDueToWeakAccount;
	}

	public static boolean isBlockedDueToWeakAccount() {
		return isBlockedDueToWeakAccount;
	}

	public static void setIsWebOpenDueToEmailVerification(boolean isWebOpenDueToEmailVerification) {
		MegaApplication.isWebOpenDueToEmailVerification = isWebOpenDueToEmailVerification;
	}

	public static boolean isWebOpenDueToEmailVerification() {
		return isWebOpenDueToEmailVerification;
	}

	public void setIsLoggingRunning (boolean isLoggingRunning) {
		MegaApplication.isLoggingRunning = isLoggingRunning;
	}

	public boolean isIsLoggingRunning() {
		return isLoggingRunning;
	}

	public Activity getCurrentActivity() {
		return currentActivity;
	}
}
