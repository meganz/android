package mega.privacy.mobile.home.presentation.continuewhereleftoff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffItemsUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ContinueWhereLeftOffListViewModel @Inject constructor(
    monitorContinueWhereLeftOffItemsUseCase: MonitorContinueWhereLeftOffItemsUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContinueWhereLeftOffListUiState())
    val uiState: StateFlow<ContinueWhereLeftOffListUiState> = _uiState.asStateFlow()

    init {
        monitorContinueWhereLeftOffItemsUseCase(limit = MAX_LIST_ITEMS)
            .onEach { items ->
                _uiState.update {
                    it.copy(items = items, isLoading = false)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onItemClicked(nodeHandle: Long) {
        viewModelScope.launch {
            runCatching {
                getNodeByIdUseCase(NodeId(nodeHandle)) as? TypedFileNode
            }.onSuccess { node ->
                node?.let { _uiState.update { it.copy(openNodeEvent = triggered(node)) } }
            }.onFailure {
                Timber.d(it)
            }
        }
    }

    fun onOpenNodeEventConsumed() {
        _uiState.update { it.copy(openNodeEvent = consumed()) }
    }

    companion object {
        private const val MAX_LIST_ITEMS = 50
    }
}
