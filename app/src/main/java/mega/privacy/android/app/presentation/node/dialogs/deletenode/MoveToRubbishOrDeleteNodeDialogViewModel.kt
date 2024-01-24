package mega.privacy.android.app.presentation.node.dialogs.deletenode

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.node.DeleteNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for Move to rubbish or delete view Model
 * @property applicationScope [CoroutineScope]
 * @property deleteNodesUseCase [DeleteNodesUseCase]
 * @property moveNodesToRubbishUseCase [MoveNodesToRubbishUseCase]
 * @property moveRequestMessageMapper [MoveRequestMessageMapper]
 * @property snackBarHandler [SnackBarHandler]
 */
@HiltViewModel
class MoveToRubbishOrDeleteNodeDialogViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val deleteNodesUseCase: DeleteNodesUseCase,
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase,
    private val moveRequestMessageMapper: MoveRequestMessageMapper,
    private val snackBarHandler: SnackBarHandler
) : ViewModel() {

    /**
     * Delete nodes
     *
     * @param handles
     */
    fun deleteNodes(handles: List<Long>) {
        applicationScope.launch {
            val nodeHandles = handles.map {
                NodeId(it)
            }
            val result = runCatching {
                deleteNodesUseCase(nodeHandles)
            }.onFailure {
                Timber.e(it)
            }
            result.getOrNull()?.let { res ->
                val message = moveRequestMessageMapper(res)
                snackBarHandler.postSnackbarMessage(message)
            }
        }
    }

    /**
     * Move nodes to rubbish
     *
     * @param handles
     */
    fun moveNodesToRubbishBin(handles: List<Long>) {
        applicationScope.launch {
            val handlesList = handles.map {
                it
            }
            val result = runCatching {
                moveNodesToRubbishUseCase(handlesList)
            }.onFailure {
                Timber.e(it)
            }
            result.getOrNull()?.let { res ->
                val message = moveRequestMessageMapper(res)
                snackBarHandler.postSnackbarMessage(message)
            }
        }
    }
}