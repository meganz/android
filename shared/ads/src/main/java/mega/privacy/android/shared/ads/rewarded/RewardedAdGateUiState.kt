package mega.privacy.android.shared.ads.rewarded

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * Immutable UI state for the Rewarded Ad Gate dialog.
 */
data class RewardedAdGateUiState(
    val showDialog: Boolean = false,
    val isLoading: Boolean = false,
    val isCheckingEligibility: Boolean = true,
    val isEligible: Boolean = false,
    val currentAttemptCount: Int = 0,
    val skipAdEvent: StateEvent = consumed,
)
