package mega.privacy.android.app

import android.app.Application
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.StrictMode
import androidx.work.Configuration
import dagger.Lazy
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.appstate.global.initialisation.GlobalInitialiser
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.components.PushNotificationSettingManagement
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.globalmanagement.SmsVerificationState
import mega.privacy.android.app.workmanager.WorkManagerConfigurationProvider
import javax.inject.Inject

/**
 * Mega application
 *
 * @property activityLifecycleHandler
 * @property pushNotificationSettingManagement
 * @property chatManagement
 */
@HiltAndroidApp
class MegaApplication : Application(), Configuration.Provider {
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
    lateinit var smsVerificationState: SmsVerificationState

    /**
     * On create
     *
     */
    override fun onCreate() {
        super.onCreate()
        enableStrictMode()

        registerActivityLifecycleCallbacks(activityLifecycleHandler)
        smsVerificationState.isVerificationShown = false

        globalInitialiser.onAppCreate()
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
