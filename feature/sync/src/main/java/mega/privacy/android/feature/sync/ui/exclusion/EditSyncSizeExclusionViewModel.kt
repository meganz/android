package mega.privacy.android.feature.sync.ui.exclusion

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncSizeExclusionPrefillMapper
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.EditSyncSizeExclusionUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit

/**
 * Holds the draft of the Edit exclusion by-size screen while the user changes the file's bounds.
 *
 * The bounds arrive with the destination rather than being read here, so the form shows the pair
 * the user actually chose from the list even once another client has written to the same
 * `.megaignore`.
 */
@HiltViewModel(assistedFactory = EditSyncSizeExclusionViewModel.Factory::class)
internal class EditSyncSizeExclusionViewModel @AssistedInject constructor(
    @Assisted exclusion: SyncSizeExclusion,
    prefillMapper: SyncSizeExclusionPrefillMapper,
) : ViewModel() {

    val uiState: StateFlow<EditSyncSizeExclusionUiState>
        field = MutableStateFlow(EditSyncSizeExclusionUiState(bySize = prefillMapper(exclusion)))

    fun onConditionSelected(condition: SyncSizeCondition) {
        updateForm { it.copy(condition = condition) }
    }

    fun onFirstBoundValueChanged(value: String) {
        updateForm { it.copy(firstBound = it.firstBound.copy(value = value)) }
    }

    fun onFirstBoundUnitSelected(unit: SyncSizeUnit) {
        updateForm { it.copy(firstBound = it.firstBound.copy(unit = unit)) }
    }

    fun onSecondBoundValueChanged(value: String) {
        updateForm { it.copy(secondBound = it.secondBound.copy(value = value)) }
    }

    fun onSecondBoundUnitSelected(unit: SyncSizeUnit) {
        updateForm { it.copy(secondBound = it.secondBound.copy(unit = unit)) }
    }

    private fun updateForm(
        transform: (AddExclusionBySizeFormState) -> AddExclusionBySizeFormState,
    ) {
        uiState.update { it.copy(bySize = transform(it.bySize)) }
    }

    @AssistedFactory
    interface Factory {
        fun create(exclusion: SyncSizeExclusion): EditSyncSizeExclusionViewModel
    }
}
