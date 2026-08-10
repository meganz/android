package mega.privacy.android.feature.photos.presentation.handler

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
internal fun MediaSelectionModelHandler(
    type: MediaSelectionModeType,
    onClearTimelinePhotosSelection: () -> Unit,
    onClearAlbumsSelection: () -> Unit,
    onClearVideosSelection: () -> Unit,
    onClearPlaylistsSelection: () -> Unit,
) {
    BackHandler(enabled = type != MediaSelectionModeType.None) {
        when (type) {
            MediaSelectionModeType.Timeline -> onClearTimelinePhotosSelection()
            MediaSelectionModeType.Albums -> onClearAlbumsSelection()
            MediaSelectionModeType.Videos -> onClearVideosSelection()
            MediaSelectionModeType.Playlists -> onClearPlaylistsSelection()
            else -> Unit
        }
    }
}

enum class MediaSelectionModeType {
    None,
    Timeline,
    Albums,
    Videos,
    Playlists;

    companion object {
        internal fun MediaSelectionModeType.isAnActiveSelection(): Boolean =
            this == Timeline || this == Albums || this == Videos || this == Playlists
    }
}
