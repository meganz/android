package mega.privacy.android.feature.sync.domain.usecase.exclusion

import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.equalsIgnoringEnabled
import javax.inject.Inject

/**
 * Puts a rule into a `.megaignore`.
 */
internal class AddSyncExclusionUseCase @Inject constructor() {

    /**
     * A rule that differs from one already in the file only by being switched off is not a new
     * rule — the disabled copy is still there as a commented out line — so it is switched back on
     * where it already sits. [SyncExclusionRule.enabled] is therefore honoured only when the rule
     * is appended.
     *
     * A new rule goes on the end, the strongest position the file offers since the SDK evaluates
     * filters last match wins. That is no promise it takes effect: a re-enabled rule stays where it
     * sat, so a later re-include can still override it. Offering that conflict to the user belongs
     * to the screens, not to this transform.
     *
     * @param current the file as parsed
     * @param rule the rule to add. Its pattern is taken verbatim, and has to arrive already trimmed
     * of the trailing whitespace the SDK strips on read, because de-duplication compares patterns
     * exactly. Refusing untrimmed input is the validator's job
     * @return [current] holding the rule
     */
    operator fun invoke(
        current: ParsedMegaIgnore,
        rule: SyncExclusionRule,
    ): ParsedMegaIgnore {
        val existingIndex = current.entries.indexOfFirst {
            it is MegaIgnoreEntry.RuleEntry && it.rule.equalsIgnoringEnabled(rule)
        }
        val entries = if (existingIndex < 0) {
            current.entries + MegaIgnoreEntry.RuleEntry(rule)
        } else {
            val existing = (current.entries[existingIndex] as MegaIgnoreEntry.RuleEntry).rule
            current.entries.mapIndexed { index, entry ->
                if (index == existingIndex) {
                    MegaIgnoreEntry.RuleEntry(existing.copy(enabled = true))
                } else {
                    entry
                }
            }
        }
        return current.copy(entries = entries)
    }
}
