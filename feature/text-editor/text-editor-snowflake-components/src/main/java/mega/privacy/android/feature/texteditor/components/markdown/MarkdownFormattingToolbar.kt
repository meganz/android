package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack

/** Formatting actions the toolbar can emit. [Link] opens the link dialog before any text edit. */
enum class MarkdownFormatAction {
    Bold,
    Italic,
    Underline,
    Strikethrough,
    InlineCode,
    HeadingCycle,
    BulletList,
    OrderedList,
    TaskList,
    Quote,
    Link,
}

const val MARKDOWN_TOOLBAR_TAG = "markdown_formatting_toolbar"

// TODO Replace the hardcoded content descriptions with shared string resources and run the
//  Weblate flow.

fun markdownToolbarActionTag(action: MarkdownFormatAction): String =
    "markdown_formatting_toolbar:action_${action.name.lowercase()}"

/**
 * Text formatting toolbar floating above the keyboard while editing Markdown in rich text mode.
 * Toggle buttons reflect the formats covering the current cursor/selection.
 */
@Composable
fun MarkdownFormattingToolbar(
    formats: MarkdownSelectionFormats,
    onAction: (MarkdownFormatAction) -> Unit,
    modifier: Modifier = Modifier,
    disabledActions: Set<MarkdownFormatAction> = emptySet(),
) {
    // Same floating pill styling as core-ui's MegaFloatingToolbar.
    Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = DSTokens.colors.background.surface1,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = ToolbarElevation),
        modifier = modifier
            .fillMaxWidth()
            .testTag(MARKDOWN_TOOLBAR_TAG),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ToolbarHeight),
        ) {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolbarToggleButton(
                    action = MarkdownFormatAction.HeadingCycle,
                    icon = IconPack.Medium.Thin.Outline.FileText, // TODO update to dropdown
                    contentDescription = "Heading",
                    isActive = formats.headingLevel != null,
                    enabled = MarkdownFormatAction.HeadingCycle !in disabledActions,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.Bold,
                    icon = IconPack.Medium.Thin.Outline.TextBold,
                    contentDescription = "Bold",
                    isActive = formats.isBold,
                    enabled = MarkdownFormatAction.Bold !in disabledActions,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.Italic,
                    icon = IconPack.Medium.Thin.Outline.TextItalic,
                    contentDescription = "Italic",
                    isActive = formats.isItalic,
                    enabled = MarkdownFormatAction.Italic !in disabledActions,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.Underline,
                    icon = IconPack.Medium.Thin.Outline.TextUnderline,
                    contentDescription = "Underline",
                    isActive = formats.isUnderline,
                    enabled = MarkdownFormatAction.Underline !in disabledActions,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.Strikethrough,
                    icon = IconPack.Medium.Thin.Outline.TextStrikethrough,
                    contentDescription = "Strikethrough",
                    isActive = formats.isStrikethrough,
                    enabled = MarkdownFormatAction.Strikethrough !in disabledActions,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.TaskList,
                    icon = IconPack.Medium.Thin.Outline.Checklist,
                    contentDescription = "Checklist",
                    isActive = formats.isTaskList,
                    enabled = MarkdownFormatAction.TaskList !in disabledActions,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.BulletList,
                    icon = IconPack.Medium.Thin.Outline.ListSmall,
                    contentDescription = "Bulleted list",
                    isActive = formats.isBulletList,
                    enabled = MarkdownFormatAction.BulletList !in disabledActions,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.OrderedList,
                    icon = IconPack.Medium.Thin.Outline.ListNumbers,
                    contentDescription = "Numbered list",
                    isActive = formats.isOrderedList,
                    enabled = MarkdownFormatAction.OrderedList !in disabledActions,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.Quote,
                    icon = IconPack.Medium.Thin.Outline.TextQuote,
                    contentDescription = "Quote",
                    isActive = formats.isQuote,
                    enabled = MarkdownFormatAction.Quote !in disabledActions,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.InlineCode,
                    icon = IconPack.Medium.Thin.Outline.Code,
                    contentDescription = "Code",
                    isActive = formats.isInlineCode,
                    enabled = MarkdownFormatAction.InlineCode !in disabledActions,
                    onAction = onAction,
                )
                ToolbarToggleButton(
                    action = MarkdownFormatAction.Link,
                    icon = IconPack.Medium.Thin.Outline.Url,
                    contentDescription = "Link",
                    isActive = formats.isLink,
                    enabled = MarkdownFormatAction.Link !in disabledActions,
                    onAction = onAction,
                )
            }
            if (scrollState.canScrollBackward) {
                ScrollEdgeShade(isStart = true, modifier = Modifier.align(Alignment.CenterStart))
            }
            if (scrollState.canScrollForward) {
                ScrollEdgeShade(isStart = false, modifier = Modifier.align(Alignment.CenterEnd))
            }
        }
    }
}

@Composable
private fun ScrollEdgeShade(
    isStart: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = DSTokens.colors.background.surface1
    val colors = listOf(background, background.copy(alpha = 0f))
    Box(
        modifier = modifier
            .width(ToolbarShadeWidth)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(if (isStart) colors else colors.reversed()),
            ),
    )
}

@Composable
private fun ToolbarToggleButton(
    action: MarkdownFormatAction,
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    enabled: Boolean,
    onAction: (MarkdownFormatAction) -> Unit,
) {
    IconToggleButton(
        checked = isActive,
        onCheckedChange = { onAction(action) },
        enabled = enabled,
        colors = IconButtonDefaults.iconToggleButtonColors(
            containerColor = Color.Transparent,
            contentColor = DSTokens.colors.icon.primary,
            checkedContainerColor = DSTokens.colors.background.surface3,
            checkedContentColor = DSTokens.colors.icon.primary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = DSTokens.colors.icon.disabled,
        ),
        shape = CircleShape,
        modifier = Modifier
            .size(36.dp)
            .testTag(markdownToolbarActionTag(action)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
        )
    }
}

private val ToolbarHeight = 48.dp
private val ToolbarShadeWidth = 24.dp
private val ToolbarElevation = 3.dp

@CombinedThemePreviews
@Composable
private fun MarkdownFormattingToolbarPreview() {
    AndroidThemeForPreviews {
        MarkdownFormattingToolbar(
            formats = MarkdownSelectionFormats.Empty,
            onAction = {},
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
        )
    }
}
