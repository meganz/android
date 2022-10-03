package mega.privacy.android.app;

import static mega.privacy.android.app.utils.CacheFolderManager.clearPublicCache;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.API_SERVER;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.API_SERVER_PREFERENCES;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.PRODUCTION_SERVER_VALUE;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.SANDBOX3_SERVER_VALUE;
import static mega.privacy.android.app.utils.ChangeApiServerUtil.getApiServerFromValue;
import static mega.privacy.android.app.utils.Constants.ACTION_LOG_OUT;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.DBUtil.callToAccountDetails;
import static mega.privacy.android.app.utils.DBUtil.callToExtendedAccountDetails;
import static mega.privacy.android.app.utils.DBUtil.callToPaymentMethods;
import static mega.privacy.android.app.utils.Util.isSimplifiedChinese;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.provider.FontRequest;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;
import androidx.work.Configuration;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.memory.PoolConfig;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.jeremyliao.liveeventbus.LiveEventBus;

import org.webrtc.ContextUtils;

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
import mega.privacy.android.domain.qualifier.ApplicationScope;
import mega.privacy.android.app.di.MegaApi;
import mega.privacy.android.app.di.MegaApiFolder;
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType;
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.GetCookieSettingsUseCase;
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler;
import mega.privacy.android.app.globalmanagement.BackgroundRequestListener;
import mega.privacy.android.app.globalmanagement.CallChangesObserver;
import mega.privacy.android.app.globalmanagement.MegaChatNotificationHandler;
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler;
import mega.privacy.android.app.globalmanagement.MyAccountInfo;
import mega.privacy.android.app.globalmanagement.SortOrderManagement;
import mega.privacy.android.app.globalmanagement.TransfersManagement;
import mega.privacy.android.app.listeners.GlobalChatListener;
import mega.privacy.android.app.listeners.GlobalListener;
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
import mega.privacy.android.app.utils.FrescoNativeMemoryChunkPoolParams;
import mega.privacy.android.domain.usecase.InitialiseLogging;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;
import timber.log.Timber;

@HiltAndroidApp
public class MegaApplication extends MultiDexApplication implements Configuration.Provider, DefaultLifecycleObserver {

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
    @Inject
    CallChangesObserver callChangesObserver;

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

    private MeetingListener meetingListener = new MeetingListener();
    private GlobalChatListener globalChatListener = new GlobalChatListener(this);

    private final CallSoundsController soundsController = new CallSoundsController();

    public static void smsVerifyShowed(boolean isShowed) {
        isVerifySMSShowed = isShowed;
    }

    public void handleUncaughtException(Throwable throwable) {
        Timber.e(throwable, "UNCAUGHT EXCEPTION");
        crashReporter.report(throwable);
    }

    public static MegaApplication getInstance() {
        return singleApplicationInstance;
    }

    @Override
    public void onCreate() {
        singleApplicationInstance = this;

        super.onCreate();

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        initialiseLogging();

        themeModeState.initialise();
        callChangesObserver.init();

        // Setup handler and RxJava for uncaught exceptions.
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> handleUncaughtException(e));
        RxJavaPlugins.setErrorHandler(this::handleUncaughtException);

        registerActivityLifecycleCallbacks(activityLifecycleHandler);

        isVerifySMSShowed = false;

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

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        int backgroundStatus = megaChatApi.getBackgroundStatus();
        Timber.d("Application start with backgroundStatus: %s", backgroundStatus);
        if (backgroundStatus != -1 && backgroundStatus != 0) {
            megaChatApi.setBackgroundStatus(false);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        int backgroundStatus = megaChatApi.getBackgroundStatus();
        Timber.d("Application stop with backgroundStatus: %s", backgroundStatus);
        if (backgroundStatus != -1 && backgroundStatus != 1) {
            megaChatApi.setBackgroundStatus(true);
        }
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
        callChangesObserver.showOneCallNotification(incomingCall);
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
        callChangesObserver.checkOneCall(incomingCallChatId);
    }

    public void checkSeveralCall(MegaHandleList listAllCalls, int callStatus, boolean isRinging, long incomingCallChatId) {
        callChangesObserver.checkSeveralCall(listAllCalls, callStatus, isRinging, incomingCallChatId);
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
