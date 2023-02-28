package mega.privacy.android.core.ui.model

/**
 * Span indicator
 *
 * @property openTag
 * @property closeTag
 */
data class SpanIndicator(val openTag: String, val closeTag: String) {
    constructor(character: Char) : this("[${character}]", "[/${character}]")
}