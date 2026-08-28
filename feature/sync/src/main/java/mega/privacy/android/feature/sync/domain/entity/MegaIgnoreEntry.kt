package mega.privacy.android.feature.sync.domain.entity

/**
 * A single line of a `.megaignore` file, in the position it occupies in that file.
 *
 * Every line of the file maps to exactly one entry, including lines this app does
 * not model. That is what allows a parse then serialize round trip to reproduce the
 * original file: the SDK evaluates filters last match wins, so reordering or
 * dropping a line silently changes which rule takes effect.
 */
sealed interface MegaIgnoreEntry {

    /**
     * A name, subtree or path rule that this app models and can edit.
     *
     * @property rule the modelled rule
     */
    data class RuleEntry(val rule: SyncExclusionRule) : MegaIgnoreEntry

    /**
     * An `exclude-smaller` line.
     *
     * @property bytes the bound, in bytes
     */
    data class SizeSmallerEntry(val bytes: Long) : MegaIgnoreEntry

    /**
     * An `exclude-larger` line.
     *
     * @property bytes the bound, in bytes
     */
    data class SizeLargerEntry(val bytes: Long) : MegaIgnoreEntry

    /**
     * A line this app does not model, kept verbatim and in position so it survives
     * a round trip. Covers regex rules, includes, comments, blank lines and the
     * `sync:.megaignore` directive.
     *
     * @property line the line exactly as it was read, without its line terminator
     */
    data class PreservedEntry(val line: String) : MegaIgnoreEntry
}
