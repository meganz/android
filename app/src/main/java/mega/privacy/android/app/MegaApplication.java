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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.provider.FontRequest;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;
import androidx.lifecycle.Observer;
import androidx.multidex.MultiDexApplication;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.jeremyliao.liveeventbus.LiveEventBus;

import org.webrtc.ContextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.leolin.shortcutbadger.ShortcutBadger;
import mega.privacy.android.app.components.ChatManagement;
import mega.privacy.android.app.components.PushNotificationSettingManagement;
import mega.privacy.android.app.components.transferWidget.TransfersManagement;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiManagerShortcodes;
import mega.privacy.android.app.components.twemoji.TwitterEmojiProvider;
import mega.privacy.android.app.di.MegaApi;
import mega.privacy.android.app.di.MegaApiFolder;
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder;
import mega.privacy.android.app.fcm.IncomingCallService;
import mega.privacy.android.app.fcm.KeepAliveService;
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType;
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.GetCookieSettingsUseCase;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.listeners.GetCuAttributeListener;
import mega.privacy.android.app.listeners.GlobalChatListener;
import mega.privacy.android.app.listeners.GlobalListener;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager;
import mega.privacy.android.app.lollipop.megachat.BadgeIntentService;
import mega.privacy.android.app.meeting.CallService;
import mega.privacy.android.app.meeting.listeners.MeetingListener;
import mega.privacy.android.app.objects.PasscodeManagement;
import mega.privacy.android.app.receivers.NetworkStateReceiver;
import mega.privacy.android.app.service.ads.AdsLibInitializer;
import mega.privacy.android.app.utils.ThemeHelper;
import mega.privacy.android.app.utils.Util;
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
import nz.mega.sdk.MegaChatSession;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaPricing;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_TYPE;
import static mega.privacy.android.app.sync.BackupToolsKt.initCuSync;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CacheFolderManager.clearPublicCache;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.getMegaUserNameDB;
import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.IncomingCallNotification.*;
import static mega.privacy.android.app.utils.JobUtil.scheduleCameraUploadJob;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static nz.mega.sdk.MegaChatCall.CALL_STATUS_USER_NO_PRESENT;

@HiltAndroidApp
public class MegaApplication extends MultiDexApplication implements Application.ActivityLifecycleCallbacks, MegaChatRequestListenerInterface, MegaChatNotificationListenerInterface, NetworkStateReceiver.NetworkStateReceiverListener, MegaChatListenerInterface {

	final String TAG = "MegaApplication";

	private static PushNotificationSettingManagement pushNotificationSettingManagement;
	private static TransfersManagement transfersManagement;
	private static PasscodeManagement passcodeManagement;
	private static ChatManagement chatManagement;

	@MegaApi
	@Inject
	MegaApiAndroid megaApi;
	@MegaApiFolder
	@Inject
	MegaApiAndroid megaApiFolder;
	@Inject
	MegaChatApiAndroid megaChatApi;
	@Inject
	DatabaseHandler dbH;
	@Inject
	GetCookieSettingsUseCase getCookieSettingsUseCase;
	@Inject
	SortOrderManagement sortOrderManagement;

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

	private static boolean showInfoChatMessages = false;

	private static long openChatId = -1;

	private static boolean closedChat = true;
	private static HashMap<Long, Boolean> hashMapVideo = new HashMap<>();
	private static HashMap<Long, Boolean> hashMapSpeaker = new HashMap<>();
	private static HashMap<Long, Boolean> hashMapOutgoingCall = new HashMap<>();
	private static HashMap<Long, Boolean> hashOpeningMeetingLink = new HashMap<>();
	private static HashMap<Long, Boolean> hashCreatingMeeting = new HashMap<>();

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
	private static boolean isWaitingForCall = false;
	public static boolean isSpeakerOn = false;
	private static boolean arePreferenceCookiesEnabled = false;
	private static boolean areAdvertisingCookiesEnabled = false;
	private static long userWaitingForCall = MEGACHAT_INVALID_HANDLE;

	private static boolean verifyingCredentials;

