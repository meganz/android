package mega.privacy.android.feature.sync.ui.exclusion

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionEditPrefillMapper
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
) : ViewModel() {

    val uiState: StateFlow<EditSyncExclusionRuleUiState>
        field = MutableStateFlow(EditSyncExclusionRuleUiState(byName = prefillMapper(rule)))

    fun onTypeOptionSelected(typeOption: SyncExclusionTypeOption) {
        updateForm { it.copy(typeOption = typeOption) }
    }

    fun onConditionSelected(condition: SyncExclusionCondition) {
        updateForm { it.copy(condition = condition) }
    }

    fun onValueChanged(value: String) {
        updateForm { it.copy(value = value) }
    }

    private fun updateForm(
        transform: (AddExclusionByNameFormState) -> AddExclusionByNameFormState,
    ) {
        uiState.update { it.copy(byName = transform(it.byName)) }
    }

    @AssistedFactory
    interface Factory {
        fun create(rule: SyncExclusionRule): EditSyncExclusionRuleViewModel
    }
}
