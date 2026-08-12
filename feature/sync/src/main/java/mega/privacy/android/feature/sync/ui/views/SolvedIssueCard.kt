package mega.privacy.android.feature.sync.ui.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.icon.pack.R as IconPackR

/**
 * Test Tags for the Solved Issue Card
 */
internal const val SOLVED_ISSUE_MAIN_CONTAINER = "solved_issue_card:row_content_container"
internal const val SOLVED_ISSUE_NODE_IMAGE = "solved_issue_card:image_node_image"
internal const val SOLVED_ISSUE_NODE_TITLE = "solved_issue_card:text_node_title"
internal const val SOLVED_ISSUE_NODE_SUBTITLE = "solved_issue_card:text_node_subtitle"
internal const val SOLVED_ISSUE_BODY_ICON = "solved_issue_card:icon_body_icon"
internal const val SOLVED_ISSUE_BODY = "solved_issue_card:text_body"

/**
 * A [Composable] Card which displays the Solved Issue Information
 *
 * The basic required information displayed is the [title], [body] and [nodeIcon]
 * An optional [Icon] [bodyIcon] can be provided to display the [Icon] on the left side of the [body]
 * An optional [subTitle] can be provided to display additional information
 *
 * @param title The Node Title
 * @param body The Node Body
 * @param nodeIcon The Node Icon
 * @param modifier The [Modifier] object
 * @param subTitle An optional Subtitle
 * @param bodyIcon The Icon displayed on the left side of the [body], which does not exist by default
 * @param bodyColor The Text [Color] applied to the [body], which defaults to [TextColor.Success]
 * @param statusColor The [SupportColor] applied to the [bodyIcon] and the [body] text, which defaults to [SupportColor.Success]
 * @param nodeIconColor The Node Icon [Color]. By default, no [Color] is applied
 */
@Composable
fun SolvedIssueCard(
    title: String,
    body: String,
    @DrawableRes nodeIcon: Int,
    modifier: Modifier = Modifier,
    subTitle: String? = null,
    @DrawableRes bodyIcon: Int? = CoreUiR.drawable.ic_check_circle,
    bodyColor: TextColor = TextColor.Success,
    statusColor: SupportColor = SupportColor.Success,
    nodeIconColor: Color? = null,
) {
    Row(
        modifier = modifier
            .testTag(SOLVED_ISSUE_MAIN_CONTAINER)
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Image(
            modifier = Modifier
                .testTag(SOLVED_ISSUE_NODE_IMAGE)
                .size(48.dp),
            painter = painterResource(nodeIcon),
            contentDescription = "Node Icon",
            colorFilter = nodeIconColor?.let { ColorFilter.tint(it) },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
        ) {
            MegaText(
                modifier = Modifier.testTag(SOLVED_ISSUE_NODE_TITLE),
                text = title,
                style = AppTheme.typography.bodyLarge,
                textColor = TextColor.Primary,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1,
            )
            subTitle?.let {
                MegaText(
                    modifier = Modifier.testTag(SOLVED_ISSUE_NODE_SUBTITLE),
                    text = subTitle,
                    style = AppTheme.typography.bodyMedium,
                    textColor = TextColor.Secondary,
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 1,
                )
            }
            Row {
                bodyIcon?.let { nonNullBodyIcon ->
                    MegaIcon(
                        modifier = Modifier
                            .testTag(SOLVED_ISSUE_BODY_ICON)
                            .padding(top = 2.dp, end = 4.dp)
                            .size(16.dp),
                        painter = painterResource(nonNullBodyIcon),
                        contentDescription = "Body Icon",
                        supportTint = statusColor,
                    )
                }
                MegaText(
                    modifier = Modifier
                        .testTag(SOLVED_ISSUE_BODY)
                        .padding(top = 2.dp),
                    text = body,
                    style = AppTheme.typography.bodySmall,
                    textColor = bodyColor,
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 1,
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
private fun SolvedIssueCardPreview() {
    AndroidThemeForPreviews {
        SolvedIssueCard(
            title = "Node Title",
            body = "Node Body",
            nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SolvedIssueCardWithVeryLongTitlePreview() {
    AndroidThemeForPreviews {
        SolvedIssueCard(
            title = "This is a very long Title that exceeds the maximum number of two lines. An ellipsis is added for additional text",
            subTitle = "This is a very long SubTitle that exceeds the maximum number of two lines. An ellipsis is added for additional text",
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
private fun SolvedIssueCardWithBodyIconPreview() {
    AndroidThemeForPreviews {
        SolvedIssueCard(
            title = "Node Title",
            subTitle = "Subtitle",
            body = "Node Body",
            nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
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
private fun SolvedIssueCardWithVeryLongBodyPreview() {
    AndroidThemeForPreviews {
        SolvedIssueCard(
            title = "Backup Folder",
            body = "Sync or backup has been stopped as you've logged out or closed the session. To re-enable, go to Settings in the desktop app, select the Sync or Backup tab, and check the relevant folder.",
            bodyColor = TextColor.Brand,
            nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
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
private fun SolvedIssueCardWithBodyIconAndVeryLongBodyPreview() {
    AndroidThemeForPreviews {
        SolvedIssueCard(
            title = "Backup Folder",
            subTitle = "Subtitle",
            body = "Sync or backup has been stopped as you've logged out or closed the session. To re-enable, go to Settings in the desktop app, select the Sync or Backup tab, and check the relevant folder.",
            bodyColor = TextColor.Brand,
            nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
        )
    }
}
