package mega.privacy.android.app.appstate.global.initialisation.appcreate

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.usecase.notifcation.DisplaySyncNotificationUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiser
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Monitors sync state to pause or resume syncs and to surface sync notifications.
 *
 * Async: both collectors already ran fire-and-forget in the application scope at androidx.startup
 * provider time; nothing at boot waits on them.
 */
internal class SyncMonitorInitialiser @Inject constructor(
    private val monitorShouldSyncUseCase: MonitorShouldSyncUseCase,
    private val monitorSyncNotificationsUseCase: MonitorSyncNotificationsUseCase,
    private val pauseResumeSyncsBasedOnBatteryAndWiFiUseCase: PauseResumeSyncsBasedOnBatteryAndWiFiUseCase,
    private val displaySyncNotificationUseCase: DisplaySyncNotificationUseCase,
) : AsyncAppCreateInitialiser {
    override val name = "SyncMonitorInitialiser"

    override suspend operator fun invoke(): Unit = coroutineScope {
        launch {
            monitorShouldSyncUseCase()
                .distinctUntilChanged()
                .retry {
                    Timber.e("SyncMonitorInitialiser: Error monitoring sync state: $it")
                    delay(1.seconds)
                    true
                }
                .collect { shouldSync ->
                    Timber.d("SyncMonitorInitialiser: Should sync: $shouldSync")
                    try {
                        pauseResumeSyncsBasedOnBatteryAndWiFiUseCase(shouldSync)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "SyncMonitorInitialiser: Error updating sync state")
                    }
                }
        }

        launch {
            monitorSyncNotificationsUseCase()
                .retry {
                    Timber.e("SyncMonitorInitialiser: Error monitoring notifications: $it")
                    delay(1.seconds)
                    true
                }
                .collect { notification ->
                    notification?.let {
                        try {
                            displaySyncNotificationUseCase(notification)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.e(e, "SyncMonitorInitialiser: Error displaying notification")
                        }
                    }
                }
        }
        Timber.d("SyncMonitorInitialiser: Started monitoring")
    }
}
