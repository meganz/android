package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.feature.texteditor.components.markdownHeadingStyle
import mega.privacy.android.icon.pack.IconPack

/** Formatting actions the toolbar can emit. [Link] opens the link dialog before any text edit. */
enum class MarkdownFormatAction {
    Bold,
    Italic,
    Underline,
    Strikethrough,
    InlineCode,
    BulletList,
    OrderedList,
    TaskList,
    Quote,
    Link,
}

const val MARKDOWN_TOOLBAR_TAG = "markdown_formatting_toolbar"
const val MARKDOWN_TEXT_STYLE_CHIP_TAG = "markdown_formatting_toolbar:text_style_chip"
const val MARKDOWN_TEXT_STYLE_MENU_TAG = "markdown_formatting_toolbar:text_style_menu"

// TODO Replace the hardcoded content descriptions with shared string resources and run the
//  Weblate flow.

fun markdownToolbarActionTag(action: MarkdownFormatAction): String =
    "markdown_formatting_toolbar:action_${action.name.lowercase()}"

fun markdownTextStyleOptionTag(level: Int?): String =
    "markdown_formatting_toolbar:text_style_option_${level ?: "body"}"

/**
 * Text formatting toolbar floating above the keyboard while editing Markdown in rich text mode.
 * The leading chip names the focused block's text style and opens the style menu; toggle
 * buttons reflect the formats covering the current cursor/selection.
 */
@Composable
fun MarkdownFormattingToolbar(
    formats: MarkdownSelectionFormats,
    onAction: (MarkdownFormatAction) -> Unit,
    onTextStyleSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    textStyleEnabled: Boolean = true,
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
                TextStyleChip(
                    headingLevel = formats.headingLevel,
                    enabled = textStyleEnabled,
                    onTextStyleSelected = onTextStyleSelected,
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
private fun TextStyleChip(
    headingLevel: Int?,
    enabled: Boolean,
    onTextStyleSelected: (Int?) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        // Local chip: MegaChip pins 32dp height and a 16dp corner shape, so a 36dp fully
        // rounded variant needs its own container.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .height(TextStyleChipHeight)
                .clip(CircleShape)
                .background(
                    if (headingLevel != null) {
                        DSTokens.colors.background.surface3
                    } else {
                        Color.Transparent
                    },
                )
                .clickable(enabled = enabled) { menuExpanded = true }
                .padding(start = 12.dp, end = 10.dp)
                .testTag(MARKDOWN_TEXT_STYLE_CHIP_TAG),
        ) {
            Text(
                text = textStyleLabel(headingLevel),
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    DSTokens.colors.text.primary
                } else {
                    DSTokens.colors.text.disabled
                },
            )
            Icon(
                imageVector = IconPack.Small.Thin.Outline.ChevronDown,
                contentDescription = null,
                tint = if (enabled) {
                    DSTokens.colors.icon.primary
                } else {
                    DSTokens.colors.icon.disabled
                },
                modifier = Modifier.size(16.dp),
            )
        }
        if (menuExpanded) {
            // Raw Popup instead of DropdownMenu: the menu always sits directly above the
            // chip with an exact gap, which DropdownMenu's internal positioning cannot
            // guarantee. Non-focusable so window focus stays on the text field and the
            // keyboard never closes.
            val density = LocalDensity.current
            val menuGapPx = with(density) { TextStyleMenuGap.roundToPx() }
            val startNudgePx = with(density) { TextStyleMenuStartNudge.roundToPx() }
            // The card is inset by this margin inside the popup window so its shadow has
            // room to render; the position math compensates to keep the card on target.
            val marginPx = with(density) { TextStyleMenuShadowMargin.roundToPx() }
            val positionProvider = remember(menuGapPx, marginPx, startNudgePx) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize,
                    ): IntOffset = IntOffset(
                        x = (anchorBounds.left - marginPx - startNudgePx)
                            .coerceAtMost(windowSize.width - popupContentSize.width)
                            .coerceAtLeast(0),
                        y = (anchorBounds.top - popupContentSize.height - menuGapPx + marginPx)
                            .coerceAtLeast(0),
                    )
                }
            }
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { menuExpanded = false },
                properties = PopupProperties(focusable = false),
            ) {
                Box(modifier = Modifier.padding(TextStyleMenuShadowMargin)) {
                    Surface(
                        shape = DSTokens.shapes.medium,
                        color = DSTokens.colors.background.pageBackground,
                        shadowElevation = TextStyleMenuElevation,
                        modifier = Modifier.testTag(MARKDOWN_TEXT_STYLE_MENU_TAG),
                    ) {
                        // Intrinsic width: DropdownMenuItem fills its parent, and without
                        // this the column would stretch to the popup window's full width.
                        Column(
                            modifier = Modifier
                                .width(IntrinsicSize.Max)
                                .padding(vertical = 8.dp),
                        ) {
                            TextStyleMenuItems(
                                headingLevel = headingLevel,
                                onSelect = { level ->
                                    menuExpanded = false
                                    if (level != headingLevel) onTextStyleSelected(level)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextStyleMenuItems(
    headingLevel: Int?,
    onSelect: (Int?) -> Unit,
) {
    TextStyleLevels.forEach { level ->
        DropdownMenuItem(
            text = {
                Text(
                    text = textStyleLabel(level),
                    style = if (level == null) {
                        MaterialTheme.typography.bodyLarge
                    } else {
                        markdownHeadingStyle(level)
                    },
                    color = DSTokens.colors.text.primary,
                )
            },
            trailingIcon = {
                if (level == headingLevel) {
                    Icon(
                        imageVector = IconPack.Medium.Thin.Outline.Check,
                        contentDescription = "Selected",
                        tint = DSTokens.colors.icon.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
            onClick = { onSelect(level) },
            contentPadding = PaddingValues(horizontal = TextStyleMenuItemPadding),
            modifier = Modifier
                .widthIn(min = TextStyleMenuMinWidth)
                .testTag(markdownTextStyleOptionTag(level)),
        )
    }
}

private fun textStyleLabel(level: Int?): String = if (level == null) "Body" else "Heading $level"

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

private val TextStyleLevels: List<Int?> = listOf(1, 2, 3, null)
private val ToolbarHeight = 48.dp
private val ToolbarShadeWidth = 24.dp
private val ToolbarElevation = 3.dp
private val TextStyleChipHeight = 36.dp
private val TextStyleMenuMinWidth = 220.dp
private val TextStyleMenuItemPadding = 16.dp

private val TextStyleMenuElevation = 3.dp

/**
 * Space between the menu's bottom edge and the anchor chip's top. The toolbar pill's top
 * edge sits 6dp above the chip, so the visible gap to the pill is this minus 6dp.
 */
private val TextStyleMenuGap = 14.dp
private val TextStyleMenuShadowMargin = 16.dp

/** Pulls the menu's start edge slightly left of the chip's start. */
private val TextStyleMenuStartNudge = 4.dp

@CombinedThemePreviews
@Composable
private fun MarkdownFormattingToolbarPreview() {
    AndroidThemeForPreviews {
        MarkdownFormattingToolbar(
            formats = MarkdownSelectionFormats.Empty,
            onAction = {},
            onTextStyleSelected = {},
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
            onTextStyleSelected = {},
        )
    }
}
