package mega.privacy.android.core.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.tokens.TextColor

/**
 * MegaSpanStyle : SpanStyle with TextColor
 *
 * @param spanStyle [SpanStyle]
 * @param color [TextColor]
 */
data class MegaSpanStyle(val spanStyle: SpanStyle, val color: TextColor?) {
    constructor(spanStyle: SpanStyle) : this(spanStyle, null)
    constructor(color: TextColor) : this(SpanStyle(), color)

    /**
     * Converts MegaSpanStyle to SpanStyle
     */
    @Composable
    fun toSpanStyle(): SpanStyle {
        return color?.let {
            spanStyle.copy(color = MegaTheme.textColor(textColor = color))
        } ?: spanStyle
    }
}
