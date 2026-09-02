package mega.privacy.android.feature.sync.data.mapper.exclusion

import mega.privacy.android.feature.sync.data.mapper.exclusion.MegaIgnoreLineVerdict.Companion.UNSIGNED_64_MAX
import mega.privacy.android.feature.sync.domain.entity.InvalidLineReason
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.domain.entity.SyncMatchStrategy
import java.math.BigInteger
import javax.inject.Inject

/**
 * Maps a single `.megaignore` line to a [MegaIgnoreEntry] and back, and answers what the SDK makes
 * of that line, mirroring the grammar it implements in `syncfilter.cpp`:
 *
 * ```
 * <class><target?><type?><strategy?>:<pattern>
 * exclude-smaller:<size>
 * exclude-larger:<size>
 * ```
 *
 * Parsing returns `null` for any line this model cannot represent — regex rules, the
 * `±sync:.megaignore` directive, comments, blank lines, size bounds wider than a [Long] and syntax
 * errors alike. Whether such a line is one the SDK accepts is a separate question, answered by
 * [classify]: the two must not be conflated, because the SDK evaluates filters last-match-wins and
 * abandons the whole file on a single bad line, so calling a line we merely cannot edit a bad one
 * would report a working file as inert.
 *
 * Serialization emits the minimal form, omitting any letter whose absence re-parses to the same
 * value. The type letter is the exception worth knowing about: the SDK infers a missing type from
 * whether the pattern contains a `/`, so a subtree-name pattern that does contain one — or a path
 * pattern that does not — has to spell the letter out or it changes meaning on the next read.
 *
 * Patterns with trailing ASCII whitespace or line breaks are not representable in this grammar;
 * rejecting them is the validation layer's job, not this mapper's.
 */
internal class MegaIgnoreLineMapper @Inject constructor() {

    /**
     * Parses a single line, or returns `null` when the caller must keep it verbatim.
     */
    operator fun invoke(line: String): MegaIgnoreEntry? = when {
        line.startsWith(COMMENT_MARKER) -> ruleEntry(line.drop(1), enabled = false)

        line.startsWith(EXCLUDE_SMALLER_PREFIX) || line.startsWith(EXCLUDE_LARGER_PREFIX) ->
            sizeEntry(line)

        else -> ruleEntry(line, enabled = true)
    }

    /**
     * Serializes an entry back to its `.megaignore` line.
     */
    operator fun invoke(entry: MegaIgnoreEntry): String = when (entry) {
        is MegaIgnoreEntry.RuleEntry -> serializeRule(entry.rule)
        is MegaIgnoreEntry.SizeSmallerEntry -> "$EXCLUDE_SMALLER_PREFIX${entry.bytes}"
        is MegaIgnoreEntry.SizeLargerEntry -> "$EXCLUDE_LARGER_PREFIX${entry.bytes}"
        is MegaIgnoreEntry.PreservedEntry -> entry.line
        is MegaIgnoreEntry.InvalidEntry -> entry.line
    }

    /**
     * Answers what the SDK's loader does with [line], so that a line it refuses can be told from
     * one it accepts but this app cannot model.
     *
     * A blank line and a comment are [MegaIgnoreLineVerdict.Accepted] because the SDK's reader
     * drops the first and skips the second. Whitespace-only lines are dropped as well, which is
     * why a stray indent only matters on a line that has content.
     *
     * The equal bounds rule is the one rejection this cannot answer alone: it depends on the size
     * lines that came before, so it is left to the caller folding
     * [MegaIgnoreLineVerdict.MovesSizeBound] over the file.
     */
    fun classify(line: String): MegaIgnoreLineVerdict = when {
        line.all { it in SDK_WHITESPACE } -> MegaIgnoreLineVerdict.Accepted

        line.first() == COMMENT_MARKER -> MegaIgnoreLineVerdict.Accepted

        line.first() == SIZE_DIRECTIVE_INITIAL -> classifySize(line)

        else -> when (val scan = scanRule(line)) {
            is RuleScan.Rejected -> rejected(scan.reason)
            is RuleScan.Fields, RuleScan.SyncDirective -> MegaIgnoreLineVerdict.Accepted
        }
    }

    private fun ruleEntry(line: String, enabled: Boolean): MegaIgnoreEntry.RuleEntry? {
        val fields = scanRule(line) as? RuleScan.Fields ?: return null
        if (fields.strategy.isRegex || fields.pattern.isEmpty()) return null

        return MegaIgnoreEntry.RuleEntry(
            SyncExclusionRule(
                pattern = fields.pattern,
                exclusionClass = fields.exclusionClass,
                target = fields.target,
                matchType = fields.matchType ?: inferMatchType(fields.pattern),
                strategy = fields.strategy,
                enabled = enabled,
            )
        )
    }

