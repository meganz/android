package mega.privacy.android.core.ui.controls.lists

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.red_500

/**
 * Test Tags for the Menu Action Node Header with Body
 */
internal const val HEADER_MAIN_CONTAINER =
    "menu_action_node_header_with_body:row_content_container"
internal const val HEADER_NODE_IMAGE = "menu_action_node_header_with_body:image_node_image"
internal const val HEADER_NODE_TITLE = "menu_action_node_header_with_body:text_node_title"
internal const val HEADER_NODE_BODY_ICON = "menu_action_node_header_with_body:icon_node_body_icon"
internal const val HEADER_NODE_BODY = "menu_action_node_header_with_body:text_node_body"

/**
 * A [Composable] Bottom Dialog Header which displays the Node Information
 *
 * The basic required information displayed is the [title], [body] and [nodeIcon]
 * An optional [Icon] [bodyIcon] can be provided to display the [Icon] on the left side of the [body]
 *
 * Furthermore, all UI Elements can change its [Color] to accommodate different Scenarios
 *
 * @param title The Node Title
 * @param body The Node Body
 * @param nodeIcon The Node Icon
 * @param modifier The [Modifier] object
 * @param bodyIcon The Icon displayed on the left side of the [body], which does not exist by default
 * @param bodyColor The Text [Color] applied to the [body], which defaults to [textColorSecondary]
 * @param bodyIconColor The Body Icon [Color], which defaults to [Color.Unspecified]
 * @param nodeIconColor The Node Icon [Color]. By default, no [Color] is applied
 */
@Composable
fun MenuActionNodeHeaderWithBody(
    title: String,
    body: String,
    @DrawableRes nodeIcon: Int,
    modifier: Modifier = Modifier,
    @DrawableRes bodyIcon: Int? = null,
    bodyColor: Color = MaterialTheme.colors.textColorSecondary,
    bodyIconColor: Color = Color.Unspecified,
    nodeIconColor: Color? = null,
) {
    Row(
        modifier = modifier
            .testTag(HEADER_MAIN_CONTAINER)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(12.dp)
    ) {
        Image(
            modifier = modifier
                .testTag(HEADER_NODE_IMAGE)
                .size(48.dp),
            painter = painterResource(nodeIcon),
            contentDescription = "Node Icon",
            colorFilter = nodeIconColor?.let { ColorFilter.tint(it) },
        )
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Text(
                modifier = modifier.testTag(HEADER_NODE_TITLE),
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.textColorPrimary,
            )
            Row {
                bodyIcon?.let { nonNullBodyIcon ->
                    Icon(
                        modifier = modifier
                            .testTag(HEADER_NODE_BODY_ICON)
                            .padding(top = 2.dp, end = 8.dp)
                            .size(16.dp),
                        painter = painterResource(nonNullBodyIcon),
                        contentDescription = "Body Icon",
                        tint = bodyIconColor,
                    )
                }
                Text(
                    modifier = modifier.testTag(HEADER_NODE_BODY),
                    text = body,
                    style = MaterialTheme.typography.subtitle2,
                    color = bodyColor,
                )
            }
        }
    }
}

/**
 * A Preview Composable that displays content with only the required parameters provided
 */
@CombinedThemePreviews
@Composable
private fun PreviewHeader() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MenuActionNodeHeaderWithBody(
            title = "Node Title",
            body = "Node Body",
            nodeIcon = R.drawable.ic_folder_list,
        )
    }
}

/**
 * A Preview Composable that displays content with an optional Body icon
 */
@CombinedThemePreviews
@Composable
private fun PreviewHeaderWithBodyIcon() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MenuActionNodeHeaderWithBody(
            title = "Node Title",
            body = "Node Body",
            nodeIcon = R.drawable.ic_folder_list,
            bodyIcon = R.drawable.ic_info,
            bodyIconColor = MaterialTheme.colors.textColorSecondary,
        )
    }
}

/**
 * A Preview Composable that displays content with a very long Body
 *
 * The Container height automatically adjusts to display the entire Body
 */
@CombinedThemePreviews
@Composable
private fun PreviewHeaderWithVeryLongBody() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MenuActionNodeHeaderWithBody(
            title = "Backup Folder",
            body = "Sync or backup has been stopped as you’ve logged out or closed the session. To re-enable, go to Settings in the desktop app, select the Sync or Backup tab, and check the relevant folder.",
            bodyColor = red_500,
            nodeIcon = R.drawable.ic_folder_list,
            bodyIconColor = MaterialTheme.colors.textColorSecondary,
        )
    }
}

/**
 * A Preview Composable that displays content with an optional Body icon and a very long Body
 *
 * The Container height automatically adjusts to display the entire Body
 */
@CombinedThemePreviews
@Composable
private fun PreviewHeaderWithBodyIconAndVeryLongBody() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MenuActionNodeHeaderWithBody(
            title = "Backup Folder",
            body = "Sync or backup has been stopped as you’ve logged out or closed the session. To re-enable, go to Settings in the desktop app, select the Sync or Backup tab, and check the relevant folder.",
            bodyColor = red_500,
            nodeIcon = R.drawable.ic_folder_list,
            bodyIcon = R.drawable.ic_x_circle,
            bodyIconColor = red_500,
        )
    }
}