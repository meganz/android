package mega.privacy.android.shared.original.core.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.controls.text.megaSpanStyle

/**
 * MegaSpanStyle : SpanStyle with TextColor & LinkColor
 * Note: If both TextColor & LinkColor are provided, TextColor will be used
 * @param spanStyle [SpanStyle]
 * @param color [TextColor]
 * @param linkColor [LinkColor]
 */
data class MegaSpanStyle(
    val spanStyle: SpanStyle = SpanStyle(),
    val color: TextColor? = null,
    val linkColor: LinkColor? = null,
) {
    /**
     * Converts MegaSpanStyle to SpanStyle
     */
    @Composable
    internal fun toSpanStyle(): SpanStyle {
        return color?.let { textColor ->
            megaSpanStyle(base = spanStyle, color = textColor)
        } ?: linkColor?.let { linkColor ->
            megaSpanStyle(base = spanStyle, linkColor = linkColor)
        } ?: spanStyle
    }
}
