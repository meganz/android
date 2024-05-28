package mega.privacy.android.app.mediaplayer.queue.view

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Visibility
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.legacy.core.ui.controls.lists.MediaQueueItemView
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun MediaQueueItemWithHeaderAndFooterView(
    @DrawableRes icon: Int,
    name: String,
    currentPlayingPosition: String,
    duration: String,
    thumbnailData: Any?,
    isSearchMode: Boolean,
    isHeaderVisible: Boolean,
    isFooterVisible: Boolean,
    queueItemType: MediaQueueItemType,
    isAudio: Boolean,
    isPaused: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .background(
                colorResource(
                    id = if (!isSearchMode && queueItemType == MediaQueueItemType.Next) {
                        if (isAudio) {
                            R.color.grey_020_grey_800
                        } else {
                            R.color.grey_800
                        }
                    } else {
                        if (isAudio) {
                            R.color.white_dark_grey
                        } else {
                            R.color.dark_grey
                        }
                    }
                )
            )
    ) {
        val (header, item, footer, divider) = createRefs()
        MegaText(
            modifier = Modifier
                .padding(start = 72.dp, top = 20.dp)
                .constrainAs(header) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    visibility =
                        if (isHeaderVisible) {
                            Visibility.Visible
                        } else {
                            Visibility.Gone
                        }
                }
                .testTag(MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG),
            text = stringResource(
                id = if (queueItemType == MediaQueueItemType.Playing) {
                    if (isPaused) {
                        R.string.audio_player_now_playing_paused
                    } else {
                        R.string.audio_player_now_playing
                    }
                } else {
                    if (isAudio) {
                        R.string.media_player_audio_playlist_previous
                    } else {
                        R.string.media_player_video_playlist_previous
                    }
                }
            ),
            textColor = TextColor.Accent,
            style = MaterialTheme.typography.body2
        )

        MediaQueueItemView(
            modifier = Modifier.constrainAs(item) {
                top.linkTo(header.bottom)
            },
            icon = icon,
            name = name,
            currentPlayingPosition = currentPlayingPosition,
            duration = duration,
            thumbnailData = thumbnailData,
            isPaused = isPaused,
            isItemPlaying = queueItemType == MediaQueueItemType.Playing,
            isReorderEnabled = !isSearchMode && queueItemType == MediaQueueItemType.Next,
            isSelected = isSelected,
            onClick = onClick
        )

        MediaQueueFooter(
            modifier = Modifier.constrainAs(footer) {
                top.linkTo(item.bottom)
                start.linkTo(parent.start)
                visibility = if (isFooterVisible) {
                    Visibility.Visible
                } else {
                    Visibility.Gone
                }
            },
            isAudio = isAudio
        )

        MediaQueueItemDivider(
            modifier = Modifier.constrainAs(divider) {
                top.linkTo(footer.bottom)
                start.linkTo(parent.start)
                visibility = if (isSearchMode || queueItemType != MediaQueueItemType.Playing) {
                    Visibility.Visible
                } else {
                    Visibility.Gone
                }
            },
            isAudio = isAudio,
            isLightColor = queueItemType == MediaQueueItemType.Previous || isSearchMode
        )
    }
}

@Composable
private fun MediaQueueFooter(
    modifier: Modifier,
    isAudio: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                colorResource(
                    id = if (isAudio) {
                        R.color.grey_020_grey_800
                    } else {
                        R.color.grey_800
                    }
                )
            )
            .testTag(MEDIA_QUEUE_ITEM_FOOTER_LAYOUT_VIEW_TEST_TAG)
    ) {
        MegaText(
            modifier = Modifier
                .padding(horizontal = 72.dp, vertical = 10.dp)
                .testTag(MEDIA_QUEUE_ITEM_FOOTER_TEXT_VIEW_TEST_TAG),
            text = stringResource(id = R.string.media_player_audio_playlist_next),
            textColor = TextColor.Accent,
            style = MaterialTheme.typography.caption
        )
    }
}

