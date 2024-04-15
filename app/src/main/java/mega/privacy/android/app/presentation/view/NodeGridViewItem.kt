package mega.privacy.android.app.presentation.view

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.core.R as CoreUiR
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.view.extension.getPainter
import mega.privacy.android.core.ui.controls.images.ThumbnailView
import mega.privacy.android.legacy.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.core.ui.theme.extensions.background_white_alpha_005
import mega.privacy.android.core.ui.theme.extensions.color_button_brand
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.red_800_red_400
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.grey_alpha_040
import mega.privacy.android.core.ui.theme.transparent
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper

/**
 * Grid view item for file/folder info
 * @param modifier [Modifier]
 * @param nodeUIItem [NodeUIItem]
 * @param onLongClick onLongItemClick
 * @param onItemClicked itemClick
 * @param onMenuClick three dots click
 * @param thumbnailData Thumbnail data to get Thumbnail (File, Uri, ThumbnailRequest)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <T : TypedNode> NodeGridViewItem(
    modifier: Modifier,
    nodeUIItem: NodeUIItem<T>,
    onMenuClick: (NodeUIItem<T>) -> Unit,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    onLongClick: (NodeUIItem<T>) -> Unit,
    thumbnailData: Any?,
    fileTypeIconMapper: FileTypeIconMapper,
) {
    if (nodeUIItem.node is FolderNode) {
        ConstraintLayout(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .alpha(if (nodeUIItem.isInvisible) 0f else 1f)
                .border(
                    width = 1.dp,
                    color = if (nodeUIItem.isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                    shape = RoundedCornerShape(5.dp)
                )
                .background(MaterialTheme.colors.background_white_alpha_005)
                .combinedClickable(
                    onClick = { onItemClicked(nodeUIItem) },
                    onLongClick = { onLongClick(nodeUIItem) }
                )
                .padding(start = 8.dp),
        ) {
            val (menuImage, txtTitle, thumbImage, takenDownImage) = createRefs()
            Image(
                painter = painterResource(id = CoreUiR.drawable.ic_dots_vertical_grey),
                contentDescription = "3 dots",
                modifier = Modifier
                    .clickable { onMenuClick.invoke(nodeUIItem) }
                    .constrainAs(menuImage) {
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .testTag(FOLDER_VIEW_MORE_ICON_TEST_TAG),
            )
            Image(
                painter = if (nodeUIItem.isSelected) painterResource(id = CoreUiR.drawable.ic_select_folder) else nodeUIItem.node.getPainter(
                    fileTypeIconMapper
                ),
                contentDescription = "Folder",
                modifier = Modifier
                    .height(24.dp)
                    .width(24.dp)
                    .constrainAs(thumbImage) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .testTag(SELECTED_FOLDER_TEST_TAG)
            )
            Image(
                modifier = Modifier
                    .constrainAs(takenDownImage) {
                        end.linkTo(menuImage.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        visibility =
                            if (nodeUIItem.isTakenDown) Visibility.Visible else Visibility.Gone
                    }
                    .height(16.dp)
                    .width(16.dp)
                    .testTag(FOLDER_VIEW_TAKEN_TEST_TAG),
                painter = painterResource(id = iconPackR.drawable.ic_alert_triangle_medium_regular_outline),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.color_button_brand),
                contentDescription = "Taken Down")
            MiddleEllipsisText(
                text = nodeUIItem.name,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .constrainAs(txtTitle) {
                        end.linkTo(takenDownImage.start)
                        start.linkTo(thumbImage.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.fillToConstraints
                    }
                    .testTag(FOLDER_VIEW_NODE_TITLE_TEXT_TEST_TAG),
                style = MaterialTheme.typography.subtitle2,
                color = if (nodeUIItem.isTakenDown) MaterialTheme.colors.red_800_red_400 else MaterialTheme.colors.textColorPrimary
            )

        }
    } else if (nodeUIItem.node is FileNode) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (nodeUIItem.isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                    shape = RoundedCornerShape(5.dp)
                )
                .background(MaterialTheme.colors.background_white_alpha_005)
                .combinedClickable(
                    onClick = { onItemClicked(nodeUIItem) },
                    onLongClick = { onLongClick(nodeUIItem) }
                )
        ) {
            Box(contentAlignment = Alignment.TopStart) {
                ThumbnailView(
                    modifier = Modifier
                        .height(172.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .padding(1.dp)
                        .testTag(THUMBNAIL_FILE_TEST_TAG),
                    contentDescription = "File",
                    data = thumbnailData,
                    defaultImage = fileTypeIconMapper(nodeUIItem.node.type.extension),
                    contentScale = ContentScale.Crop,
                )
                nodeUIItem.fileDuration?.let {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        transparent,
                                        grey_alpha_040
                                    )
                                )
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier.testTag(VIDEO_PLAY_ICON_TEST_TAG),
                            painter = painterResource(id = R.drawable.ic_play_arrow_white_24dp),
                            contentDescription = "Video duration",
                            colorFilter = ColorFilter.tint(color = MaterialTheme.colors.textColorPrimary)
                        )
                        Text(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .testTag(VIDEO_DURATION_TEST_TAG),
                            text = it,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.textColorPrimary
                        )
                    }
                }
                if (nodeUIItem.isSelected) {
                    Image(
                        painter = painterResource(id = CoreUiR.drawable.ic_select_folder),
                        contentDescription = "checked",
                        modifier = Modifier
                            .padding(12.dp)
                            .testTag(SELECTED_FILE_TEST_TAG)
                    )
                }
            }
            Divider(
                color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                modifier = Modifier.height(1.dp)
            )
            ConstraintLayout(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                val (menuImage, txtTitle, takenDownImage) = createRefs()
                Image(
                    painter = painterResource(id = CoreUiR.drawable.ic_dots_vertical_grey),
                    contentDescription = "3 dots",
                    modifier = Modifier
                        .clickable { onMenuClick.invoke(nodeUIItem) }
                        .constrainAs(menuImage) {
                            end.linkTo(parent.end)
                        }
                        .testTag(FILE_VIEW_MORE_ICON_TEST_TAG)
                )
                Image(
                    modifier = Modifier
                        .constrainAs(takenDownImage) {
                            end.linkTo(menuImage.start)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            visibility =
                                if (nodeUIItem.isTakenDown) Visibility.Visible else Visibility.Gone
                        }
                        .height(16.dp)
                        .width(16.dp)
                        .testTag(FILE_VIEW_TAKEN_TEST_TAG),
                    painter = painterResource(id = iconPackR.drawable.ic_alert_triangle_medium_regular_outline),
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.color_button_brand),
                    contentDescription = "Taken Down")
                MiddleEllipsisText(
                    text = nodeUIItem.name,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .constrainAs(txtTitle) {
                            start.linkTo(parent.start)
                            end.linkTo(takenDownImage.start)
                            width = Dimension.fillToConstraints
                        }
                        .testTag(FILE_VIEW_NODE_TITLE_TEXT_TEST_TAG),
                    style = MaterialTheme.typography.subtitle2,
                    color = if (nodeUIItem.isTakenDown) MaterialTheme.colors.red_800_red_400 else MaterialTheme.colors.textColorPrimary
                )
            }
        }
    }
}


/**
 * Test tag for node title
 */
