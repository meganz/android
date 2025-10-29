package mega.privacy.android.feature.photos.presentation

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

data class MediaMainUiState(
    val newAlbumDialogEvent: StateEvent = consumed,
)