package mega.privacy.android.feature.mediaplayer.presentation

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.feature.mediaplayer.components.AudioPlayerGradientBackground
import mega.privacy.android.feature.mediaplayer.components.AudioPlayerWindowEffect
import mega.privacy.android.feature.mediaplayer.data.model.AudioQueueItem
import mega.privacy.android.feature.mediaplayer.presentation.model.AudioPlayerQueueUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun AudioPlayerQueueScreen(
    uiState: AudioPlayerQueueUiState,
    onBack: () -> Unit,
    onQueueItemClick: (Int) -> Unit,
    onSetContinuousPlayback: (Boolean) -> Unit,
    onMoreActionsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    AndroidTheme(isDark = true, useLegacyStatusBarColor = false) {
        AudioPlayerWindowEffect()
        AudioPlayerGradientBackground(
            modifier = modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true },
            reversed = true,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AudioPlayerTopBar(
                    onBackPressed = onBack,
                    onMoreActionsClicked = onMoreActionsClicked,
                )

                if (uiState is AudioPlayerQueueUiState.Data) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = lazyListState,
                        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                    ) {
                        item {
                            ContinuousPlaybackRow(
                                isEnabled = uiState.isContinuousPlayback,
                                // Ignore toggle changes mid-scroll — they are almost always
                                // accidental touches while flinging the list.
                                onToggle = { enabled ->
                                    if (!lazyListState.isScrollInProgress) {
                                        onSetContinuousPlayback(enabled)
                                    }
                                },
                            )
                        }
                        item {
                            SubtleDivider()
                        }
                        item {
                            CurrentlyPlayingRow(
                                title = uiState.currentTitle,
                                artist = uiState.currentArtist,
                                artworkUri = uiState.currentArtworkUri,
                                thumbnailData = uiState.currentThumbnailData,
                            )
                        }
                        item {
                            PlayingFromHeader(playingFromName = uiState.playingFromName)
                        }
                        itemsIndexed(
                            items = uiState.items,
                            key = { _, item -> item.mediaId },
                        ) { index, item ->
                            QueueItemRow(
                                item = item,
                                isCurrentlyPlaying = index == uiState.currentQueueIndex,
                                onClick = { onQueueItemClick(index) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinuousPlaybackRow(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MegaText(
                text = stringResource(sharedR.string.audio_player_continuous_playback_title),
                style = MaterialTheme.typography.bodyLarge,
                textColor = TextColor.Primary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            MegaText(
                text = stringResource(sharedR.string.audio_player_continuous_playback_description),
                style = MaterialTheme.typography.bodyMedium,
                textColor = TextColor.Secondary,
            )
        }
        Toggle(
            isChecked = isEnabled,
            onCheckedChange = { onToggle(it) },
        )
    }
}

@Composable
private fun CurrentlyPlayingRow(
    title: String?,
    artist: String?,
    artworkUri: String?,
    thumbnailData: ThumbnailData?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (artworkUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artworkUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    error = painterResource(iconPackR.drawable.ic_audio_medium_solid),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                NodeThumbnailView(
                    data = thumbnailData,
                    contentDescription = null,
                    defaultImage = iconPackR.drawable.ic_audio_medium_solid,
                    layoutType = ThumbnailLayoutType.FullSize,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            MegaText(
                text = title ?: "",
                style = MaterialTheme.typography.titleLarge,
                textColor = TextColor.Primary,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
            if (!artist.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                MegaText(
                    text = artist,
                    style = MaterialTheme.typography.titleSmall,
                    textColor = TextColor.Secondary,
                )
            }
        }
    }
}

@Composable
private fun PlayingFromHeader(
    playingFromName: String?,
    modifier: Modifier = Modifier,
) {
    if (playingFromName.isNullOrBlank()) return
    MegaSpannedText(
        value = stringResource(sharedR.string.audio_player_playing_from_label, playingFromName),
        baseStyle = MaterialTheme.typography.titleMedium,
        styles = mapOf(SpanIndicator('A') to MegaSpanStyle(color = TextColor.Primary)),
        color = TextColor.Secondary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        maxLines = 1,
    )
}

@Composable
private fun QueueItemRow(
    item: AudioQueueItem,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            NodeThumbnailView(
                data = item.handle?.let { ThumbnailRequest.fromHandle(it) },
                contentDescription = null,
                defaultImage = iconPackR.drawable.ic_audio_medium_solid,
                layoutType = ThumbnailLayoutType.FullSize,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isCurrentlyPlaying) {
                    MegaIcon(
                        painter = rememberVectorPainter(IconPack.Small.Thin.Outline.Waveform),
                        tint = IconColor.Primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                MegaText(
                    text = item.title ?: item.mediaId,
                    style = MaterialTheme.typography.titleMedium,
                    textColor = TextColor.Primary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
            }
            if (!item.artist.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                MegaText(
                    text = item.artist,
                    style = MaterialTheme.typography.bodySmall,
                    textColor = TextColor.Secondary,
                )
            }
        }

        MegaIcon(
            painter = rememberVectorPainter(IconPack.Small.Thin.Outline.QueueLine),
            tint = IconColor.Secondary,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview
@Composable
private fun AudioPlayerQueueScreenPreview() {
    AudioPlayerQueueScreen(
        uiState = AudioPlayerQueueUiState.Data(
            currentTitle = "Track A",
            currentArtist = "Artist A",
            playingFromName = "My Music",
            isContinuousPlayback = true,
            currentQueueIndex = 0,
            currentArtworkUri = null,
            currentThumbnailData = null,
            items = listOf(
                AudioQueueItem(mediaId = "a", title = "Track A", artist = "Artist A"),
                AudioQueueItem(mediaId = "b", title = "Track B", artist = "Artist B"),
                AudioQueueItem(mediaId = "c", title = "Track C", artist = null),
            ),
        ),
        onBack = {},
        onQueueItemClick = {},
        onSetContinuousPlayback = {},
        onMoreActionsClicked = {},
    )
}

@Preview
@Composable
private fun QueueItemRowPlayingPreview() {
    QueueItemRow(
        item = AudioQueueItem(mediaId = "a", title = "Track A", artist = "Artist A"),
        isCurrentlyPlaying = true,
        onClick = {},
    )
}

@Preview
@Composable
private fun QueueItemRowIdlePreview() {
    QueueItemRow(
        item = AudioQueueItem(mediaId = "b", title = "Track B", artist = null),
        isCurrentlyPlaying = false,
        onClick = {},
    )
}
