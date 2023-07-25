package mega.privacy.android.core.ui.controls.lists

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.images.ThumbnailView
import mega.privacy.android.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.red_800_red_400
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import java.io.File

/**
 * List view item for file/folder info
 * @param modifier [Modifier]
 * @param isSelected is item selected
 * @param folderInfo folder info, if null the item is a File
 * @param icon icon resource
 * @param fileSize file size
 * @param modifiedDate modified date
 * @param name name
 * @param isTakenDown is taken down
 * @param isFavourite is favourite
 * @param isSharedWithPublicLink is shared with public link
 * @param onLongClick onLongItemClick
 * @param onItemClicked itemClick
 * @param onMenuClick three dots click
 * @param imageState Thumbnail state
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeListViewItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    folderInfo: String?,
    @DrawableRes icon: Int,
    fileSize: String?,
    modifiedDate: String?,
    name: String,
    showMenuButton: Boolean,
    isTakenDown: Boolean,
    isFavourite: Boolean,
    isSharedWithPublicLink: Boolean,
    imageState: State<File?>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    onMenuClick: () -> Unit = {},
) {
    Column(
        modifier = if (isEnabled) {
            modifier
                .alpha(1f)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        } else {
            modifier
                .alpha(0.5f)
                .clickable(enabled = false) { }
        }
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
            if (isSelected) {
                Image(
                    modifier = thumbNailModifier
                        .testTag(SELECTED_TEST_TAG),
                    painter = painterResource(R.drawable.ic_select_folder),
                    contentDescription = "Selected",
                )
            } else {
                if (folderInfo != null) {
                    Image(
                        modifier = thumbNailModifier
                            .testTag(FOLDER_TEST_TAG),
                        painter = painterResource(id = icon),
                        contentDescription = "Folder Thumbnail"
                    )
                } else {
                    imageState.value
                    ThumbnailView(
                        modifier = thumbNailModifier
                            .testTag(FILE_TEST_TAG),
                        imageFile = imageState.value,
                        defaultImage = icon,
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
                    if (showMenuButton) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_dots_vertical_grey),
                            contentDescription = "3 dots",
                            modifier = Modifier
                                .constrainAs(threeDots) {
                                    end.linkTo(parent.end)
                                    top.linkTo(parent.top)
                                    bottom.linkTo(parent.bottom)
                                }
                                .clickable { onMenuClick() }
                        )
                    }
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
                            text = name,
                            style = MaterialTheme.typography.subtitle1,
                            color = if (isTakenDown) MaterialTheme.colors.red_800_red_400 else MaterialTheme.colors.textColorPrimary,
                        )

                        if (isFavourite) {
                            Image(
                                alignment = Alignment.Center,
                                modifier = iconModifier
                                    .testTag(FAVORITE_TEST_TAG),
                                painter = painterResource(id = R.drawable.ic_favorite),
                                contentDescription = "Favorite",

                                )
                        }
                        if (isSharedWithPublicLink) {
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
                        if (isTakenDown) {
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
                    text = folderInfo ?: "$fileSize · $modifiedDate",
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


@CombinedThemePreviews
@Composable
private fun FilePreview() {
    val imageState = remember {
        mutableStateOf(null as File?)
    }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            modifier = Modifier,
            isSelected = false,
            folderInfo = null,
            icon = R.drawable.ic_pdf_list,
            fileSize = "1.2 MB",
            modifiedDate = "Dec 29, 2022",
            name = "documentation.pdf",
            showMenuButton = true,
            isFavourite = false,
            isSharedWithPublicLink = false,
            isTakenDown = false,
            onClick = {},
            imageState = imageState
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FolderPreview() {
    val imageState = remember {
        mutableStateOf(null as File?)
    }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NodeListViewItem(
            modifier = Modifier,
            isSelected = false,
            folderInfo = "Empty Folder",
            icon = R.drawable.ic_folder_list,
            fileSize = "1.2 MB",
            modifiedDate = "Dec 29, 2022",
            name = "documentation.pdf",
            showMenuButton = true,
            isFavourite = false,
            isSharedWithPublicLink = false,
            isTakenDown = false,
            onClick = {},
            imageState = imageState
        )
    }
}