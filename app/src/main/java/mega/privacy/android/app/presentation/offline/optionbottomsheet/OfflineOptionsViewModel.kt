package mega.privacy.android.app.presentation.offline.optionbottomsheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.offline.optionbottomsheet.model.OfflineOptionsUiState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationByIdUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model class for [OfflineOptionsBottomSheetDialogFragment]
 */
@HiltViewModel
internal class OfflineOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getOfflineFileInformationByIdUseCase: GetOfflineFileInformationByIdUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase
) : ViewModel() {

    /**
     * Node Id from [SavedStateHandle]
     */
    private val nodeId = NodeId(savedStateHandle.get<Long>(NODE_HANDLE) ?: -1L)

    /**
     * private mutable UI state
     */
    private val _uiState = MutableStateFlow(OfflineOptionsUiState(nodeId = nodeId))

    /**
     * public immutable UI State for view
     */
    val uiState = _uiState.asStateFlow()

    init {
        monitorConnectivity()
        loadOfflineNodeInformation()
    }

    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .catch { Timber.e(it) }
                .collectLatest { isOnline -> _uiState.update { it.copy(isOnline = isOnline) } }
        }
    }

    private fun loadOfflineNodeInformation() {
        viewModelScope.launch {
            runCatching {
                getOfflineFileInformationByIdUseCase(nodeId)?.let { offlineFileInformation ->
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

    fun onErrorEventConsumed() =
        _uiState.update { state -> state.copy(errorEvent = consumed()) }

    companion object {
        const val NODE_HANDLE = "handle"
    }
}

