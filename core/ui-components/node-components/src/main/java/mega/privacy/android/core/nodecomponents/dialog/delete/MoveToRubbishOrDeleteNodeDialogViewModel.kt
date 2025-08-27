package mega.privacy.android.core.nodecomponents.dialog.delete

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.message.NodeMoveRequestMessageMapper
import mega.privacy.android.core.sharedcomponents.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.node.DeleteNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for Move to rubbish or delete view Model
 * @property applicationScope [kotlinx.coroutines.CoroutineScope]
 * @property deleteNodesUseCase [mega.privacy.android.domain.usecase.node.DeleteNodesUseCase]
 * @property moveNodesToRubbishUseCase [mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase]
 * @property nodeMoveRequestMessageMapper [mega.privacy.android.core.nodecomponents.mapper.message.NodeMoveRequestMessageMapper]
 * @property snackBarHandler [mega.privacy.android.core.sharedcomponents.snackbar.SnackBarHandler]
 */
@HiltViewModel
class MoveToRubbishOrDeleteNodeDialogViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val deleteNodesUseCase: DeleteNodesUseCase,
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase,
    private val nodeMoveRequestMessageMapper: NodeMoveRequestMessageMapper,
    private val snackBarHandler: SnackBarHandler
) : ViewModel() {

    /**
     * Delete nodes permanently
     *
     * @param nodeHandles List of node handles to delete
     */
    fun deleteNodes(nodeHandles: List<Long>) {
        applicationScope.launch {
            val nodeIds = nodeHandles.map { NodeId(it) }
            runCatching {
                deleteNodesUseCase(nodeIds)
            }.onSuccess { result ->
                val message = nodeMoveRequestMessageMapper(result)
                snackBarHandler.postSnackbarMessage(message)
            }.onFailure { exception ->
                Timber.e(exception, "Failed to delete nodes: $nodeHandles")
            }
        }
    }

    /**
     * Move nodes to rubbish bin
     *
     * @param nodeHandles List of node handles to move to rubbish
     */
    fun moveNodesToRubbishBin(nodeHandles: List<Long>) {
        applicationScope.launch {
            runCatching {
                moveNodesToRubbishUseCase(nodeHandles)
            }.onSuccess { result ->
                val message = nodeMoveRequestMessageMapper(result)
                snackBarHandler.postSnackbarMessage(message)
            }.onFailure { exception ->
                Timber.e(exception, "Failed to move nodes to rubbish: $nodeHandles")
            }
        }
    }
}