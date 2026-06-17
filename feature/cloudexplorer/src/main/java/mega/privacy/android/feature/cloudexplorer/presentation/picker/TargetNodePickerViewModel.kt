package mega.privacy.android.feature.cloudexplorer.presentation.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import mega.privacy.android.shared.nodes.extension.orInvalid
import timber.log.Timber

/**
 * Base ViewModel for the copy and move flows. Resolves the explorer root and the back-stack path
 * to the last picked target; subclasses only provide the flow-specific "latest target" source.
 */
internal abstract class TargetNodePickerViewModel(
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val getNodeNavigationStackUseCase: GetNodeNavigationStackUseCase,
) : ViewModel() {

    /** The handle of the last picked target for this flow, or null/-1 when there is none. */
    protected abstract suspend fun getLatestTargetPath(): Long?

    val uiState: StateFlow<TargetNodePickerUiState> by lazy(LazyThreadSafetyMode.NONE) {
        flow {
            val rootNodeId = getRootNodeIdUseCase.orInvalid()
            val resumeTarget = resolveResumeTarget(rootNodeId)
            emit(
                TargetNodePickerUiState.Data(
                    rootNodeId = rootNodeId,
                    targetPath = resumeTarget.path,
                    nodeSourceType = resumeTarget.sourceType,
                )
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, TargetNodePickerUiState.Loading)
    }

    /**
     * Resolves the back-stack path (top-down) to the last target so the explorer can resume there,
     * along with the source type it lives under. A target under the cloud-drive root resumes on
     * [NodeSourceType.CLOUD_DRIVE] (excluding the root, which is the explorer's base); otherwise it
     * is treated as an incoming-shares target ([NodeSourceType.INCOMING_SHARES]), whose full
     * ancestor chain (including the share root) must be pushed. Returns an empty path when there is
     * no valid last target.
     */
    private suspend fun resolveResumeTarget(rootNodeId: NodeId): ResumeTarget {
        val targetHandle = runCatching { getLatestTargetPath() }
            .onFailure { Timber.e(it) }
            .getOrNull()
            ?.takeIf { it != -1L } ?: return ResumeTarget()
        val targetId = NodeId(targetHandle)

        if (targetId == rootNodeId) return ResumeTarget()

        val navigationStack = runCatching { getNodeNavigationStackUseCase(targetId) }
            .onFailure { Timber.e(it) }
            .getOrNull()
            ?.takeIf { it.stack.isNotEmpty() } ?: return ResumeTarget()

        return ResumeTarget(
            path = navigationStack.stack,
            sourceType = if (navigationStack.isUnderRootNode) {
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
