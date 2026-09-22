package mega.privacy.android.feature.sync.ui.exclusion

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionEdit
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionEditPrefillMapper
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionEditResultMapper
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.EditSyncExclusionRuleUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption

/**
 * Holds the draft of the Edit exclusion screen while the user changes an existing rule.
 *
 * The rule arrives with the destination rather than being read here, so that the form shows the
 * rule the user actually chose from the list even once another client has written to the same
 * `.megaignore`.
 */
@HiltViewModel(assistedFactory = EditSyncExclusionRuleViewModel.Factory::class)
internal class EditSyncExclusionRuleViewModel @AssistedInject constructor(
    @Assisted rule: SyncExclusionRule,
    prefillMapper: SyncExclusionEditPrefillMapper,
    private val resultMapper: SyncExclusionEditResultMapper,
) : ViewModel() {

    val uiState: StateFlow<EditSyncExclusionRuleUiState>
        field = MutableStateFlow(gated(EditSyncExclusionRuleUiState(byName = prefillMapper(rule))))

    /** The change the draft describes, or null while it describes none. */
    fun buildEdit(): SyncExclusionEdit? = resultMapper(uiState.value.byName)

    fun onTypeOptionSelected(typeOption: SyncExclusionTypeOption) {
        updateForm { it.copy(typeOption = typeOption) }
    }

    fun onConditionSelected(condition: SyncExclusionCondition) {
        updateForm { it.copy(condition = condition) }
    }

    fun onValueChanged(value: String) {
        updateForm { it.copy(value = value) }
    }

    /**
     * The one way the draft changes, so that a field added later is covered by the save gate
     * without its author having to remember it.
     */
    private fun updateForm(
        transform: (AddExclusionByNameFormState) -> AddExclusionByNameFormState,
    ) {
        uiState.update { gated(it.copy(byName = transform(it.byName))) }
    }

    /** [state] with the save gate taken from what the mapper would build out of it. */
    private fun gated(state: EditSyncExclusionRuleUiState) =
        state.copy(canSave = resultMapper(state.byName) != null)

    @AssistedFactory
    interface Factory {
        fun create(rule: SyncExclusionRule): EditSyncExclusionRuleViewModel
    }
}
