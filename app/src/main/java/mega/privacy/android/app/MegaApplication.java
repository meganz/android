package mega.privacy.android.app;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_TYPE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_COMPOSITION_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_RINGING_STATUS_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_STATUS_CHANGE;
import static mega.privacy.android.app.utils.CacheFolderManager.clearPublicCache;
import static mega.privacy.android.app.utils.CallUtil.callStatusToString;
import static mega.privacy.android.app.utils.CallUtil.clearIncomingCallNotification;
import static mega.privacy.android.app.utils.CallUtil.incomingCall;
import static mega.privacy.android.app.utils.CallUtil.ongoingCall;
import static mega.privacy.android.app.utils.CallUtil.openMeetingRinging;
import static mega.privacy.android.app.utils.CallUtil.participatingInACall;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.API_SERVER;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.API_SERVER_PREFERENCES;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.PRODUCTION_SERVER_VALUE;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.SANDBOX3_SERVER_VALUE;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.getApiServerFromValue;
import static mega.privacy.android.app.utils.Constants.ACTION_INCOMING_SHARED_FOLDER_NOTIFICATION;
import static mega.privacy.android.app.utils.Constants.ACTION_LOG_OUT;
import static mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CALL_IN_PROGRESS;
import static mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CALL_OUTGOING;
import static mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CALL_RINGING;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_CHANNEL_CLOUDDRIVE_NAME;
import static mega.privacy.android.app.utils.Constants.NOTIFICATION_PUSH_CLOUD_DRIVE;
import static mega.privacy.android.app.utils.Constants.UPDATE_ACCOUNT_DETAILS;
import static mega.privacy.android.app.utils.ContactUtil.getMegaUserNameDB;
import static mega.privacy.android.app.utils.DBUtil.callToAccountDetails;
import static mega.privacy.android.app.utils.DBUtil.callToExtendedAccountDetails;
import static mega.privacy.android.app.utils.DBUtil.callToPaymentMethods;
import static mega.privacy.android.app.utils.IncomingCallNotification.shouldNotify;
import static mega.privacy.android.app.utils.IncomingCallNotification.toSystemSettingNotification;
import static mega.privacy.android.app.utils.Util.isSimplifiedChinese;
import static mega.privacy.android.app.utils.Util.toCDATA;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static nz.mega.sdk.MegaChatCall.CALL_STATUS_USER_NO_PRESENT;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.text.Spanned;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.provider.FontRequest;
import androidx.core.text.HtmlCompat;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.lifecycle.Observer;
import androidx.multidex.MultiDexApplication;
import androidx.work.Configuration;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.memory.PoolConfig;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.jeremyliao.liveeventbus.LiveEventBus;

import org.webrtc.ContextUtils;

import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlinx.coroutines.CoroutineScope;
import mega.privacy.android.app.components.ChatManagement;
import mega.privacy.android.app.components.PushNotificationSettingManagement;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiManagerShortcodes;
import mega.privacy.android.app.components.twemoji.TwitterEmojiProvider;
import mega.privacy.android.app.di.ApplicationScope;
import mega.privacy.android.app.di.MegaApi;
import mega.privacy.android.app.di.MegaApiFolder;
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder;
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType;
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.GetCookieSettingsUseCase;
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler;
import mega.privacy.android.app.globalmanagement.BackgroundRequestListener;
import mega.privacy.android.app.globalmanagement.MegaChatNotificationHandler;
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler;
import mega.privacy.android.app.globalmanagement.MyAccountInfo;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.globalmanagement.TransfersManagement;
import mega.privacy.android.app.listeners.GlobalChatListener;
import mega.privacy.android.app.listeners.GlobalListener;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.meeting.CallService;
import mega.privacy.android.app.meeting.CallSoundsController;
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway;
import mega.privacy.android.app.meeting.listeners.MeetingListener;
import mega.privacy.android.app.middlelayer.reporter.CrashReporter;
import mega.privacy.android.app.middlelayer.reporter.PerformanceReporter;
import mega.privacy.android.app.objects.PasscodeManagement;
import mega.privacy.android.app.presentation.logging.InitialiseLoggingUseCaseJavaWrapper;
import mega.privacy.android.app.presentation.theme.ThemeModeState;
import mega.privacy.android.app.receivers.NetworkStateReceiver;
import mega.privacy.android.app.usecase.call.GetCallSoundsUseCase;
import mega.privacy.android.app.utils.CallUtil;
import mega.privacy.android.app.utils.FrescoNativeMemoryChunkPoolParams;
import mega.privacy.android.domain.usecase.InitialiseLogging;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatSession;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

