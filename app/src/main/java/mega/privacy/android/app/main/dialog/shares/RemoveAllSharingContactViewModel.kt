package mega.privacy.android.app.main.dialog.shares

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.shares.GetOutShareByNodeIdUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Remove all sharing contact view model
 *
 * @param savedStateHandle
 */
@HiltViewModel
internal class RemoveAllSharingContactViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getOutShareByNodeIdUseCase: GetOutShareByNodeIdUseCase,
) : ViewModel() {
    private val nodeIds =
        savedStateHandle.get<LongArray>(RemoveAllSharingContactDialogFragment.EXTRA_NODE_IDS)
            ?: LongArray(0)
    private val _state =
        MutableStateFlow(RemoveAllSharingContactUiState(numberOfShareFolder = nodeIds.size))

    /**
     * State
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (nodeIds.size == 1) {
                runCatching {
                    getOutShareByNodeIdUseCase(NodeId(nodeIds[0]))
                }.onSuccess { shares ->
                    _state.update { it.copy(numberOfShareContact = shares.size) }
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }
}