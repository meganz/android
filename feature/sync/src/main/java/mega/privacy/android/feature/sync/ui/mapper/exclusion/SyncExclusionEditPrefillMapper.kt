package mega.privacy.android.feature.sync.ui.mapper.exclusion

import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionDisplay
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import javax.inject.Inject

/**
 * Fills the Edit exclusion form from the rule the user chose.
 *
 * Only the target and the pattern reach the form. Every other field stays on the rule itself, so
 * that an edit cannot express, and therefore cannot drop, grammar this screen never renders.
 */
internal class SyncExclusionEditPrefillMapper @Inject constructor(
    private val patternMapper: SyncExclusionPatternMapper,
) {

    /**
     * The form [rule] opens with.
     *
     * A rule this app has no type for opens showing its whole pattern instead of a fragment of it.
     * The form describes a fragment through the chips, and the chips go with the type, so a rule
     * carrying no type would show a fragment nothing on screen frames: "draft" for `draft*`, or a
     * re-include described as an exclusion. Showing the pattern verbatim is what the exclusions
     * list already does for the same rules.
     */
    operator fun invoke(rule: SyncExclusionRule): AddExclusionByNameFormState {
        val typeOption = rule.typeOption
        val display = (patternMapper(rule) as? SyncExclusionDisplay.Condition)
            ?.takeIf { typeOption != null }
            ?: SyncExclusionDisplay.Condition(
                condition = SyncExclusionCondition.MatchingWildcard,
                value = rule.pattern,
            )
        return AddExclusionByNameFormState(
            typeOption = typeOption,
            condition = display.condition,
            value = display.value,
        )
    }

    /**
     * The Type entry standing for the rule, or null for a rule no entry describes: the sheet names
     * node types of an exclusion only, and offering one for a re-include or for a symlink rule
     * would state the opposite of, or more than, what the rule does.
     */
    private val SyncExclusionRule.typeOption: SyncExclusionTypeOption?
        get() = if (exclusionClass != SyncExclusionClass.Exclude) {
            null
        } else when (target) {
            SyncExclusionTarget.All -> SyncExclusionTypeOption.FilesAndFolders
            SyncExclusionTarget.Directories -> SyncExclusionTypeOption.Folders
            SyncExclusionTarget.Files -> SyncExclusionTypeOption.Files
            SyncExclusionTarget.Symlinks -> null
        }
}
