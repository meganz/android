package mega.privacy.android.feature.sync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.collectChunked
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncNotificationUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.SetSyncNotificationShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase.Companion.LOW_BATTERY_LEVEL
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for monitoring sync state and sync transfers
 */
@HiltViewModel
class SyncMonitorViewModel @Inject constructor(
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val handleTransferEventUseCase: HandleTransferEventUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase,
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val pauseResumeSyncsBasedOnBatteryAndWiFiUseCase: PauseResumeSyncsBasedOnBatteryAndWiFiUseCase,
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    private val setSyncNotificationShownUseCase: SetSyncNotificationShownUseCase,
    private val getSyncNotificationUseCase: GetSyncNotificationUseCase,
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncMonitorState())

    /**
     * State of the view model
     */
    val state: StateFlow<SyncMonitorState> = _state.asStateFlow()

    private var monitorTransferEventsJob: Job? = null
    private var monitorSyncsStateJob: Job? = null
    private var monitorNotificationsJob: Job? = null

    /**
     * Start monitoring sync state and sync transfers progress
     */
    fun startMonitoring() {
        monitorCompletedSyncTransfers()
        monitorSyncState()
        monitorNotifications()
    }

    private fun monitorCompletedSyncTransfers() {
        if (monitorTransferEventsJob == null || monitorTransferEventsJob?.isCancelled == true) {
            monitorTransferEventsJob = viewModelScope.launch {
                monitorTransferEventsUseCase()
                    .catch { Timber.e(it) }
                    .filter { it.transfer.isSyncTransfer }
                    .collectChunked(
                        chunkDuration = 2.seconds,
                        flushOnIdleDuration = 200.milliseconds,
                    ) { transferEvents ->
                        withContext(NonCancellable) {
                            launch {
                                handleTransferEventUseCase(events = transferEvents.toTypedArray())
                            }
                        }
                    }
            }
        }
    }

    private fun monitorSyncState() {
        if (monitorSyncsStateJob == null || monitorSyncsStateJob?.isCancelled == true) {
            monitorSyncsStateJob = viewModelScope.launch {
                combine(
                    monitorConnectivityUseCase(),
                    monitorSyncByWiFiUseCase(),
                    monitorBatteryInfoUseCase(),
                    monitorAccountDetailUseCase()
                ) { connectedToInternet: Boolean, syncByWifi: Boolean, batteryInfo: BatteryInfo, accountDetail: AccountDetail ->
                    Triple(
                        batteryInfo,
                        Pair(
                            connectedToInternet,
                            syncByWifi,
                        ),
                        accountDetail.levelDetail?.accountType == AccountType.FREE
                    )
                }
                    .distinctUntilChanged()
                    .collect { (batteryInfo, connectionDetails, isFreeAccount) ->
                        val (connectedToInternet, syncByWifi) = connectionDetails
                        updateSyncState(connectedToInternet, syncByWifi, batteryInfo, isFreeAccount)
                    }
            }
        }
    }

    private suspend fun updateSyncState(
        connectedToInternet: Boolean,
        syncOnlyByWifi: Boolean,
        batteryInfo: BatteryInfo,
        isFreeAccount: Boolean,
    ) {
        pauseResumeSyncsBasedOnBatteryAndWiFiUseCase(
            connectedToInternet = connectedToInternet,
            syncOnlyByWifi = syncOnlyByWifi,
            batteryInfo = batteryInfo,
            isFreeAccount = isFreeAccount
        )
    }

    private fun monitorNotifications() {
        if (monitorNotificationsJob == null || monitorNotificationsJob?.isCancelled == true) {
            monitorNotificationsJob = combine(
                monitorSyncStalledIssuesUseCase(),
                monitorSyncsUseCase(),
                monitorBatteryInfoUseCase(),
                monitorSyncByWiFiUseCase(),
                monitorConnectivityUseCase()
            ) { stalledIssues: List<StalledIssue>, syncs: List<FolderPair>, batteryInfo: BatteryInfo, syncByWifi: Boolean, _ ->
                runCatching {
                    getSyncNotificationUseCase(
                        isBatteryLow = batteryInfo.level < LOW_BATTERY_LEVEL && !batteryInfo.isCharging,
                        isUserOnWifi = isOnWifiNetworkUseCase(),
                        isSyncOnlyByWifi = syncByWifi,
                        syncs = syncs,
                        stalledIssues = stalledIssues
                    )
                }.onSuccess { notification ->
                    notification?.let {
                        _state.update { it.copy(displayNotification = notification) }
                    }
                }
                    .onFailure {
                        Timber.e(it)
                    }
            }
                .distinctUntilChanged()
                .launchIn(viewModelScope)
        }
    }

    /**
     * Notify that the notification has been shown
     */
    fun onNotificationShown(syncNotificationMessage: SyncNotificationMessage) {
        _state.update { it.copy(displayNotification = null) }
        viewModelScope.launch {
            setSyncNotificationShownUseCase(syncNotificationMessage)
        }
    }
}