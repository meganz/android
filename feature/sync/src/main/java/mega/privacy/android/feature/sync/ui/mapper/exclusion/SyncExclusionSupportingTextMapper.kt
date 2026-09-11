package mega.privacy.android.feature.sync.ui.mapper.exclusion

import androidx.annotation.StringRes
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The sentence a rule row shows above its pattern.
 *
 * Every scope and match mode pair is its own string rather than a noun joined to a phrase, because
 * the joined form cannot be translated into languages that inflect either half.
 */
internal class SyncExclusionSupportingTextMapper @Inject constructor() {

    /**
     * The supporting text for a rule, or null when no approved copy covers it: a re-include rule,
     * or one targeting symbolic links. Null rather than a nearest fit, because describing a
     * re-include as an exclusion tells the user the opposite of what the rule does.
     */
    @StringRes
    operator fun invoke(
        exclusionClass: SyncExclusionClass,
        target: SyncExclusionTarget,
        condition: SyncExclusionCondition,
    ): Int? = when (exclusionClass) {
        SyncExclusionClass.Exclude -> when (target) {
            SyncExclusionTarget.All -> filesAndFolders(condition)
            SyncExclusionTarget.Directories -> folders(condition)
            SyncExclusionTarget.Files -> files(condition)
            SyncExclusionTarget.Symlinks -> null
        }

        SyncExclusionClass.Include -> null
    }

    @StringRes
    private fun filesAndFolders(condition: SyncExclusionCondition): Int = when (condition) {
        SyncExclusionCondition.BeginningWith ->
            sharedR.string.sync_exclusions_rule_files_and_folders_beginning_with

        SyncExclusionCondition.Containing ->
            sharedR.string.sync_exclusions_rule_files_and_folders_containing

        SyncExclusionCondition.EndingWith ->
            sharedR.string.sync_exclusions_rule_files_and_folders_ending_with

        SyncExclusionCondition.MatchingExactly ->
            sharedR.string.sync_exclusions_rule_files_and_folders_matching_exactly

        SyncExclusionCondition.MatchingWildcard ->
            sharedR.string.sync_exclusions_rule_files_and_folders_matching_wildcard
    }

    @StringRes
    private fun folders(condition: SyncExclusionCondition): Int = when (condition) {
        SyncExclusionCondition.BeginningWith ->
            sharedR.string.sync_exclusions_rule_folders_beginning_with

        SyncExclusionCondition.Containing ->
            sharedR.string.sync_exclusions_rule_folders_containing

        SyncExclusionCondition.EndingWith ->
            sharedR.string.sync_exclusions_rule_folders_ending_with

        SyncExclusionCondition.MatchingExactly ->
            sharedR.string.sync_exclusions_rule_folders_matching_exactly

        SyncExclusionCondition.MatchingWildcard ->
            sharedR.string.sync_exclusions_rule_folders_matching_wildcard
    }

    @StringRes
    private fun files(condition: SyncExclusionCondition): Int = when (condition) {
        SyncExclusionCondition.BeginningWith ->
            sharedR.string.sync_exclusions_rule_files_beginning_with

        SyncExclusionCondition.Containing ->
            sharedR.string.sync_exclusions_rule_files_containing

        SyncExclusionCondition.EndingWith ->
            sharedR.string.sync_exclusions_rule_files_ending_with

        SyncExclusionCondition.MatchingExactly ->
            sharedR.string.sync_exclusions_rule_files_matching_exactly

        SyncExclusionCondition.MatchingWildcard ->
            sharedR.string.sync_exclusions_rule_files_matching_wildcard
    }
}
