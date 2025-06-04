package mega.privacy.android.feature.sync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncIssueNotificationByTypeUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationTypeUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SyncIssueNotificationViewModel @Inject constructor(
    private val monitorSyncNotificationTypeUseCase: MonitorSyncNotificationTypeUseCase,
    private val getSyncIssueNotificationByTypeUseCase: GetSyncIssueNotificationByTypeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncMonitorState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorSyncNotificationTypeUseCase().catch {
                Timber.e(it)
            }.collect { notificationType ->
                val notificationMessage =
                    if (notificationType == SyncNotificationType.NOT_CONNECTED_TO_WIFI
                        || notificationType == SyncNotificationType.CHANGE_SYNC_ROOT
                    ) runCatching {
                        getSyncIssueNotificationByTypeUseCase(
                            notificationType
                        )
                    }.getOrNull() else null
                _state.update {
                    it.copy(
                        displayNotification = notificationMessage,
                        syncNotificationType = notificationType
                    )
                }
            }
        }
    }

    fun dismissNotification() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    displayNotification = null,
                    syncNotificationType = null
                )
            }
        }
    }
}
