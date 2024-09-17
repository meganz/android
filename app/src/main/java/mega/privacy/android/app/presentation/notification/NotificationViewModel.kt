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
import mega.privacy.android.domain.usecase.AcknowledgeUserAlertsUseCase
import mega.privacy.android.domain.usecase.MonitorUserAlertsUseCase
import mega.privacy.android.domain.usecase.notifications.GetPromoNotificationsUseCase
import mega.privacy.android.domain.usecase.notifications.SetLastReadNotificationUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * NotificationViewModel
 *
 * @property acknowledgeUserAlertsUseCase Acknowledge user alerts use case
 * @property monitorUserAlertsUseCase Monitor user alerts use case
 * @property getPromoNotificationsUseCase Get promo notifications use case
 * @property setLastReadNotificationUseCase Set last read notification use case
 * @property notificationMapper Notification mapper
 * @constructor Create empty Notification view model
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val acknowledgeUserAlertsUseCase: AcknowledgeUserAlertsUseCase,
    private val monitorUserAlertsUseCase: MonitorUserAlertsUseCase,
    private val getPromoNotificationsUseCase: GetPromoNotificationsUseCase,
    private val setLastReadNotificationUseCase: SetLastReadNotificationUseCase,
    private val notificationMapper: NotificationMapper,
) : ViewModel() {

    private var isPromoNotificationsEnabled = false
    private val _state = MutableStateFlow(NotificationState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorUserAlertsUseCase().mapLatest { list ->
                list.map { notificationMapper(it) }
            }.collect { userAlerts ->
                _state.update { state ->
                    state.copy(
                        notifications = userAlerts,
                        promoNotifications = getPromoNotifications(),
                        scrollToTop = areNewItemsAdded(userAlerts, state.notifications)
                    )
                }
            }
        }
    }

    private suspend fun getPromoNotifications() =
        runCatching {
            getPromoNotificationsUseCase()
        }.onFailure {
            Timber.e("Failed to fetch promo notifications with error: ${it.message}")
        }.getOrDefault(emptyList())


    private fun areNewItemsAdded(
        it: List<Notification>,
        notifications: List<Notification>,
    ) = notifications.isNotEmpty() && it.firstOrNull() != notifications.firstOrNull()

    /**
     * On notifications loaded
     *
     */
    fun onNotificationsLoaded() {
        viewModelScope.launch {
            runCatching {
                acknowledgeUserAlertsUseCase()
            }.onFailure {
                Timber.e("Failed to acknowledge user alerts with error: ${it.message}")
            }
        }
        viewModelScope.launch {
            val lastReadPromoNotificationID =
                state.value.promoNotifications.firstOrNull()?.promoID
            lastReadPromoNotificationID?.let {
                runCatching {
                    setLastReadNotificationUseCase(it)
                }.onFailure {
                    Timber.e("Failed to set last read notification with error: ${it.message}")
                }
            }
        }
    }
}