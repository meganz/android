package mega.privacy.android.app.main.dialog.rubbishbin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import javax.inject.Inject

@HiltViewModel
internal class ConfirmMoveToRubbishBinViewModel @Inject constructor(
    private val isNodeInRubbish: IsNodeInRubbish,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(ConfirmMoveToRubbishBinUiState())
    val state = _state.asStateFlow()

    val handles =
        savedStateHandle.get<LongArray>(ConfirmMoveToRubbishBinDialogFragment.EXTRA_HANDLES)
            ?.toList().orEmpty()

    init {
        viewModelScope.launch {
            val handle = handles.firstOrNull() ?: return@launch
            val isNodeInRubbish =
                async { runCatching { isNodeInRubbish(handle) }.getOrDefault(false) }
            _state.update { state ->
                state.copy(
                    isNodeInRubbish = isNodeInRubbish.await(),
                )
            }
        }
    }
}

internal data class ConfirmMoveToRubbishBinUiState(
    val isNodeInRubbish: Boolean = false,
)
