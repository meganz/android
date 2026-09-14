package mega.privacy.android.app

import android.app.Application
import android.os.Build
import android.os.Build.VERSION.SDK_INT
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
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.globalmanagement.SmsVerificationState
import mega.privacy.android.app.workmanager.WorkManagerConfigurationProvider
import mega.privacy.android.data.gateway.LogFlushGateway
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaChatApiAndroid
import timber.log.Timber
import javax.inject.Inject

/**
 * Mega application
 *
 * @property _dbH
 * @property myAccountInfo
 * @property activityLifecycleHandler
 * @property pushNotificationSettingManagement
 * @property chatManagement
 * @property applicationScope
 */
@HiltAndroidApp
class MegaApplication : Application(), DefaultLifecycleObserver, Configuration.Provider {
    @Inject
    lateinit var _megaChatApi: Lazy<MegaChatApiAndroid>

    private val megaChatApi: MegaChatApiAndroid
        get() = _megaChatApi.get()

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
    lateinit var _myAccountInfo: Lazy<MyAccountInfo>

    val myAccountInfo: MyAccountInfo
        get() = _myAccountInfo.get()

    @ApplicationScope
    @Inject
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var activityLifecycleHandler: ActivityLifecycleHandler

    @Inject
    lateinit var _pushNotificationSettingManagement: Lazy<PushNotificationSettingManagement>

    val pushNotificationSettingManagement: PushNotificationSettingManagement
        @JvmName("pushNotificationSettingManagement")
        get() = _pushNotificationSettingManagement.get()

    @Inject
    lateinit var _chatManagement: Lazy<ChatManagement>

    val chatManagement: ChatManagement
        @JvmName("chatManagement")
        get() = _chatManagement.get()

    @Inject
    lateinit var globalInitialiser: GlobalInitialiser

    @Inject
    lateinit var logFlushGateway: LogFlushGateway

    @Inject
    lateinit var smsVerificationState: SmsVerificationState

    /**
     * On create
     *
     */
    override fun onCreate() {
        super<Application>.onCreate()
        enableStrictMode()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        registerActivityLifecycleCallbacks(activityLifecycleHandler)
        smsVerificationState.isVerificationShown = false

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
     * Reset my account info
     *
     */
    fun resetMyAccountInfo() = myAccountInfo.resetDefaults()

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
         * Is web open due to email verification
         */
        var isWebOpenDueToEmailVerification = false
            private set

        /**
         * Set is web open due to email verification
         *
         * @param isWebOpenDueToEmailVerification
         */
        @JvmStatic
        fun setIsWebOpenDueToEmailVerification(isWebOpenDueToEmailVerification: Boolean) {
            this.isWebOpenDueToEmailVerification = isWebOpenDueToEmailVerification
        }
    }
}
