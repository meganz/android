package mega.privacy.android.feature.sync.domain.entity

/**
 * A parsed `.megaignore` file, held as the ordered list of its lines.
 *
 * [entries] is the authoritative representation and its order is the order of the
 * file. The properties below are projections over it, offered for reading only;
 * none of them may be used to rebuild the file, because the file cannot be
 * reassembled from buckets without losing the original line order.
 *
 * @property entries every line of the file, in file order
 * @property hadBom whether the source began with a UTF-8 byte order mark, so that
 * editing a file authored elsewhere keeps its encoding signature
 */
data class ParsedMegaIgnore(
    val entries: List<MegaIgnoreEntry>,
    val hadBom: Boolean = false,
) {

    /**
     * The modelled rules, in file order.
     *
     * A file may legitimately repeat a rule, so two of these can be equal. Neither
     * a rule's value nor its position in this list identifies it; the index of its
     * [MegaIgnoreEntry.RuleEntry] within [entries] does, and that index is also the
     * anchor an edit has to write back to.
     */
    val rules: List<SyncExclusionRule>
        get() = entries.filterIsInstance<MegaIgnoreEntry.RuleEntry>().map { it.rule }

    /**
     * The size bounds actually in effect.
     *
     * Each bound is taken from the last matching line rather than the first,
     * because the SDK applies the last occurrence. A file may legitimately
     * repeat a bound, and reporting the first would show a limit that is not
     * the one being enforced.
     */
    val size: SyncSizeExclusion
        get() = SyncSizeExclusion(
            excludeSmallerThanBytes = entries
                .filterIsInstance<MegaIgnoreEntry.SizeSmallerEntry>()
                .lastOrNull()
                ?.bytes,
            excludeLargerThanBytes = entries
                .filterIsInstance<MegaIgnoreEntry.SizeLargerEntry>()
                .lastOrNull()
                ?.bytes,
        )

    /**
     * The lines this app does not model, in file order.
     *
     * Useful for telling the user how much of a file is beyond editing here. Not
     * useful for writing the file back, which must go through [entries].
     */
    val preservedLines: List<String>
        get() = entries.filterIsInstance<MegaIgnoreEntry.PreservedEntry>().map { it.line }

    /**
     * The lines the SDK refuses to parse, in file order, each with the line number to show the
     * user.
     *
     * The number is derived from the entry's position rather than stored on it, because [entries]
     * is what an edit rewrites and a number held on an entry would survive the line moving.
     */
    val invalidLines: List<InvalidLine>
        get() = entries.mapIndexedNotNull { index, entry ->
            (entry as? MegaIgnoreEntry.InvalidEntry)
                ?.let { InvalidLine(lineNumber = index + FIRST_LINE_NUMBER, entry = it) }
        }

    /**
     * Whether the SDK refuses this file, and with it every rule the file holds.
     *
     * The SDK's loader abandons the file on the first line it cannot parse and commits nothing, so
     * a single bad line leaves the sync with no exclusions at all rather than with the rules that
     * parsed. Anything reporting what is being excluded has to consult this first: presenting
     * [rules] as active while this is true tells the user the opposite of what the sync is doing.
     */
    val isRejectedBySdk: Boolean
        get() = entries.any { it is MegaIgnoreEntry.InvalidEntry }

    /**
     * A line the SDK refuses, together with where it sits in the file.
     *
     * @property lineNumber the line's position, counting from one, as the file will be written
     * @property entry the line itself
     */
    data class InvalidLine(
        val lineNumber: Int,
        val entry: MegaIgnoreEntry.InvalidEntry,
    )

    private companion object {
        const val FIRST_LINE_NUMBER = 1
    }
}
