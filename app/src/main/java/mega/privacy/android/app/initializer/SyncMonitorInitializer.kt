package mega.privacy.android.app.initializer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationsUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.SetSyncNotificationShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager
import mega.privacy.android.feature_flags.AppFeatures
import timber.log.Timber

/**
 * Initializer for monitoring sync state and showing notifications
 * This runs at app startup and is independent of any Activity
 */
@Suppress("EnsureInitializerMetadata")
class SyncMonitorInitializer : Initializer<Unit> {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncMonitorInitializerEntryPoint {

        @ApplicationScope
        fun appScope(): CoroutineScope

        // Domain use cases
        fun monitorShouldSyncUseCase(): MonitorShouldSyncUseCase
        fun monitorSyncNotificationsUseCase(): MonitorSyncNotificationsUseCase
        fun pauseResumeSyncsBasedOnBatteryAndWiFiUseCase(): PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
        fun setSyncNotificationShownUseCase(): SetSyncNotificationShownUseCase

        fun getFeatureFlagValueUseCase(): GetFeatureFlagValueUseCase

        // Notification manager
        fun syncNotificationManager(): SyncNotificationManager
    }

    override fun create(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            SyncMonitorInitializerEntryPoint::class.java
        )

        entryPoint.appScope().launch {
            val isSingleActivity =
                entryPoint.getFeatureFlagValueUseCase().invoke(AppFeatures.SingleActivity)
            if (isSingleActivity) {
                launch {
                    entryPoint.monitorShouldSyncUseCase().invoke()
                        .distinctUntilChanged()
                        .retry {
                            Timber.e("SyncMonitorInitializer: Error monitoring sync state: $it")
                            true
                        }
                        .collect { shouldSync ->
                            Timber.d("SyncMonitorInitializer: Should sync: $shouldSync")
                            entryPoint.pauseResumeSyncsBasedOnBatteryAndWiFiUseCase()
                                .invoke(shouldSync)
                        }
                }

                // Monitor and show sync notifications
                launch {
                    entryPoint.monitorSyncNotificationsUseCase().invoke()
                        .retry {
                            Timber.e("SyncMonitorInitializer: Error monitoring notifications: $it")
                            true
                        }
                        .collect { notification ->
                            notification?.let {
                                // Check permission before showing notification
                                runCatching {
                                    if (ActivityCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        val syncNotificationManager =
                                            entryPoint.syncNotificationManager()

                                        // Only show if not already displayed
                                        if (!syncNotificationManager.isSyncNotificationDisplayed()) {
                                            val notificationId =
                                                syncNotificationManager.show(context, notification)
                                            // Mark notification as shown
                                            entryPoint.setSyncNotificationShownUseCase().invoke(
                                                syncNotificationMessage = notification,
                                                notificationId = notificationId,
                                            )
                                        }
                                    }
                                }

                            }
                        }
                }
            }
            Timber.d("SyncMonitorInitializer: Started monitoring")
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(LoggerInitializer::class.java)
}
