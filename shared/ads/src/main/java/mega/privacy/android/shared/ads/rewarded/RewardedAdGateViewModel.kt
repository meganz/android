package mega.privacy.android.shared.ads.rewarded

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Rewarded Ad Gate.
 *
 * Manages dialog state only. The pending action lambda is held by [RewardedAdGateHandler]
 * in the Compose scope, not here, to avoid stale references after config changes.
 */
@HiltViewModel
class RewardedAdGateViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    val uiState: StateFlow<RewardedAdGateUiState>
        field = MutableStateFlow(RewardedAdGateUiState())

    /**
     * Check whether the ad dialog should be shown. If the user is eligible (currently based
     * on the [ApiFeatures.RewardedAds] feature flag), show the dialog. Otherwise trigger
     * [RewardedAdGateUiState.skipAdEvent] so the caller can skip the ad and continue.
     */
    fun requestShowDialog() {
        viewModelScope.launch {
            val isEligible = runCatching {
                getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)
            }.onFailure {
                Timber.e(it, "Failed to read RewardedAds feature flag")
            }.getOrDefault(false)

            if (isEligible) {
                uiState.update { it.copy(showDialog = true) }
            } else {
                uiState.update { it.copy(skipAdEvent = triggered) }
            }
        }
    }

    fun onSkipAdEventConsumed() {
        uiState.update { it.copy(skipAdEvent = consumed) }
    }

    fun dismiss() {
        uiState.update { RewardedAdGateUiState() }
    }

    fun setLoading() {
        uiState.update { it.copy(isLoading = true) }
    }

    fun setLoadingComplete() {
        uiState.update { it.copy(isLoading = false) }
    }
}
