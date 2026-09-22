package mega.privacy.android.feature.sync.ui.model.exclusion

/**
 * What the Edit exclusion screen shows.
 *
 * There is one form rather than the two tabs of Add: a rule is already by name or by size when the
 * screen opens, and offering the other tab would let a save rewrite the rule into something the
 * user never chose to edit.
 *
 * @property canSave whether the form describes a change. Read from the mapper that builds the edit
 * rather than judged here, so a rule this form cannot describe — a re-include, a symlink rule —
 * offers no save that would stage nothing. The trash stays available for those
 */
internal data class EditSyncExclusionRuleUiState(
    val byName: AddExclusionByNameFormState = AddExclusionByNameFormState(),
    val canSave: Boolean = false,
)
