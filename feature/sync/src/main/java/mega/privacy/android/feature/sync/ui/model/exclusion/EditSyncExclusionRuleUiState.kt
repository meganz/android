package mega.privacy.android.feature.sync.ui.model.exclusion

/**
 * What the Edit exclusion screen shows.
 *
 * There is one form rather than the two tabs of Add: a rule is already by name or by size when the
 * screen opens, and offering the other tab would let a save rewrite the rule into something the
 * user never chose to edit.
 */
internal data class EditSyncExclusionRuleUiState(
    val byName: AddExclusionByNameFormState = AddExclusionByNameFormState(),
)
