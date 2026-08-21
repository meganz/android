package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens

/** Formatting actions the toolbar can emit. [Link] opens the link dialog before any text edit. */
enum class MarkdownFormatAction {
    Bold,
    Italic,
    Strikethrough,
    InlineCode,
    HeadingCycle,
    BulletList,
    OrderedList,
    Quote,
    Link,
    SwitchEditMode,
}

const val MARKDOWN_TOOLBAR_TAG = "markdown_formatting_toolbar"

// TODO Replace the hardcoded content descriptions with shared string resources and run the
//  Weblate flow.

fun markdownToolbarActionTag(action: MarkdownFormatAction): String =
    "markdown_formatting_toolbar:action_${action.name.lowercase()}"

/**
 * Text formatting toolbar docked above the keyboard while editing Markdown. Toggle buttons
 * reflect the formats covering the current cursor/selection; the trailing slot switches between
 * the Markdown and rich text edit modes.
 *
 * Icons are intentionally isolated here (material extended set) so a later swap to icon-pack
 * assets touches one file.
 */
@Composable
fun MarkdownFormattingToolbar(
    formats: MarkdownSelectionFormats,
    onAction: (MarkdownFormatAction) -> Unit,
    modifier: Modifier = Modifier,
    showModeSwitch: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DSTokens.colors.background.pageBackground)
            .testTag(MARKDOWN_TOOLBAR_TAG),
    ) {
        HorizontalDivider(
            color = DSTokens.colors.border.subtle,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolbarToggleButton(
                    action = MarkdownFormatAction.Bold,
                    icon = Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    isActive = formats.isBold,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.Italic,
                    icon = Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    isActive = formats.isItalic,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.Strikethrough,
                    icon = Icons.Default.FormatStrikethrough,
                    contentDescription = "Strikethrough",
                    isActive = formats.isStrikethrough,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.HeadingCycle,
                    icon = Icons.Default.Title,
                    contentDescription = "Heading",
                    isActive = formats.headingLevel != null,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.BulletList,
                    icon = Icons.Default.FormatListBulleted,
                    contentDescription = "Bulleted list",
                    isActive = formats.isBulletList,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.OrderedList,
                    icon = Icons.Default.FormatListNumbered,
                    contentDescription = "Numbered list",
                    isActive = formats.isOrderedList,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.Quote,
                    icon = Icons.Default.FormatQuote,
                    contentDescription = "Quote",
                    isActive = formats.isQuote,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.InlineCode,
                    icon = Icons.Default.Code,
                    contentDescription = "Code",
                    isActive = formats.isInlineCode,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.Link,
                    icon = Icons.Default.Link,
                    contentDescription = "Link",
                    isActive = formats.isLink,
                    onAction = onAction,
                )
            }
            if (showModeSwitch) {
                VerticalDivider(
                    color = DSTokens.colors.border.subtle,
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 4.dp),
                )
                IconButton(
                    onClick = { onAction(MarkdownFormatAction.SwitchEditMode) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = DSTokens.colors.icon.primary,
                    ),
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(40.dp)
                        .testTag(markdownToolbarActionTag(MarkdownFormatAction.SwitchEditMode)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Switch editing mode",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarToggleButton(
    action: MarkdownFormatAction,
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onAction: (MarkdownFormatAction) -> Unit,
) {
    IconToggleButton(
        checked = isActive,
        onCheckedChange = { onAction(action) },
        colors = IconButtonDefaults.iconToggleButtonColors(
            containerColor = DSTokens.colors.background.pageBackground,
            contentColor = DSTokens.colors.icon.primary,
            checkedContainerColor = DSTokens.colors.background.surface2,
            checkedContentColor = DSTokens.colors.icon.accent,
        ),
        shape = IconButtonDefaults.filledShape,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(40.dp)
            .testTag(markdownToolbarActionTag(action)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MarkdownFormattingToolbarPreview() {
    AndroidThemeForPreviews {
        MarkdownFormattingToolbar(
            formats = MarkdownSelectionFormats.Empty,
            onAction = {},
            showModeSwitch = true,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MarkdownFormattingToolbarActiveFormatsPreview() {
    AndroidThemeForPreviews {
        MarkdownFormattingToolbar(
            formats = MarkdownSelectionFormats(
                isBold = true,
                isItalic = true,
                headingLevel = 2,
                isBulletList = true,
            ),
            onAction = {},
            showModeSwitch = true,
        )
    }
}