	private NetworkStateReceiver networkStateReceiver;
	private BroadcastReceiver logoutReceiver;
    private AppRTCAudioManager rtcAudioManager = null;
	private ArrayList<MegaChatListItem> currentActiveGroupChat = new ArrayList<>();
	private ArrayList<Long> notificationShown = new ArrayList<>();
    private AppRTCAudioManager rtcAudioManagerRingInCall;
    private static MegaApplication singleApplicationInstance;
	private PowerManager.WakeLock wakeLock;

	private MeetingListener meetingListener = new MeetingListener();
	private GlobalChatListener globalChatListener = new GlobalChatListener(this);

    @Override
	public void networkAvailable() {
		logDebug("Net available: Broadcast to ManagerActivity");
		Intent intent = new Intent(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE);
		intent.putExtra(ACTION_TYPE, GO_ONLINE);
		sendBroadcast(intent);
	}

	@Override
	public void networkUnavailable() {
		logDebug("Net unavailable: Broadcast to ManagerActivity");
		Intent intent = new Intent(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE);
		intent.putExtra(ACTION_TYPE, GO_OFFLINE);
		sendBroadcast(intent);
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
				sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED));
				return;
			}

			if (request.getType() == MegaRequest.TYPE_LOGOUT) {
				logDebug("Logout finished: " + e.getErrorString() + "(" + e.getErrorCode() +")");
				if (e.getErrorCode() == MegaError.API_OK) {
					logDebug("END logout sdk request - wait chat logout");
				} else if (e.getErrorCode() == MegaError.API_EINCOMPLETE) {
					if (request.getParamType() == MegaError.API_ESSL) {
						logWarning("SSL verification failed");
						Intent intent = new Intent(BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED);
						sendBroadcast(intent);
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
					logDebug("Get CU attribute on fetch nodes.");
					megaApi.getUserAttribute(USER_ATTR_CAMERA_UPLOADS_FOLDER, new GetCuAttributeListener(getApplicationContext()));

					// Init CU sync data after login successfully
					initCuSync();

					//Login check resumed pending transfers
					TransfersManagement.checkResumedPendingTransfers();
				}
			}
			else if(request.getType() == MegaRequest.TYPE_GET_ATTR_USER){
				if (request.getParamType() == MegaApiJava.USER_ATTR_PUSH_SETTINGS) {
					if (e.getErrorCode() == MegaError.API_OK || e.getErrorCode() == MegaError.API_ENOENT) {
						pushNotificationSettingManagement.sendPushNotificationSettings(request.getMegaPushNotificationSettings());
					}
				}else if (e.getErrorCode() == MegaError.API_OK) {
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
			}else if(request.getType() == MegaRequest.TYPE_SET_ATTR_USER){
				if(request.getParamType() == MegaApiJava.USER_ATTR_PUSH_SETTINGS){
					if (e.getErrorCode() == MegaError.API_OK) {
						pushNotificationSettingManagement.sendPushNotificationSettings(request.getMegaPushNotificationSettings());
					} else {
						logError("Chat notification settings cannot be updated");
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
					intent.putExtra(ACTION_TYPE, UPDATE_GET_PRICING);
					sendBroadcast(intent);
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
					intent.putExtra(ACTION_TYPE, UPDATE_PAYMENT_METHODS);
					sendBroadcast(intent);
				}
			}
			else if(request.getType() == MegaRequest.TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS){
				if (e.getErrorCode() == MegaError.API_OK){
					if(myAccountInfo!=null){
						myAccountInfo.setNumberOfSubscriptions(request.getNumber());
						logDebug("NUMBER OF SUBS: " + myAccountInfo.getNumberOfSubscriptions());
					}

					Intent intent = new Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS);
					intent.putExtra(ACTION_TYPE, UPDATE_CREDIT_CARD_SUBSCRIPTION);
					sendBroadcast(intent);
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
			} else if (request.getType() == MegaRequest.TYPE_PAUSE_TRANSFERS) {
				dbH.setTransferQueueStatus(request.getFlag());
			}
		}

		@Override
		public void onRequestTemporaryError(MegaApiJava api,
				MegaRequest request, MegaError e) {
			logDebug("BackgroundRequestListener: onRequestTemporaryError: " + request.getRequestString());
		}
		
	}

	public void sendBroadcastUpdateAccountDetails() {
		sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
				.putExtra(ACTION_TYPE, UPDATE_ACCOUNT_DETAILS));
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
							MegaHandleList callsInProgress = megaChatApi.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS);
							if (callsInProgress == null || callsInProgress.size() <= 0) {
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

	private final Observer<MegaChatCall> callStatusObserver = call -> {
		int callStatus = call.getStatus();
		boolean isOutgoing = call.isOutgoing();
		boolean isRinging = call.isRinging();
		long callId = call.getCallId();
		long chatId = call.getChatid();
		stopService(new Intent(getInstance(), IncomingCallService.class));

		logDebug("Call status is " + callStatusToString(callStatus));
		switch (callStatus) {
			case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:
			case MegaChatCall.CALL_STATUS_JOINING:
			case MegaChatCall.CALL_STATUS_IN_PROGRESS:
				MegaHandleList listAllCalls = megaChatApi.getChatCalls();
				if (listAllCalls == null || listAllCalls.size() == 0) {
					logError("Calls not found");
					return;
				}

				if (callStatus == CALL_STATUS_USER_NO_PRESENT && isRinging) {
					logDebug("Is incoming call");
					incomingCall(listAllCalls, chatId, callStatus);
				}

				if ((callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_JOINING)) {
					logDebug("Is ongoing call");
					ongoingCall(chatId, (isOutgoing && isRequestSent(callId)) ? AUDIO_MANAGER_CALL_OUTGOING : AUDIO_MANAGER_CALL_IN_PROGRESS);
				}
				break;

			case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
				logDebug("The user is no longer participating");
				clearIncomingCallNotification(callId);
				removeValues(chatId);
				setRequestSentCall(callId, false);
				break;

			case MegaChatCall.CALL_STATUS_DESTROYED:
				logDebug("Call has ended");
				MegaApplication.setOpeningMeetingLink(chatId, false);
				int termCode = call.getTermCode();
				boolean isIgnored = call.isIgnored();
				checkCallDestroyed(chatId, callId, termCode, isIgnored);
				setRequestSentCall(callId, false);
				break;
		}
	};

	private final Observer<MegaChatCall> callRingingStatusObserver = call -> {
		int callStatus = call.getStatus();
		boolean isRinging = call.isRinging();
		MegaHandleList listAllCalls = megaChatApi.getChatCalls();
		if (listAllCalls == null || listAllCalls.size() == 0) {
			logError("Calls not found");
			return;
		}
		if (isRinging) {
			logDebug("Is incoming call");
			incomingCall(listAllCalls, call.getChatid(), callStatus);
		}
	};

	private final Observer<Pair> sessionStatusObserver = sessionAndCall -> {
		MegaChatSession session = (MegaChatSession) sessionAndCall.second;
		int sessionStatus = session.getStatus();
		if (sessionStatus == MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
			logDebug("Session is in progress");
			long callId = (long) sessionAndCall.first;
			setRequestSentCall(callId, false);
			updateRTCAudioMangerTypeStatus(AUDIO_MANAGER_CALL_IN_PROGRESS);
		}
	};

	/**
	 * Method that performs the necessary actions when there is an incoming call.
	 *
	 * @param listAllCalls List of all current calls
	 * @param chatId       Chat ID
	 * @param callStatus   Call Status
	 */
	private void incomingCall(MegaHandleList listAllCalls, long chatId, int callStatus) {
		if (!megaApi.isChatNotifiable(chatId))
			return;

		if(!isOpeningMeetingLink(chatId)){
			logDebug("Incoming call");
			createOrUpdateAudioManager(false, AUDIO_MANAGER_CALL_RINGING);
			controlNumberOfCalls(listAllCalls, chatId, callStatus, true);
		}
	}

	/**
	 * Method that performs the necessary actions when there is an outgoing call or incoming call.
	 *
	 * @param chatId           Chat ID
	 * @param typeAudioManager audio Manager type
	 */
	public void ongoingCall(long chatId, int typeAudioManager) {
		if (rtcAudioManager != null && rtcAudioManager.getTypeAudioManager() == typeAudioManager)
			return;

		logDebug("Controlling outgoing/in progress call");
		if (typeAudioManager == AUDIO_MANAGER_CALL_OUTGOING && isOpeningMeetingLink(chatId)) {
			clearIncomingCallNotification(chatId);
			return;
		}

		if (!isCreatingMeeting(chatId)) {
			createOrUpdateAudioManager(getSpeakerStatus(chatId), typeAudioManager);
		}
	}

	/**
	 * Method to control whether there is one or more calls.
	 *
	 * @param listAllCalls   List of all current calls
	 * @param chatId         Chat ID
	 * @param callStatus     Call Status
	 * @param isIncomingCall If the current call is an incoming call
	 */
	private void controlNumberOfCalls(MegaHandleList listAllCalls, long chatId, int callStatus, boolean isIncomingCall) {
		if (listAllCalls.size() == 1) {
			checkOneCall(listAllCalls.get(0));
		} else {
			checkSeveralCall(listAllCalls, callStatus, isIncomingCall);
		}
	}

	/**
	 * Broadcast for controlling changes in screen.
	 */
	BroadcastReceiver screenOnOffReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null || intent.getAction() == null)
				return;

			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				muteOrUnmute(true);
			}
		}
	};

	/**
	 * Broadcast for controlling changes in the volume.
	 */
	BroadcastReceiver volumeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null || intent.getAction() == null)
				return;

			if (intent.getAction().equals(VOLUME_CHANGED_ACTION) && rtcAudioManagerRingInCall != null) {
				int type = (Integer) intent.getExtras().get(EXTRA_VOLUME_STREAM_TYPE);
				if(type != AudioManager.STREAM_RING)
					return;

				int newVolume = (Integer) intent.getExtras().get(EXTRA_VOLUME_STREAM_VALUE);
				if (newVolume != INVALID_VOLUME) {
					rtcAudioManagerRingInCall.checkVolume(newVolume);
				}
			}
		}
	};

	public boolean isAnIncomingCallRinging() {
		return rtcAudioManagerRingInCall != null;
	}

	BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null || intent.getAction() == null)
				return;

			if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
				muteOrUnmute(true);
			}
		}
	};

	public void muteOrUnmute(boolean mute){
		if (rtcAudioManagerRingInCall != null) {
			rtcAudioManagerRingInCall.muteOrUnmuteIncomingCall(mute);
		}
	}

	public static MegaApplication getInstance() {
		return singleApplicationInstance;
	}

	@Override
	public void onCreate() {
		singleApplicationInstance = this;

		super.onCreate();

		ThemeHelper.INSTANCE.initTheme(this);

		// Setup handler for uncaught exceptions.
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable e) {
				handleUncaughtException(thread, e);
			}
		});

		registerActivityLifecycleCallbacks(this);

		isVerifySMSShowed = false;

		keepAliveHandler.postAtTime(keepAliveRunnable, System.currentTimeMillis()+interval);
		keepAliveHandler.postDelayed(keepAliveRunnable, interval);

		initLoggerSDK();
		initLoggerKarere();

		checkAppUpgrade();

		setupMegaApi();
		setupMegaApiFolder();
		setupMegaChatApi();

        scheduleCameraUploadJob(getApplicationContext());
        storageState = dbH.getStorageState();
        pushNotificationSettingManagement = new PushNotificationSettingManagement();
        transfersManagement = new TransfersManagement();
        passcodeManagement = new PasscodeManagement(null, 0, true);
        chatManagement = new ChatManagement();

		//Logout check resumed pending transfers
		TransfersManagement.checkResumedPendingTransfers();

		boolean staging = true;
