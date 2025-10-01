package mega.privacy.android.feature.sync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationsUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.SetSyncNotificationShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for monitoring sync state and sync transfers
 */
@HiltViewModel
class SyncMonitorViewModel @Inject constructor(
    private val monitorShouldSyncUseCase: MonitorShouldSyncUseCase,
    private val monitorSyncNotificationsUseCase: MonitorSyncNotificationsUseCase,
    private val pauseResumeSyncsBasedOnBatteryAndWiFiUseCase: PauseResumeSyncsBasedOnBatteryAndWiFiUseCase,
    private val setSyncNotificationShownUseCase: SetSyncNotificationShownUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncMonitorState())

    /**
     * State of the view model
     */
    val state: StateFlow<SyncMonitorState> = _state.asStateFlow()
    private var monitorSyncsStateJob: Job? = null
    private var monitorNotificationsJob: Job? = null

    /**
     * Start monitoring sync state and sync transfers progress
     */
    fun startMonitoring() {
        monitorSyncState()
        monitorNotifications()
    }

    private fun monitorSyncState() {
        if (monitorSyncsStateJob == null || monitorSyncsStateJob?.isCancelled == true) {
            monitorSyncsStateJob = viewModelScope.launch {
                monitorShouldSyncUseCase()
                    .distinctUntilChanged()
                    .catch { Timber.e("Error Monitoring SyncState $it") }
                    .collect {
                        Timber.d("Monitoring Sync State: $it")
                        pauseResumeSyncsBasedOnBatteryAndWiFiUseCase(it)
                    }
            }
        }
    }

    private fun monitorNotifications() {
        if (monitorNotificationsJob == null || monitorNotificationsJob?.isCancelled == true) {
            monitorNotificationsJob =
                monitorSyncNotificationsUseCase()
                    .catch { Timber.e("Error Monitoring SyncNotification $it") }
                    .onEach { notification ->
                        notification?.let {
                            _state.update { it.copy(displayNotification = notification) }
                        }
                    }.launchIn(viewModelScope)
        }
    }

    /**
     * Notify that the notification has been shown
     */
    fun onNotificationShown(
        syncNotificationMessage: SyncNotificationMessage,
        notificationId: Int?,
    ) {
        _state.update { it.copy(displayNotification = null) }
        viewModelScope.launch {
            setSyncNotificationShownUseCase(
                syncNotificationMessage = syncNotificationMessage,
                notificationId = notificationId,
            )
        }
    }
}
