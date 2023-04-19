package mega.privacy.android.app.presentation.versions.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.versions.dialog.model.VersionsBottomSheetDialogState
import javax.inject.Inject

/**
 * ViewModel class for [mega.privacy.android.app.modalbottomsheet.VersionsBottomSheetDialogFragment]
 *
 * @property getNodeByHandle Retrieves the Node from the given Handle
 */
@HiltViewModel
class VersionsBottomSheetDialogViewModel @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(VersionsBottomSheetDialogState())

    /**
     * UI State class for [mega.privacy.android.app.modalbottomsheet.VersionsBottomSheetDialogFragment]
     */
    val state: StateFlow<VersionsBottomSheetDialogState> = _state.asStateFlow()

    /**
     * Initializes the ViewModel
     *
     * @param nodeHandle A potentially nullable Node Handle
     */
    fun init(nodeHandle: Long?) = viewModelScope.launch {
        nodeHandle?.let { nonNullHandle ->
            val node = getNodeByHandle(nonNullHandle)
            _state.update {
                it.copy(
                    node = node,
                )
            }
        }
    }
}