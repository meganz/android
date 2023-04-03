@file:OptIn(ExperimentalComposeUiApi::class)

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.core.ui.theme.extensions.grey_012_white_012
import mega.privacy.android.core.ui.theme.extensions.red_800_red_400
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import java.text.DecimalFormat

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
                        painter = getPainter(nodeUIItem = nodeUIItem.node),
                        contentDescription = "Folder Thumbnail"
                    )
                } else if (nodeUIItem.node is FileNode) {
                    Image(
                        modifier = thumbNailModifier
                            .testTag(FILE_TEST_TAG),
                        painter = rememberAsyncImagePainter(model = nodeUIItem.node.thumbnailPath),
                        contentDescription = "Thumbnail"
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row {
                    Text(
                        text = nodeUIItem.name,
                        style = MaterialTheme.typography.subtitle1,
                        color = if (nodeUIItem.isTakenDown) MaterialTheme.colors.red_800_red_400 else MaterialTheme.colors.textColorPrimary,
                        maxLines = 1
                    )
                    val iconModifier = Modifier
                        .align(Alignment.CenterVertically)
                        .absolutePadding(left = 4.dp)
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
                Text(
                    text = if (nodeUIItem.node is FolderNode) {
                        getFolderInfo(
                            nodeUIItem.node.childFolderCount,
                            nodeUIItem.node.childFileCount,
                        )
                    } else {
                        val fileItem = nodeUIItem.node as FileNode
                        "${
                            getUnitString(
                                unit = fileItem.size,
                                isSpeed = false
                            )
                        } · ${
                            stringUtilWrapper.formatLongDateTime(
                                fileItem.modificationTime
                            )
                        }"
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

            Spacer(modifier = Modifier.weight(1f))
            MenuItem(onMenuClick)
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 64.dp, top = 8.dp),
            color = MaterialTheme.colors.grey_012_white_012, thickness = 1.dp
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun getFolderInfo(numFolders: Int, numFiles: Int): String {
    return if (numFolders == 0 && numFiles == 0) {
        stringResource(R.string.file_browser_empty_folder)
    } else if (numFolders == 0 && numFiles > 0) {
        pluralStringResource(R.plurals.num_files_with_parameter, numFiles, numFiles)
    } else if (numFiles == 0 && numFolders > 0) {
        pluralStringResource(
            R.plurals.num_folders_with_parameter,
            numFolders,
            numFolders
        )
    } else {
        pluralStringResource(
            R.plurals.num_folders_num_files,
            numFolders,
            numFolders
        ) + pluralStringResource(
            R.plurals.num_folders_num_files_2,
            numFiles,
            numFiles
        )
    }
}

@Composable
fun getUnitString(unit: Long, isSpeed: Boolean): String? {
    val df = DecimalFormat("#.##")
    val KB = 1024f
    val MB = KB * 1024
    val GB = MB * 1024
    val TB = GB * 1024
    val PB = TB * 1024
    val EB = PB * 1024
    return if (unit < KB) {
        stringResource(
            if (isSpeed) R.string.label_file_speed_byte else R.string.label_file_size_byte,
            unit.toString()
        )
    } else if (unit < MB) {
        stringResource(
            if (isSpeed) R.string.label_file_speed_kilo_byte else R.string.label_file_size_kilo_byte,
            df.format((unit / KB).toDouble())
        )
    } else if (unit < GB) {
        stringResource(
            if (isSpeed) R.string.label_file_speed_mega_byte else R.string.label_file_size_mega_byte,
            df.format((unit / MB).toDouble())
        )
    } else if (unit < TB) {
        stringResource(
            if (isSpeed) R.string.label_file_speed_giga_byte else R.string.label_file_size_giga_byte,
            df.format((unit / GB).toDouble())
        )
    } else if (unit < PB) {
        stringResource(
            if (isSpeed) R.string.label_file_speed_tera_byte else R.string.label_file_size_tera_byte,
            df.format((unit / TB).toDouble())
        )
    } else if (unit < EB) {
        stringResource(R.string.label_file_size_peta_byte, df.format((unit / PB).toDouble()))
    } else {
        stringResource(R.string.label_file_size_exa_byte, df.format((unit / EB).toDouble()))
    }
}

