package mega.privacy.android.app.presentation.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.ContactFileListActivity]
 */
@HiltViewModel
class ContactFileListViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ContactFileListUiState())

    /**
     * Ui State
     */
    val state = _state.asStateFlow()

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Is online
     *
     * @return
     */
    fun isOnline(): Boolean = monitorConnectivityUseCase().value

    /**
     * Move nodes to rubbish
     *
     * @param nodeHandles
     */
    fun moveNodesToRubbish(nodeHandles: List<Long>) {
        viewModelScope.launch {
            runCatching {
                moveNodesToRubbishUseCase(nodeHandles)
            }.onSuccess {
                _state.update { state -> state.copy(moveRequestResult = it) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Mark handle move request result
     *
     */
    fun markHandleMoveRequestResult() {
        _state.update { it.copy(moveRequestResult = null) }
    }
}