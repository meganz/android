package mega.privacy.android.app.presentation.node

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.node.model.NodeBottomSheetState
import mega.privacy.android.app.presentation.node.model.mapper.NodeBottomSheetActionMapper
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.NodeBottomSheetMenuItem
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.app.presentation.versions.mapper.VersionHistoryRemoveMessageMapper
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import timber.log.Timber
import javax.inject.Inject

/**
 * Node options bottom sheet view model
 *
 * @property nodeBottomSheetActionMapper
 * @property bottomSheetOptions
 * @property getNodeAccessPermission
 * @property isNodeInRubbish
 * @property isNodeInBackupsUseCase
 * @property monitorConnectivityUseCase
 * @property getNodeByIdUseCase
 * @property checkNodesNameCollisionUseCase
 * @property moveNodesUseCase
 * @property copyNodesUseCase
 * @property setMoveLatestTargetPathUseCase
 * @property setCopyLatestTargetPathUseCase
 * @property deleteNodeVersionsUseCase
 * @property applicationScope
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
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val moveNodesUseCase: MoveNodesUseCase,
    private val copyNodesUseCase: CopyNodesUseCase,
    private val setMoveLatestTargetPathUseCase: SetMoveLatestTargetPathUseCase,
    private val setCopyLatestTargetPathUseCase: SetCopyLatestTargetPathUseCase,
    private val deleteNodeVersionsUseCase: DeleteNodeVersionsUseCase,
    private val snackBarHandler: SnackBarHandler,
    private val moveRequestMessageMapper: MoveRequestMessageMapper,
    private val versionHistoryRemoveMessageMapper: VersionHistoryRemoveMessageMapper,
    @ApplicationScope private val applicationScope: CoroutineScope,
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
    }

    /**
     * Get bottom sheet options
     *
     * @param nodeId [TypedNode]
     * @return state
     */
    fun getBottomSheetOptions(nodeId: Long) = viewModelScope.launch {
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


    /**
     * Check move nodes name collision
     *
     * @param nodes
     * @param targetNode
     */
    fun checkNodesNameCollision(
        nodes: List<Long>,
        targetNode: Long,
        type: NodeNameCollisionType,
    ) {
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionUseCase(
                    nodes.associateWith { targetNode },
                    type
                )
            }.onSuccess { result ->
                _state.update { it.copy(nodeNameCollisionResult = triggered(result)) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Move nodes
     *
     * @param nodes
     */
    fun moveNodes(nodes: Map<Long, Long>) {
        applicationScope.launch {
            runCatching {
                moveNodesUseCase(nodes)
            }.onSuccess {
                setMoveTargetPath(nodes.values.first())
                snackBarHandler.postSnackbarMessage(moveRequestMessageMapper(it))
            }.onFailure {
                manageCopyMoveError(it)
                Timber.e(it)
            }
        }
    }

    /**
     * Copy nodes
     *
     * @param nodes
     */
    fun copyNodes(nodes: Map<Long, Long>) {
        applicationScope.launch {
            runCatching {
                copyNodesUseCase(nodes)
            }.onSuccess {
                setCopyTargetPath(nodes.values.first())
                snackBarHandler.postSnackbarMessage(moveRequestMessageMapper(it))
            }.onFailure {
                manageCopyMoveError(it)
                Timber.e(it)
            }
        }
    }

    private fun manageCopyMoveError(error: Throwable?) = when (error) {
        is ForeignNodeException -> _state.update { it.copy(showForeignNodeDialog = triggered) }
        is QuotaExceededMegaException -> _state.update {
            it.copy(showQuotaDialog = triggered(true))
        }

        is NotEnoughQuotaMegaException -> _state.update {
            it.copy(showQuotaDialog = triggered(false))
        }

        else -> Timber.e("Error copying/moving nodes $error")
    }

    /**
     * Set last used path of move as target path for next move
     */
    private fun setMoveTargetPath(path: Long) {
        viewModelScope.launch {
            runCatching { setMoveLatestTargetPathUseCase(path) }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Set last used path of copy as target path for next copy
     */
    private fun setCopyTargetPath(path: Long) {
        viewModelScope.launch {
            runCatching { setCopyLatestTargetPathUseCase(path) }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Mark handle node name collision result
     */
    fun markHandleNodeNameCollisionResult() {
        _state.update { it.copy(nodeNameCollisionResult = consumed()) }
    }

    /**
     * Delete version history of selected node
     */
    fun deleteVersionHistory(it: Long) = applicationScope.launch {
        val result = runCatching {
            deleteNodeVersionsUseCase(NodeId(it))
        }
        versionHistoryRemoveMessageMapper(result.exceptionOrNull()).let {
            snackBarHandler.postSnackbarMessage(it)
        }
    }

    /**
     * Mark foreign node dialog shown
     */
    fun markForeignNodeDialogShown() {
        _state.update { it.copy(showForeignNodeDialog = consumed) }
    }

    /**
     * Mark quota dialog shown
     */
    fun markQuotaDialogShown() {
        _state.update { it.copy(showQuotaDialog = consumed()) }
    }

}