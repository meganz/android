package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.photos.albums.view.MiddleEllipsisText
import mega.privacy.android.app.presentation.view.extension.fileSize
import mega.privacy.android.app.presentation.view.extension.folderInfo
import mega.privacy.android.app.presentation.view.extension.formattedModifiedDate
import mega.privacy.android.app.presentation.view.extension.getPainter
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.red_800_red_400
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import java.io.File

/**
 * List view item for file/folder info
 * @param modifier [Modifier]
 * @param nodeUIItem [NodeUIItem]
 * @param onLongClick onLongItemClick
 * @param onItemClicked itemClick
 * @param onMenuClick three dots click
 * @param imageState Thumbnail state
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <T : TypedNode> NodeListViewItem(
    modifier: Modifier,
    nodeUIItem: NodeUIItem<T>,
    onMenuClick: (NodeUIItem<T>) -> Unit,
    onItemClicked: (NodeUIItem<T>) -> Unit,
    onLongClick: (NodeUIItem<T>) -> Unit,
    imageState: State<File?>,
) {
    Column(
        modifier = modifier
            .combinedClickable(
                onClick = { onItemClicked(nodeUIItem) },
                onLongClick = { onLongClick(nodeUIItem) }
            )
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbNailModifier = Modifier
                .height(48.dp)
                .width(48.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
            if (nodeUIItem.isSelected) {
                Image(
                    modifier = thumbNailModifier
                        .testTag(SELECTED_TEST_TAG),
                    painter = painterResource(R.drawable.ic_select_folder),
                    contentDescription = "Selected",
                )
            } else {
                if (nodeUIItem.node is FolderNode) {
                    Image(
                        modifier = thumbNailModifier
                            .testTag(FOLDER_TEST_TAG),
                        painter = nodeUIItem.node.getPainter(),
                        contentDescription = "Folder Thumbnail"
                    )
                } else if (nodeUIItem.node is FileNode) {
                    imageState.value
                    ThumbnailView<T>(
                        modifier = thumbNailModifier
                            .testTag(FILE_TEST_TAG),
                        imageFile = imageState.value,
                        node = nodeUIItem,
                        contentDescription = "Thumbnail"
                    )
                }
            }
            Column(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                ConstraintLayout(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val (nodeInfo, threeDots) = createRefs()
                    Image(
                        painter = painterResource(id = R.drawable.ic_dots_vertical_grey),
                        contentDescription = "3 dots",
                        modifier = Modifier
                            .constrainAs(threeDots) {
                                end.linkTo(parent.end)
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                            }
                            .clickable { onMenuClick.invoke(nodeUIItem) }
                    )
                    Row(modifier = Modifier
                        .constrainAs(nodeInfo) {
                            top.linkTo(parent.top)
                            end.linkTo(threeDots.start)
                            start.linkTo(parent.start)
                            width = Dimension.fillToConstraints
                        }
                        .padding(end = 4.dp)) {
                        val iconModifier = Modifier
                            .align(Alignment.CenterVertically)
                            .absolutePadding(left = 4.dp)
                        MiddleEllipsisText(
                            text = nodeUIItem.name,
                            style = MaterialTheme.typography.subtitle1,
                            color = if (nodeUIItem.isTakenDown) MaterialTheme.colors.red_800_red_400 else MaterialTheme.colors.textColorPrimary,
                            maxLines = 1
                        )

                        if (nodeUIItem.isFavourite) {
                            Image(
                                alignment = Alignment.Center,
                                modifier = iconModifier
                                    .testTag(FAVORITE_TEST_TAG),
                                painter = painterResource(id = R.drawable.ic_favorite),
                                contentDescription = "Favorite",

                                )
                        }
                        if (nodeUIItem.exportedData != null) {
                            Image(
                                alignment = Alignment.Center,
                                modifier = iconModifier
                                    .testTag(EXPORTED_TEST_TAG),
                                painter = painterResource(id = R.drawable.link_ic),
                                contentDescription = "Link",
                                colorFilter = ColorFilter.tint(
                                    MaterialTheme.colors.textColorSecondary
                                )
                            )
                        }
                        if (nodeUIItem.isTakenDown) {
                            Image(
                                alignment = Alignment.Center,
                                modifier = iconModifier
                                    .testTag(TAKEN_TEST_TAG),
                                painter = painterResource(id = R.drawable.ic_taken_down),
                                contentDescription = "Taken Down",
                            )
                        }
                    }
                }
                Text(
                    text = if (nodeUIItem.node is FolderNode) {
                        nodeUIItem.node.folderInfo()
                    } else {
                        val fileItem = nodeUIItem.node as FileNode
                        val current = Locale.current
                        val javaLocale = java.util.Locale(
                            current.language, current.region
                        )
                        "${fileItem.fileSize()} · ${fileItem.formattedModifiedDate(javaLocale)}"
                    },
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.textColorSecondary,
                    modifier = Modifier
                        .testTag(INFO_TEXT_TEST_TAG)
                        .padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 64.dp, top = 8.dp),
            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012, thickness = 1.dp
        )
    }
}

/**
 * Test tag for info text
 */
const val INFO_TEXT_TEST_TAG = "Info Text"

/**
 * Text tag for selected item
 */
const val SELECTED_TEST_TAG = "Selected Tag"

/**
 * Test tag for folder item
 */
const val FOLDER_TEST_TAG = "Folder Tag"

/**
 * Test tag for file item
 */
const val FILE_TEST_TAG = "File Tag"

/**
 * Test tag for favorite item
 */
const val FAVORITE_TEST_TAG = "favorite Tag"

/**
 * Test tag for exported item
 */
const val EXPORTED_TEST_TAG = "exported Tag"

/**
 * Test tag for taken item
 */
const val TAKEN_TEST_TAG = "taken Tag"