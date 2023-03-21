package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.core.ui.theme.extensions.grey_012_white_012
import mega.privacy.android.core.ui.theme.extensions.grey_087_white
import mega.privacy.android.core.ui.theme.extensions.grey_white_alpha_060
import mega.privacy.android.core.ui.theme.extensions.red_800_red_400
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId

/**
 * List view item for file/folder info
 * @param modifier [Modifier]
 * @param nodeUIItem [NodeUIItem]
 * @param stringUtilWrapper [StringUtilWrapper] to format Info
 * @param onLongClick onLongItemClick
 * @param onItemClicked itemClick
 * @param onMenuClick three dots click
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NodeListViewItem(
    modifier: Modifier,
    nodeUIItem: NodeUIItem,
    stringUtilWrapper: StringUtilWrapper,
    onMenuClick: () -> Unit,
    onItemClicked: (NodeUIItem) -> Unit,
    onLongClick: (NodeUIItem) -> Unit,
) {
    Column(
        modifier = modifier
            .combinedClickable(
                onClick = { onItemClicked(nodeUIItem) },
                onLongClick = { onLongClick(nodeUIItem) }
            )

            .fillMaxWidth()
            .absolutePadding(left = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbNailModifier = Modifier
                .height(48.dp)
                .width(48.dp)
            if (nodeUIItem.isSelected) {
                Image(
                    modifier = thumbNailModifier
                        .testTag("Selected Tag"),
                    painter = painterResource(R.drawable.ic_select_folder),
                    contentDescription = "Selected",
                )
            } else {
                if (nodeUIItem.node is FolderNode) {
                    Image(
                        modifier = thumbNailModifier
                            .testTag("Folder Tag"),
                        painter = getPainter(nodeUIItem = nodeUIItem.node),
                        contentDescription = "Folder Thumbnail"
                    )
                } else {
                    Image(
                        modifier = thumbNailModifier
                            .testTag("File Tag"),
                        bitmap = ThumbnailUtils.getThumbnailFromCache(nodeUIItem.id.longValue)
                            .asImageBitmap(),
                        contentDescription = "Thumbnail"
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    Text(
                        text = nodeUIItem.name,
                        style = MaterialTheme.typography.subtitle1,
                        color = if (nodeUIItem.isTakenDown) MaterialTheme.colors.red_800_red_400 else MaterialTheme.colors.grey_087_white,
                        maxLines = 1
                    )
                    val iconModifier = Modifier
                        .align(Alignment.CenterVertically)
                        .absolutePadding(left = 4.dp)
                    if (nodeUIItem.isFavourite) {
                        Image(
                            alignment = Alignment.Center,
                            modifier = iconModifier
                                .testTag("favorite Tag"),
                            painter = painterResource(id = R.drawable.ic_favorite),
                            contentDescription = "Favorite",

                            )
                    }
                    if (nodeUIItem.isExported) {
                        Image(
                            alignment = Alignment.Center,
                            modifier = iconModifier
                                .testTag("exported Tag"),
                            painter = painterResource(id = R.drawable.link_ic),
                            contentDescription = "Link",
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colors.grey_white_alpha_060
                            )
                        )
                    }
                    if (nodeUIItem.isTakenDown) {
                        Image(
                            alignment = Alignment.Center,
                            modifier = iconModifier
                                .testTag("taken Tag"),
                            painter = painterResource(id = R.drawable.ic_taken_down),
                            contentDescription = "Taken Down",
                        )
                    }
                }
                Text(
                    text = if (nodeUIItem.node is FolderNode) {
                        stringUtilWrapper.getFolderInfo(
                            nodeUIItem.node.childFolderCount,
                            nodeUIItem.node.childFileCount
                        )
                    } else {
                        val fileItem = nodeUIItem.node as FileNode
                        "${stringUtilWrapper.getSizeString(fileItem.size)} · ${
                            stringUtilWrapper.formatLongDateTime(
                                fileItem.modificationTime
                            )
                        }"
                    },
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.grey_white_alpha_060,
                    modifier = Modifier.testTag("Info Text"),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            MenuItem(onMenuClick)
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .absolutePadding(left = 64.dp),
            color = MaterialTheme.colors.grey_012_white_012, thickness = 1.dp
        )
    }
}
