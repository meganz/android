package mega.privacy.android.core.formatter.emoji

/**
 * Converts emoji short-codes embedded in a string into their emoji glyphs.
 *
 * For example the short-code `:smile:` is replaced by its corresponding emoji.
 * Text that contains no recognised short-codes is returned unchanged.
 */
interface EmojiShortcodeConverter {

    /**
     * Replaces every recognised emoji short-code in [text] with its emoji glyph.
     *
     * @param text the source text potentially containing short-codes.
     * @return the text with short-codes converted to emoji glyphs.
     */
    fun convert(text: String): String
}
