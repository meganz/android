package mega.privacy.android.core.nodecomponents.components.selectionmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeSelectionModeActionMapper
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction.Companion.DEFAULT_MAX_VISIBLE_ITEMS
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.qualifier.features.CloudDrive
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NodeSelectionModeViewModel @Inject constructor(
    @CloudDrive private val cloudDriveOptions: Lazy<Set<@JvmSuppressWildcards NodeSelectionMenuItem<*>>>,
    private val nodeSelectionModeActionMapper: NodeSelectionModeActionMapper,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val checkNodeCanBeMovedToTargetNode: CheckNodeCanBeMovedToTargetNode,
    private val nodeSelectionActionUiMapper: NodeSelectionActionUiMapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NodeSelectionModeUiState())

    /**
     * public UI State
     */
    val uiState = _uiState.asStateFlow()

    private var rubbishBinNode: UnTypedNode? = null

    init {
        getRubbishBinNode()
    }

    private fun getRubbishBinNode() {
        viewModelScope.launch {
            runCatching {
                getRubbishNodeUseCase()
            }.onSuccess { rubbishBin ->
                rubbishBinNode = rubbishBin
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun updateState(
        selectedNodes: Set<TypedNode>,
        nodeSourceType: NodeSourceType,
    ) {
        viewModelScope.launch {
            Timber.d("Update state called with ${selectedNodes.size} nodes")
            val options = getOptions(nodeSourceType)
            if (options.isEmpty()) {
                Timber.w("No options available for node source type: $nodeSourceType")
                _uiState.update { it.copy(visibleActions = emptyList()) }
                return@launch
            }

            val (canBeMovedToTarget, anyNodeInBackups, hasAccessPermission) = when (nodeSourceType) {
                NodeSourceType.RUBBISH_BIN -> Triple(false, false, true)
                NodeSourceType.INCOMING_SHARES -> Triple(
                    canSelectedNodesBeMovedToRubbishBin(selectedNodes),
                    anyNodesInBackups(selectedNodes),
                    hasFullAccessPermission(selectedNodes)
                )

                else -> Triple(
                    canSelectedNodesBeMovedToRubbishBin(selectedNodes),
                    anyNodesInBackups(selectedNodes),
                    true
                )
            }

            val allActions = nodeSelectionModeActionMapper(
                options = options,
                hasNodeAccessPermission = hasAccessPermission,
                selectedNodes = selectedNodes.toList(),
                allNodeCanBeMovedToTarget = canBeMovedToTarget,
                noNodeInBackups = !anyNodeInBackups
            ).mapNotNull { nodeSelectionActionUiMapper(it) }

            val visibleActions = if (allActions.size > DEFAULT_MAX_VISIBLE_ITEMS) {
                allActions.take(DEFAULT_MAX_VISIBLE_ITEMS) + NodeSelectionAction.More
            } else {
                allActions
            }

            _uiState.update { it.copy(visibleActions = visibleActions) }
        }
    }

    private suspend fun canSelectedNodesBeMovedToRubbishBin(
        selectedNodes: Set<TypedNode>,
    ) = rubbishBinNode?.let { rubbishBinNode ->
        runCatching {
            selectedNodes.any { node ->
                checkNodeCanBeMovedToTargetNode(nodeId = node.id, targetNodeId = rubbishBinNode.id)
            }
        }.getOrElse {
            Timber.e(it)
            true
        }
    } ?: true

    private suspend fun hasFullAccessPermission(selectedNodes: Set<TypedNode>): Boolean {
        return runCatching {
            selectedNodes.all { getNodeAccessPermission(it.id) == AccessPermission.FULL }
        }.onFailure {
            Timber.e(it)
        }.getOrDefault(false)
    }

    private suspend fun anyNodesInBackups(selectedNodes: Set<TypedNode>) =
        selectedNodes.any {
            runCatching {
                isNodeInBackupsUseCase(handle = it.id.longValue)
            }.getOrElse { e ->
                Timber.e(e)
                false
            }
        }

    private fun getOptions(nodeSourceType: NodeSourceType) =
        when (nodeSourceType) {
            NodeSourceType.CLOUD_DRIVE -> cloudDriveOptions.get()
            // Update when other source types are supported
            else -> emptySet()
        }
}