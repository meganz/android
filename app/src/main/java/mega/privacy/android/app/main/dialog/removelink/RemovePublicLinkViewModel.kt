package mega.privacy.android.app.main.dialog.removelink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import javax.inject.Inject

@HiltViewModel
internal class RemovePublicLinkViewModel @Inject constructor(
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(RemovePublicLinkUiState())
    val state = _state.asStateFlow()

    private val ids = savedStateHandle.get<LongArray>(RemovePublicLinkDialogFragment.EXTRA_NODE_IDS)
        ?: LongArray(0)

    init {
        viewModelScope.launch {
            if (ids.size == 1) {
                runCatching {
                    getNodeByHandleUseCase(ids[0])
                }.onSuccess { node ->
                    _state.update { state -> state.copy(isNodeTakenDown = node == null || node.isTakenDown) }
                }
            }
        }
    }
}

/**
 * Remove public link ui state
 *
 * @property isNodeTakenDown
 */
data class RemovePublicLinkUiState(
    val isNodeTakenDown: Boolean = false,
)