package mega.privacy.android.shared.account.inactivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.account.AcknowledgeLastPurgeUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountInactivityUseCase
import mega.privacy.android.domain.usecase.account.SuppressPurgeTimestampUseCase
import mega.privacy.mobile.analytics.event.InactivityPurgeBannerCloseButtonPressedEvent
import mega.privacy.mobile.analytics.event.InactivityPurgeBannerDisplayedEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * Drives the account inactivity banner: shown whenever an inactive purge event has been received,
 * hidden for the rest of the session once dismissed.
 */
@HiltViewModel
class InactivityBannerViewModel @Inject constructor(
    private val monitorAccountInactivityUseCase: MonitorAccountInactivityUseCase,
    private val acknowledgeLastPurgeUseCase: AcknowledgeLastPurgeUseCase,
    private val suppressPurgeTimestampUseCase: SuppressPurgeTimestampUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    val uiState: StateFlow<InactivityBannerUiState>
        field = MutableStateFlow(InactivityBannerUiState())

    init {
        viewModelScope.launch {
            var displayedEventTracked = false
            monitorAccountInactivityUseCase()
                .catch { Timber.e(it) }
                .collect { inactivity ->
                    if (inactivity != null && !displayedEventTracked) {
                        displayedEventTracked = true
                        Analytics.tracker.trackEvent(InactivityPurgeBannerDisplayedEvent)
                    }
                    uiState.update {
                        it.copy(
                            inactivityMonths = inactivity?.inactivityMonths,
                            purgeTimestamp = inactivity?.purgeTimestamp,
                        )
                    }
                }
        }
    }

    /**
     * Hides the banner app-wide for the rest of the session at once, then acknowledges the purge on
     * the server. The acknowledgement outlives this screen; on failure the event simply re-fires on
     * the next session.
     *
     * @param purgeTimestamp the Unix timestamp (seconds) of the purge to acknowledge
     */
    fun dismiss(purgeTimestamp: Long) {
        Analytics.tracker.trackEvent(InactivityPurgeBannerCloseButtonPressedEvent)
        suppressPurgeTimestampUseCase(purgeTimestamp)
        applicationScope.launch {
            runCatching { acknowledgeLastPurgeUseCase(purgeTimestamp) }
                .onFailure { Timber.e(it, "Acknowledging the purge failed, purgeTs=$purgeTimestamp") }
        }
    }
}

/**
 * @property inactivityMonths months of inactivity reported with the purge, null when there is no
 * purge to show
 * @property purgeTimestamp the Unix timestamp (seconds) of the purge
 */
data class InactivityBannerUiState(
    val inactivityMonths: Int? = null,
    val purgeTimestamp: Long? = null,
) {
    /**
     * Shown whenever a purge event has been received, whatever the computed months
     */
    val isVisible: Boolean
        get() = inactivityMonths != null
}
