package mega.privacy.android.app

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.Lazy
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.components.PushNotificationSettingManagement
import mega.privacy.android.app.fcm.FcmManager
import mega.privacy.android.app.fetcher.MegaAvatarFetcher
import mega.privacy.android.app.fetcher.MegaAvatarKeyer
import mega.privacy.android.app.fetcher.MegaThumbnailFetcher
import mega.privacy.android.app.fetcher.MegaThumbnailKeyer
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.globalmanagement.CallChangesObserver
import mega.privacy.android.app.globalmanagement.MegaChatNotificationHandler
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.GlobalChatListener
import mega.privacy.android.app.meeting.CallService
import mega.privacy.android.app.meeting.CallSoundType
import mega.privacy.android.app.meeting.CallSoundsController
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.MeetingListener
import mega.privacy.android.app.presentation.theme.ThemeModeState
import mega.privacy.android.app.receivers.GlobalNetworkStateHandler
import mega.privacy.android.app.usecase.call.MonitorCallSoundsUseCase
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.greeter.Greeter
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.logging.Log
import mega.privacy.android.domain.logging.Logger
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.apiserver.UpdateApiServerUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.GetMiscFlagsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorAndHandleTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorTransferEventsToStartWorkersIfNeededUseCase
import mega.privacy.android.feature_flags.AppFeatures
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import okhttp3.OkHttpClient
import org.webrtc.ContextUtils
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Mega application
 *
 * @property megaApi
 * @property megaApiFolder
 * @property megaChatApi
 * @property _dbH
 * @property getMiscFlagsUseCase
 * @property isUserLoggedInUseCase
 * @property myAccountInfo
 * @property crashReporter
 * @property updateCrashAndPerformanceReportersUseCase
 * @property monitorCallSoundsUseCase
 * @property themeModeState
 * @property activityLifecycleHandler
 * @property megaChatNotificationHandler
 * @property pushNotificationSettingManagement
 * @property chatManagement
 * @property chatRequestHandler
 * @property rtcAudioManagerGateway
 * @property callChangesObserver
 * @property globalChatListener
 * @property localIpAddress
 * @property globalNetworkStateHandler
 * @property monitorAndHandleTransferEventsUseCase
 * @property monitorTransferEventsToStartWorkersIfNeededUseCase
 * @property applicationScope
 */
