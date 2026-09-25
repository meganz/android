package mega.privacy.android.feature.sync.ui.mapper.exclusion

import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.navigation.AddSyncExclusionResult
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByExtensionFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionTab
import mega.privacy.android.feature.sync.ui.model.exclusion.AddSyncExclusionRuleUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import javax.inject.Inject

/**
 * Reads what the Add exclusion screen has authored, from whichever of its three bodies is showing.
 *
 * Only the showing body is read, so a draft left behind on the tab the user moved away from cannot
 * reach the file.
 */
internal class AddSyncExclusionResultMapper @Inject constructor(
    private val patternMapper: SyncExclusionPatternMapper,
    private val sizeFormMapper: SyncSizeExclusionFormMapper,
) {

    /**
     * What [state] would add, or null while it would add nothing: a form still incomplete, or an
     * extensions field holding nothing a pattern can be built from.
     */
    operator fun invoke(state: AddSyncExclusionRuleUiState): AddSyncExclusionResult? =
        when (state.selectedTab) {
            AddExclusionTab.BySize -> sizeFormMapper(state.bySize)
                ?.let(AddSyncExclusionResult::SizeBounds)

            AddExclusionTab.ByName ->
                if (state.byName.typeOption == SyncExclusionTypeOption.Extensions) {
                    extensionRules(state.byExtension)
                } else {
                    nameRule(state.byName)
                }
        }

    /**
     * The rule one typed [extension] stands for, or null when no pattern can be built from it.
     *
     * Left at every node type so that it is the same shape as a predefined extension: a narrower
     * target is shown as a row rather than a chip, and no longer folds into the default holding
     * that pattern, which leaves the file saying it twice.
     */
    fun extensionRule(extension: String): SyncExclusionRule? =
        patternMapper.extensionPattern(extension)?.let {
            SyncExclusionRule(
                pattern = it,
                matchType = SyncExclusionMatchType.SubtreeName,
            )
        }

    /**
     * One rule per extension the field names. Entries no pattern can be built from are dropped
     * rather than failing the rest, and repeats are folded: de-duplication elsewhere compares
     * patterns exactly, so a list would otherwise stage the same rule twice.
     */
    private fun extensionRules(
        form: AddExclusionByExtensionFormState,
    ): AddSyncExclusionResult.Rules? = form.extensions
        .mapNotNull(::extensionRule)
        .distinct()
        .takeIf { it.isNotEmpty() }
        ?.let { AddSyncExclusionResult.Rules(rules = it, fromExtensionForm = true) }

    private fun nameRule(form: AddExclusionByNameFormState): AddSyncExclusionResult.Rules? {
        val target = form.typeOption?.target ?: return null
        val condition = form.condition ?: return null
        if (form.value.isBlank()) return null
        return AddSyncExclusionResult.Rules(
            rules = listOf(
                SyncExclusionRule(
                    pattern = patternMapper(condition, form.value),
                    target = target,
                    matchType = SyncExclusionMatchType.SubtreeName,
                ),
            ),
            fromExtensionForm = false,
        )
    }

    /**
     * The node types the chosen entry stands for, or null for the extensions entry, which names a
     * mode of this form rather than a target the grammar has.
     */
    private val SyncExclusionTypeOption.target: SyncExclusionTarget?
        get() = when (this) {
            SyncExclusionTypeOption.Files -> SyncExclusionTarget.Files
            SyncExclusionTypeOption.Folders -> SyncExclusionTarget.Directories
            SyncExclusionTypeOption.FilesAndFolders -> SyncExclusionTarget.All
            SyncExclusionTypeOption.Extensions -> null
        }
}
