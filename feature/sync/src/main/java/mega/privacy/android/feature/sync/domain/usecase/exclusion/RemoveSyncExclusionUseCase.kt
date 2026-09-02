package mega.privacy.android.feature.sync.domain.usecase.exclusion

import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.entriesIndexOfRule
import mega.privacy.android.feature.sync.domain.exception.SyncExclusionEditError
import javax.inject.Inject

/**
 * Takes one rule out of a `.megaignore`, leaving every other line where it was.
 */
internal class RemoveSyncExclusionUseCase @Inject constructor() {

    /**
     * @param current the file as parsed
     * @param ruleIndex the rule's position in [ParsedMegaIgnore.rules]
     * @param expected the rule the caller read at [ruleIndex], which the index has to still be
     * pointing at in a file every other client of the account can edit. Its
     * [SyncExclusionRule.enabled] is not held against the file: removing a rule is the same intent
     * whether or not it is currently in effect
     * @return [current] without that rule, every other line still in the position it held, or
     * [SyncExclusionEditError.StaleRuleTarget]
     */
    operator fun invoke(
        current: ParsedMegaIgnore,
        ruleIndex: Int,
        expected: SyncExclusionRule,
    ): Result<ParsedMegaIgnore> {
        val entriesIndex = current.entriesIndexOfRule(ruleIndex, expected)
            ?: return Result.failure(SyncExclusionEditError.StaleRuleTarget(ruleIndex))
        return Result.success(
            current.copy(
                entries = current.entries.filterIndexed { index, _ -> index != entriesIndex },
            )
        )
    }
}
