package mega.privacy.android.app.presentation.node

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.node.model.NodeBottomSheetState
import mega.privacy.android.app.presentation.node.model.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.NodeBottomSheetMenuItem
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetRouteNodeIdArg
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import javax.inject.Inject

/**
 * Node options bottom sheet view model
 *
 * @property nodeBottomSheetActionMapper
 * @property bottomSheetOptions
 * @property getNodeAccessPermission
 * @property isNodeInRubbish
 * @property isNodeInBackupsUseCase
 */
@HiltViewModel
class NodeOptionsBottomSheetViewModel @Inject constructor(
    private val nodeBottomSheetActionMapper: NodeBottomSheetActionMapper,
    private val bottomSheetOptions: Set<@JvmSuppressWildcards NodeBottomSheetMenuItem<MenuActionWithIcon>>,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val isNodeInRubbish: IsNodeInRubbish,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    stateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(NodeBottomSheetState())

    /**
     * public UI State
     */
    val state: StateFlow<NodeBottomSheetState> = _state

    init {
        viewModelScope.launch {
            monitorConnectivityUseCase().collect { isConnected ->
                _state.update {
                    it.copy(isOnline = isConnected)
                }
            }
        }
        viewModelScope.launch {
            val nodeId = stateHandle.get<Long>(nodeBottomSheetRouteNodeIdArg) ?: return@launch
            getBottomSheetOptions(nodeId)
        }
    }

    /**
     * Get bottom sheet options
     *
     * @param nodeId [TypedNode]
     * @return state
     */
    private fun getBottomSheetOptions(nodeId: Long) = viewModelScope.launch {
        val node = async { runCatching { getNodeByIdUseCase(NodeId(nodeId)) }.getOrNull() }
        val isNodeInRubbish =
            async { runCatching { isNodeInRubbish(nodeId) }.getOrDefault(false) }
        val accessPermission =
            async { runCatching { getNodeAccessPermission(NodeId(nodeId)) }.getOrNull() }
        val isInBackUps =
            async { runCatching { isNodeInBackupsUseCase(nodeId) }.getOrDefault(false) }
        val typedNode = node.await()
        typedNode?.let {
            val bottomSheetItems = nodeBottomSheetActionMapper(
                toolbarOptions = bottomSheetOptions,
                selectedNode = typedNode,
                isNodeInRubbish = isNodeInRubbish.await(),
                accessPermission = accessPermission.await(),
                isInBackUps = isInBackUps.await(),
                isConnected = state.value.isOnline,
            )
            _state.update {
                it.copy(
                    name = typedNode.name,
                    actions = bottomSheetItems,
                    node = typedNode,
                    error = if (bottomSheetItems.isEmpty()) triggered(Exception("No actions available")) else consumed()
                )
            }
        } ?: run {
            _state.update {
                it.copy(error = triggered(Exception("Node is null")))
            }
        }
    }

    /**
     * When error consumed
     */
    fun onConsumeErrorState() {
        _state.update { it.copy(error = consumed()) }
    }
}