    private fun sizeEntry(line: String): MegaIgnoreEntry? {
        val verdict = classifySize(line) as? MegaIgnoreLineVerdict.MovesSizeBound ?: return null
        val bytes = verdict.bytes.takeIf { it <= LONG_MAX }?.toLong() ?: return null

        return when (verdict.bound) {
            SizeBound.Lower -> MegaIgnoreEntry.SizeSmallerEntry(bytes)
            SizeBound.Upper -> MegaIgnoreEntry.SizeLargerEntry(bytes)
        }
    }

    /**
     * Reads the grammar's letters off a rule line, in the fixed order the SDK reads them.
     *
     * Each of the three optional letters is consumed at most once and only in its own position, so
     * a duplicated or reordered letter is left sitting where the `:` was expected. That is how the
     * SDK comes to refuse `-gd:temp`. Whether the line holds a `:` further on is the only
     * refinement made on top, and it separates a line whose flag region is wrong from one that has
     * no separator at all.
     */
    private fun scanRule(line: String): RuleScan {
        var index = 0

        val exclusionClass = SyncExclusionClass.entries
            .firstOrNull { it.letter == line.getOrNull(index) }
            ?: return RuleScan.Rejected(
                if (line.trimStart(*SDK_WHITESPACE).startsWith(COMMENT_MARKER)) {
                    InvalidLineReason.CommentNotAtLineStart
                } else {
                    InvalidLineReason.MissingExclusionClass
                }
            )
        index++

        if (line.substring(index) == SYNC_SELF_DIRECTIVE) return RuleScan.SyncDirective

        val target = SyncExclusionTarget.entries
            .firstOrNull { it.letter == line.getOrNull(index) }
            ?.also { index++ }
            ?: SyncExclusionTarget.All

        val matchType = SyncExclusionMatchType.entries
            .firstOrNull { it.letter == line.getOrNull(index) }
            ?.also { index++ }

        val strategy = SyncMatchStrategy.entries
            .firstOrNull { it.letter == line.getOrNull(index) }
            ?.also { index++ }
            ?: SyncMatchStrategy.GlobCaseInsensitive

        if (line.getOrNull(index) != SEPARATOR) {
            return RuleScan.Rejected(
                if (line.indexOf(SEPARATOR, index) < 0) {
                    InvalidLineReason.MissingSeparator
                } else {
                    InvalidLineReason.UnexpectedFlagCharacter
                }
            )
        }

        return RuleScan.Fields(
            exclusionClass = exclusionClass,
            target = target,
            matchType = matchType,
            strategy = strategy,
            pattern = line.substring(index + 1).trimEnd(*SDK_WHITESPACE),
        )
    }

    /**
     * Reads a size directive, holding the value unsigned as the SDK holds it.
     *
     * The SDK skips whitespace between the `:` and the digits but reads the character straight
     * after them as the unit suffix, only skipping whitespace once it has one. A trailing space is
     * therefore a syntax error on `exclude-larger:100` and legal on `exclude-larger:100k`.
     */
    private fun classifySize(line: String): MegaIgnoreLineVerdict {
        val separator = line.indexOf(SEPARATOR)
        val bound = when (line.take(if (separator < 0) 0 else separator)) {
            EXCLUDE_SMALLER -> SizeBound.Lower
            EXCLUDE_LARGER -> SizeBound.Upper
            else -> return rejected(InvalidLineReason.UnknownDirective)
        }

        val value = line.substring(separator + 1).trimStart(*SDK_WHITESPACE)
        val digits = value.takeWhile { it in '0'..'9' }
        if (digits.isEmpty()) return rejected(InvalidLineReason.MalformedSizeValue)

        val bytes = BigInteger(digits)
        if (bytes > UNSIGNED_64_MAX) return rejected(InvalidLineReason.SizeValueOutOfRange)

        val suffix = value.substring(digits.length)
        if (suffix.isEmpty()) return MegaIgnoreLineVerdict.MovesSizeBound(bound, bytes)

        val shift = SIZE_SHIFTS[suffix.first().lowercaseChar()] ?: return rejected(
            if (suffix.first() in SDK_WHITESPACE) {
                InvalidLineReason.IllegalTrailingWhitespace
            } else {
                InvalidLineReason.MalformedSizeValue
            }
        )
        if (suffix.drop(1).any { it !in SDK_WHITESPACE }) {
            return rejected(InvalidLineReason.MalformedSizeValue)
        }

        val scaled = bytes.shiftLeft(shift)
        return if (scaled > UNSIGNED_64_MAX) {
            rejected(InvalidLineReason.SizeValueOutOfRange)
        } else {
            MegaIgnoreLineVerdict.MovesSizeBound(bound, scaled)
        }
    }

