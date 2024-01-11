package mega.privacy.android.app.presentation.node.dialogs.removelink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model to remove link
 */
@HiltViewModel
class RemoveNodeLinkViewModel @Inject constructor(
    private val disableExportNodesUseCase: DisableExportNodesUseCase,
    private val removePublicLinkResultMapper: RemovePublicLinkResultMapper
) : ViewModel() {

    private val _state = MutableStateFlow(RemoveNodeLinkState())

    /**
     * state for MoveToRubbishOrDeleteNodeDialog
     */
    val state: StateFlow<RemoveNodeLinkState> = _state.asStateFlow()

    /**
     * Disable export nodes
     * @param nodeIds
     */
    fun disableExport(nodeIds: List<Long>) {
        viewModelScope.launch {
            runCatching {
                disableExportNodesUseCase(nodeIds.map { NodeId(it) })
            }.onFailure {
                Timber.e(it)
            }.onSuccess { result ->
                val message = removePublicLinkResultMapper(result)
                _state.update { state ->
                    state.copy(
                        removeLinkEvent = triggered(message),
                    )
                }
            }
        }
    }

    /**
     * Consumed Delete event
     */
    fun consumeDeleteEvent() {
        _state.update {
            it.copy(removeLinkEvent = consumed())
        }
    }
}