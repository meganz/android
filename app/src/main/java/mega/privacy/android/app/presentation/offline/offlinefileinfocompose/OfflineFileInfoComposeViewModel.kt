package mega.privacy.android.app.presentation.offline.offlinefileinfocompose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationByIdUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model class for [OfflineFileInfoComposeFragment]
 */
@HiltViewModel
internal class OfflineFileInfoComposeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getOfflineFileInformationByIdUseCase: GetOfflineFileInformationByIdUseCase,
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
) : ViewModel() {

    /**
     * Node Id from [SavedStateHandle]
     */
    private val nodeId = NodeId(savedStateHandle.get<Long>(NODE_HANDLE) ?: -1L)

    /**
     * private mutable UI state
     */
    private val _uiState = MutableStateFlow(OfflineFileInfoUiState())

    /**
     * public immutable UI State for view
     */
    val uiState = _uiState.asStateFlow()

    init {
        loadOfflineNodeInformation()
    }

    private fun loadOfflineNodeInformation() {
        viewModelScope.launch {
            runCatching {
                getOfflineFileInformationByIdUseCase(nodeId, true)?.let { offlineFileInformation ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            offlineFileInformation = offlineFileInformation,
                        )
                    }
                } ?: run {
                    handleError()
                }
            }.onFailure {
                handleError()
                Timber.e(it)
            }
        }
    }

    private fun handleError() {
        _uiState.update {
            it.copy(
                errorEvent = triggered(true)
            )
        }
    }

    /**
     * Remove the node from database and cache storage
     */
    fun removeFromOffline() {
        viewModelScope.launch {
            runCatching {
                removeOfflineNodeUseCase(nodeId)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun onErrorEventConsumed() =
        _uiState.update { state -> state.copy(errorEvent = consumed()) }

    companion object {
        const val NODE_HANDLE = "handle"
    }
}

