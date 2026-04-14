package mega.privacy.android.shared.ads.rewarded

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.advertisements.IncrementRewardedAdAttemptCountUseCase
import mega.privacy.android.domain.usecase.advertisements.MonitorRewardedAdAttemptCountUseCase
import mega.privacy.android.domain.usecase.advertisements.ResetRewardedAdAttemptCountUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.ads.rewarded.RewardedAdGateViewModel.Companion.AD_SHOW_THRESHOLD
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Rewarded Ad Gate.
 *
 * Manages dialog state only. The pending action lambda is held by [RewardedAdGateHandler]
 * in the Compose scope, not here, to avoid stale references after config changes.
 *
 * Tracks a persisted attempt counter — the dialog is only shown once the counter reaches
 * [AD_SHOW_THRESHOLD]. The counter is reset only when the user earns the reward.
 */
@HiltViewModel
class RewardedAdGateViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorRewardedAdAttemptCountUseCase: MonitorRewardedAdAttemptCountUseCase,
    private val incrementRewardedAdAttemptCountUseCase: IncrementRewardedAdAttemptCountUseCase,
    private val resetRewardedAdAttemptCountUseCase: ResetRewardedAdAttemptCountUseCase,
) : ViewModel() {

    val uiState: StateFlow<RewardedAdGateUiState>
        field = MutableStateFlow(RewardedAdGateUiState())

    init {
        checkEligibility()
        monitorAttemptCount()
    }

    private fun checkEligibility() {
        viewModelScope.launch {
            val isEligible = runCatching {
                getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)
            }.onFailure {
                Timber.e(it, "Failed to read RewardedAds feature flag")
            }.getOrDefault(false)

            uiState.update {
                it.copy(
                    isCheckingEligibility = false,
                    isEligible = isEligible,
                )
            }
        }
    }

    private fun monitorAttemptCount() {
        viewModelScope.launch {
            monitorRewardedAdAttemptCountUseCase().collectLatest { count ->
                uiState.update { it.copy(currentAttemptCount = count) }
            }
        }
    }

    /**
     * Check whether the ad dialog should be shown. If the eligibility check is still in
     * progress or the user is not eligible (currently based on the [ApiFeatures.RewardedAds]
     * feature flag), trigger [RewardedAdGateUiState.skipAdEvent] so the caller can skip the
     * ad and continue without being blocked while flags load.
     *
     * For eligible users, the counter is incremented on every attempt. The dialog is shown
     * once the counter reaches [AD_SHOW_THRESHOLD]; until then the action skips immediately.
     */
    fun requestShowDialog() {
        val state = uiState.value
        val isEligible = !state.isCheckingEligibility && state.isEligible
        val shouldShowDialog = isEligible && state.currentAttemptCount + 1 >= AD_SHOW_THRESHOLD

        if (shouldShowDialog) {
            uiState.update { it.copy(showDialog = true) }
        } else {
            uiState.update { it.copy(skipAdEvent = triggered) }
        }

        if (isEligible) {
            viewModelScope.launch {
                runCatching { incrementRewardedAdAttemptCountUseCase() }
                    .onFailure { Timber.e(it, "Failed to increment rewarded ad attempt count") }
            }
        }
    }

    fun onSkipAdEventConsumed() {
        uiState.update { it.copy(skipAdEvent = consumed) }
    }

    fun dismiss() {
        uiState.update {
            it.copy(
                showDialog = false,
                isLoading = false,
                skipAdEvent = consumed,
            )
        }
    }

    fun setLoading() {
        uiState.update { it.copy(isLoading = true) }
    }

    fun setLoadingComplete() {
        uiState.update { it.copy(isLoading = false) }
    }

    /**
     * Reset the rewarded ad attempt counter back to 0. Called when the user earns the reward.
     */
    fun resetAttemptCount() {
        viewModelScope.launch {
            runCatching { resetRewardedAdAttemptCountUseCase() }
                .onFailure { Timber.e(it, "Failed to reset rewarded ad attempt count") }
        }
    }

    companion object {
        /** Number of attempts before the rewarded ad dialog is shown. */
        const val AD_SHOW_THRESHOLD = 5
    }
}
