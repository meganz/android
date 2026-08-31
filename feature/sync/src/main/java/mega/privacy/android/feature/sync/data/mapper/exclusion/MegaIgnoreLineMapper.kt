package mega.privacy.android.feature.sync.data.mapper.exclusion

import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.domain.entity.SyncMatchStrategy
import javax.inject.Inject

/**
 * Maps a single `.megaignore` line to a [MegaIgnoreEntry] and back, mirroring the grammar the
 * SDK implements in `syncfilter.cpp`:
 *
 * ```
 * <class><target?><type?><strategy?>:<pattern>
 * exclude-smaller:<size>
 * exclude-larger:<size>
 * ```
 *
 * Parsing returns `null` for any line this model cannot represent — regex rules, the
 * `±sync:.megaignore` directive, comments, blank lines and syntax errors alike. The caller is
 * expected to keep such lines verbatim as [MegaIgnoreEntry.PreservedEntry], because the SDK
 * evaluates filters last-match-wins and rejects the whole file on a single bad line.
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
     * Parses a single line, or returns `null` when the caller must preserve it verbatim.
     */
    operator fun invoke(line: String): MegaIgnoreEntry? = when {
        line.startsWith(DISABLED_RULE_PREFIX) ->
            parseRule(line.removePrefix(DISABLED_RULE_PREFIX), enabled = false)

        line.startsWith(EXCLUDE_SMALLER) ->
            parseBytes(line.removePrefix(EXCLUDE_SMALLER))
                ?.let { MegaIgnoreEntry.SizeSmallerEntry(it) }

        line.startsWith(EXCLUDE_LARGER) ->
            parseBytes(line.removePrefix(EXCLUDE_LARGER))
                ?.let { MegaIgnoreEntry.SizeLargerEntry(it) }

        line == SYNC_SELF_INCLUDE || line == SYNC_SELF_EXCLUDE -> null

        else -> parseRule(line, enabled = true)
    }

    /**
     * Serializes an entry back to its `.megaignore` line.
     */
    operator fun invoke(entry: MegaIgnoreEntry): String = when (entry) {
        is MegaIgnoreEntry.RuleEntry -> serializeRule(entry.rule)
        is MegaIgnoreEntry.SizeSmallerEntry -> "$EXCLUDE_SMALLER${entry.bytes}"
        is MegaIgnoreEntry.SizeLargerEntry -> "$EXCLUDE_LARGER${entry.bytes}"
        is MegaIgnoreEntry.PreservedEntry -> entry.line
    }

    private fun parseRule(line: String, enabled: Boolean): MegaIgnoreEntry.RuleEntry? {
        var index = 0

        val exclusionClass = SyncExclusionClass.entries
            .firstOrNull { it.letter == line.getOrNull(index) } ?: return null
        index++

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

        if (line.getOrNull(index) != SEPARATOR) return null
        if (strategy.isRegex) return null

        val pattern = line.substring(index + 1).trimEnd(*SDK_TRAILING_WHITESPACE)
        if (pattern.isEmpty()) return null

        return MegaIgnoreEntry.RuleEntry(
            SyncExclusionRule(
                pattern = pattern,
                exclusionClass = exclusionClass,
                target = target,
                matchType = matchType ?: inferMatchType(pattern),
                strategy = strategy,
                enabled = enabled,
            )
        )
    }

    private fun serializeRule(rule: SyncExclusionRule): String = buildString {
        if (!rule.enabled) append(DISABLED_RULE_PREFIX)
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

    private fun parseBytes(rawValue: String): Long? {
        val value = rawValue.trim()
        val multiplier = value.lastOrNull()?.lowercaseChar()?.let { SIZE_MULTIPLIERS[it] }
        val digits = if (multiplier == null) value else value.dropLast(1)
        if (digits.isEmpty() || digits.any { it !in '0'..'9' }) return null

        val bytes = digits.toLongOrNull() ?: return null
        val scale = multiplier ?: 1L
        return if (bytes > Long.MAX_VALUE / scale) null else bytes * scale
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
        const val DISABLED_RULE_PREFIX = "#"
        const val EXCLUDE_SMALLER = "exclude-smaller:"
        const val EXCLUDE_LARGER = "exclude-larger:"
        const val SYNC_SELF_INCLUDE = "+sync:.megaignore"
        const val SYNC_SELF_EXCLUDE = "-sync:.megaignore"
        const val SEPARATOR = ':'
        const val PATH_SEPARATOR = '/'

        /**
         * The SDK trims the pattern with a byte-wise C-locale `isspace`, so anything outside this
         * set — a no-break space ending a name pasted from a browser, say — is pattern content to
         * it. Kotlin's own [String.trimEnd] would eat those and silently retarget the rule.
         */
        val SDK_TRAILING_WHITESPACE = charArrayOf(' ', '\t', '\n', '\u000B', '\u000C', '\r')

        val SIZE_MULTIPLIERS = mapOf(
            'k' to (1L shl 10),
            'm' to (1L shl 20),
            'g' to (1L shl 30),
            't' to (1L shl 40),
        )
    }
}
