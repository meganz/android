package mega.privacy.android.feature.sync.domain.entity

/**
 * The position within [ParsedMegaIgnore.entries] of the rule [ParsedMegaIgnore.rules] holds at
 * [ruleIndex], or null if the file holds no rule there.
 *
 * `rules` drops comments, blank lines, size bounds, regex rules and lines the SDK refuses, so the
 * two positions coincide only in a file whose rules run unbroken from the top. Editing at the rules
 * index would overwrite whichever unmodelled line occupies that slot.
 */
internal fun ParsedMegaIgnore.entriesIndexOfRule(ruleIndex: Int): Int? {
    if (ruleIndex < 0) return null
    var seen = 0
    entries.forEachIndexed { index, entry ->
        if (entry is MegaIgnoreEntry.RuleEntry) {
            if (seen == ruleIndex) return index
            seen++
        }
    }
    return null
}

/**
 * The position within [ParsedMegaIgnore.entries] of the rule at [ruleIndex], provided that rule is
 * still [expected], and null otherwise.
 *
 * The file belongs to every client of the account, and an insertion above shifts every rule down
 * while leaving each index resolvable, so only comparing the rule itself catches a stale target.
 *
 * [SyncExclusionRule.enabled] is left out of the comparison: it is not rendered beside a rule, so a
 * caller cannot have chosen on it. A caller changing `enabled` has to compare that field itself.
 */
internal fun ParsedMegaIgnore.entriesIndexOfRule(
    ruleIndex: Int,
    expected: SyncExclusionRule,
): Int? = entriesIndexOfRule(ruleIndex)
    ?.takeIf { (entries[it] as MegaIgnoreEntry.RuleEntry).rule.equalsIgnoringEnabled(expected) }
