package mega.privacy.android.app.presentation.recentactions.view

import mega.privacy.android.core.R as CoreR
import mega.privacy.android.icon.pack.R as IconPackR
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.core.ui.controls.text.MiddleEllipsisText
import mega.privacy.android.core.ui.model.MegaSpanStyle
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.shared.theme.MegaAppTheme


/**
 * Composable for the recent actions list item
 */
@Composable
fun RecentActionListViewItem(
    firstLineText: String,
    @DrawableRes icon: Int = IconPackR.drawable.ic_generic_medium_solid,
    @DrawableRes shareIcon: Int? = null,
    @DrawableRes actionIcon: Int = R.drawable.ic_recents_up,
    parentFolderName: String,
    showMenuButton: Boolean = true,
    time: String,
    updatedByText: String? = null,
    isFavourite: Boolean = false,
    @ColorRes labelColor: Int? = null,
    onMenuClick: () -> Unit = {},
    onItemClick: () -> Unit = {},
) {

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onItemClick()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Image(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .testTag(ICON_TEST_TAG),
                painter = painterResource(icon),
                contentDescription = "Thumbnail"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 12.dp, top = 5.dp)
            ) {
                Row {
                    val iconModifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 4.dp)
                    MiddleEllipsisText(
                        text = firstLineText,
                        color = TextColor.Primary,
                        modifier = Modifier.testTag(FIRST_LINE_TEST_TAG),
                        style = MaterialTheme.typography.subtitle1,
                    )

                    labelColor?.let {
                        Box(
                            modifier = iconModifier
                                .size(10.dp)
                                .background(
                                    shape = CircleShape,
                                    color = colorResource(id = labelColor)
                                )
                                .testTag(LABEL_TEST_TAG)
                        )
                    }

                    if (isFavourite) {
                        Image(
                            alignment = Alignment.Center,
                            modifier = iconModifier
                                .testTag(FAVORITE_TEST_TAG),
                            painter = painterResource(id = CoreR.drawable.ic_favorite),
                            contentDescription = "Favorite"
                        )
                    }
                }

                updatedByText?.let { text ->
                    MegaSpannedText(
                        value = text,
                        baseStyle = MaterialTheme.typography.body2,
                        styles = mapOf(
                            SpanIndicator('A') to MegaSpanStyle(
                                spanStyle = SpanStyle(
                                    fontWeight = FontWeight.Normal
                                ),
                                color = TextColor.Secondary
                            ),
                            SpanIndicator('B') to MegaSpanStyle(
                                spanStyle = SpanStyle(
                                    fontWeight = FontWeight.Normal
                                ),
                            )
                        ),
                        color = TextColor.Primary,
                        modifier = Modifier.testTag(SECOND_LINE_TEST_TAG),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Text(
                        text = parentFolderName,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag(FOLDER_NAME_TEST_TAG),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.textColorSecondary
                    )

                    shareIcon?.let { icon ->
                        Image(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                                .testTag(SHARES_ICON_TEST_TAG),
                            painter = painterResource(id = icon),
                            contentDescription = "Shares"
                        )
                    }

                    Image(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(14.dp)
                            .testTag(ACTION_ICON_TEST_TAG),
                        colorFilter = ColorFilter.tint(
                            MaterialTheme.colors.grey_alpha_054_white_alpha_054
                        ),
                        painter = painterResource(id = actionIcon),
                        contentDescription = "Action"
                    )

                    Text(
                        text = time,
                        modifier = Modifier.testTag(TIME_TEST_TAG),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.textColorSecondary
                    )
                }
            }

            if (showMenuButton) {
                Image(
                    painter = painterResource(id = CoreR.drawable.ic_dots_vertical_grey),
                    contentDescription = "3 dots",
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .clickable {
                            onMenuClick()
                        }
                        .testTag(MENU_TEST_TAG)
                )
            }
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 72.dp),
            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
            thickness = 1.dp
        )
    }
}

internal const val ICON_TEST_TAG = "recent_action_list_view_item:icon"
internal const val FIRST_LINE_TEST_TAG = "recent_action_list_view_item:first_line"
internal const val SECOND_LINE_TEST_TAG = "recent_action_list_view_item:second_line"
internal const val LABEL_TEST_TAG = "recent_action_list_view_item:label"
internal const val FAVORITE_TEST_TAG = "recent_action_list_view_item:favorite"
internal const val FOLDER_NAME_TEST_TAG = "recent_action_list_view_item:folder_name"
internal const val SHARES_ICON_TEST_TAG = "recent_action_list_view_item:shares_icon"
internal const val ACTION_ICON_TEST_TAG = "recent_action_list_view_item:action_icon"
internal const val TIME_TEST_TAG = "recent_action_list_view_item:time"
internal const val MENU_TEST_TAG = "recent_action_list_view_item:menu"

@CombinedThemePreviews
@Composable
private fun RecentActionListViewItemPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        RecentActionListViewItem(
            firstLineText = "First line text",
            time = "12:00 PM",
            parentFolderName = "Folder Name"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RecentActionListViewItem2Preview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        RecentActionListViewItem(
            firstLineText = "First line text",
            updatedByText = "[A]Updated by[/A] [B]John Doe[/B]",
            isFavourite = true,
            labelColor = R.color.red_200,
            shareIcon = IconPackR.drawable.ic_folder_incoming_medium_solid,
            time = "08:00 PM",
            parentFolderName = "Folder Name"
        )
    }
}