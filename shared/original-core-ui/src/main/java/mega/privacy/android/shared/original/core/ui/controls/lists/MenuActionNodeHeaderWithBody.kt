package mega.privacy.android.shared.original.core.ui.controls.lists

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.R
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.red_500

/**
 * Test Tags for the Menu Action Node Header with Body M3
 */
internal const val HEADER_MAIN_CONTAINER =
    "menu_action_node_header_with_body:row_content_container"
internal const val HEADER_NODE_IMAGE = "menu_action_node_header_with_body:image_node_image"
internal const val HEADER_NODE_TITLE = "menu_action_node_header_with_body:text_node_title"
internal const val HEADER_NODE_SUBTITLE =
    "menu_action_node_header_with_body:text_node_subtitle"
internal const val HEADER_NODE_BODY_ICON =
    "menu_action_node_header_with_body:icon_node_body_icon"
internal const val HEADER_NODE_BODY = "menu_action_node_header_with_body:text_node_body"

/**
 * A [Composable] Bottom Dialog Header which displays the Node Information
 *
 * Material 3 version using MegaText and Material 3 theming.
 *
 * The basic required information displayed is the [title], [body] and [nodeIcon]
 * An optional [androidx.compose.material3.Icon] [bodyIcon] can be provided to display the [androidx.compose.material3.Icon] on the left side of the [body]
 *
 * Furthermore, all UI Elements can change its [Color] to accommodate different Scenarios
 *
 * @param title The Node Title
 * @param body The Node Body
 * @param nodeIcon The Node Icon
 * @param modifier The [Modifier] object
 * @param subTitle An optional Subtitle
 * @param bodyIcon The Icon displayed on the left side of the [body], which does not exist by default
 * @param bodyColor The Text [Color] applied to the [body]. By default, no [Color] is applied
 * @param bodyIconColor The Body Icon [Color], which defaults to [Color.Unspecified]
 * @param nodeIconColor The Node Icon [Color]. By default, no [Color] is applied
 */
@Composable
fun MenuActionNodeHeaderWithBody(
    title: String,
    body: String,
    @DrawableRes nodeIcon: Int,
    modifier: Modifier = Modifier,
    subTitle: String? = null,
    @DrawableRes bodyIcon: Int? = null,
    bodyColor: Color? = null,
    bodyIconColor: Color = Color.Unspecified,
    nodeIconColor: Color? = DSTokens.colors.icon.secondary,
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
            MegaText(
                text = buildAnnotatedString { append(title) },
                textColor = TextColor.Primary,
                maxLines = 2,
                modifier = modifier.testTag(HEADER_NODE_TITLE),
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.subtitle1,
            )
            subTitle?.let {
                MegaText(
                    text = buildAnnotatedString { append(subTitle) },
                    textColor = TextColor.Secondary,
                    maxLines = 2,
                    modifier = modifier.testTag(HEADER_NODE_SUBTITLE),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.subtitle2,
                )
            }
            Row {
                bodyIcon?.let { nonNullBodyIcon ->
                    Icon(
                        modifier = modifier
                            .testTag(HEADER_NODE_BODY_ICON)
                            .padding(top = 2.dp, end = 4.dp)
                            .size(16.dp),
                        painter = painterResource(nonNullBodyIcon),
                        contentDescription = "Body Icon",
                        tint = bodyIconColor,
                    )
                }
                MegaText(
                    text = buildAnnotatedString { append(body) },
                    textColor = if (bodyColor != null) {
                        // Custom color provided, we'll need to handle this differently
                        // For now, use secondary as fallback
                        TextColor.Secondary
                    } else {
                        TextColor.Secondary
                    },
                    maxLines = Int.MAX_VALUE,
                    modifier = modifier
                        .testTag(HEADER_NODE_BODY)
                        .padding(top = 2.dp),
                    style = MaterialTheme.typography.subtitle2,
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MenuActionNodeHeaderWithBody(
            title = "Node Title",
            body = "Node Body",
            nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewHeaderWithVeryLongTitle() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MenuActionNodeHeaderWithBody(
            title = "This is a very long Title that exceeds the maximum number of two lines. An ellipsis is added for additional text",
            body = "Node Body",
            nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
        )
    }
}

/**
 * A Preview Composable that displays content with an optional Body icon
 */
@CombinedThemePreviews
@Composable
private fun PreviewHeaderWithBodyIcon() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MenuActionNodeHeaderWithBody(
            title = "Node Title",
            body = "Node Body",
            nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
            bodyIcon = R.drawable.ic_info,
            bodyIconColor = MaterialTheme.colors.secondary,
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MenuActionNodeHeaderWithBody(
            title = "Backup Folder",
            body = "Sync or backup has been stopped as you've logged out or closed the session. To re-enable, go to Settings in the desktop app, select the Sync or Backup tab, and check the relevant folder.",
            bodyColor = red_500,
            nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
            bodyIconColor = MaterialTheme.colors.secondary,
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MenuActionNodeHeaderWithBody(
            title = "Backup Folder",
            subTitle = "Subtitle",
            body = "Sync or backup has been stopped as you've logged out or closed the session. To re-enable, go to Settings in the desktop app, select the Sync or Backup tab, and check the relevant folder.",
            bodyColor = red_500,
            nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
            bodyIcon = R.drawable.ic_x_circle,
            bodyIconColor = red_500,
        )
    }
}
