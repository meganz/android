package mega.privacy.android.shared.ads.rewarded

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * Immutable UI state for the Rewarded Ad Gate dialog.
 */
data class RewardedAdGateUiState(
    val showDialog: Boolean = false,
    val isAdLoading: Boolean = false,
    val isFeatureFlagEnabled: Boolean = false,
    val isGoogleConsentLoaded: Boolean = false,
    val canRequestAds: Boolean = false,
    val isAdsAllowedForScreen: Boolean = true,
    val currentAttemptCount: Int = 0,
    val skipAdEvent: StateEvent = consumed,
)
