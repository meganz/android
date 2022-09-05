package mega.privacy.android.app.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.NotificationState
import mega.privacy.android.app.presentation.notification.model.mapper.NotificationMapper
import mega.privacy.android.domain.usecase.AcknowledgeUserAlerts
import mega.privacy.android.domain.usecase.MonitorUserAlerts
import javax.inject.Inject

/**
 * Notification view model
 *
 * @property monitorUserAlerts
 * @property acknowledgeUserAlerts
 * @property state
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val monitorUserAlerts: MonitorUserAlerts,
    private val acknowledgeUserAlerts: AcknowledgeUserAlerts,
    private val notificationMapper: NotificationMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationState(emptyList()))
    val state = _state.asStateFlow()


    init {
        viewModelScope.launch {
            monitorUserAlerts().mapLatest { list ->
                list.map { notificationMapper(it) }
            }.collect() {
                _state.update { (notifications) ->
                    NotificationState(
                        notifications = it,
                        scrollToTop = areNewItemsAdded(it, notifications)
                    )
                }
            }
        }
    }

    private fun areNewItemsAdded(
        it: List<Notification>,
        notifications: List<Notification>,
    ) = notifications.isNotEmpty() && it.firstOrNull() != notifications.firstOrNull()

    /**
     * On notifications loaded
     *
     */
    fun onNotificationsLoaded() {
        viewModelScope.launch { acknowledgeUserAlerts() }
    }
}