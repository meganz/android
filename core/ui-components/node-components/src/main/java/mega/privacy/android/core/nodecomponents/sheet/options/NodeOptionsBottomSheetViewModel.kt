package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.SnackbarAttributes
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetState
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.menu.registry.NodeMenuProviderRegistry
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import javax.inject.Inject

/**
 * Node options bottom sheet view model
 *
 * @property nodeBottomSheetActionMapper
 * @property getNodeAccessPermission
 * @property isNodeInRubbishBinUseCase
 * @property isNodeInBackupsUseCase
 * @property monitorConnectivityUseCase
 * @property getNodeByIdUseCase
 */
@HiltViewModel
class NodeOptionsBottomSheetViewModel @Inject constructor(
    private val nodeBottomSheetActionMapper: NodeBottomSheetActionMapper,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val isNodeDeletedFromBackupsUseCase: IsNodeDeletedFromBackupsUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val snackbarEventQueue: SnackbarEventQueue,
    private val nodeMenuProviderRegistry: NodeMenuProviderRegistry,
) : ViewModel() {

    internal val uiState: StateFlow<NodeBottomSheetState>
        field = MutableStateFlow(NodeBottomSheetState())

    init {
        viewModelScope.launch {
            monitorConnectivityUseCase().collect { isConnected ->
                uiState.update {
                    it.copy(isOnline = isConnected)
                }
            }
        }
    }

    suspend fun showSnackbar(attributes: SnackbarAttributes) =
        snackbarEventQueue.queueMessage(attributes)

    /**
     * Get bottom sheet options
     *
     * @param nodeId [mega.privacy.android.domain.entity.node.TypedNode]
     * @return state
     */
    fun getBottomSheetOptions(nodeId: Long, nodeSourceType: NodeSourceType) {
        viewModelScope.launch {
            val bottomSheetOptions = nodeMenuProviderRegistry.getBottomSheetOptions(nodeSourceType)

            uiState.update {
                it.copy(
                    actions = persistentListOf(),
                    node = null
                )
            }
            val node = async { runCatching { getNodeByIdUseCase(NodeId(nodeId)) }.getOrNull() }
            val isNodeInRubbish =
                runCatching { isNodeInRubbishBinUseCase(NodeId(nodeId)) }.getOrDefault(false)
            val accessPermission =
                async { runCatching { getNodeAccessPermission(NodeId(nodeId)) }.getOrNull() }
            val isInBackUps =
                async {
                    runCatching {
                        if (isNodeInRubbish) {
                            isNodeDeletedFromBackupsUseCase(NodeId(nodeId))
                        } else {
                            isNodeInBackupsUseCase(nodeId)
                        }
                    }.getOrDefault(false)
                }
            val typedNode = node.await()
            val permission = accessPermission.await()
            typedNode?.let {
                val bottomSheetItems = nodeBottomSheetActionMapper(
                    toolbarOptions = bottomSheetOptions,
                    selectedNode = typedNode,
                    isNodeInRubbish = isNodeInRubbish,
                    accessPermission = permission,
                    isInBackUps = isInBackUps.await(),
                    isConnected = uiState.value.isOnline
                ).sortedBy { it.orderInGroup }
                    .toImmutableList()
                val nodeUiItem = nodeUiItemMapper(listOf(typedNode)).firstOrNull()

                uiState.update {
                    it.copy(
                        actions = bottomSheetItems,
                        node = nodeUiItem,
                        error = if (bottomSheetItems.isEmpty()) triggered(Exception("No actions available")) else consumed(),
                    )
                }
            } ?: run {
                uiState.update {
                    it.copy(error = triggered(Exception("Node is null")))
                }
            }
        }
    }

    /**
     * When error consumed
     */
    fun onConsumeErrorState() {
        uiState.update { it.copy(error = consumed()) }
    }

}