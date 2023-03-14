package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.core.ui.theme.extensions.grey_087_white
import mega.privacy.android.core.ui.theme.extensions.red_800_red_400
import mega.privacy.android.core.ui.theme.white_alpha_005
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Grid view item for file/folder info
 * @param modifier [Modifier]
 * @param nodeUIItem [NodeUIItem]
 * @param onLongClick onLongItemClick
 * @param onItemClicked itemClick
 * @param onMenuClick three dots click
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NodeGridViewItem(
    modifier: Modifier,
    nodeUIItem: NodeUIItem,
    onMenuClick: () -> Unit,
    onItemClicked: (NodeId) -> Unit,
    onLongClick: (NodeId) -> Unit,
) {
    if (nodeUIItem.node is FolderNode) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 2.dp,
                    color = if (nodeUIItem.isSelected) MaterialTheme.colors.secondary else white_alpha_005,
                    shape = RoundedCornerShape(5.dp)
                )
                .combinedClickable(
                    onClick = { onItemClicked(nodeUIItem.id) },
                    onLongClick = { onLongClick(nodeUIItem.id) }
                )
                .padding(horizontal = 16.dp)
                .alpha(if (nodeUIItem.isInvisible) 0f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = if (nodeUIItem.isSelected) painterResource(id = R.drawable.ic_select_folder) else getPainter(
                    nodeUIItem = nodeUIItem.node
                ),
                contentDescription = "Folder",
                modifier = Modifier
                    .height(24.dp)
                    .width(24.dp)
                    .align(Alignment.CenterVertically)
            )
            Text(
                text = nodeUIItem.name,
                style = MaterialTheme.typography.subtitle1,
                maxLines = 1,
                color = if (nodeUIItem.isTakenDown) MaterialTheme.colors.red_800_red_400 else MaterialTheme.colors.grey_087_white,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            MenuItem(onMenuClick)
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = if (nodeUIItem.isSelected) MaterialTheme.colors.secondary else white_alpha_005,
                    shape = RoundedCornerShape(16.dp)
                )
                .combinedClickable(
                    onClick = { onItemClicked(nodeUIItem.id) },
                    onLongClick = { onLongClick(nodeUIItem.id) }
                )
        ) {
            Box(contentAlignment = Alignment.TopStart) {
                Image(
                    bitmap = ThumbnailUtils.getThumbnailFromCache(nodeUIItem.id.longValue)
                        .asImageBitmap(),
                    contentDescription = "File",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(172.dp)
                        .fillMaxSize()
                )
                if (nodeUIItem.isSelected) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_select_folder),
                        contentDescription = "checked",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = nodeUIItem.name,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 1,
                    color = if (nodeUIItem.isTakenDown) MaterialTheme.colors.red_800_red_400 else MaterialTheme.colors.grey_087_white
                )
                Spacer(modifier = Modifier.weight(1f))
                MenuItem(onMenuClick)
            }
        }
    }
}


