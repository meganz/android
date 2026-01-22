package mega.privacy.android.feature.photos.presentation.handler

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun MediaSelectionModelHandler(
    type: MediaSelectionModeType,
    onClearTimelinePhotosSelection: () -> Unit,
    onClearAlbumsSelection: () -> Unit,
    onClearVideosSelection: () -> Unit,
    onClearPlaylistsSelection: () -> Unit,
    setNavigationItemVisibility: (Boolean) -> Unit,
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

    LaunchedEffect(key1 = type) {
        setNavigationItemVisibility(type == MediaSelectionModeType.None)
    }
}

enum class MediaSelectionModeType {
    None,
    Timeline,
    Albums,
    Videos,
    Playlists
}
