package mega.privacy.android.app.presentation.videoplayer.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaCheckbox
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import timber.log.Timber

@Composable
internal fun SubtitleFileInfoListView(
    subtitleInfoList: List<SubtitleFileInfoItem>,
    hiddenNodesEnabled: Boolean,
    onClicked: (SubtitleFileInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Timber.d("render SubtitleFileInfoListView")
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            count = subtitleInfoList.size,
            key = { subtitleInfoList[it].subtitleFileInfo.id },
            itemContent = {
                SubtitleFileInfoListItem(
                    subtitleFileInfoItem = subtitleInfoList[it],
                    hiddenNodesEnabled = hiddenNodesEnabled,
                    onSubtitleFileInfoClicked = onClicked,
                )
            },
        )
    }
}

@Composable
internal fun SubtitleFileInfoListItem(
    subtitleFileInfoItem: SubtitleFileInfoItem,
    hiddenNodesEnabled: Boolean,
    onSubtitleFileInfoClicked: (SubtitleFileInfo) -> Unit,
) {
    val isSensitive = isSensitiveSubtitleItem(hiddenNodesEnabled, subtitleFileInfoItem)

    GenericListItem(
        modifier = Modifier.alpha(if (isSensitive) 0.5f else 1f),
        title = {
            MegaText(
                text = subtitleFileInfoItem.subtitleFileInfo.name,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                textColor = TextColor.Primary,
                style = AppTheme.typography.bodyLarge
            )
        },
        subtitle = {
            subtitleFileInfoItem.subtitleFileInfo.parentName?.let { subtitle ->
                MegaText(
                    text = subtitle,
                    textColor = TextColor.Secondary,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        },
        leadingElement = {
            MegaIcon(
                modifier = Modifier
                    .size(32.dp)
                    .blur(if (isSensitive) 16.dp else 0.dp),
                painter = painterResource(iconPackR.drawable.ic_text_medium_solid),
                contentDescription = null,
                tint = IconColor.Secondary,
            )
        },
        trailingElement = {
            if (subtitleFileInfoItem.selected) {
                MegaCheckbox(
                    modifier = Modifier.testTag(VIDEO_PLAYER_SELECT_SUBTITLE_ITEM_CHECKBOX_TEST_TAG),
                    checked = true,
                    onCheckedChange = { onSubtitleFileInfoClicked(subtitleFileInfoItem.subtitleFileInfo) },
                    rounded = false,
                )
            }
        },
        onClickListener = {
            onSubtitleFileInfoClicked(subtitleFileInfoItem.subtitleFileInfo)
        },
    )
}

private fun isSensitiveSubtitleItem(
    hiddenNodesEnabled: Boolean,
    subtitleFileInfoItem: SubtitleFileInfoItem,
) = hiddenNodesEnabled
        && (subtitleFileInfoItem.subtitleFileInfo.isMarkedSensitive
        || subtitleFileInfoItem.subtitleFileInfo.isSensitiveInherited)

private val previewSubtitleFileInfo = SubtitleFileInfo(
    id = 1L,
    name = "subtitle.srt",
    url = null,
    parentName = "Movies",
    isMarkedSensitive = false,
    isSensitiveInherited = false,
)

@CombinedThemePreviews
@Composable
private fun SubtitleFileInfoListViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SubtitleFileInfoListView(
            subtitleInfoList = listOf(
                SubtitleFileInfoItem(subtitleFileInfo = previewSubtitleFileInfo),
                SubtitleFileInfoItem(
                    subtitleFileInfo = previewSubtitleFileInfo.copy(
                        id = 2L,
                        name = "subtitles_extended.srt",
                        parentName = "Downloads",
                    ),
                    selected = true,
                ),
            ),
            hiddenNodesEnabled = false,
            onClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SubtitleFileInfoListItemPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SubtitleFileInfoListItem(
            subtitleFileInfoItem = SubtitleFileInfoItem(subtitleFileInfo = previewSubtitleFileInfo),
            hiddenNodesEnabled = false,
            onSubtitleFileInfoClicked = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SubtitleFileInfoListItemSelectedPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SubtitleFileInfoListItem(
            subtitleFileInfoItem = SubtitleFileInfoItem(
                subtitleFileInfo = previewSubtitleFileInfo,
                selected = true,
            ),
            hiddenNodesEnabled = false,
            onSubtitleFileInfoClicked = {},
        )
    }
}
