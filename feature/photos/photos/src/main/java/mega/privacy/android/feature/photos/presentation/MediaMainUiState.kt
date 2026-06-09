package mega.privacy.android.feature.photos.presentation

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * UI state for Media Main Screen
 *
 * @property newAlbumDialogEvent Event to show new album dialog
 */
data class MediaMainUiState(
    val newAlbumDialogEvent: StateEvent = consumed,
)