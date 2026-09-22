package mega.privacy.android.feature.sync.ui.exclusion

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.feature.sync.navigation.AddSyncExclusionResult
import mega.privacy.android.feature.sync.ui.mapper.exclusion.AddSyncExclusionResultMapper
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
internal class AddSyncExclusionRuleViewModel @Inject constructor(
    private val resultMapper: AddSyncExclusionResultMapper,
) : ViewModel() {

    val uiState: StateFlow<AddSyncExclusionRuleUiState>
        field = MutableStateFlow(gated(AddSyncExclusionRuleUiState()))

    /** What the drafts would add, or null while they would add nothing. */
    fun buildResult(): AddSyncExclusionResult? = resultMapper(uiState.value)

    fun onTabSelected(tab: AddExclusionTab) {
        updateState { it.copy(selectedTab = tab) }
    }

    /**
     * Picking a type also drops the condition when the new type carries none, so a rule cannot
     * keep a condition the form has stopped showing.
     */
    fun onTypeOptionSelected(typeOption: SyncExclusionTypeOption) {
        val keepsCondition = typeOption != SyncExclusionTypeOption.Extensions
        updateState { state ->
            state.copy(
                byName = state.byName.copy(
                    typeOption = typeOption,
                    condition = state.byName.condition.takeIf { keepsCondition },
                ),
            )
        }
    }

    fun onConditionSelected(condition: SyncExclusionCondition) {
        updateState { it.copy(byName = it.byName.copy(condition = condition)) }
    }

    fun onNameValueChanged(value: String) {
        updateState { it.copy(byName = it.byName.copy(value = value)) }
    }

    fun onExtensionValueChanged(value: String) {
        updateState { it.copy(byExtension = it.byExtension.copy(value = value)) }
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
        updateState { it.copy(bySize = transform(it.bySize)) }
    }

    /**
     * The one way a draft changes, so that a field added later is covered by the confirm gate
     * without its author having to remember it.
     */
    private fun updateState(
        transform: (AddSyncExclusionRuleUiState) -> AddSyncExclusionRuleUiState,
    ) {
        uiState.update { gated(transform(it)) }
    }

    /** [state] with the confirm gate taken from what the mapper would build out of it. */
    private fun gated(state: AddSyncExclusionRuleUiState) =
        state.copy(canConfirm = resultMapper(state) != null)
}
