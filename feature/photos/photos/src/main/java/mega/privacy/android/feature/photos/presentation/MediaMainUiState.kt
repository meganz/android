package mega.privacy.android.feature.photos.presentation

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * UI state for Media Main Screen
 *
 * @property newAlbumDialogEvent Event to show new album dialog
 * @property isMediaRevampPhase2Enabled Flag to indicate if media revamp phase 2 is enabled
 */
data class MediaMainUiState(
    val newAlbumDialogEvent: StateEvent = consumed,
    val isMediaRevampPhase2Enabled: Boolean = false,
)