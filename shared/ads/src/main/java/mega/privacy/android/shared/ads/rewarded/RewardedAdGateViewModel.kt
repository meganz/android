package mega.privacy.android.shared.ads.rewarded

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.ump.ConsentInformation
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.MonitorUpdateUserDataUseCase
import mega.privacy.android.domain.usecase.advertisements.IncrementRewardedAdAttemptCountUseCase
import mega.privacy.android.domain.usecase.advertisements.MonitorGoogleConsentLoadedUseCase
import mega.privacy.android.domain.usecase.advertisements.MonitorRewardedAdAttemptCountUseCase
import mega.privacy.android.domain.usecase.advertisements.ResetRewardedAdAttemptCountUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.ads.rewarded.RewardedAdGateViewModel.Companion.AD_SHOW_THRESHOLD
import mega.privacy.mobile.analytics.event.RewardedAdGateActionRequestedEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Rewarded Ad Gate.
 */
@HiltViewModel
class RewardedAdGateViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorRewardedAdAttemptCountUseCase: MonitorRewardedAdAttemptCountUseCase,
    private val incrementRewardedAdAttemptCountUseCase: IncrementRewardedAdAttemptCountUseCase,
    private val resetRewardedAdAttemptCountUseCase: ResetRewardedAdAttemptCountUseCase,
    private val consentInformation: ConsentInformation,
    private val monitorGoogleConsentLoadedUseCase: MonitorGoogleConsentLoadedUseCase,
    private val monitorUpdateUserDataUseCase: MonitorUpdateUserDataUseCase,
) : ViewModel() {

    val uiState: StateFlow<RewardedAdGateUiState>
        field = MutableStateFlow(RewardedAdGateUiState())

    init {
        checkFeatureFlags()
        monitorAttemptCount()
        monitorConsentLoaded()
        monitorUserDataUpdates()
    }

    private fun checkFeatureFlags() {
        viewModelScope.launch {
            val flagsEnabled = runCatching {
                getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag) &&
                        getFeatureFlagValueUseCase(ApiFeatures.RewardedAds)
            }.onFailure {
                Timber.e(it, "Failed to check rewarded ads feature flags")
            }.getOrDefault(false)

            uiState.update { it.copy(isFeatureFlagEnabled = flagsEnabled) }
        }
    }

    private fun monitorAttemptCount() {
        viewModelScope.launch {
            monitorRewardedAdAttemptCountUseCase()
                .catch { Timber.e(it) }
                .collectLatest { count ->
                    uiState.update { it.copy(currentAttemptCount = count) }
                }
        }
    }

    private fun monitorConsentLoaded() {
        viewModelScope.launch {
            monitorGoogleConsentLoadedUseCase()
                .catch { Timber.e(it) }
                .collect { isLoaded ->
                    if (isLoaded) {
                        Timber.d("User consent is loaded, checking if can request ads")
                        uiState.update {
                            it.copy(
                                isGoogleConsentLoaded = true,
                                canRequestAds = consentInformation.canRequestAds(),
                            )
                        }
                    }
                }
        }
    }

    private fun monitorUserDataUpdates() {
        viewModelScope.launch {
            monitorUpdateUserDataUseCase()
                .catch { Timber.e(it) }
                .drop(1) // Ignore initial update
                .collectLatest {
                    Timber.d("Account updated, resetting rewarded ads feature flag")
                    uiState.update { it.copy(isFeatureFlagEnabled = false) }
                    checkFeatureFlags()
                }
        }
    }

    /**
     * Update whether ads are allowed for the current screen. For link screens this is the per-link
     * `QueryAdsUseCase` result — a link created by a Pro user is not eligible for ads, so the gate
     * skips the dialog and runs the pending action immediately.
     */
    fun setAdsAllowedForScreen(allowed: Boolean) {
        uiState.update { it.copy(isAdsAllowedForScreen = allowed) }
    }

    /**
     * Check if the user is eligible for rewarded ads based on the current [state].
     *
     * All conditions must be met:
     * - Feature flags ([ApiFeatures.GoogleAdsFeatureFlag] and [ApiFeatures.RewardedAds]) are enabled
     * - Google consent has been loaded
     * - The user has consented to ads
     * - Ads are allowed for the current screen (e.g. per-link `QueryAdsUseCase` result)
     * Else rewarded ad dialog won't be shown and pendingAction will be executed instantly.
     * Ad will be skipped in case eligibility check takes too long or fails.
     */
    private fun isEligible(state: RewardedAdGateUiState): Boolean =
        state.isFeatureFlagEnabled && state.isGoogleConsentLoaded &&
                state.canRequestAds && state.isAdsAllowedForScreen

    /**
     * Check whether the ad dialog should be shown. If the user is not eligible,
     * trigger [RewardedAdGateUiState.skipAdEvent] so the caller can skip the ad and continue without being blocked.
     *
     * For eligible users, the counter is incremented on every attempt. The dialog is shown
     * once the counter reaches [AD_SHOW_THRESHOLD]; until then the action skips immediately.
     */
    fun requestShowDialog() {
        val state = uiState.value
        val isEligible = isEligible(state)
        val shouldShowDialog = isEligible && state.currentAttemptCount + 1 >= AD_SHOW_THRESHOLD

        if (shouldShowDialog) {
            uiState.update { it.copy(showDialog = true) }
        } else {
            uiState.update { it.copy(skipAdEvent = triggered) }
        }

        if (isEligible) {
            Analytics.tracker.trackEvent(RewardedAdGateActionRequestedEvent)
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
                isAdLoading = false,
                skipAdEvent = consumed,
            )
        }
    }

    fun setAdLoading() {
        uiState.update { it.copy(isAdLoading = true) }
    }

    fun setAdLoadingComplete() {
        uiState.update { it.copy(isAdLoading = false) }
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
