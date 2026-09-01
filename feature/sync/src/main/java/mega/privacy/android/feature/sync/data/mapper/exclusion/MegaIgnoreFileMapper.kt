package mega.privacy.android.feature.sync.data.mapper.exclusion

import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import javax.inject.Inject

/**
 * Maps the whole text of a `.megaignore` file to a [ParsedMegaIgnore] and back, handing each line
 * to [MegaIgnoreLineMapper].
 *
 * Lines are kept in file order and never grouped by kind, because the SDK evaluates filters
 * last-match-wins: reordering them changes which rule wins. A line the line mapper cannot model —
 * a regex rule, a comment, a `±sync:.megaignore` directive, a syntax error — is carried through as
 * a [MegaIgnoreEntry.PreservedEntry] rather than dropped, since dropping it would silently change
 * what the SDK enforces. Nothing here ever synthesizes a `sync:.megaignore` line.
 *
 * A line break is any of CR, LF or CRLF, which is what the SDK's own reader accepts, so a file
 * written on Windows splits into the same lines here as it does there.
 *
 * Output is canonical: LF separators, no trailing newline, and a leading BOM only when the source
 * had one, so a file authored by Desktop keeps its encoding signature while files we create carry
 * none. A source using CR or CRLF, or ending in a line break, is therefore normalized rather than
 * reproduced byte-for-byte; the SDK reads a final line that has no terminator, so this is safe.
 * Only the final terminator is dropped, so a file ending in several blank lines loses one of them
 * per save; the SDK ignores blank lines, so this changes nothing it enforces.
 */
internal class MegaIgnoreFileMapper @Inject constructor(
    private val megaIgnoreLineMapper: MegaIgnoreLineMapper,
) {

    /**
     * Parses whole-file text. An empty file — or one holding nothing but a BOM — yields no entries.
     *
     * The empty element that a trailing line break leaves behind is the terminator rather than a
     * line of the file, so it is dropped; blank lines within the file are kept.
     */
    operator fun invoke(content: String): ParsedMegaIgnore {
        val hadBom = content.startsWith(BOM)
        val lines = (if (hadBom) content.drop(1) else content).lines()

        return ParsedMegaIgnore(
            entries = lines
                .dropLast(if (lines.last().isEmpty()) 1 else 0)
                .map { line -> megaIgnoreLineMapper(line) ?: MegaIgnoreEntry.PreservedEntry(line) },
            hadBom = hadBom,
        )
    }

    /**
     * Serializes a model back to whole-file text.
     *
     * Entry content is written as it stands, so a pattern holding a line break would split across
     * two lines and make the SDK reject the file entire rather than just that rule;
     * `SyncExclusionValidation.ContainsLineBreak` is where such a pattern is refused.
     */
    operator fun invoke(parsedMegaIgnore: ParsedMegaIgnore): String = buildString {
        if (parsedMegaIgnore.hadBom) append(BOM)
        parsedMegaIgnore.entries.joinTo(this, LINE_TERMINATOR) { megaIgnoreLineMapper(it) }
    }

    private companion object {
        const val BOM = '\uFEFF'
        const val LINE_TERMINATOR = "\n"
    }
}
