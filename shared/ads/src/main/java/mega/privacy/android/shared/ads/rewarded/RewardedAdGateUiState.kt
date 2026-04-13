package mega.privacy.android.shared.ads.rewarded

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * Immutable UI state for the Rewarded Ad Gate dialog.
 */
data class RewardedAdGateUiState(
    val showDialog: Boolean = false,
    val isLoading: Boolean = false,
    val skipAdEvent: StateEvent = consumed,
)
