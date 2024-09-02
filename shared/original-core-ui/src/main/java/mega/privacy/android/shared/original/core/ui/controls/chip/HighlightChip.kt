package mega.privacy.android.shared.original.core.ui.controls.chip

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.HighlightedText
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Chip with highlighted text inside
 *
 * @param selected if chip is selected or not
 * @param text text of chip
 * @param highlightText highlighted part of text
 * @param modifier optional modifier
 * @param style style of chip
 * @param onClick callback this chip is clicked
 * @param enabled if chip is enabled or grayed out
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HighlightChip(
    text: String,
    highlightText: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    style: ChipStyle = TransparentChipStyle,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
) {
    FilterChip(
        modifier = modifier
            .clearAndSetSemantics {
                this.contentDescription = text
            }
            .height(32.dp),
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        colors = style.selectableChipColors(),
        border = style.borderStyle(),
        shape = style.shape()
    ) {
        ProvideTextStyle(
            value = style.typography(),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides style.selectableChipColors()
                    .contentColor(selected, enabled).value,
            ) {
                HighlightedText(
                    text = text,
                    highlightText = highlightText,
                    highlightFontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun HighlightChipPreview(
    @PreviewParameter(BooleanProvider::class) selected: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        HighlightChip(
            selected = selected,
            text = "#ThisIsATag",
            highlightText = "ATag"
        )
    }
}