    private fun rejected(reason: InvalidLineReason) = MegaIgnoreLineVerdict.Rejected(reason)

    private fun serializeRule(rule: SyncExclusionRule): String = buildString {
        if (!rule.enabled) append(COMMENT_MARKER)
        append(rule.exclusionClass.letter)
        if (rule.target != SyncExclusionTarget.All) append(rule.target.letter)
        if (rule.matchType != inferMatchType(rule.pattern)) append(rule.matchType.letter)
        if (rule.strategy != SyncMatchStrategy.GlobCaseInsensitive) append(rule.strategy.letter)
        append(SEPARATOR)
        append(rule.pattern)
    }

    private fun inferMatchType(pattern: String): SyncExclusionMatchType =
        if (pattern.contains(PATH_SEPARATOR)) {
            SyncExclusionMatchType.Path
        } else {
            SyncExclusionMatchType.SubtreeName
        }

    /**
     * The letters a rule line carries, or why the SDK refuses it.
     */
    private sealed interface RuleScan {

        data class Fields(
            val exclusionClass: SyncExclusionClass,
            val target: SyncExclusionTarget,
            val matchType: SyncExclusionMatchType?,
            val strategy: SyncMatchStrategy,
            val pattern: String,
        ) : RuleScan

        /**
         * The `±sync:.megaignore` directive, which the SDK matches whole before it reads any
         * target letter, so its `s` is not a symlink target.
         */
        data object SyncDirective : RuleScan

        data class Rejected(val reason: InvalidLineReason) : RuleScan
    }

    private val SyncExclusionClass.letter: Char
        get() = when (this) {
            SyncExclusionClass.Exclude -> '-'
            SyncExclusionClass.Include -> '+'
        }

    private val SyncExclusionTarget.letter: Char
        get() = when (this) {
            SyncExclusionTarget.All -> 'a'
            SyncExclusionTarget.Directories -> 'd'
            SyncExclusionTarget.Files -> 'f'
            SyncExclusionTarget.Symlinks -> 's'
        }

    private val SyncExclusionMatchType.letter: Char
        get() = when (this) {
            SyncExclusionMatchType.LocalName -> 'N'
            SyncExclusionMatchType.SubtreeName -> 'n'
            SyncExclusionMatchType.Path -> 'p'
        }

    private val SyncMatchStrategy.letter: Char
        get() = when (this) {
            SyncMatchStrategy.GlobCaseInsensitive -> 'g'
            SyncMatchStrategy.GlobCaseSensitive -> 'G'
            SyncMatchStrategy.RegexCaseInsensitive -> 'r'
            SyncMatchStrategy.RegexCaseSensitive -> 'R'
        }

    private val SyncMatchStrategy.isRegex: Boolean
        get() = when (this) {
            SyncMatchStrategy.RegexCaseInsensitive, SyncMatchStrategy.RegexCaseSensitive -> true
            SyncMatchStrategy.GlobCaseInsensitive, SyncMatchStrategy.GlobCaseSensitive -> false
        }

    private companion object {
        /**
         * A comment marker, which is also how a rule the user has turned off is held: the SDK
         * reads it at the first byte only, so it both disables the line and keeps its text.
         */
        const val COMMENT_MARKER = '#'
        const val EXCLUDE_SMALLER = "exclude-smaller"
        const val EXCLUDE_LARGER = "exclude-larger"
        const val SYNC_SELF_DIRECTIVE = "sync:.megaignore"
        const val SEPARATOR = ':'
        const val PATH_SEPARATOR = '/'

        /**
         * The SDK routes every line beginning with this letter to the size directives, so such a
         * line naming neither of them is a syntax error rather than a rule.
         */
        const val SIZE_DIRECTIVE_INITIAL = 'e'

        const val EXCLUDE_SMALLER_PREFIX = "$EXCLUDE_SMALLER$SEPARATOR"
        const val EXCLUDE_LARGER_PREFIX = "$EXCLUDE_LARGER$SEPARATOR"

        val LONG_MAX: BigInteger = BigInteger.valueOf(Long.MAX_VALUE)

        /**
         * The SDK reads whitespace with a byte-wise C-locale `isspace`, so anything outside this
         * set — a no-break space ending a name pasted from a browser, say — is pattern content to
         * it. Kotlin's own [String.trimEnd] would eat those and silently retarget the rule.
         */
        val SDK_WHITESPACE = charArrayOf(' ', '\t', '\n', '\u000B', '\u000C', '\r')

        val SIZE_SHIFTS = mapOf(
            'k' to 10,
            'm' to 20,
            'g' to 30,
            't' to 40,
        )
    }
}
