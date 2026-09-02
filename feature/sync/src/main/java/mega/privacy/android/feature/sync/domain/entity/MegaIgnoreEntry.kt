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
     * A line the SDK accepts but this app does not model, kept verbatim and in
     * position so it survives a round trip. Covers regex rules, comments, blank
     * lines, the `sync:.megaignore` directive and size bounds too large to hold in
     * a [Long].
     *
     * A line the SDK would reject is not one of these; it is an [InvalidEntry].
     * Collapsing the two would either declare a file with a regex rule inert or
     * present a file the SDK refuses as being enforced.
     *
     * @property line the line exactly as it was read, without its line terminator
     */
    data class PreservedEntry(val line: String) : MegaIgnoreEntry

    /**
     * A line the SDK refuses to parse, kept verbatim and in position.
     *
     * The SDK abandons the whole file on the first such line, so one of these means
     * none of the file's rules are in effect, however well formed the rest of it is.
     *
     * Where the line sits in the file is deliberately not held here. It is the
     * position of the entry within [ParsedMegaIgnore.entries], which is the only
     * place that stays true once an earlier line is edited away, and read back
     * through [ParsedMegaIgnore.invalidLines].
     *
     * @property line the line exactly as it was read, without its line terminator
     * @property reason why the SDK refuses it
     */
    data class InvalidEntry(
        val line: String,
        val reason: InvalidLineReason,
    ) : MegaIgnoreEntry
}
