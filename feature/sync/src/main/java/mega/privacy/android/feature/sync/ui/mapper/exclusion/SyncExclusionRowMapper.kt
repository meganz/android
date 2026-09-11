package mega.privacy.android.feature.sync.ui.mapper.exclusion

import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionDisplay
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionRowDescription
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionRowUiItem
import javax.inject.Inject

/**
 * Builds the rows of the exclusions list from a parsed file.
 *
 * Neither the match type nor the case sensitivity of a rule reaches the row: no copy describes
 * them, so two rules differing only in those read alike.
 */
internal class SyncExclusionRowMapper @Inject constructor(
    private val patternMapper: SyncExclusionPatternMapper,
    private val supportingTextMapper: SyncExclusionSupportingTextMapper,
) {

    /**
     * The rows for [parsed], in file order. A rule shown as an extension chip is left out, since
     * one rendered in both places is a rule the user can act on twice. Every other rule gets a row,
     * described or not, so that a rule written by another client stays visible and deletable.
     */
    operator fun invoke(parsed: ParsedMegaIgnore): List<SyncExclusionRowUiItem> {
        var nextRuleIndex = 0
        return parsed.entries.mapNotNull { entry ->
            val rule = (entry as? MegaIgnoreEntry.RuleEntry)?.rule
                ?: return@mapNotNull null
            val ruleIndex = nextRuleIndex++
            when (val display = patternMapper(rule)) {
                is SyncExclusionDisplay.Extension -> null
                is SyncExclusionDisplay.Condition -> SyncExclusionRowUiItem(
                    ruleIndex = ruleIndex,
                    rule = rule,
                    description = describe(rule, display),
                )
            }
        }
    }

    private fun describe(
        rule: SyncExclusionRule,
        condition: SyncExclusionDisplay.Condition,
    ): SyncExclusionRowDescription = supportingTextMapper(
        exclusionClass = rule.exclusionClass,
        target = rule.target,
        condition = condition.condition,
    )?.let { SyncExclusionRowDescription.Described(it, condition.value) }
        ?: SyncExclusionRowDescription.Undescribed(rule.pattern)
}
