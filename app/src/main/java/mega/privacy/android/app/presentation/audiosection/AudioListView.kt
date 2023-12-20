package mega.privacy.android.app.presentation.audiosection

import mega.privacy.android.core.R as coreR
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.audiosection.model.UIAudio
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.icon.pack.R
import mega.privacy.android.legacy.core.ui.controls.lists.HeaderViewItem
import mega.privacy.android.legacy.core.ui.controls.lists.NodeListViewItem
import nz.mega.sdk.MegaNode

@Composable
internal fun AudioListView(
    items: List<UIAudio>,
    lazyListState: LazyListState,
    sortOrder: String,
    modifier: Modifier,
    onChangeViewTypeClick: () -> Unit,
    onClick: (item: UIAudio, index: Int) -> Unit,
    onMenuClick: (UIAudio) -> Unit,
    onSortOrderClick: () -> Unit,
    onLongClick: ((item: UIAudio, index: Int) -> Unit) = { _, _ -> },
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
                isSelected = audioItem.isSelected,
                folderInfo = null,
                icon = R.drawable.ic_audio_list,
                infoIcon = if (audioItem.hasVersions) coreR.drawable.ic_version_small else null,
                fileSize = formatFileSize(audioItem.size, LocalContext.current),
                modifiedDate = formatModifiedDate(
                    java.util.Locale(
                        Locale.current.language, Locale.current.region
                    ),
                    audioItem.modificationTime
                ),
                name = audioItem.name,
                isTakenDown = audioItem.isTakenDown,
                showMenuButton = true,
                thumbnailData = if (audioItem.thumbnail?.exists() == true) {
                    audioItem.thumbnail
                } else {
                    ThumbnailRequest(audioItem.id)
                },
                isFavourite = audioItem.isFavourite,
                isSharedWithPublicLink = audioItem.isExported,
                labelColor = if (audioItem.label != MegaNode.NODE_LBL_UNKNOWN)
                    colorResource(
                        id = MegaNodeUtil.getNodeLabelColor(
                            audioItem.label
                        )
                    ) else null,
                nodeAvailableOffline = audioItem.nodeAvailableOffline,
                onClick = { onClick(audioItem, it) },
                onLongClick = { onLongClick(audioItem, it) },
                onMenuClick = { onMenuClick(audioItem) },
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