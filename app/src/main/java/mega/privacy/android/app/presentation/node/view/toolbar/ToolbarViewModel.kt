package mega.privacy.android.app.presentation.node.view.toolbar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.CloudDrive
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.IncomingShares
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.Links
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.OutgoingShares
import mega.privacy.android.app.di.ui.toolbaritem.qualifier.RubbishBin
import mega.privacy.android.app.presentation.node.model.mapper.NodeToolbarActionMapper
import mega.privacy.android.app.presentation.node.model.toolbarmenuitems.NodeToolbarMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.CheckNodeCanBeMovedToTargetNode
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import timber.log.Timber
import javax.inject.Inject

/**
 * View model for Toolbar
 * @property cloudDriveToolbarOptions
 * @property incomingSharesToolbarOptions
 * @property outgoingSharesToolbarOptions
 * @property linksToolbarOptions
 * @property rubbishBinToolbarOptions
 * @property nodeToolbarActionMapper [NodeToolbarActionMapper]
 * @property getRubbishNodeUseCase [GetRubbishNodeUseCase]
 * @property isNodeInBackupsUseCase [IsNodeInBackupsUseCase]
 * @property getNodeAccessPermission [GetNodeAccessPermission]
 * @property checkNodeCanBeMovedToTargetNode [CheckNodeCanBeMovedToTargetNode]
 */
@HiltViewModel
class ToolbarViewModel @Inject constructor(
    @CloudDrive private val cloudDriveToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>>,
    @IncomingShares private val incomingSharesToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>>,
    @OutgoingShares private val outgoingSharesToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>>,
    @Links private val linksToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>>,
    @RubbishBin private val rubbishBinToolbarOptions: Set<@JvmSuppressWildcards NodeToolbarMenuItem<*>>,
    private val nodeToolbarActionMapper: NodeToolbarActionMapper,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val checkNodeCanBeMovedToTargetNode: CheckNodeCanBeMovedToTargetNode,
) : ViewModel() {

    private val _state = MutableStateFlow(ToolbarState())

    /**
     * public UI State
     */
    val state = _state.asStateFlow()

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

    /**
     * Update toolbar state based on selected node
     * @param selectedNodes Set of [TypedNode]
     * @param resultCount count of result
     * @param nodeSourceType [NodeSourceType]
     */
    fun updateToolbarState(
        selectedNodes: Set<TypedNode>,
        resultCount: Int,
        nodeSourceType: NodeSourceType,
    ) {
        viewModelScope.launch {
            val toolbarOptions = getToolbarOptions(NodeSourceType.CLOUD_DRIVE)
            val canBeMovedToTarget = if (nodeSourceType != NodeSourceType.RUBBISH_BIN) {
                canNodeBeMovedToRubbishBin(selectedNodes)
            } else false
            val anyNodeInBackups = if (nodeSourceType != NodeSourceType.RUBBISH_BIN) {
                selectedNodes.any {
                    runCatching {
                        isNodeInBackupsUseCase(handle = it.id.longValue)
                    }.getOrElse {
                        Timber.e(it)
                        false
                    }
                }
            } else false
            val hasAccessPermission = if (nodeSourceType == NodeSourceType.INCOMING_SHARES) {
                checkIfNodeHasFullAccessPermission(selectedNodes)
            } else true
            val list = nodeToolbarActionMapper(
                toolbarOptions = toolbarOptions,
                hasNodeAccessPermission = hasAccessPermission,
                selectedNodes = selectedNodes,
                resultCount = resultCount,
                allNodeCanBeMovedToTarget = canBeMovedToTarget,
                noNodeInBackups = anyNodeInBackups.not()
            )
            _state.update {
                it.copy(menuActions = list)
            }
        }
    }

    private fun getToolbarOptions(nodeSourceType: NodeSourceType) = when (nodeSourceType) {
        NodeSourceType.INCOMING_SHARES -> incomingSharesToolbarOptions
        NodeSourceType.OUTGOING_SHARES -> outgoingSharesToolbarOptions
        NodeSourceType.LINKS -> linksToolbarOptions
        NodeSourceType.RUBBISH_BIN -> rubbishBinToolbarOptions
        else -> cloudDriveToolbarOptions
    }

    private suspend fun canNodeBeMovedToRubbishBin(
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

    private suspend fun checkIfNodeHasFullAccessPermission(selectedNodes: Set<TypedNode>): Boolean {
        return runCatching {
            selectedNodes.all { getNodeAccessPermission(it.id) == AccessPermission.FULL }
        }.onFailure {
            Timber.e(it)
        }.getOrDefault(false)
    }
}