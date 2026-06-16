package mega.privacy.android.feature.cloudexplorer.presentation.move

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.account.GetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class MoveViewModel @Inject constructor(
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val getMoveLatestTargetPathUseCase: GetMoveLatestTargetPathUseCase,
    private val getNodeNavigationStackUseCase: GetNodeNavigationStackUseCase,
) : ViewModel() {

    val uiState: StateFlow<MoveUiState> by lazy(LazyThreadSafetyMode.NONE) {
        flow {
            val rootNodeId = runCatching { getRootNodeIdUseCase() }
                .onFailure { Timber.e(it) }
                .getOrNull() ?: NodeId(-1)
            val resumeTarget = resolveResumeTarget(rootNodeId)
            emit(
                MoveUiState.Data(
                    rootNodeId = rootNodeId,
                    targetPath = resumeTarget.path,
                    nodeSourceType = resumeTarget.sourceType,
                )
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, MoveUiState.Loading)
    }

    /**
     * Resolves the back-stack path (top-down) to the last move target so the explorer can resume
     * there, along with the source type it lives under. A target under the cloud-drive root resumes
     * on [NodeSourceType.CLOUD_DRIVE] (excluding the root, which is the explorer's base); otherwise
     * it is treated as an incoming-shares target ([NodeSourceType.INCOMING_SHARES]), whose full
     * ancestor chain (including the share root) must be pushed. Returns an empty path when there is
     * no valid last target.
     */
    private suspend fun resolveResumeTarget(rootNodeId: NodeId): ResumeTarget {
        val targetHandle = runCatching { getMoveLatestTargetPathUseCase() }
            .onFailure { Timber.e(it) }
            .getOrNull()
            ?.takeIf { it != -1L } ?: return ResumeTarget()
        val targetId = NodeId(targetHandle)
        if (targetId == rootNodeId) return ResumeTarget()
        val navigationPath = runCatching { getNodeNavigationStackUseCase(targetId) }
            .onFailure { Timber.e(it) }
            .getOrNull()
            ?.takeIf { it.stack.isNotEmpty() } ?: return ResumeTarget()
        return ResumeTarget(
            path = navigationPath.stack,
            sourceType = if (navigationPath.isUnderRootNode) {
                NodeSourceType.CLOUD_DRIVE
            } else {
                NodeSourceType.INCOMING_SHARES
            },
        )
    }

    private data class ResumeTarget(
        val path: List<NodeId> = emptyList(),
        val sourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    )
}