//		if (dbH != null) {
//			MegaAttributes attrs = dbH.getAttributes();
//			if (attrs != null && attrs.getStaging() != null) {
//				staging = Boolean.parseBoolean(attrs.getStaging());
//			}
//		}

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
		registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

		LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall.class).observeForever(callStatusObserver);
		LiveEventBus.get(EVENT_RINGING_STATUS_CHANGE, MegaChatCall.class).observeForever(callRingingStatusObserver);
		LiveEventBus.get(EVENT_SESSION_STATUS_CHANGE, Pair.class).observeForever(sessionStatusObserver);

		logoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context,Intent intent) {
                if (intent != null) {
                    if (intent.getAction().equals(ACTION_LOG_OUT)) {
                        storageState = MegaApiJava.STORAGE_STATE_UNKNOWN; //Default value
                    }
                }
            }
        };

		registerReceiver(logoutReceiver, new IntentFilter(ACTION_LOG_OUT));

		EmojiManager.install(new TwitterEmojiProvider());
		EmojiManagerShortcodes.initEmojiData(getApplicationContext());
		EmojiManager.install(new TwitterEmojiProvider());
		final EmojiCompat.Config config;
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
		EmojiCompat.init(config);

		// clear the cache files stored in the external cache folder.
        clearPublicCache(this);

		ContextUtils.initialize(getApplicationContext());

		Fresco.initialize(this);

		AdsLibInitializer.INSTANCE.init(this);

		Util.writeAppLaunchedTime(this);
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
		return megaApiFolder;
	}

	public void disableMegaChatApi() {
		try {
			if (megaChatApi != null) {
				megaChatApi.removeChatRequestListener(this);
				megaChatApi.removeChatNotificationListener(this);
				megaChatApi.removeChatListener(globalChatListener);
				//megaChatApi.removeChatCallListener(callListener);
				megaChatApi.removeChatCallListener(meetingListener);
				registeredChatListeners = false;
			}
		} catch (Exception ignored) {
		}
	}

	private void setupMegaApi() {
		megaApi.retrySSLerrors(true);

		megaApi.setDownloadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
		megaApi.setUploadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);

		requestListener = new BackgroundRequestListener();
		logDebug("ADD REQUESTLISTENER");
		megaApi.addRequestListener(requestListener);

		megaApi.addGlobalListener(new GlobalListener());

		String language = Locale.getDefault().toString();
		boolean languageString = megaApi.setLanguage(language);
		logDebug("Result: " + languageString + " Language: " + language);
		if (!languageString) {
			language = Locale.getDefault().getLanguage();
			languageString = megaApi.setLanguage(language);
			logDebug("Result: " + languageString + " Language: " + language);
		}

		// Set the proper resource limit to try avoid issues when the number of parallel transfers is very big.
		final int DESIRABLE_R_LIMIT = 20000; // SDK team recommended value
		int currentLimit = megaApi.platformGetRLimitNumFile();
		logDebug("Current resource limit is set to " + currentLimit);
		if (currentLimit < DESIRABLE_R_LIMIT) {
			logDebug("Resource limit is under desirable value. Trying to increase the resource limit...");
			if (!megaApi.platformSetRLimitNumFile(DESIRABLE_R_LIMIT)) {
				logWarning("Error setting resource limit.");
			}

			// Check new resource limit after set it in order to see if had been set successfully to the
			// desired value or maybe to a lower value limited by the system.
			logDebug("Resource limit is set to " + megaApi.platformGetRLimitNumFile());
		}
	}

	/**
	 * Setup the MegaApiAndroid instance for folder link.
	 */
	private void setupMegaApiFolder() {
		megaApiFolder.retrySSLerrors(true);

		megaApiFolder.setDownloadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
		megaApiFolder.setUploadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
	}

	private void setupMegaChatApi() {
		if (!registeredChatListeners) {
			logDebug("Add listeners of megaChatApi");
			megaChatApi.addChatRequestListener(this);
			megaChatApi.addChatNotificationListener(this);
			megaChatApi.addChatListener(globalChatListener);
			//megaChatApi.addChatCallListener(callListener);
			megaChatApi.addChatCallListener(meetingListener);
			megaChatApi.setPublicKeyPinning(false);
			registeredChatListeners = true;
		}
	}

	/**
	 * Check current enabled cookies and set the corresponding flags to true/false
	 */
	public void checkEnabledCookies() {
		getCookieSettingsUseCase.get()
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe((cookies, throwable) -> {
					if (throwable == null) {
						setPreferenceCookiesEnabled(cookies.contains(CookieType.PREFERENCE));
						setAdvertisingCookiesEnabled(cookies.contains(CookieType.ADVERTISEMENT));
					}
				});
	}

	public MegaApiAndroid getMegaApi() {
		return megaApi;
	}

	public MegaChatApiAndroid getMegaChatApi() {
		setupMegaChatApi();
		return megaChatApi;
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
						.setColor(ContextCompat.getColor(this, R.color.red_600_red_300));

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
					notificationBuilder.setColor(ContextCompat.getColor(this, R.color.red_600_red_300));
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

			sortOrderManagement.resetDefaults();

			try{
				if (megaChatApi != null){
					megaChatApi.removeChatRequestListener(this);
					megaChatApi.removeChatNotificationListener(this);
					megaChatApi.removeChatListener(globalChatListener);
					//megaChatApi.removeChatCallListener(callListener);
					megaChatApi.removeChatCallListener(meetingListener);
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

					if (isLoggingRunning()) {
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
		} else if (request.getType() == MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT) {
			chatManagement.removeJoiningChatId(request.getChatHandle());
			chatManagement.removeJoiningChatId(request.getUserHandle());
		} else if (request.getType() == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM
				&& request.getUserHandle() == INVALID_HANDLE) {
			chatManagement.removeLeavingChatId(request.getChatHandle());
		}
	}

	@Override
	public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
		logWarning("onRequestTemporaryError (CHAT): "+e.getErrorString());
	}

	/**
	 * Method for showing an incoming group call notification.
	 *
	 * @param chatId The chat ID of the chat with call.
	 */
	private void showGroupCallNotification(long chatId) {
		logDebug("Show group call notification");
		notificationShown.add(chatId);
		stopService(new Intent(this, IncomingCallService.class));
		ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);
		notificationBuilder.checkOneGroupCall(chatId);
	}

	public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {
		if (!item.isGroup() || notificationShown == null)
			return;

		if (item.getChanges() == 0 && !currentActiveGroupChat.contains(item)) {
			currentActiveGroupChat.add(item);
		}

		if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV)) {
			if (item.getOwnPrivilege() != MegaChatRoom.PRIV_RM) {
				if (!currentActiveGroupChat.contains(item)) {
					currentActiveGroupChat.add(item);
					MegaChatCall call = api.getChatCall(item.getChatId());
					if (call != null && call.getStatus() == CALL_STATUS_USER_NO_PRESENT) {
						if (notificationShown.isEmpty() || !notificationShown.contains(item.getChatId())) {
							if(!isOpeningMeetingLink(item.getChatId())){
								notificationShown.add(item.getChatId());
								showGroupCallNotification(item.getChatId());
							}
						}
						return;

					}
				}
			} else {
				currentActiveGroupChat.remove(item);
				notificationShown.remove(item.getChatId());
			}
		}

		if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_CLOSED)) {
			currentActiveGroupChat.remove(item);
			notificationShown.remove(item.getChatId());
		}
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
			sendBroadcast(intent);
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
		int callStatus = callToLaunch.getStatus();

		if (callStatus > MegaChatCall.CALL_STATUS_IN_PROGRESS){
			logWarning("Launch not in correct status");
			return;
		}
		MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
		if (callToLaunch.getStatus() == CALL_STATUS_USER_NO_PRESENT && callToLaunch.isRinging() && chatRoom != null && chatRoom.isGroup()) {
			showGroupCallNotification(chatId);
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
			logDebug("The call screen should be displayed");
			launchCallActivity(callToLaunch);
		}
	}

	private void checkSeveralCall(MegaHandleList listAllCalls, int callStatus, boolean isRinging) {
		logDebug("Several calls = " + listAllCalls.size() + "- Current call Status: " + callStatusToString(callStatus));
		if (isRinging && participatingInACall()) {
			logDebug("Several calls: show notification");
			checkQueuedCalls();
			return;
		}

		MegaHandleList handleList = megaChatApi.getChatCalls(callStatus);
		if (handleList == null || handleList.size() == 0)
			return;

		MegaChatCall callToLaunch = null;

		for (int i = 0; i < handleList.size(); i++) {
			if (openCallChatId != handleList.get(i)) {
				if (megaChatApi.getChatCall(handleList.get(i)) != null && !megaChatApi.getChatCall(handleList.get(i)).isOnHold()) {
					callToLaunch = megaChatApi.getChatCall(handleList.get(i));
				}
			} else {
				logDebug("The call is already opened");
			}
		}

		if (callToLaunch != null) {
			logDebug("The call screen should be displayed");
			launchCallActivity(callToLaunch);
		}
	}

	private void removeValues(long chatId) {
		removeStatusVideoAndSpeaker(chatId);

        if (!existsAnOngoingOrIncomingCall()) {
            removeRTCAudioManager();
            removeRTCAudioManagerRingIn();
        } else if (participatingInACall()) {
            removeRTCAudioManagerRingIn();
        }
	}

	private void checkCallDestroyed(long chatId, long callId, int termCode, boolean isIgnored) {
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
			if(termCode == MegaChatCall.TERM_CODE_ERROR && !isIgnored){
				if (megaApi.isChatNotifiable(chatId)) {
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

	private void removeStatusVideoAndSpeaker(long chatId) {
		hashMapSpeaker.remove(chatId);
		hashMapVideo.remove(chatId);
	}

	public AppRTCAudioManager getAudioManager() {
		return rtcAudioManager;
	}

	public void createOrUpdateAudioManager(boolean isSpeakerOn, int type) {
		logDebug("Create or update audio manager, type is " + type);
		if (type == AUDIO_MANAGER_CALL_RINGING) {
			if (rtcAudioManagerRingInCall != null) {
				removeRTCAudioManagerRingIn();
			}

			IntentFilter filterScreen = new IntentFilter();
			filterScreen.addAction(Intent.ACTION_SCREEN_ON);
			filterScreen.addAction(Intent.ACTION_SCREEN_OFF);
			filterScreen.addAction(Intent.ACTION_USER_PRESENT);
			registerReceiver(screenOnOffReceiver, filterScreen);

			registerReceiver(volumeReceiver, new IntentFilter(VOLUME_CHANGED_ACTION));
			registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
			logDebug("Creating RTC Audio Manager (ringing mode)");
			rtcAudioManagerRingInCall = AppRTCAudioManager.create(this, false, AUDIO_MANAGER_CALL_RINGING);
		} else {
			if (rtcAudioManager != null) {
				rtcAudioManager.setTypeAudioManager(type);
				return;
			}

			logDebug("Creating RTC Audio Manager");
			removeRTCAudioManagerRingIn();
			MegaApplication.isSpeakerOn = isSpeakerOn;
			rtcAudioManager = AppRTCAudioManager.create(this, isSpeakerOn, type);
			startProximitySensor();
		}
	}

    /**
     * Remove the incoming call AppRTCAudioManager.
     */
    public void removeRTCAudioManagerRingIn() {
		if (rtcAudioManagerRingInCall == null)
            return;

        try {
            logDebug("Removing RTC Audio Manager");
            rtcAudioManagerRingInCall.stop();
			rtcAudioManagerRingInCall = null;

			unregisterReceiver(screenOnOffReceiver);
			unregisterReceiver(volumeReceiver);
			unregisterReceiver(becomingNoisyReceiver);
		} catch (Exception e) {
            logError("Exception stopping speaker audio manager", e);
        }
    }

    /**
     * Remove the ongoing call AppRTCAudioManager.
     */
    public void removeRTCAudioManager() {
		if (rtcAudioManager == null)
            return;

        try {
            logDebug("Removing RTC Audio Manager");
            rtcAudioManager.stop();
            rtcAudioManager = null;
        } catch (Exception e) {
            logError("Exception stopping speaker audio manager", e);
        }
    }

    /**
     * Method for updating the call status of the Audio Manger.
     *
     * @param callStatus Call status.
     */
    private void updateRTCAudioMangerTypeStatus(int callStatus) {
        removeRTCAudioManagerRingIn();
        stopSounds();
        if (rtcAudioManager != null) {
			rtcAudioManager.setTypeAudioManager(callStatus);
        }
    }

	/**
	 * Method for updating the call status of the Speaker status .
	 * @param isSpeakerOn If the speaker is on.
	 * @param typeStatus type AudioManager.
	 */
    public void updateSpeakerStatus(boolean isSpeakerOn, int typeStatus) {
        if (rtcAudioManager != null) {
            rtcAudioManager.updateSpeakerStatus(isSpeakerOn, typeStatus);
            return;
        }
    }

    /**
     * Activate the proximity sensor.
     */
    public void startProximitySensor() {
        if (rtcAudioManager != null) {
            logDebug("Starting proximity sensor...");
            rtcAudioManager.startProximitySensor();
            rtcAudioManager.setOnProximitySensorListener(isNear -> {
				LiveEventBus.get(EVENT_PROXIMITY_SENSOR_CHANGE, Boolean.class).post(isNear);
            });
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

	/*
     * Method for stopping the sound of incoming or outgoing calls.
     */
    public void stopSounds() {
        if (rtcAudioManager != null) {
            rtcAudioManager.stopAudioSignals();
        }
        if (rtcAudioManagerRingInCall != null) {
            rtcAudioManagerRingInCall.stopAudioSignals();
        }
    }

    public void openCallService(long chatId) {
    	if(chatId != MEGACHAT_INVALID_HANDLE){
			logDebug("Start call Service. Chat iD = " + chatId);
			Intent intentService = new Intent(this, CallService.class);
			intentService.putExtra(CHAT_ID, chatId);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				this.startForegroundService(intentService);
			} else {
				this.startService(intentService);
			}
		}
    }

	public void checkQueuedCalls() {
		try {
			stopService(new Intent(this, IncomingCallService.class));
			ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);
			notificationBuilder.checkQueuedCalls();
		} catch (Exception e) {
			logError("EXCEPTION", e);
		}
	}

	public void launchCallActivity(MegaChatCall call) {
		logDebug("Show the call screen: " + callStatusToString(call.getStatus()) + ", callId = " + call.getCallId());
		openMeetingRinging(this, call.getChatid());
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

	public void resetMyAccountInfo() {
    	myAccountInfo = new MyAccountInfo();
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

	public static boolean isCreatingMeeting(long chatId) {
		boolean entryExists = hashCreatingMeeting.containsKey(chatId);
		if (entryExists) {
			return hashCreatingMeeting.get(chatId);
		}

		return false;
	}

	public static void setCreatingMeeting(long chatId, boolean isCreatingMeeting) {
		hashCreatingMeeting.put(chatId, isCreatingMeeting);
	}

	private static boolean isOpeningMeetingLink(long chatId) {
		boolean entryExists = hashOpeningMeetingLink.containsKey(chatId);
		if (entryExists) {
			return hashOpeningMeetingLink.get(chatId);
		}

		return false;
	}

	public static void setOpeningMeetingLink(long chatId, boolean isOpeningMeetingLink) {
		hashOpeningMeetingLink.put(chatId, isOpeningMeetingLink);
	}

	public static boolean isRequestSent(long callId) {
		boolean entryExists = hashMapOutgoingCall.containsKey(callId);
		if (entryExists) {
			return hashMapOutgoingCall.get(callId);
		}

		return false;
	}

	public static void setRequestSentCall(long callId, boolean isRequestSent) {
		if(isRequestSent(callId) == isRequestSent)
    		return;

		hashMapOutgoingCall.put(callId, isRequestSent);
		if(!isRequestSent){
			LiveEventBus.get(EVENT_NOT_OUTGOING_CALL, Long.class).post(callId);
		}
	}

	public int getStorageState() {
	    return storageState;
	}

    public void setStorageState(int state) {
	    this.storageState = state;
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

	public boolean isLoggingRunning() {
		return isLoggingRunning;
	}

    public static PushNotificationSettingManagement getPushNotificationSettingManagement() {
        return pushNotificationSettingManagement;
    }

	public static TransfersManagement getTransfersManagement() {
		return transfersManagement;
	}

	public static ChatManagement getChatManagement() {
		return chatManagement;
	}

	public static void setVerifyingCredentials(boolean verifyingCredentials) {
		MegaApplication.verifyingCredentials = verifyingCredentials;
	}

	public static boolean isVerifyingCredentials() {
		return MegaApplication.verifyingCredentials;
	}

	public Activity getCurrentActivity() {
		return currentActivity;
	}

	public static boolean isWaitingForCall() {
		return isWaitingForCall;
	}

	public static void setIsWaitingForCall(boolean isWaitingForCall) {
		MegaApplication.isWaitingForCall = isWaitingForCall;
	}

	public static long getUserWaitingForCall() {
		return userWaitingForCall;
	}

	public static void setUserWaitingForCall(long userWaitingForCall) {
		MegaApplication.userWaitingForCall = userWaitingForCall;
	}

	public static PasscodeManagement getPasscodeManagement() {
		return passcodeManagement;
	}

	public static boolean arePreferenceCookiesEnabled() {
		return arePreferenceCookiesEnabled;
	}

	public static void setPreferenceCookiesEnabled(boolean enabled) {
		arePreferenceCookiesEnabled = enabled;
	}

	public static boolean areAdvertisingCookiesEnabled() {
		return areAdvertisingCookiesEnabled;
	}

	public static void setAdvertisingCookiesEnabled(boolean enabled) {
		areAdvertisingCookiesEnabled = enabled;
	}
}
