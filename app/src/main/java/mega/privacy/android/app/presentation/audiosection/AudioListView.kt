package mega.privacy.android.app.presentation.audiosection

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.audiosection.model.AudioUiEntity
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.shared.original.core.ui.controls.lists.NodeListViewItem
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.icon.pack.R
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import nz.mega.sdk.MegaNode

@Composable
internal fun AudioListView(
    items: List<AudioUiEntity>,
    accountType: AccountType?,
    lazyListState: LazyListState,
    sortOrder: String,
    modifier: Modifier,
    onChangeViewTypeClick: () -> Unit,
    onClick: (item: AudioUiEntity, index: Int) -> Unit,
    onMenuClick: (AudioUiEntity) -> Unit,
    onSortOrderClick: () -> Unit,
    inSelectionMode: Boolean = false,
    onLongClick: ((item: AudioUiEntity, index: Int) -> Unit) = { _, _ -> },
) {
    LazyColumn(state = lazyListState, modifier = modifier) {
        item(
            key = "header"
        ) {
            HeaderViewItem(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                onSortOrderClick = onSortOrderClick,
                onChangeViewTypeClick = onChangeViewTypeClick,
                onEnterMediaDiscoveryClick = {},
                sortOrder = sortOrder,
                isListView = true,
                showSortOrder = true,
                showChangeViewType = true,
                showMediaDiscoveryButton = false,
            )
        }

        items(count = items.size, key = { items[it].id.longValue }) {
            val audioItem = items[it]
            NodeListViewItem(
                title = audioItem.name,
                subtitle = formatFileSize(audioItem.size, LocalContext.current).plus(" · ")
                    .plus(
                        formatModifiedDate(
                            java.util.Locale(
                                Locale.current.language, Locale.current.region
                            ),
                            audioItem.modificationTime
                        )
                    ),
                icon = R.drawable.ic_audio_medium_solid,
                thumbnailData = if (audioItem.thumbnail?.exists() == true) {
                    audioItem.thumbnail
                } else {
                    ThumbnailRequest(audioItem.id)
                },
                isTakenDown = audioItem.isTakenDown,
                isSelected = audioItem.isSelected,
                showFavourite = audioItem.isFavourite,
                showLink = audioItem.isExported,
                labelColor = if (audioItem.label != MegaNode.NODE_LBL_UNKNOWN)
                    colorResource(
                        id = MegaNodeUtil.getNodeLabelColor(
                            audioItem.label
                        )
                    ) else null,
                onMoreClicked = { onMenuClick(audioItem) }.takeIf { inSelectionMode.not() },
                onItemClicked = { onClick(audioItem, it) },
                onLongClick = { onLongClick(audioItem, it) },
                showOffline = audioItem.nodeAvailableOffline,
                showVersion = audioItem.hasVersions,
                modifier = Modifier
                    .alpha(0.5f.takeIf {
                        accountType?.isPaid == true && (audioItem.isMarkedSensitive || audioItem.isSensitiveInherited)
                    } ?: 1f),
                isSensitive = accountType?.isPaid == true && (audioItem.isMarkedSensitive || audioItem.isSensitiveInherited),
                showBlurEffect = true,
            )
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 72.dp),
                color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                thickness = 1.dp
            )
        }
    }
}