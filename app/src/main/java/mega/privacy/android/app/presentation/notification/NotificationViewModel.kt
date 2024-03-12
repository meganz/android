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
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.NotificationState
import mega.privacy.android.app.presentation.notification.model.mapper.NotificationMapper
import mega.privacy.android.domain.usecase.AcknowledgeUserAlertsUseCase
import mega.privacy.android.domain.usecase.MonitorUserAlertsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.notifications.GetPromoNotificationsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Notification view model
 *
 * @property acknowledgeUserAlertsUseCase
 * @property monitorUserAlertsUseCase
 * @property state
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val acknowledgeUserAlertsUseCase: AcknowledgeUserAlertsUseCase,
    private val monitorUserAlertsUseCase: MonitorUserAlertsUseCase,
    private val getPromoNotificationsUseCase: GetPromoNotificationsUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val notificationMapper: NotificationMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getPromoNotifications()
            monitorUserAlertsUseCase().mapLatest { list ->
                list.map { notificationMapper(it) }
            }.collect {
                _state.update { state ->
                    state.copy(
                        notifications = it,
                        scrollToTop = areNewItemsAdded(it, state.notifications)
                    )
                }
            }
        }
    }

    private suspend fun getPromoNotifications() {
        runCatching {
            if (getFeatureFlagValueUseCase(AppFeatures.PromoNotifications)) {
                val promoNotifications = getPromoNotificationsUseCase()
                _state.update { it.copy(promoNotifications = promoNotifications) }
            }
        }.onFailure {
            Timber.e("Failed to fetch promo notifications with error: ${it.message}")
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
        viewModelScope.launch { acknowledgeUserAlertsUseCase() }
    }
}