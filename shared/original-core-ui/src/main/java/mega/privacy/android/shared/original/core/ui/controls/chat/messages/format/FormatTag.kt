package mega.privacy.android.shared.original.core.ui.controls.chat.messages.format

/**
 * Enum class defining all the possible format tags for a text.
 *
 * @property tag The tag used to format the text.
 * @property type [FormatType].
 */
enum class FormatTag(val tag: Char, val type: FormatType) {
    /**
     * Bold tag.
     */
    Bold('*', type = FormatType.Bold),

    /**
     * Italic tag.
     */
    Italic('_', type = FormatType.Italic),

    /**
     * Strikethrough tag.
     */
    Strikethrough('~', type = FormatType.Strikethrough),

    /**
     * Quote tag.
     */
    Quote('`', type = FormatType.Quote),
}