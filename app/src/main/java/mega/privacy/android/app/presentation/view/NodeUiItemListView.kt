package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode

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
private fun ListViewItem(
    modifier: Modifier,
    nodeUIItem: NodeUIItem,
    stringUtilWrapper: StringUtilWrapper,
    onMenuClick: () -> Unit,
    onItemClicked: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
) {
    Column(
        modifier = modifier
            .combinedClickable(
                onClick = { onItemClicked(nodeUIItem.id.longValue) },
                onLongClick = { onLongClick(nodeUIItem.id.longValue) }
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
                    val painter = if (nodeUIItem.isIncomingShare) {
                        painterResource(id = R.drawable.ic_folder_incoming)
                    } else if (nodeUIItem.isExported) {
                        painterResource(id = R.drawable.ic_folder_outgoing)
                    } else {
                        painterResource(id = R.drawable.ic_folder_list)
                    }
                    Image(
                        modifier = thumbNailModifier
                            .testTag("Folder Tag"),
                        painter = painter,
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
                        color = if (nodeUIItem.isTakenDown) colorResource(id = R.color.red_800_red_400) else if (MaterialTheme.colors.isLight) colorResource(
                            id = R.color.grey_alpha_087
                        ) else colorResource(
                            id = R.color.white
                        ),
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
                                if (MaterialTheme.colors.isLight) colorResource(id = R.color.grey_alpha_060) else colorResource(
                                    id = R.color.white_alpha_060
                                )
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
                    color = if (MaterialTheme.colors.isLight) colorResource(id = R.color.grey_alpha_060) else colorResource(
                        id = R.color.white_alpha_060
                    ),
                    modifier = Modifier.testTag("Info Text"),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.ic_dots_vertical_grey),
                contentDescription = "3 dots",
                modifier = Modifier.clickable { onMenuClick.invoke() }
            )
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .absolutePadding(left = 64.dp),
            color = colorResource(id = R.color.grey_012_white_012), thickness = 1.dp
        )
    }
}

/**
 * /**
 * List view for file/folder list
 * @param modifier [Modifier]
 * @param nodeUIItem [NodeUIItem]
 * @param stringUtilWrapper [StringUtilWrapper] to format Info
 * @param onLongClick onLongItemClick
 * @param onItemClicked itemClick
 * @param onMenuClick three dots click
*/
 */
@Composable
fun ListView(
    modifier: Modifier,
    nodeUIItem: List<NodeUIItem>,
    stringUtilWrapper: StringUtilWrapper,
    onMenuClick: () -> Unit,
    onItemClicked: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
) {
    LazyColumn {
        items(nodeUIItem.size) {
            ListViewItem(
                modifier = modifier,
                nodeUIItem = nodeUIItem[it],
                stringUtilWrapper = stringUtilWrapper,
                onMenuClick,
                onItemClicked,
                onLongClick
            )
        }
    }
}