@HiltAndroidApp
class MegaApplication : MultiDexApplication(), DefaultLifecycleObserver,
    SingletonImageLoader.Factory, Configuration.Provider {
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
    lateinit var _dbH: Lazy<LegacyDatabaseHandler>

    /**
     * Database handler
     */
    val dbH: LegacyDatabaseHandler
        get() {
            return _dbH.get()
        }

    @Inject
    lateinit var getMiscFlagsUseCase: GetMiscFlagsUseCase

    @Inject
    lateinit var isUserLoggedInUseCase: IsUserLoggedInUseCase

    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var updateCrashAndPerformanceReportersUseCase: UpdateCrashAndPerformanceReportersUseCase

    @Inject
    lateinit var getCookieSettingsUseCase: GetCookieSettingsUseCase

    @Inject
    lateinit var monitorCallSoundsUseCase: MonitorCallSoundsUseCase


    @ApplicationScope
    @Inject
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var themeModeState: ThemeModeState

    @Inject
    lateinit var activityLifecycleHandler: ActivityLifecycleHandler

    @Inject
    lateinit var megaChatNotificationHandler: MegaChatNotificationHandler

    @Inject
    @get:JvmName("pushNotificationSettingManagement")
    lateinit var pushNotificationSettingManagement: PushNotificationSettingManagement

    @Inject
    @get:JvmName("chatManagement")
    lateinit var chatManagement: ChatManagement

    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    @Inject
    lateinit var rtcAudioManagerGateway: RTCAudioManagerGateway

    @Inject
    lateinit var callChangesObserver: CallChangesObserver

    @Inject
    lateinit var globalChatListener: GlobalChatListener

    @Inject
    lateinit var globalNetworkStateHandler: GlobalNetworkStateHandler

    @Inject
    internal lateinit var greeter: Provider<Greeter>

    @Inject
    internal lateinit var thumbnailFactory: MegaThumbnailFetcher.Factory

    @Inject
    internal lateinit var avatarFactory: MegaAvatarFetcher.Factory

    @Inject
    internal lateinit var updateApiServerUseCase: UpdateApiServerUseCase

    @Inject
    lateinit var monitorAndHandleTransferEventsUseCase: MonitorAndHandleTransferEventsUseCase


    @Inject
    lateinit var monitorTransferEventsToStartWorkersIfNeededUseCase: MonitorTransferEventsToStartWorkersIfNeededUseCase

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var domainLogger: Logger

    @Inject
    lateinit var fcmManager: FcmManager

    var localIpAddress: String? = ""

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
        applicationScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.SingleActivity).not()) {
                Log.setLogger(domainLogger)
            }
        }
        themeModeState.initialise()
        callChangesObserver.init()

        // Setup handler and RxJava for uncaught exceptions.
        if (!BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler { _: Thread?, e: Throwable? ->
                handleUncaughtException(e)
            }
        } else {
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(false)
        }

        registerActivityLifecycleCallbacks(activityLifecycleHandler)
        isVerifySMSShowed = false

        monitorTransferEvents()
        monitorTransferEventsToStartWorkersIfNeeded()
        setupMegaChatApi()
        getMiscFlagsIfNeeded()
        applicationScope.launch {
            runCatching { updateApiServerUseCase() }
        }

        myAccountInfo.resetDefaults()
        ContextUtils.initialize(applicationContext)

        if (BuildConfig.ACTIVATE_GREETER) greeter.get().initialize()

        // Subscribe to all users FCM topic
        fcmManager.subscribeToAllUsersTopic()
    }

    // Image loader for coil3
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = {
                            OkHttpClient()
                        }
                    )
                )
                add(VideoFrameDecoder.Factory())
                add(SvgDecoder.Factory())
                add(thumbnailFactory)
                add(avatarFactory)
                add(MegaThumbnailKeyer)
                add(MegaAvatarKeyer)
            }
            .build()
    }

    /**
     * On start
     *
     */
    override fun onStart(owner: LifecycleOwner) {
        applicationScope.launch {
            val backgroundStatus = megaChatApi.backgroundStatus
            Timber.d("Application start with backgroundStatus: %s", backgroundStatus)
            if (backgroundStatus != -1 && backgroundStatus != 0) {
                megaChatApi.setBackgroundStatus(false)
            }
        }
    }

    /**
     * On stop
     *
     */
    override fun onStop(owner: LifecycleOwner) {
        applicationScope.launch {
            val backgroundStatus = megaChatApi.backgroundStatus
            Timber.d("Application stop with backgroundStatus: %s", backgroundStatus)
            if (backgroundStatus != -1 && backgroundStatus != 1) {
                megaChatApi.setBackgroundStatus(true)
            }
        }
    }

    private fun enableStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyDeathOnNetwork()
                    .penaltyLog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )

            if (SDK_INT >= Build.VERSION_CODES.S) {
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder() // Other StrictMode checks that you've previously added.
                        .detectUnsafeIntentLaunch()
                        .penaltyLog()
                        .build()
                )
            }
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

    /**
     * Setup mega chat api
     *
     */
    fun setupMegaChatApi() {
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
        applicationScope.launch {
            monitorCallSoundsUseCase()
                .collectLatest { next: CallSoundType ->
                    soundsController.playSound(next)
                }
        }
    }

    private fun monitorTransferEvents() {
        applicationScope.launch(Dispatchers.IO) {
            var reconnectDelay = Duration.ZERO
            monitorAndHandleTransferEventsUseCase()
                .retry {
                    // In case of an error we need to keep monitoring the events, but we add a exponential delay before retrying to avoid potential infinite sync loops in case of recurrent error
                    Timber.e(it, "Error monitoring transfer events, retrying in $reconnectDelay")
                    delay(reconnectDelay)
                    reconnectDelay = (reconnectDelay * 2).coerceAtLeast(100.milliseconds)
                    true
                }
                .collect {
                    // reset the delay on each successful collect
                    reconnectDelay = Duration.ZERO
                    Timber.v("$it transfer events processed")
                }
        }
    }

    private fun monitorTransferEventsToStartWorkersIfNeeded() {
        applicationScope.launch(Dispatchers.IO) {
            var reconnectDelay = Duration.ZERO
            monitorTransferEventsToStartWorkersIfNeededUseCase()
                .retry {
                    Timber.e(it, "Error starting Workers, retrying in $reconnectDelay")
                    delay(reconnectDelay)
                    reconnectDelay = (reconnectDelay * 2).coerceAtLeast(100.milliseconds)
                    true
                }
                .collect {
                    reconnectDelay = Duration.ZERO
                    Timber.v("Worker started for $it")
                }
        }
    }

    /**
     * Get the misc flags
     */
    private fun getMiscFlagsIfNeeded() {
        applicationScope.launch {
            runCatching {
                val isUserLoggedOut = isUserLoggedInUseCase().not()
                if (isUserLoggedOut) {
                    getMiscFlagsUseCase()
                }
            }.onFailure {
                Timber.e("Failed to get misc flags: $it")
            }
        }
    }

    /**
     * Check current enabled cookies and set the corresponding flags to true/false
     */
    fun checkEnabledCookies() {
        applicationScope.launch {
            runCatching {
                val enabledCookies = getCookieSettingsUseCase()
                updateCrashAndPerformanceReportersUseCase(enabledCookies)
            }.onFailure {
                Timber.e("Failed to get cookie settings: $it")
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
     * Send signal presence activity
     *
     */
    fun sendSignalPresenceActivity() {
        Timber.d("sendSignalPresenceActivity")
        megaChatApi.run { signalPresenceActivity() }
    }

    /**
     * Method for showing an incoming group or one-to-one call notification.
     *
     * @param incomingCall The incoming call
     */
    fun showOneCallNotification(incomingCall: MegaChatCall) =
        callChangesObserver.showOneCallNotification(incomingCall)

    /**
     * Create or update audio manager
     *
     * @param isSpeakerOn
     * @param type
     */
    fun createOrUpdateAudioManager(isSpeakerOn: Boolean, type: Int) {
        Timber.d("Create or update audio manager, type is %s", type)
        chatManagement.registerScreenReceiver()
        Handler(Looper.getMainLooper()).post {
            rtcAudioManagerGateway.createOrUpdateAudioManager(isSpeakerOn, type)
        }
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
                startForegroundService(this)
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

    override val workManagerConfiguration: Configuration
        get() {
            val workManagerEntryPoint = EntryPointAccessors.fromApplication(
                this,
                WorkManagerInitializerEntryPoint::class.java
            )
            return Configuration.Builder()
                .setWorkerFactory(workManagerEntryPoint.hiltWorkerFactory())
                .build()
        }

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    internal interface WorkManagerInitializerEntryPoint {
        /**
         * HiltWorkerFactory
         */
        fun hiltWorkerFactory(): HiltWorkerFactory
    }

    companion object {
        /**
         * App Key
         */
        const val APP_KEY = "6tioyn8ka5l6hty"

        /**
         * Is logging out
         */
        @JvmStatic
        var isLoggingOut = false

        /**
         * Is is heart beat alive
         */
        @JvmStatic
        @Volatile
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
         * Url confirmation link
         */
        @JvmStatic
        @Volatile
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
