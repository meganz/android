package mega.privacy.android.feature.sync.ui.exclusion

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionTab
import mega.privacy.android.feature.sync.ui.model.exclusion.AddSyncExclusionRuleUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import javax.inject.Inject

/**
 * Holds the drafts of the Add exclusion screen while the user builds a rule.
 *
 * The tabs are kept apart here rather than in the forms, which are stateless bodies, so that
 * showing one tab cannot lose what was typed into the other.
 */
@HiltViewModel
internal class AddSyncExclusionRuleViewModel @Inject constructor() : ViewModel() {

    val uiState: StateFlow<AddSyncExclusionRuleUiState>
        field = MutableStateFlow(AddSyncExclusionRuleUiState())

    fun onTabSelected(tab: AddExclusionTab) {
        uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * Picking a type also drops the condition when the new type carries none, so a rule cannot
     * keep a condition the form has stopped showing.
     */
    fun onTypeOptionSelected(typeOption: SyncExclusionTypeOption) {
        val keepsCondition = typeOption != SyncExclusionTypeOption.Extensions
        uiState.update { state ->
            state.copy(
                byName = state.byName.copy(
                    typeOption = typeOption,
                    condition = state.byName.condition.takeIf { keepsCondition },
                ),
            )
        }
    }

    fun onConditionSelected(condition: SyncExclusionCondition) {
        uiState.update { it.copy(byName = it.byName.copy(condition = condition)) }
    }

    fun onNameValueChanged(value: String) {
        uiState.update { it.copy(byName = it.byName.copy(value = value)) }
    }

    fun onExtensionValueChanged(value: String) {
        uiState.update { it.copy(byExtension = it.byExtension.copy(value = value)) }
    }

    fun onSizeConditionSelected(condition: SyncSizeCondition) {
        updateSize { it.copy(condition = condition) }
    }

    fun onFirstBoundValueChanged(value: String) {
        updateSize { it.copy(firstBound = it.firstBound.copy(value = value)) }
    }

    fun onFirstBoundUnitSelected(unit: SyncSizeUnit) {
        updateSize { it.copy(firstBound = it.firstBound.copy(unit = unit)) }
    }

    fun onSecondBoundValueChanged(value: String) {
        updateSize { it.copy(secondBound = it.secondBound.copy(value = value)) }
    }

    fun onSecondBoundUnitSelected(unit: SyncSizeUnit) {
        updateSize { it.copy(secondBound = it.secondBound.copy(unit = unit)) }
    }

    private fun updateSize(
        transform: (AddExclusionBySizeFormState) -> AddExclusionBySizeFormState,
    ) {
        uiState.update { it.copy(bySize = transform(it.bySize)) }
    }
}
