package mega.privacy.android.feature.sync.ui.mapper.exclusion

import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionChipUiItem
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionDisplay
import javax.inject.Inject

/**
 * Builds the extension chips of the exclusions list from a parsed file.
 *
 * The complement of [SyncExclusionRowMapper]: the two split the file's rules between them, and
 * both number a rule by its position among every rule entry rather than among the ones they
 * themselves render. A rule the other shows therefore still advances the count, which is what
 * keeps a chip's [SyncExclusionChipUiItem.ruleIndex] pointing at the rule the edit and remove APIs
 * will find at that position.
 *
 * Neither the match type, the case sensitivity nor the enabled state of a rule reaches the chip:
 * the chip is only its extension, so a rule matching local names and one matching a whole subtree
 * read alike, and so does a rule that is commented out beside one that is not. Removing either
 * still removes the right line, because the index addresses it, but the chip that stays looks like
 * the one that went.
 */
internal class SyncExclusionChipMapper @Inject constructor(
    private val patternMapper: SyncExclusionPatternMapper,
) {

    /**
     * The chips for [parsed], in file order. Only a rule the pattern mapper reads as a plain
     * extension exclusion becomes a chip; every other rule is left to the rows, so that no rule is
     * rendered twice and none is dropped.
     */
    operator fun invoke(parsed: ParsedMegaIgnore): List<SyncExclusionChipUiItem> {
        var nextRuleIndex = 0
        return parsed.entries.mapNotNull { entry ->
            val rule = (entry as? MegaIgnoreEntry.RuleEntry)?.rule
                ?: return@mapNotNull null
            val ruleIndex = nextRuleIndex++
            when (val display = patternMapper(rule)) {
                is SyncExclusionDisplay.Extension -> SyncExclusionChipUiItem(
                    ruleIndex = ruleIndex,
                    rule = rule,
                    extension = display.extension,
                )

                is SyncExclusionDisplay.Condition -> null
            }
        }
    }
}
