package mega.privacy.android.app.appstate.content.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.preference.MonitorCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Drives the one-time "Customise navigation" onboarding tooltip shown above the bottom
 * navigation bar on the landing screen.
 */
@HiltViewModel
class CustomiseNavigationTooltipViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorCustomiseNavigationTooltipShownUseCase: MonitorCustomiseNavigationTooltipShownUseCase,
    private val setCustomiseNavigationTooltipShownUseCase: SetCustomiseNavigationTooltipShownUseCase,
) : ViewModel() {

    private val dismissedChannel = Channel<Boolean>(Channel.CONFLATED)

    /**
     * Emits true while the tooltip should be visible.
     *
     * The shown preference is only read once so that marking the tooltip as shown while it is
     * being displayed does not hide it; it disappears through [onTooltipDismissed] instead.
     */
    val uiState: StateFlow<Boolean> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            flow { emit(getFeatureFlagValueUseCase(ApiFeatures.CustomisableBottomNavigation)) },
            monitorCustomiseNavigationTooltipShownUseCase().take(1),
            dismissedChannel.receiveAsFlow().onStart { emit(false) },
        ) { isCustomisationEnabled, isTooltipShown, isDismissed ->
            isCustomisationEnabled && !isTooltipShown && !isDismissed
        }.catch {
            Timber.e(it, "Failed to resolve Customise navigation tooltip visibility")
        }.asUiStateFlow(viewModelScope, false)
    }

    /**
     * Marks the tooltip as shown the moment it is displayed, so it is never shown again
     * regardless of how it is closed.
     */
    fun onTooltipDisplayed() {
        viewModelScope.launch {
            runCatching { setCustomiseNavigationTooltipShownUseCase() }
                .onFailure { Timber.e(it, "Failed to mark Customise navigation tooltip as shown") }
        }
    }

    /**
     * Hides the tooltip for the rest of the session.
     */
    fun onTooltipDismissed() {
        dismissedChannel.trySend(true)
    }
}
