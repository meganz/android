package mega.privacy.android.app.presentation.photos.albums.add

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.view.VideoSectionLoadingView
import mega.privacy.android.app.presentation.videosection.view.playlist.CreateVideoPlaylistDialog
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistInfoView
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistThumbnailView
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_050_grey_800
import mega.android.core.ui.theme.values.TextColor

@Composable
internal fun AddToPlaylistPage(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    playlists: List<VideoPlaylistUIEntity>,
    selectedPlaylist: VideoPlaylistUIEntity?,
    isCreatingPlaylist: Boolean,
    playlistNameSuggestion: String,
    playlistNameErrorMessageRes: Int?,
    onSelectPlaylist: (VideoPlaylistUIEntity) -> Unit,
    onSetupNewPlaylist: (String) -> Unit,
    onCancelPlaylistCreation: () -> Unit,
    onClearPlaylistNameErrorMessage: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
) {
    val defaultPlaylistName = stringResource(sharedR.string.video_to_playlist_new_playlist_text)

    if (isCreatingPlaylist) {
        CreateVideoPlaylistDialog(
            title = stringResource(id = sharedR.string.video_section_playlists_create_playlist_dialog_title),
            positiveButtonText = stringResource(id = R.string.general_create),
            inputPlaceHolderText = { playlistNameSuggestion },
            errorMessage = playlistNameErrorMessageRes,
            onDialogInputChange = { onClearPlaylistNameErrorMessage() },
            onDismissRequest = {
                onCancelPlaylistCreation()
                onClearPlaylistNameErrorMessage()
            },
            onDialogPositiveButtonClicked = {
                val playlistName = it.trim().ifEmpty { playlistNameSuggestion }
                onCreatePlaylist(playlistName)
            },
            isInputValid = { playlistNameErrorMessageRes == null },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface),
        content = {
            TextMegaButton(
                text = stringResource(id = sharedR.string.video_to_playlist_new_playlist_text),
                onClick = { onSetupNewPlaylist(defaultPlaylistName) },
                contentPadding = PaddingValues(horizontal = 16.dp)
            )

            if (isLoading) {
                VideoSectionLoadingView()
            } else if (playlists.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = {
                        items(
                            count = playlists.size,
                            key = { index ->
                                "${playlists[index].id}"
                            },
                            itemContent = { index ->
                                val playlist = playlists[index]
                                PlaylistCard(
                                    playlist = playlist,
                                    isSelected = playlist.id == selectedPlaylist?.id,
                                    onSelect = onSelectPlaylist,
                                )
                            },
                        )
                    },
                )
            }
        },
    )
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Image(
                painter = painterResource(id = iconPackR.drawable.ic_playlist_glass),
                contentDescription = null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            MegaText(
                text = stringResource(sharedR.string.video_section_playlists_empty_hint_playlist),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body1,
            )
        },
    )
}

@Composable
private fun PlaylistCard(
    modifier: Modifier = Modifier,
    playlist: VideoPlaylistUIEntity,
    isSelected: Boolean,
    onSelect: (VideoPlaylistUIEntity) -> Unit,
) {
    Row(
        modifier = modifier.clickable { onSelect(playlist) },
        verticalAlignment = Alignment.CenterVertically,
        content = {
            VideoPlaylistThumbnailView(
                modifier = Modifier
                    .width(126.dp)
                    .aspectRatio(1.77f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colors.grey_050_grey_800),
                emptyPlaylistIcon = iconPackR.drawable.ic_video_playlist_default_thumbnail,
                noThumbnailIcon = iconPackR.drawable.ic_video_playlist_no_thumbnail,
                thumbnailList = playlist.thumbnailList?.map { ThumbnailRequest(it) },
            )

            VideoPlaylistInfoView(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                title = playlist.title,
                numberOfVideos = playlist.numberOfVideos,
                totalDuration = playlist.totalDuration.ifEmpty { "00:00:00" },
            )

            RadioButton(
                selected = isSelected,
                onClick = null,
                modifier = Modifier.padding(8.dp),
                colors = RadioButtonDefaults.colors(
                    unselectedColor = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                ),
            )
        },
    )
}