@HiltAndroidApp
public class MegaApplication extends MultiDexApplication implements Configuration.Provider {

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
    @Inject
    MyAccountInfo myAccountInfo;
    @Inject
    PasscodeManagement passcodeManagement;
    @Inject
    CrashReporter crashReporter;
    @Inject
    PerformanceReporter performanceReporter;
    @Inject
    InitialiseLogging initialiseLoggingUseCase;
    @Inject
    GetCallSoundsUseCase getCallSoundsUseCase;
    @Inject
    HiltWorkerFactory workerFactory;
    @Inject
    ThemeModeState themeModeState;
    @ApplicationScope
    @Inject
    CoroutineScope sharingScope;
    @Inject
    public TransfersManagement transfersManagement;
    @Inject
    ActivityLifecycleHandler activityLifecycleHandler;
    @Inject
    GlobalListener globalListener;
    @Inject
    MegaChatNotificationHandler megaChatNotificationHandler;
    @Inject
    PushNotificationSettingManagement pushNotificationSettingManagement;
    @Inject
    ChatManagement chatManagement;
    @Inject
    BackgroundRequestListener requestListener;
    @Inject
    MegaChatRequestHandler chatRequestHandler;
    @Inject
    RTCAudioManagerGateway rtcAudioManagerGateway;

    String localIpAddress = "";
    final static public String APP_KEY = "6tioyn8ka5l6hty";
    final static private String APP_SECRET = "hfzgdtrma231qdm";

    boolean esid = false;

    private int storageState = MegaApiJava.STORAGE_STATE_UNKNOWN; //Default value

    private static boolean isLoggingIn = false;
    private static boolean isLoggingOut = false;

    private static boolean isHeartBeatAlive = false;

    private static boolean showInfoChatMessages = false;

    private static long openChatId = -1;

    private static boolean closedChat = true;

    private static long openCallChatId = -1;

    private static boolean showRichLinkWarning = false;
    private static int counterNotNowRichLinkWarning = -1;
    private static boolean enabledRichLinks = false;

    private static boolean enabledGeoLocation = false;

    private static int disableFileVersions = -1;

    private static boolean recentChatVisible = false;

    private static String urlConfirmationLink = null;

    private static boolean registeredChatListeners = false;

    private static boolean isVerifySMSShowed = false;

    private static boolean isBlockedDueToWeakAccount = false;
    private static boolean isWebOpenDueToEmailVerification = false;
    private static boolean isWaitingForCall = false;
    private static long userWaitingForCall = MEGACHAT_INVALID_HANDLE;

    private BroadcastReceiver logoutReceiver;
    private static MegaApplication singleApplicationInstance;
    private PowerManager.WakeLock wakeLock;

    private MeetingListener meetingListener = new MeetingListener();
    private GlobalChatListener globalChatListener = new GlobalChatListener(this);

    private final CallSoundsController soundsController = new CallSoundsController();

    public static void smsVerifyShowed(boolean isShowed) {
        isVerifySMSShowed = isShowed;
    }



    private final int interval = 3000;
    private Handler keepAliveHandler = new Handler();
    int backgroundStatus = -1;

