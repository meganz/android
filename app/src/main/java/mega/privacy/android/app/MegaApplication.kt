package mega.privacy.android.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.Lazy
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.appstate.global.initialisation.GlobalInitialiser
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.components.PushNotificationSettingManagement
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.globalmanagement.CallChangesObserver
import mega.privacy.android.app.globalmanagement.ChatApiListenerCoordinator
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.meeting.CallService
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.workmanager.WorkManagerConfigurationProvider
import mega.privacy.android.data.gateway.LogFlushGateway
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import mega.privacy.android.navigation.destination.ChatNavKey
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatCall
import timber.log.Timber
import javax.inject.Inject

/**
 * Mega application
 *
 * @property megaApi
 * @property megaApiFolder
 * @property megaChatApi
 * @property _dbH
 * @property myAccountInfo
 * @property updateCrashAndPerformanceReportersUseCase
 * @property activityLifecycleHandler
 * @property pushNotificationSettingManagement
 * @property chatManagement
 * @property rtcAudioManagerGateway
 * @property callChangesObserver
 * @property applicationScope
 */
@HiltAndroidApp
class MegaApplication : Application(), DefaultLifecycleObserver, Configuration.Provider {
    @MegaApi
    @Inject
    lateinit var _megaApi: Lazy<MegaApiAndroid>

    val megaApi: MegaApiAndroid
        @JvmName("getMegaApi")
        get() = _megaApi.get()

    @MegaApiFolder
    @Inject
    lateinit var _megaApiFolder: Lazy<MegaApiAndroid>

    val megaApiFolder: MegaApiAndroid
        @JvmName("getMegaApiFolder")
        get() = _megaApiFolder.get()

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
    lateinit var myAccountInfo: MyAccountInfo

    @Inject
    lateinit var updateCrashAndPerformanceReportersUseCase: UpdateCrashAndPerformanceReportersUseCase

    @Inject
    lateinit var getCookieSettingsUseCase: GetCookieSettingsUseCase

    @ApplicationScope
    @Inject
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var activityLifecycleHandler: ActivityLifecycleHandler

    @Inject
    @get:JvmName("pushNotificationSettingManagement")
    lateinit var pushNotificationSettingManagement: PushNotificationSettingManagement

    @Inject
    @get:JvmName("chatManagement")
    lateinit var chatManagement: ChatManagement

    @Inject
    lateinit var rtcAudioManagerGateway: RTCAudioManagerGateway

    @Inject
    lateinit var callChangesObserver: CallChangesObserver

    @Inject
    lateinit var chatApiListenerCoordinator: ChatApiListenerCoordinator

    @Inject
    lateinit var globalInitialiser: GlobalInitialiser

    @Inject
    lateinit var logFlushGateway: LogFlushGateway

    /**
     * On create
     *
     */
    override fun onCreate() {
        instance = this
        super<Application>.onCreate()
        enableStrictMode()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        registerActivityLifecycleCallbacks(activityLifecycleHandler)
        isVerifySMSShowed = false

        globalInitialiser.onAppCreate()
    }

    /**
     * On start
     *
     */
    override fun onStart(owner: LifecycleOwner) {
        globalInitialiser.onAppStart()
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
        applicationScope.launch { logFlushGateway.flush() }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            applicationScope.launch { logFlushGateway.flush() }
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
    fun disableMegaChatApi() = chatApiListenerCoordinator.unregister()

    /**
     * Setup mega chat api
     *
     */
    fun setupMegaChatApi() = chatApiListenerCoordinator.register()

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
        chatApiListenerCoordinator.register()
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
                putExtra(ChatNavKey.LEGACY_CHAT_ID, chatId)
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

    /**
     * WorkManager resolves this on-demand, which can happen before Hilt has injected this
     * Application's fields (e.g. from an androidx.startup [androidx.startup.Initializer] that
     * runs during the ContentProvider phase). Fetch [WorkManagerConfigurationProvider] through an
     * entry point so it only depends on the Dagger component existing, not on field injection order.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface WorkManagerConfigurationEntryPoint {
        fun workManagerConfigurationProvider(): WorkManagerConfigurationProvider
    }

    override val workManagerConfiguration: Configuration
        get() = EntryPointAccessors.fromApplication(
            this,
            WorkManagerConfigurationEntryPoint::class.java
        ).workManagerConfigurationProvider().workManagerConfiguration

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
         * Is verify s m s showed
         */
        var isVerifySMSShowed = false
            private set

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
