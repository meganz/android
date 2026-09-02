package mega.privacy.android.feature.sync.data.mapper.exclusion

import mega.privacy.android.feature.sync.data.mapper.exclusion.MegaIgnoreLineVerdict.Companion.UNSIGNED_64_MAX
import mega.privacy.android.feature.sync.domain.entity.InvalidLineReason
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import java.math.BigInteger
import javax.inject.Inject

/**
 * Maps the whole text of a `.megaignore` file to a [ParsedMegaIgnore] and back, handing each line
 * to [MegaIgnoreLineMapper].
 *
 * Lines are kept in file order and never grouped by kind, because the SDK evaluates filters
 * last-match-wins: reordering them changes which rule wins. A line the line mapper cannot model is
 * carried through verbatim rather than dropped, since dropping it would silently change what the
 * SDK enforces — as a [MegaIgnoreEntry.PreservedEntry] when the SDK accepts it, and as a
 * [MegaIgnoreEntry.InvalidEntry] when it does not. Nothing here ever synthesizes a
 * `sync:.megaignore` line.
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
        val bounds = SizeBounds()

        return ParsedMegaIgnore(
            entries = lines
                .dropLast(if (lines.last().isEmpty()) 1 else 0)
                .map { line -> entryFor(line, bounds) },
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

    private fun entryFor(line: String, bounds: SizeBounds): MegaIgnoreEntry =
        reasonFor(line, bounds)?.let { MegaIgnoreEntry.InvalidEntry(line, it) }
            ?: megaIgnoreLineMapper(line)
            ?: MegaIgnoreEntry.PreservedEntry(line)

    /**
     * Why the SDK refuses [line], if it does.
     *
     * Only the stateful half of that answer stops at the first refusal. A line malformed in its
     * own text stays reported past that point, because nothing about it depends on how far the
     * loader had read, and a user fixing a file is better served seeing every such line at once
     * than one more of them per save. A bound collision is the opposite: it is a claim about
     * bounds the loader had applied, so past the point where it stopped there is no such claim to
     * make.
     */
    private fun reasonFor(line: String, bounds: SizeBounds): InvalidLineReason? =
        when (val verdict = megaIgnoreLineMapper.classify(line)) {
            is MegaIgnoreLineVerdict.Rejected -> {
                bounds.abandon()
                verdict.reason
            }

            is MegaIgnoreLineVerdict.MovesSizeBound -> InvalidLineReason.EqualSizeBounds
                .takeIf { bounds.applyAndRejects(verdict.bound, verdict.bytes) }

            MegaIgnoreLineVerdict.Accepted -> null
        }

    /**
     * The pair of size bounds as the SDK holds them while it reads a file: one object, both ends
     * starting wide open, each `exclude-` line overwriting one end.
     *
     * The bounds are compared after every line rather than at the end, which is what makes a file
     * whose final bounds are perfectly sane still refused over an intermediate pair.
     *
     * Folding stops at the first line the SDK refuses, because its loader returns there and reads
     * no further. Carrying the bounds on past that point would blame the next size line for
     * colliding with a bound the SDK never applied.
     */
    private class SizeBounds {
        private var lower: BigInteger = BigInteger.ZERO
        private var upper: BigInteger = UNSIGNED_64_MAX
        private var abandoned = false

        /**
         * Applies one bound and answers whether the SDK abandons the file at this line.
         */
        fun applyAndRejects(bound: SizeBound, bytes: BigInteger): Boolean {
            if (abandoned) return false
            when (bound) {
                SizeBound.Lower -> lower = bytes
                SizeBound.Upper -> upper = bytes
            }
            abandoned = lower == upper
            return abandoned
        }

        /**
         * Records that the loader stopped at a line refused for some other reason.
         */
        fun abandon() {
            abandoned = true
        }
    }

    private companion object {
        const val BOM = '\uFEFF'
        const val LINE_TERMINATOR = "\n"
    }
}
