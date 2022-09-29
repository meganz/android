package mega.privacy.android.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.anggrayudi.storage.extension.toInt
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.memory.PoolConfig
import com.facebook.imagepipeline.memory.PoolFactory
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.HiltAndroidApp
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.components.PushNotificationSettingManagement
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.di.MegaApiFolder
import mega.privacy.android.app.fragments.settingsFragments.cookie.data.CookieType
import mega.privacy.android.app.fragments.settingsFragments.cookie.usecase.GetCookieSettingsUseCase
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.globalmanagement.BackgroundRequestListener
import mega.privacy.android.app.globalmanagement.CallChangesObserver
import mega.privacy.android.app.globalmanagement.MegaChatNotificationHandler
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.listeners.GlobalChatListener
import mega.privacy.android.app.listeners.GlobalListener
import mega.privacy.android.app.meeting.CallService
import mega.privacy.android.app.meeting.CallSoundType
import mega.privacy.android.app.meeting.CallSoundsController
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.MeetingListener
import mega.privacy.android.app.middlelayer.reporter.CrashReporter
import mega.privacy.android.app.middlelayer.reporter.PerformanceReporter
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.theme.ThemeModeState
import mega.privacy.android.app.receivers.NetworkStateReceiver
import mega.privacy.android.app.usecase.call.GetCallSoundsUseCase
import mega.privacy.android.app.utils.CacheFolderManager.clearPublicCache
import mega.privacy.android.app.utils.ChangeApiServerUtil
import mega.privacy.android.app.utils.ChangeApiServerUtil.getApiServerFromValue
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContextUtils.getAvailableMemory
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.app.utils.FrescoNativeMemoryChunkPoolParams.get
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.InitialiseLogging
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaHandleList
import org.webrtc.ContextUtils
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * Mega application
 *
 * @property megaApi
 * @property megaApiFolder
 * @property megaChatApi
 * @property dbH
 * @property getCookieSettingsUseCase
 * @property myAccountInfo
 * @property passcodeManagement
 * @property crashReporter
 * @property performanceReporter
 * @property initialiseLoggingUseCase
 * @property getCallSoundsUseCase
 * @property workerFactory
 * @property themeModeState
 * @property transfersManagement
 * @property activityLifecycleHandler
 * @property globalListener
 * @property megaChatNotificationHandler
 * @property pushNotificationSettingManagement
 * @property chatManagement
 * @property requestListener
 * @property chatRequestHandler
 * @property rtcAudioManagerGateway
 * @property callChangesObserver
 * @property globalChatListener
 * @property localIpAddress
 * @property isEsid
 * @property storageState
 */
@HiltAndroidApp
class MegaApplication : MultiDexApplication(), Configuration.Provider, DefaultLifecycleObserver {
    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @MegaApiFolder
    @Inject
    lateinit var megaApiFolder: MegaApiAndroid

    @Inject
    @get:JvmName("megaChatApi")
    lateinit var megaChatApi: MegaChatApiAndroid

    @Inject
    lateinit var dbH: DatabaseHandler

    @Inject
    lateinit var getCookieSettingsUseCase: GetCookieSettingsUseCase

    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    @Inject
    lateinit var passcodeManagement: PasscodeManagement

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var performanceReporter: PerformanceReporter

    @Inject
    lateinit var initialiseLoggingUseCase: InitialiseLogging

    @Inject
    lateinit var getCallSoundsUseCase: GetCallSoundsUseCase

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var themeModeState: ThemeModeState

    @Inject
    lateinit var transfersManagement: TransfersManagement

    @Inject
    lateinit var activityLifecycleHandler: ActivityLifecycleHandler

    @Inject
    lateinit var globalListener: GlobalListener

    @Inject
    lateinit var megaChatNotificationHandler: MegaChatNotificationHandler

    @Inject
    @get:JvmName("pushNotificationSettingManagement")
    lateinit var pushNotificationSettingManagement: PushNotificationSettingManagement

    @Inject
    @get:JvmName("chatManagement")
    lateinit var chatManagement: ChatManagement

    @Inject
    lateinit var requestListener: BackgroundRequestListener

    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    @Inject
    lateinit var rtcAudioManagerGateway: RTCAudioManagerGateway

    @Inject
    lateinit var callChangesObserver: CallChangesObserver

    @Inject
    lateinit var globalChatListener: GlobalChatListener