@Composable
private fun MediaQueueItemDivider(
    modifier: Modifier,
    isLightColor: Boolean,
    isAudio: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isLightColor)
                    colorResource(
                        id = if (isAudio) {
                            R.color.white_dark_grey
                        } else {
                            R.color.dark_grey
                        }
                    )
                else
                    colorResource(
                        id = if (isAudio) {
                            R.color.grey_020_grey_800
                        } else {
                            R.color.grey_800
                        }
                    )
            )
            .testTag(MEDIA_QUEUE_ITEM_DIVIDER_LAYOUT_TEST_TAG)
    ) {
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 72.dp)
                .testTag(MEDIA_QUEUE_ITEM_DIVIDER_TEST_TAG),
            color = colorResource(
                id = if (isAudio) {
                    R.color.grey_012_white_012
                } else {
                    R.color.white_alpha_012
                }
            ),
            thickness = 1.dp
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PlayingMediaQueueItemPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MediaQueueItemWithHeaderAndFooterView(
            icon = iconPackR.drawable.ic_audio_medium_solid,
            name = "Media Name",
            currentPlayingPosition = "00:00",
            duration = "10:00",
            thumbnailData = null,
            onClick = {},
            isHeaderVisible = true,
            isFooterVisible = true,
            queueItemType = MediaQueueItemType.Playing,
            isAudio = false,
            isPaused = false,
            isSelected = false,
            isSearchMode = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PausedPlayingMediaQueueItemPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MediaQueueItemWithHeaderAndFooterView(
            icon = iconPackR.drawable.ic_audio_medium_solid,
            name = "Media Name",
            currentPlayingPosition = "00:00",
            duration = "10:00",
            thumbnailData = null,
            onClick = {},
            isHeaderVisible = true,
            isFooterVisible = true,
            queueItemType = MediaQueueItemType.Playing,
            isAudio = true,
            isPaused = true,
            isSelected = true,
            isSearchMode = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FirstMediaQueueItemPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MediaQueueItemWithHeaderAndFooterView(
            icon = iconPackR.drawable.ic_audio_medium_solid,
            name = "Media Name",
            currentPlayingPosition = "00:00",
            duration = "10:00",
            thumbnailData = null,
            onClick = {},
            isHeaderVisible = true,
            isFooterVisible = false,
            queueItemType = MediaQueueItemType.Previous,
            isAudio = true,
            isPaused = false,
            isSelected = false,
            isSearchMode = false
        )
    }
}

@CombinedThemePreviews
@Composable
private fun NextMediaQueueItemPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MediaQueueItemWithHeaderAndFooterView(
            icon = iconPackR.drawable.ic_audio_medium_solid,
            name = "Media Name",
            currentPlayingPosition = "00:00",
            duration = "10:00",
            thumbnailData = null,
            onClick = {},
            isHeaderVisible = false,
            isFooterVisible = false,
            queueItemType = MediaQueueItemType.Next,
            isAudio = true,
            isPaused = false,
            isSelected = false,
            isSearchMode = false
        )
    }
}

/**
 * Test tag for the header text view of media queue item
 */
const val MEDIA_QUEUE_ITEM_HEADER_TEXT_VIEW_TEST_TAG = "media_queue_item:text_header"

/**
 * Test tag for the footer text view of media queue item
 */
const val MEDIA_QUEUE_ITEM_FOOTER_TEXT_VIEW_TEST_TAG = "media_queue_item:text_footer"

/**
 * Test tag for the footer layout view of media queue item
 */
const val MEDIA_QUEUE_ITEM_FOOTER_LAYOUT_VIEW_TEST_TAG = "media_queue_item:box_footer"

/**
 * Test tag for the divider layout of media queue item
 */
const val MEDIA_QUEUE_ITEM_DIVIDER_LAYOUT_TEST_TAG = "media_queue_item:box_divider"

/**
 * Test tag for the divider of media queue item
 */
const val MEDIA_QUEUE_ITEM_DIVIDER_TEST_TAG = "media_queue_item:divider"