const val FILE_VIEW_NODE_TITLE_TEXT_TEST_TAG = "node_grid_view_item:file_view_node_title"

/**
 * Test tag for node title
 */
const val FOLDER_VIEW_NODE_TITLE_TEXT_TEST_TAG = "node_grid_view_item:folder_view_node_title"

/**
 * Text tag for selected item
 */
const val SELECTED_FOLDER_TEST_TAG = "node_grid_view_item:folder_selected"

/**
 * Text tag for selected item
 */
const val SELECTED_FILE_TEST_TAG = "node_grid_view_item:file_selected"


/**
 * Test tag for file item
 */
const val THUMBNAIL_FILE_TEST_TAG = "node_grid_view_item:thumbnail_file"


/**
 * Test tag for taken item for file
 */
const val FILE_VIEW_TAKEN_TEST_TAG = "node_grid_view_item:file_view_icon_taken"

/**
 * Test tag for taken item for folder
 */
const val FOLDER_VIEW_TAKEN_TEST_TAG = "node_grid_view_item:folder_view_icon_taken"

/**
 * Test tag for more icon
 */
const val FILE_VIEW_MORE_ICON_TEST_TAG = "node_grid_view_item:file_view_more_icon"

/**
 * Test tag for more icon
 */
const val FOLDER_VIEW_MORE_ICON_TEST_TAG = "node_grid_view_item:folder_view_more_icon"

/**
 * Test tag for video play icon
 */
const val VIDEO_PLAY_ICON_TEST_TAG = "node_grid_view_item:video_play_icon"

/**
 * Test tag for video duration
 */
const val VIDEO_DURATION_TEST_TAG = "node_grid_view_item:video_duration"