    @Inject
    lateinit var monitorStorageStateEvent: MonitorStorageStateEvent

    var localIpAddress: String? = ""

    var isEsid = false

    var storageState = MegaApiJava.STORAGE_STATE_UNKNOWN //Default value

    private val logoutReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Constants.ACTION_LOG_OUT) {
                storageState = MegaApiJava.STORAGE_STATE_UNKNOWN //Default value
            }
        }
    }
    private val meetingListener = MeetingListener()
    private val soundsController = CallSoundsController()

    private fun handleUncaughtException(throwable: Throwable?) {
        Timber.e(throwable, "UNCAUGHT EXCEPTION")
        crashReporter.report(throwable ?: return)
    }

    /**
     * On create
     *
     */
    override fun onCreate() {
        instance = this
        super<MultiDexApplication>.onCreate()
        enableStrictMode()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        themeModeState.initialise()
        callChangesObserver.init()
        LiveEventBus.config().enableLogger(false)

        // Setup handler and RxJava for uncaught exceptions.
        Thread.setDefaultUncaughtExceptionHandler { _: Thread?, e: Throwable? ->
            handleUncaughtException(e)
        }
        RxJavaPlugins.setErrorHandler { throwable: Throwable? -> handleUncaughtException(throwable) }

        registerActivityLifecycleCallbacks(activityLifecycleHandler)
        isVerifySMSShowed = false

        setupMegaApi()
        setupMegaApiFolder()
        setupMegaChatApi()

        storageState = dbH.storageState

        //Logout check resumed pending transfers
        transfersManagement.checkResumedPendingTransfers()
        val apiServerValue =
            getSharedPreferences(ChangeApiServerUtil.API_SERVER_PREFERENCES, MODE_PRIVATE)
                .getInt(ChangeApiServerUtil.API_SERVER, ChangeApiServerUtil.PRODUCTION_SERVER_VALUE)
        if (apiServerValue != ChangeApiServerUtil.PRODUCTION_SERVER_VALUE) {
            if (apiServerValue == ChangeApiServerUtil.SANDBOX3_SERVER_VALUE) {
                megaApi.setPublicKeyPinning(false)
            }
            val apiServer = getApiServerFromValue(apiServerValue)
            megaApi.changeApiUrl(apiServer)
            megaApiFolder.changeApiUrl(apiServer)
        }

        val useHttpsOnly = java.lang.Boolean.parseBoolean(dbH.useHttpsOnly)
        Timber.d("Value of useHttpsOnly: %s", useHttpsOnly)
        megaApi.useHttpsOnly(useHttpsOnly)
        myAccountInfo.resetDefaults()
        dbH.resetExtendedAccountDetailsTimestamp()

        @Suppress("DEPRECATION")
        registerReceiver(NetworkStateReceiver(),
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        registerReceiver(logoutReceiver, IntentFilter(Constants.ACTION_LOG_OUT))

        // clear the cache files stored in the external cache folder.
        clearPublicCache(this)
        ContextUtils.initialize(applicationContext)
        initFresco()
    }

    /**
     * On start
     *
     */
    override fun onStart(owner: LifecycleOwner) {
        val backgroundStatus = megaChatApi.backgroundStatus
        Timber.d("Application start with backgroundStatus: %s", backgroundStatus)
        if (backgroundStatus != -1 && backgroundStatus != 0) {
            megaChatApi.setBackgroundStatus(false)
        }
    }

    /**
     * On stop
     *
     */
    override fun onStop(owner: LifecycleOwner) {
        val backgroundStatus = megaChatApi.backgroundStatus
        Timber.d("Application stop with backgroundStatus: %s", backgroundStatus)
        if (backgroundStatus != -1 && backgroundStatus != 1) {
            megaChatApi.setBackgroundStatus(true)
        }
    }

    private fun enableStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder() // Other StrictMode checks that you've previously added.
                    .detectUnsafeIntentLaunch()
                    .penaltyLog()
                    .build())
            }
        }
    }

    /**
     * Get work manager configuration
     *
     */
    override fun getWorkManagerConfiguration(): Configuration = Configuration.Builder()
        .setWorkerFactory(workerFactory)
        .build()

    /**
     * Initialize Fresco library
     */
    private fun initFresco() {
        val poolFactory = PoolFactory(
            PoolConfig.newBuilder()
                .setNativeMemoryChunkPoolParams(get(getAvailableMemory()))
                .build()
        )
        Fresco.initialize(this, ImagePipelineConfig.newBuilder(this)
            .setPoolFactory(poolFactory)
            .setDownsampleEnabled(true)
            .build())
    }

    /**
     * Ask for full account info
     *
     */
    fun askForFullAccountInfo() {
        Timber.d("askForFullAccountInfo")
        megaApi.run {
            getPaymentMethods(null)
            if (monitorStorageStateEvent.getState() == StorageState.Unknown) {
                getAccountDetails()
            } else {
                getSpecificAccountDetails(false, true, true)
            }
            getPricing(null)
            creditCardQuerySubscriptions(null)
        }
    }

    /**
     * Ask for pricing
     *
     */
    fun askForPricing() = megaApi.getPricing(null)

    /**
     * Ask for payment methods
     *
     */
    fun askForPaymentMethods() {
        Timber.d("askForPaymentMethods")
        megaApi.getPaymentMethods(null)
    }

    /**
     * Ask for account details
     *
     */
    fun askForAccountDetails() {
        Timber.d("askForAccountDetails")
        dbH.resetAccountDetailsTimeStamp()
        megaApi.getAccountDetails(null)
    }

    /**
     * Ask for credit card subscriptions
     *
     */
    fun askForCCSubscriptions() = megaApi.creditCardQuerySubscriptions(null)

    /**
     * Ask for extended account details
     *
     */
    fun askForExtendedAccountDetails() {
        Timber.d("askForExtendedAccountDetails")
        dbH.resetExtendedAccountDetailsTimestamp()
        megaApi.getExtendedAccountDetails(true, false, false, null)
    }

    /**
     * Refresh account info
     *
     */
    fun refreshAccountInfo() {
        //Check if the call is recently
        if (DBUtil.callToAccountDetails() || myAccountInfo.usedFormatted.trim().isEmpty()) {
            Timber.d("megaApi.getAccountDetails SEND")
            askForAccountDetails()
        }
        if (DBUtil.callToExtendedAccountDetails()) {
            Timber.d("megaApi.getExtendedAccountDetails SEND")
            askForExtendedAccountDetails()
        }
        if (DBUtil.callToPaymentMethods()) {
            Timber.d("megaApi.getPaymentMethods SEND")
            askForPaymentMethods()
        }
    }

    /**
     * Disable mega chat api
     *
     */
    fun disableMegaChatApi() {
        try {
            megaChatApi.apply {
                removeChatRequestListener(chatRequestHandler)
                removeChatNotificationListener(megaChatNotificationHandler)
                removeChatListener(globalChatListener)
                removeChatCallListener(meetingListener)
            }
            registeredChatListeners = false
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun setupMegaApi() {
        megaApi.apply {
            Timber.d("ADD REQUEST LISTENER")
            retrySSLerrors(true)
            downloadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
            uploadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
            addRequestListener(requestListener)
            addGlobalListener(globalListener)
        }
        setSDKLanguage()

        // Set the proper resource limit to try avoid issues when the number of parallel transfers is very big.
        val desirableRLimit = 20000 // SDK team recommended value
        val currentLimit = megaApi.platformGetRLimitNumFile()
        Timber.d("Current resource limit is set to %s", currentLimit)
        if (currentLimit < desirableRLimit) {
            Timber.d("Resource limit is under desirable value. Trying to increase the resource limit...")
            if (!megaApi.platformSetRLimitNumFile(desirableRLimit)) {
                Timber.w("Error setting resource limit.")
            }

            // Check new resource limit after set it in order to see if had been set successfully to the
            // desired value or maybe to a lower value limited by the system.
            Timber.d("Resource limit is set to %s", megaApi.platformGetRLimitNumFile())
        }
    }

    /**
     * Set the language code used by the app.
     * Language code is from current system setting.
     * Need to distinguish simplified and traditional Chinese.
     */
    private fun setSDKLanguage() {
        val locale = Locale.getDefault()
        var langCode: String?

        // If it's Chinese
        langCode = if (Locale.CHINESE.toLanguageTag() == locale.language) {
            if (Util.isSimplifiedChinese()) Locale.SIMPLIFIED_CHINESE.toLanguageTag() else Locale.TRADITIONAL_CHINESE.toLanguageTag()
        } else {
            locale.toString()
        }
        var result = megaApi.setLanguage(langCode)
        if (!result) {
            langCode = locale.language
            result = megaApi.setLanguage(langCode)
        }
        Timber.d("Result: $result Language: $langCode")
    }

    /**
     * Setup the MegaApiAndroid instance for folder link.
     */
    private fun setupMegaApiFolder() {
        // If logged in set the account auth token
        megaApiFolder.apply {
            if (megaApi.isLoggedIn != 0) {
                Timber.d("Logged in. Setting account auth token for folder links.")
                accountAuth = megaApi.accountAuth
            }
            retrySSLerrors(true)
            downloadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
            uploadMethod = MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE
        }
    }

    private fun setupMegaChatApi() {
        if (!registeredChatListeners) {
            Timber.d("Add listeners of megaChatApi")
            megaChatApi.apply {
                addChatRequestListener(chatRequestHandler)
                addChatNotificationListener(megaChatNotificationHandler)
                addChatListener(globalChatListener)
                addChatCallListener(meetingListener)
            }
            registeredChatListeners = true
            checkCallSounds()
        }
    }

    /**
     * Check the changes of the meeting to play the right sound
     */
    private fun checkCallSounds() {
        getCallSoundsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { next: CallSoundType ->
                soundsController.playSound(next)
            }
    }

    /**
     * Check current enabled cookies and set the corresponding flags to true/false
     */
    fun checkEnabledCookies() {
        getCookieSettingsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { cookies: Set<CookieType?>, throwable: Throwable? ->
                if (throwable == null) {
                    val analyticsCookiesEnabled = cookies.contains(CookieType.ANALYTICS)
                    crashReporter.setEnabled(analyticsCookiesEnabled)
                    performanceReporter.setEnabled(analyticsCookiesEnabled)
                }
            }
    }

    /**
     * Get mega chat api
     *
     */
    fun getMegaChatApi(): MegaChatApiAndroid {
        setupMegaChatApi()
        return megaChatApi
    }

    /**
     * Is recent chat visible
     */
    val isRecentChatVisible: Boolean
        get() = if (activityLifecycleHandler.isActivityVisible) {
            recentChatVisible
        } else {
            false
        }

    /**
     * Send signal presence activity
     *
     */
    fun sendSignalPresenceActivity() {
        Timber.d("sendSignalPresenceActivity")
        megaChatApi.run {
            if (isSignalActivityRequired) {
                signalPresenceActivity()
            }
        }
    }

    /**
     * Method for showing an incoming group or one-to-one call notification.
     *
     * @param incomingCall The incoming call
     */
    fun showOneCallNotification(incomingCall: MegaChatCall) =
        callChangesObserver.showOneCallNotification(incomingCall)

    /**
     * Check one call
     *
     * @param incomingCallChatId
     */
    fun checkOneCall(incomingCallChatId: Long) =
        callChangesObserver.checkOneCall(incomingCallChatId)

    /**
     * Check several call
     *
     * @param listAllCalls
     * @param callStatus
     * @param isRinging
     * @param incomingCallChatId
     */
    fun checkSeveralCall(
        listAllCalls: MegaHandleList?,
        callStatus: Int,
        isRinging: Boolean,
        incomingCallChatId: Long,
    ) {
        listAllCalls?.let {
            callChangesObserver.checkSeveralCall(
                it,
                callStatus,
                isRinging,
                incomingCallChatId
            )
        }
    }

    /**
     * Create or update audio manager
     *
     * @param isSpeakerOn
     * @param type
     */
    fun createOrUpdateAudioManager(isSpeakerOn: Boolean, type: Int) {
        Timber.d("Create or update audio manager, type is %s", type)
        chatManagement.registerScreenReceiver()
        rtcAudioManagerGateway.createOrUpdateAudioManager(isSpeakerOn, type)
    }

    /**
     * Remove the incoming call AppRTCAudioManager.
     */
    fun removeRTCAudioManagerRingIn() = rtcAudioManagerGateway.removeRTCAudioManagerRingIn()

    /**
     * Activate the proximity sensor.
     */
    fun startProximitySensor() = rtcAudioManagerGateway.startProximitySensor { isNear: Boolean ->
        chatManagement.controlProximitySensor(isNear)
    }

    /**
     * Open call service
     *
     * @param chatId
     */
    fun openCallService(chatId: Long) {
        if (chatId != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            Timber.d("Start call Service. Chat iD = $chatId")
            Intent(this, CallService::class.java).run {
                putExtra(Constants.CHAT_ID, chatId)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(this)
                } else {
                    startService(this)
                }
            }
        }
    }

    /**
     * Reset my account info
     *
     */
    fun resetMyAccountInfo() = myAccountInfo.resetDefaults()

    /**
     * Current activity
     */
    val currentActivity: Activity?
        get() = activityLifecycleHandler.getCurrentActivity()

    companion object {
        /**
         * App Key
         */
        const val APP_KEY = "6tioyn8ka5l6hty"

        /**
         * Is logging in
         */
        @JvmStatic
        var isLoggingIn = false
            set(loggingIn) {
                field = loggingIn
                isLoggingOut = false
            }

        /**
         * Is logging out
         */
        @JvmStatic
        var isLoggingOut = false

        /**
         * Is is heart beat alive
         */
        @JvmStatic
        var isIsHeartBeatAlive = false
            private set

        /**
         * Is show info chat messages
         */
        @JvmStatic
        var isShowInfoChatMessages = false

        /**
         * Open chat id
         */
        @JvmStatic
        var openChatId: Long = -1

        /**
         * Is closed chat
         */
        @JvmStatic
        var isClosedChat = true

        /**
         * Is show rich link warning
         */
        @JvmStatic
        var isShowRichLinkWarning = false

        /**
         * Counter not now rich link warning
         */
        @JvmStatic
        var counterNotNowRichLinkWarning = -1

        /**
         * Is enabled rich links
         */
        var isEnabledRichLinks = false

        /**
         * Is enabled geo location
         */
        @JvmStatic
        var isEnabledGeoLocation = false

        /**
         * Is disable file versions
         */
        @JvmStatic
        var isDisableFileVersions = -1
            private set

        private var recentChatVisible = false

        /**
         * Url confirmation link
         */
        @JvmStatic
        var urlConfirmationLink: String? = null

        private var registeredChatListeners = false

        /**
         * Is verify s m s showed
         */
        var isVerifySMSShowed = false
            private set

        /**
         * Is blocked due to weak account
         */
        @JvmStatic
        var isBlockedDueToWeakAccount = false

        /**
         * Is web open due to email verification
         */
        var isWebOpenDueToEmailVerification = false
            private set

        /**
         * Is waiting for call
         */
        @JvmStatic
        var isWaitingForCall = false

        /**
         * User waiting for call
         */
        @JvmStatic
        var userWaitingForCall = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        private lateinit var instance: MegaApplication

        /**
         * Get instance
         */
        @JvmStatic
        fun getInstance(): MegaApplication = instance

        /**
         * Sms verify showed
         *
         * @param isShowed
         */
        @JvmStatic
        fun smsVerifyShowed(isShowed: Boolean) {
            isVerifySMSShowed = isShowed
        }

        /**
         * Set is web open due to email verification
         *
         * @param isWebOpenDueToEmailVerification
         */
        @JvmStatic
        fun setIsWebOpenDueToEmailVerification(isWebOpenDueToEmailVerification: Boolean) {
            this.isWebOpenDueToEmailVerification = isWebOpenDueToEmailVerification
        }

        /**
         * Set heart beat alive
         *
         * @param heartBeatAlive
         */
        @JvmStatic
        fun setHeartBeatAlive(heartBeatAlive: Boolean) {
            isIsHeartBeatAlive = heartBeatAlive
        }

        /**
         * Set recent chat visible
         *
         * @param recentChatVisible
         */
        @JvmStatic
        fun setRecentChatVisible(recentChatVisible: Boolean) {
            Timber.d("setRecentChatVisible: %s", recentChatVisible)
            this.recentChatVisible = recentChatVisible
        }

        /**
         * Set disable file versions
         *
         * @param disableFileVersions
         */
        @JvmStatic
        fun setDisableFileVersions(disableFileVersions: Boolean) {
            isDisableFileVersions = disableFileVersions.toInt()
        }

        /**
         * Get push notification setting management
         */
        @JvmStatic
        fun getPushNotificationSettingManagement(): PushNotificationSettingManagement =
            instance.pushNotificationSettingManagement

        /**
         * Get chat management
         */
        @JvmStatic
        fun getChatManagement(): ChatManagement = instance.chatManagement
    }
}