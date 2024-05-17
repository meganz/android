package mega.privacy.android.shared.original.core.ui.model

/**
 * Span indicator
 *
 * @property openTag
 * @property closeTag
 */
data class SpanIndicator(val openTag: String, val closeTag: String) {
    constructor(character: Char) : this("[${character}]", "[/${character}]")
}