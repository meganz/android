package mega.privacy.android.feature.texteditor.components.markdown

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

const val MARKDOWN_EDIT_MODE_MENU_BUTTON_TAG = "markdown_edit_mode_menu:button"

/**
 * Top bar overflow that switches a Markdown file between plain (raw source) and rich text
 * editing. The check marks the mode currently in use, which the previous single toggle icon
 * could not express.
 */
@Composable
fun MarkdownEditModeMenu(
    isRichText: Boolean,
    onSelectRichText: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag(MARKDOWN_EDIT_MODE_MENU_BUTTON_TAG),
        ) {
            MegaIcon(
                imageVector = IconPack.Medium.Thin.Outline.MoreVertical,
                tint = IconColor.Primary,
                contentDescription = stringResource(sharedR.string.text_editor_edit_mode_menu),
            )
        }
        // Material's menu rather than MegaDropDownMenu: that one pins the container shape,
        // and this menu shares the text style menu's corner radius.
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = DSTokens.shapes.medium,
            containerColor = DSTokens.colors.background.surface1,
        ) {
            MarkdownEditModeMenuContent(
                isRichText = isRichText,
                onSelectRichText = {
                    expanded = false
                    onSelectRichText(it)
                },
            )
        }
    }
}

/** The menu's rows, separate from the popup so a screenshot test can render them. */
@Composable
internal fun MarkdownEditModeMenuContent(
    isRichText: Boolean,
    onSelectRichText: (Boolean) -> Unit,
) {
    EditModeMenuItem(
        text = stringResource(sharedR.string.text_editor_edit_mode_plain_text),
        selected = !isRichText,
        onClick = { onSelectRichText(false) },
    )
    EditModeMenuItem(
        text = stringResource(sharedR.string.text_editor_edit_mode_rich_text),
        selected = isRichText,
        onClick = { onSelectRichText(true) },
    )
}

@Composable
private fun EditModeMenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = DSTokens.colors.text.primary,
            )
        },
        trailingIcon = {
            if (selected) {
                MegaIcon(
                    imageVector = IconPack.Medium.Thin.Outline.Check,
                    tint = IconColor.Primary,
                    contentDescription = stringResource(sharedR.string.text_editor_menu_option_selected),
                )
            }
        },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = EditModeMenuItemPadding),
        modifier = Modifier.widthIn(min = EditModeMenuMinWidth),
    )
}

private val EditModeMenuMinWidth = 220.dp
private val EditModeMenuItemPadding = 16.dp

@CombinedThemePreviews
@Composable
private fun MarkdownEditModeMenuPreview() {
    AndroidThemeForPreviews {
        MarkdownEditModeMenu(isRichText = true, onSelectRichText = {})
    }
}
