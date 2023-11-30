package mega.privacy.android.core.ui.controls.text

/**
 * Mega text overflow.
 *
 * @property maxLines Max lines.
 */
sealed class LongTextBehaviour(open val maxLines: Int) {

    /**
     * Clip.
     *
     * Clip the overflowing text to fix its container.
     */
    data class Clip(override val maxLines: Int = Int.MAX_VALUE) : LongTextBehaviour(maxLines)

    /**
     * Ellipsis.
     *
     * Display an ellipsis character at the end of the last line if the text is too long to fit
     */
    data class Ellipsis(override val maxLines: Int = Int.MAX_VALUE) : LongTextBehaviour(maxLines)

    /**
     * Ellipsis middle.
     *
     * Display an ellipsis character at the middle if the text is too long to fit
     */
    data object MiddleEllipsis : LongTextBehaviour(1)

    /**
     * Marquee.
     *
     * Display the text in a marquee if the text is too long to fit
     */
    data object Marquee : LongTextBehaviour(1)

    /**
     * Visible.
     *
     * Display all text, even if there is not enough space in the specified bounds
     */
    data class Visible(override val maxLines: Int = Int.MAX_VALUE) : LongTextBehaviour(maxLines)
}