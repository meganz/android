package mega.privacy.android.app.presentation.documentsection.view

import mega.privacy.android.icon.pack .R as iconPackR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.images.ThumbnailView
import mega.privacy.android.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.background_white_alpha_005
import mega.privacy.android.core.ui.theme.extensions.color_button_brand
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.red_800_red_400
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.shared.theme.MegaAppTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DocumentGridViewItem(
    isSelected: Boolean,
    @DrawableRes icon: Int,
    name: String,
    thumbnailData: Any?,
    isTakenDown: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelected)
                    MaterialTheme.colors.secondary
                else
                    MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                shape = RoundedCornerShape(5.dp)
            )
            .background(MaterialTheme.colors.background_white_alpha_005)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            )
    ) {
        Box(contentAlignment = Alignment.TopStart) {
            ThumbnailView(
                modifier = Modifier
                    .height(172.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                    .padding(1.dp)
                    .testTag(DOCUMENT_SECTION_GRID_ITEM_THUMBNAIL_TEST_TAG),
                contentDescription = DOCUMENT_SECTION_GRID_ITEM_THUMBNAIL_DESCRIPTION,
                data = thumbnailData,
                defaultImage = icon,
                contentScale = ContentScale.Crop,
            )
            if (isSelected) {
                Image(
                    painter = painterResource(id = R.drawable.ic_select_folder),
                    contentDescription = DOCUMENT_SECTION_GRID_ITEM_SELECTED_ICON_DESCRIPTION,
                    modifier = Modifier
                        .padding(12.dp)
                        .testTag(DOCUMENT_SECTION_GRID_ITEM_SELECTED_TEST_TAG)
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
                painter = painterResource(id = R.drawable.ic_dots_vertical_grey),
                contentDescription = DOCUMENT_SECTION_GRID_ITEM_MENU_ICON_DESCRIPTION,
                modifier = Modifier
                    .clickable { onMenuClick() }
                    .constrainAs(menuImage) {
                        end.linkTo(parent.end)
                    }
                    .testTag(DOCUMENT_SECTION_GRID_ITEM_MENU_TEST_TAG)
            )
            Image(
                modifier = Modifier
                    .constrainAs(takenDownImage) {
                        end.linkTo(menuImage.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        visibility =
                            if (isTakenDown) Visibility.Visible else Visibility.Gone
                    }
                    .height(16.dp)
                    .width(16.dp)
                    .testTag(DOCUMENT_SECTION_GRID_ITEM_TAKEN_DOWN_TEST_TAG),
                painter = painterResource(id = iconPackR.drawable.ic_alert_triangle_medium_regular_outline),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.color_button_brand),
                contentDescription = DOCUMENT_SECTION_GRID_ITEM_TAKEN_DOWN_ICON_DESCRIPTION
            )
            MiddleEllipsisText(
                text = name,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .constrainAs(txtTitle) {
                        start.linkTo(parent.start)
                        end.linkTo(takenDownImage.start)
                        width = Dimension.fillToConstraints
                    }
                    .testTag(DOCUMENT_SECTION_GRID_ITEM_NAME_VIEW_TEST_TAG),
                style = MaterialTheme.typography.subtitle2,
                color = TextColor.Secondary
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun DocumentGridItemViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        DocumentGridViewItem(
            isSelected = false,
            name = "Document Testing name.pdf",
            icon = MimeTypeList.typeForName("Document Testing name.pdf").iconResourceId,
            thumbnailData = null,
            isTakenDown = false
        )
    }
}

/**
 * Test tag for the gird item name view.
 */
const val DOCUMENT_SECTION_GRID_ITEM_NAME_VIEW_TEST_TAG =
    "document_section_grid_item_name_view_test_tag"

/**
 * Test tag for the thumbnail of the grid item.
 */
const val DOCUMENT_SECTION_GRID_ITEM_THUMBNAIL_TEST_TAG =
    "document_section_grid_item:thumbnail_view"

/**
 * Test tag for the selected icon of the grid item.
 */
const val DOCUMENT_SECTION_GRID_ITEM_SELECTED_TEST_TAG = "document_section_grid_item:image_selected"

/**
 * Test tag for the menu icon of the grid item.
 */
const val DOCUMENT_SECTION_GRID_ITEM_MENU_TEST_TAG = "document_section_grid_item:image_menu"

/**
 * Test tag for the taken down icon of the grid item.
 */
const val DOCUMENT_SECTION_GRID_ITEM_TAKEN_DOWN_TEST_TAG =
    "document_section_grid_item:image_taken_down"

/**
 * Description for the thumbnail of the grid item.
 */
const val DOCUMENT_SECTION_GRID_ITEM_THUMBNAIL_DESCRIPTION =
    "document_section_grid_item_thumbnail_description"

/**
 * Description for the selected icon of the grid item.
 */
const val DOCUMENT_SECTION_GRID_ITEM_SELECTED_ICON_DESCRIPTION =
    "document_section_grid_item_selected_icon_description"

/**
 * Description for the menu icon of the grid item.
 */
const val DOCUMENT_SECTION_GRID_ITEM_MENU_ICON_DESCRIPTION =
    "document_section_grid_item_menu_icon_description"

/**
 * Description for the taken down icon of the grid item.
 */
const val DOCUMENT_SECTION_GRID_ITEM_TAKEN_DOWN_ICON_DESCRIPTION =
    "document_section_grid_item_taken_down_icon_description"