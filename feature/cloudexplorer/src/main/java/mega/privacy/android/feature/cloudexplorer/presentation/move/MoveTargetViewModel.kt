package mega.privacy.android.feature.cloudexplorer.presentation.move

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import timber.log.Timber

/**
 * Whether a folder opened in the Move picker lies inside the nodes being moved, where a move
 * would fail because a folder cannot be moved into itself or its own subfolders.
 */
@HiltViewModel(assistedFactory = MoveTargetViewModel.Factory::class)
internal class MoveTargetViewModel @AssistedInject constructor(
    private val getNodeNavigationStackUseCase: GetNodeNavigationStackUseCase,
    @Assisted private val args: Args,
) : ViewModel() {

    /**
     * True while the check is pending or when [Args.targetId] is inside [Args.movedNodeIds], so
     * the folder cannot be picked before the result is known.
     */
    val isInsideMovedNodes: StateFlow<Boolean> by lazy(LazyThreadSafetyMode.NONE) {
        flow {
            val path = getNodeNavigationStackUseCase(args.targetId).stack
            emit(path.any { it in args.movedNodeIds })
        }.catch {
            Timber.e(it, "Failed to check the move target against the moved nodes")
            emit(false)
        }.asUiStateFlow(viewModelScope, true)
    }

    /**
     * @property targetId the folder currently open in the Move picker
     * @property movedNodeIds the nodes being moved
     */
    data class Args(
        val targetId: NodeId,
        val movedNodeIds: List<NodeId>,
    )

    @AssistedFactory
    interface Factory {
        fun create(args: Args): MoveTargetViewModel
    }
}