    private Runnable keepAliveRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (activityLifecycleHandler.isActivityVisible()) {
                    Timber.d("KEEPALIVE: %s", System.currentTimeMillis());
                    if (megaChatApi != null) {
                        backgroundStatus = megaChatApi.getBackgroundStatus();
                        Timber.d("backgroundStatus_activityVisible: %s", backgroundStatus);
                        if (backgroundStatus != -1 && backgroundStatus != 0) {
                            megaChatApi.setBackgroundStatus(false);
                        }
                    }

                } else {
                    Timber.d("KEEPALIVEAWAY: %s", System.currentTimeMillis());
                    if (megaChatApi != null) {
                        backgroundStatus = megaChatApi.getBackgroundStatus();
                        Timber.d("backgroundStatus_!activityVisible: %s", backgroundStatus);
                        if (backgroundStatus != -1 && backgroundStatus != 1) {
                            megaChatApi.setBackgroundStatus(true);
                        }
                    }
                }

                keepAliveHandler.postAtTime(keepAliveRunnable, System.currentTimeMillis() + interval);
                keepAliveHandler.postDelayed(keepAliveRunnable, interval);
            } catch (Exception exc) {
                Timber.e(exc, "Exception in keepAliveRunnable");
            }
        }
    };

    public void handleUncaughtException(Throwable throwable) {
        Timber.e(throwable, "UNCAUGHT EXCEPTION");
        crashReporter.report(throwable);
    }

    private final Observer<MegaChatCall> callStatusObserver = call -> {
        int callStatus = call.getStatus();
        boolean isOutgoing = call.isOutgoing();
        boolean isRinging = call.isRinging();
        long callId = call.getCallId();
        long chatId = call.getChatid();
        if (chatId == MEGACHAT_INVALID_HANDLE || callId == MEGACHAT_INVALID_HANDLE) {
            Timber.e("Error in chatId or callId");
            return;
        }

        Timber.d("Call status is %s, chat id is %d, call id is %d", callStatusToString(callStatus), chatId, callId);

        switch (callStatus) {
            case MegaChatCall.CALL_STATUS_CONNECTING:
                if ((isOutgoing && getChatManagement().isRequestSent(callId)))
                    rtcAudioManagerGateway.removeRTCAudioManager();
                break;
            case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:
            case MegaChatCall.CALL_STATUS_JOINING:
            case MegaChatCall.CALL_STATUS_IN_PROGRESS:
                MegaHandleList listAllCalls = megaChatApi.getChatCalls();
                if (listAllCalls == null || listAllCalls.size() == 0) {
                    Timber.e("Calls not found");
                    return;
                }

                if (callStatus == CALL_STATUS_USER_NO_PRESENT) {
                    if (isRinging) {
                        Timber.d("Is incoming call");
                        incomingCall(listAllCalls, chatId, callStatus);
                    } else {
                        MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
                        if (chatRoom != null && chatRoom.isGroup()) {
                            Timber.d("Check if the incoming group call notification should be displayed");
                            getChatManagement().checkActiveGroupChat(chatId);
                        }
                    }
                }

                if ((callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_JOINING)) {
                    getChatManagement().addNotificationShown(chatId);
                    Timber.d("Is ongoing call");
                    ongoingCall(rtcAudioManagerGateway, chatId, callId, (isOutgoing && getChatManagement().isRequestSent(callId)) ? AUDIO_MANAGER_CALL_OUTGOING : AUDIO_MANAGER_CALL_IN_PROGRESS);
                }
                break;

            case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
                Timber.d("The user participation in the call has ended. The termination code is %s", CallUtil.terminationCodeForCallToString(call.getTermCode()));
                getChatManagement().controlCallFinished(callId, chatId);
                break;

            case MegaChatCall.CALL_STATUS_DESTROYED:
                int endCallReason = call.getEndCallReason();
                Timber.d("Call has ended. End call reason is %s", CallUtil.endCallReasonToString(endCallReason));
                getChatManagement().controlCallFinished(callId, chatId);
                boolean isIgnored = call.isIgnored();
                checkCallDestroyed(chatId, callId, endCallReason, isIgnored);
                break;
        }
    };

    private final
    Observer<MegaChatCall> callCompositionObserver = call -> {
        MegaChatRoom chatRoom = megaChatApi.getChatRoom(call.getChatid());
        if (chatRoom != null && call.getCallCompositionChange() == 1 && call.getNumParticipants() > 1) {
            Timber.d("Stop sound");
            if (megaChatApi.getMyUserHandle() == call.getPeeridCallCompositionChange()) {
                clearIncomingCallNotification(call.getCallId());
                getChatManagement().removeValues(call.getChatid());
                if (call.getStatus() == CALL_STATUS_USER_NO_PRESENT) {
                    LiveEventBus.get(EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT, Long.class).post(call.getChatid());
                }
            }
        }
    };

    private final Observer<MegaChatCall> callRingingStatusObserver = call -> {
        int callStatus = call.getStatus();
        boolean isRinging = call.isRinging();
        MegaHandleList listAllCalls = megaChatApi.getChatCalls();
        if (listAllCalls == null || listAllCalls.size() == 0) {
            Timber.e("Calls not found");
            return;
        }
        if (isRinging) {
            Timber.d("Is incoming call");
            incomingCall(listAllCalls, call.getChatid(), callStatus);
        } else {
            clearIncomingCallNotification(call.getCallId());
            getChatManagement().removeValues(call.getChatid());
        }
    };

    private final Observer<Pair> sessionStatusObserver = callAndSession -> {
        MegaChatSession session = (MegaChatSession) callAndSession.second;
        int sessionStatus = session.getStatus();
        MegaChatCall call = (MegaChatCall) callAndSession.first;
        if (call == null)
            return;

        MegaChatRoom chat = megaChatApi.getChatRoom(call.getChatid());
        if (chat != null) {
            if (sessionStatus == MegaChatSession.SESSION_STATUS_IN_PROGRESS &&
                    (chat.isGroup() || chat.isMeeting() || session.getPeerid() != megaApi.getMyUserHandleBinary())) {
                Timber.d("Session is in progress");
                getChatManagement().setRequestSentCall(call.getCallId(), false);
                rtcAudioManagerGateway.updateRTCAudioMangerTypeStatus(AUDIO_MANAGER_CALL_IN_PROGRESS);
            }
        }
    };

    public static MegaApplication getInstance() {
        return singleApplicationInstance;
    }

    @Override
    public void onCreate() {
        singleApplicationInstance = this;

        super.onCreate();

        initialiseLogging();

        themeModeState.initialise();

        // Setup handler and RxJava for uncaught exceptions.
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> handleUncaughtException(e));
        RxJavaPlugins.setErrorHandler(this::handleUncaughtException);

        registerActivityLifecycleCallbacks(activityLifecycleHandler);

        isVerifySMSShowed = false;

        keepAliveHandler.postAtTime(keepAliveRunnable, System.currentTimeMillis() + interval);
        keepAliveHandler.postDelayed(keepAliveRunnable, interval);

        setupMegaApi();
        setupMegaApiFolder();
        setupMegaChatApi();

        LiveEventBus.config().enableLogger(false);

        storageState = dbH.getStorageState();

        //Logout check resumed pending transfers
        transfersManagement.checkResumedPendingTransfers();

        int apiServerValue = getSharedPreferences(API_SERVER_PREFERENCES, MODE_PRIVATE)
                .getInt(API_SERVER, PRODUCTION_SERVER_VALUE);

        if (apiServerValue != PRODUCTION_SERVER_VALUE) {
            if (apiServerValue == SANDBOX3_SERVER_VALUE) {
                megaApi.setPublicKeyPinning(false);
            }

            String apiServer = getApiServerFromValue(apiServerValue);
            megaApi.changeApiUrl(apiServer);
            megaApiFolder.changeApiUrl(apiServer);
        }

        boolean useHttpsOnly = false;
        if (dbH != null) {
            useHttpsOnly = Boolean.parseBoolean(dbH.getUseHttpsOnly());
            Timber.d("Value of useHttpsOnly: %s", useHttpsOnly);
            megaApi.useHttpsOnly(useHttpsOnly);
        }

        myAccountInfo.resetDefaults();

        if (dbH != null) {
            dbH.resetExtendedAccountDetailsTimestamp();
        }

        registerReceiver(new NetworkStateReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall.class).observeForever(callStatusObserver);
        LiveEventBus.get(EVENT_RINGING_STATUS_CHANGE, MegaChatCall.class).observeForever(callRingingStatusObserver);
        LiveEventBus.get(EVENT_SESSION_STATUS_CHANGE, Pair.class).observeForever(sessionStatusObserver);
        LiveEventBus.get(EVENT_CALL_COMPOSITION_CHANGE, MegaChatCall.class).observeForever(callCompositionObserver);

        logoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
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
        Timber.d("Use downloadable font for EmojiCompat");
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
                        Timber.d("EmojiCompat initialized");
                    }

                    @Override
                    public void onFailed(@Nullable Throwable throwable) {
                        Timber.w("EmojiCompat initialization failed");
                    }
                });
        EmojiCompat.init(config);

        // clear the cache files stored in the external cache folder.
        clearPublicCache(this);

        ContextUtils.initialize(getApplicationContext());

        initFresco();

    }

    private void initialiseLogging() {
        new InitialiseLoggingUseCaseJavaWrapper(initialiseLoggingUseCase).invokeUseCase(BuildConfig.DEBUG);
    }


    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }

    /**
     * Initialize Fresco library
     */
    private void initFresco() {
        long maxMemory = mega.privacy.android.app.utils.ContextUtils.getAvailableMemory(this);
        PoolFactory poolFactory = new PoolFactory(
                PoolConfig.newBuilder()
                        .setNativeMemoryChunkPoolParams(FrescoNativeMemoryChunkPoolParams.get(maxMemory))
                        .build()
        );

        Fresco.initialize(this, ImagePipelineConfig.newBuilder(this)
                .setPoolFactory(poolFactory)
                .setDownsampleEnabled(true)
                .build());
    }

    public void askForFullAccountInfo() {
        Timber.d("askForFullAccountInfo");
        megaApi.getPaymentMethods(null);

        if (storageState == MegaApiAndroid.STORAGE_STATE_UNKNOWN) {
            megaApi.getAccountDetails();
        } else {
            megaApi.getSpecificAccountDetails(false, true, true);
        }

        megaApi.getPricing(null);
        megaApi.creditCardQuerySubscriptions(null);
    }

    public void askForPricing() {
        megaApi.getPricing(null);
    }

    public void askForPaymentMethods() {
        Timber.d("askForPaymentMethods");
        megaApi.getPaymentMethods(null);
    }

    public void askForAccountDetails() {
        Timber.d("askForAccountDetails");
        if (dbH != null) {
            dbH.resetAccountDetailsTimeStamp();
        }
        megaApi.getAccountDetails(null);
    }

    public void askForCCSubscriptions() {

        megaApi.creditCardQuerySubscriptions(null);
    }

    public void askForExtendedAccountDetails() {
        Timber.d("askForExtendedAccountDetails");
        if (dbH != null) {
            dbH.resetExtendedAccountDetailsTimestamp();
        }
        megaApi.getExtendedAccountDetails(true, false, false, null);
    }

    public void refreshAccountInfo() {
        //Check if the call is recently
        if (callToAccountDetails() || myAccountInfo.getUsedFormatted().trim().length() <= 0) {
            Timber.d("megaApi.getAccountDetails SEND");
            askForAccountDetails();
        }

        if (callToExtendedAccountDetails()) {
            Timber.d("megaApi.getExtendedAccountDetails SEND");
            askForExtendedAccountDetails();
        }

        if (callToPaymentMethods()) {
            Timber.d("megaApi.getPaymentMethods SEND");
            askForPaymentMethods();
        }
    }

    public MegaApiAndroid getMegaApiFolder() {
        return megaApiFolder;
    }

    public void disableMegaChatApi() {
        try {
            if (megaChatApi != null) {
                megaChatApi.removeChatRequestListener(chatRequestHandler);
                megaChatApi.removeChatNotificationListener(megaChatNotificationHandler);
                megaChatApi.removeChatListener(globalChatListener);
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

        Timber.d("ADD REQUESTLISTENER");
        megaApi.addRequestListener(requestListener);

        megaApi.addGlobalListener(globalListener);

        setSDKLanguage();

        // Set the proper resource limit to try avoid issues when the number of parallel transfers is very big.
        final int DESIRABLE_R_LIMIT = 20000; // SDK team recommended value
        int currentLimit = megaApi.platformGetRLimitNumFile();
        Timber.d("Current resource limit is set to %s", currentLimit);
        if (currentLimit < DESIRABLE_R_LIMIT) {
            Timber.d("Resource limit is under desirable value. Trying to increase the resource limit...");
            if (!megaApi.platformSetRLimitNumFile(DESIRABLE_R_LIMIT)) {
                Timber.w("Error setting resource limit.");
            }

            // Check new resource limit after set it in order to see if had been set successfully to the
            // desired value or maybe to a lower value limited by the system.
            Timber.d("Resource limit is set to %s", megaApi.platformGetRLimitNumFile());
        }
    }

    /**
     * Set the language code used by the app.
     * Language code is from current system setting.
     * Need to distinguish simplified and traditional Chinese.
     */
    private void setSDKLanguage() {
        Locale locale = Locale.getDefault();
        String langCode;

        // If it's Chinese
        if (Locale.CHINESE.toLanguageTag().equals(locale.getLanguage())) {
            langCode = isSimplifiedChinese() ?
                    Locale.SIMPLIFIED_CHINESE.toLanguageTag() :
                    Locale.TRADITIONAL_CHINESE.toLanguageTag();
        } else {
            langCode = locale.toString();
        }

        boolean result = megaApi.setLanguage(langCode);

        if (!result) {
            langCode = locale.getLanguage();
            result = megaApi.setLanguage(langCode);
        }

        Timber.d("Result: %s Language: %s", result, langCode);
    }

    /**
     * Setup the MegaApiAndroid instance for folder link.
     */
    private void setupMegaApiFolder() {
        // If logged in set the account auth token
        if (megaApi.isLoggedIn() != 0) {
            Timber.d("Logged in. Setting account auth token for folder links.");
            megaApiFolder.setAccountAuth(megaApi.getAccountAuth());
        }

        megaApiFolder.retrySSLerrors(true);

        megaApiFolder.setDownloadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
        megaApiFolder.setUploadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
    }

    private void setupMegaChatApi() {
        if (!registeredChatListeners) {
            Timber.d("Add listeners of megaChatApi");
            megaChatApi.addChatRequestListener(chatRequestHandler);
            megaChatApi.addChatNotificationListener(megaChatNotificationHandler);
            megaChatApi.addChatListener(globalChatListener);
            megaChatApi.addChatCallListener(meetingListener);
            registeredChatListeners = true;
            checkCallSounds();
        }
    }

    /**
     * Check the changes of the meeting to play the right sound
     */
    private void checkCallSounds() {
        getCallSoundsUseCase.get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((next) -> soundsController.playSound(next));
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
                        boolean analyticsCookiesEnabled = cookies.contains(CookieType.ANALYTICS);
                        crashReporter.setEnabled(analyticsCookiesEnabled);
                        performanceReporter.setEnabled(analyticsCookiesEnabled);
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
        return dbH;
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
        setLoggingOut(false);
    }

    public static boolean isLoggingOut() {
        return isLoggingOut;
    }

    public static void setLoggingOut(boolean loggingOut) {
        isLoggingOut = loggingOut;
    }

    public static boolean isIsHeartBeatAlive() {
        return isHeartBeatAlive;
    }

    public static void setHeartBeatAlive(boolean heartBeatAlive) {
        isHeartBeatAlive = heartBeatAlive;
    }

    public static void setOpenChatId(long openChatId) {
        MegaApplication.openChatId = openChatId;
    }

    public static long getOpenCallChatId() {
        return openCallChatId;
    }

    public static void setOpenCallChatId(long value) {
        Timber.d("New open call chat ID: %s", value);
        openCallChatId = value;
    }

    public boolean isRecentChatVisible() {
        if (activityLifecycleHandler.isActivityVisible()) {
            return recentChatVisible;
        } else {
            return false;
        }
    }

    public static void setRecentChatVisible(boolean recentChatVisible) {
        Timber.d("setRecentChatVisible: %s", recentChatVisible);
        MegaApplication.recentChatVisible = recentChatVisible;
    }

    public static long getOpenChatId() {
        return openChatId;
    }

    public String getLocalIpAddress() {
        return localIpAddress;
    }

    public void setLocalIpAddress(String ip) {
        localIpAddress = ip;
    }

    public void sendSignalPresenceActivity() {
        Timber.d("sendSignalPresenceActivity");
        if (megaChatApi != null) {
            if (megaChatApi.isSignalActivityRequired()) {
                megaChatApi.signalPresenceActivity();
            }
        }
    }

    /**
     * Method for showing an incoming group or one-to-one call notification.
     *
     * @param incomingCall The incoming call
     */
    public void showOneCallNotification(MegaChatCall incomingCall) {
        Timber.d("Show incoming call notification and start to sound. Chat ID is %s", incomingCall.getChatid());
        createOrUpdateAudioManager(false, AUDIO_MANAGER_CALL_RINGING);
        getChatManagement().addNotificationShown(incomingCall.getChatid());
        ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this);
        notificationBuilder.showOneCallNotification(incomingCall);
    }

    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {
        if (!item.isGroup())
            return;

        if ((item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV))) {
            if (item.getOwnPrivilege() != MegaChatRoom.PRIV_RM) {
                getChatManagement().checkActiveGroupChat(item.getChatId());
            } else {
                getChatManagement().removeActiveChatAndNotificationShown(item.getChatId());
            }
        }

        if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_CLOSED)) {
            getChatManagement().removeActiveChatAndNotificationShown(item.getChatId());
        }
    }

    public void checkOneCall(long incomingCallChatId) {
        Timber.d("One call : Chat ID is %d, openCall Chat ID is %d", incomingCallChatId, openCallChatId);
        if (openCallChatId == incomingCallChatId) {
            Timber.d("The call is already opened");
            return;
        }

        MegaChatCall callToLaunch = megaChatApi.getChatCall(incomingCallChatId);
        if (callToLaunch == null) {
            Timber.w("Call is null");
            return;
        }

        int callStatus = callToLaunch.getStatus();
        if (callStatus > MegaChatCall.CALL_STATUS_IN_PROGRESS) {
            Timber.w("Launch not in correct status: %s", callStatus);
            return;
        }

        MegaChatRoom chatRoom = megaChatApi.getChatRoom(incomingCallChatId);
        if (chatRoom == null) {
            Timber.w("Chat room is null");
            return;
        }

        if (!CallUtil.isOneToOneCall(chatRoom) && callToLaunch.getStatus() == CALL_STATUS_USER_NO_PRESENT && callToLaunch.isRinging() && (!getChatManagement().isOpeningMeetingLink(incomingCallChatId))) {
            Timber.d("Group call or meeting, the notification should be displayed");
            showOneCallNotification(callToLaunch);
            return;
        }

        checkOneToOneIncomingCall(callToLaunch);
    }

    /**
     * Check whether an incoming 1-to-1 call should show notification or incoming call screen
     *
     * @param callToLaunch The incoming call
     */
    private void checkOneToOneIncomingCall(MegaChatCall callToLaunch) {
        if (shouldNotify(this) && !activityLifecycleHandler.isActivityVisible()) {
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ":MegaIncomingCallPowerLock");
            }
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(10 * 1000);
            }

            Timber.d("The notification should be displayed. Chat ID of incoming call %s", callToLaunch.getChatid());
            showOneCallNotification(callToLaunch);
        } else {
            Timber.d("The call screen should be displayed. Chat ID of incoming call %s", callToLaunch.getChatid());
            MegaApplication.getInstance().createOrUpdateAudioManager(false, AUDIO_MANAGER_CALL_RINGING);
            launchCallActivity(callToLaunch);
        }
    }

    public void checkSeveralCall(MegaHandleList listAllCalls, int callStatus, boolean isRinging, long incomingCallChatId) {
        Timber.d("Several calls = %d- Current call Status: %s", listAllCalls.size(), callStatusToString(callStatus));
        if (isRinging) {
            if (participatingInACall()) {
                Timber.d("Several calls: show notification");
                checkQueuedCalls(incomingCallChatId);
                return;
            }

            MegaChatRoom chatRoom = megaChatApi.getChatRoom(incomingCallChatId);
            if (callStatus == CALL_STATUS_USER_NO_PRESENT && chatRoom != null) {
                if (!CallUtil.isOneToOneCall(chatRoom) && !getChatManagement().isOpeningMeetingLink(incomingCallChatId)) {
                    Timber.d("Show incoming group call notification");
                    MegaChatCall incomingCall = megaChatApi.getChatCall(incomingCallChatId);
                    if (incomingCall != null) {
                        showOneCallNotification(incomingCall);
                    }
                    return;
                }

                if (CallUtil.isOneToOneCall(chatRoom) && openCallChatId != chatRoom.getChatId()) {
                    Timber.d("Show incoming one to one call screen");
                    MegaChatCall callToLaunch = megaChatApi.getChatCall(chatRoom.getChatId());
                    checkOneToOneIncomingCall(callToLaunch);
                    return;
                }
            }
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
                Timber.d("The call is already opened");
            }
        }

        if (callToLaunch != null) {
            checkOneToOneIncomingCall(callToLaunch);
        }
    }

    private void checkCallDestroyed(long chatId, long callId, int endCallReason, boolean isIgnored) {
        getChatManagement().setOpeningMeetingLink(chatId, false);

        if (shouldNotify(this)) {
            toSystemSettingNotification(this);
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        getChatManagement().removeNotificationShown(chatId);

        try {
            if (endCallReason == MegaChatCall.END_CALL_REASON_NO_ANSWER && !isIgnored) {
                MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
                if (chatRoom != null && !chatRoom.isGroup() && !chatRoom.isMeeting() && megaApi.isChatNotifiable(chatId)) {
                    try {
                        Timber.d("Show missed call notification");
                        ChatAdvancedNotificationBuilder.newInstance(this)
                                .showMissedCallNotification(chatId, callId);
                    } catch (Exception e) {
                        Timber.e(e, "EXCEPTION when showing missed call notification");
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "EXCEPTION when showing missed call notification");
        }
    }

    public void createOrUpdateAudioManager(boolean isSpeakerOn, int type) {
        Timber.d("Create or update audio manager, type is %s", type);
        chatManagement.registerScreenReceiver();
        rtcAudioManagerGateway.createOrUpdateAudioManager(isSpeakerOn, type);
    }

    /**
     * Remove the incoming call AppRTCAudioManager.
     */
    public void removeRTCAudioManagerRingIn() {
        rtcAudioManagerGateway.removeRTCAudioManagerRingIn();
    }

    /**
     * Activate the proximity sensor.
     */
    public void startProximitySensor() {
        rtcAudioManagerGateway.startProximitySensor(isNear -> {
            chatManagement.controlProximitySensor(isNear);
        });
    }

    public void openCallService(long chatId) {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            Timber.d("Start call Service. Chat iD = %s", chatId);
            Intent intentService = new Intent(this, CallService.class);
            intentService.putExtra(CHAT_ID, chatId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.startForegroundService(intentService);
            } else {
                this.startService(intentService);
            }
        }
    }

    public void checkQueuedCalls(long incomingCallChatId) {
        try {
            ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this);
            notificationBuilder.checkQueuedCalls(incomingCallChatId);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void launchCallActivity(MegaChatCall call) {
        Timber.d("Show the call screen: %s, callId = %d", callStatusToString(call.getStatus()), call.getCallId());
        openMeetingRinging(this, call.getChatid(), passcodeManagement);
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
        if (disableFileVersions) {
            MegaApplication.disableFileVersions = 1;
        } else {
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
        myAccountInfo.resetDefaults();
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

    public static PushNotificationSettingManagement getPushNotificationSettingManagement() {
        return getInstance().pushNotificationSettingManagement;
    }

    public static ChatManagement getChatManagement() {
        return getInstance().chatManagement;
    }

    public Activity getCurrentActivity() {
        return activityLifecycleHandler.getCurrentActivity();
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
}
