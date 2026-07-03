package mega.privacy.android.domain.usecase.node.hiddennode

import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SensitiveNodeShareWarning
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasSensitiveDescendantUseCase
import javax.inject.Inject

/**
 * Resolves whether sharing the given folder(s) with contacts should be preceded by a
 * hidden/sensitive-node warning, and which wording to use.
 *
 * Sharing a hidden folder makes its contents visible to the recipient, so the user is warned when
 * any folder being shared is itself hidden, inherits sensitivity, or contains a sensitive
 * descendant. Already-shared folders and non-folders are ignored (they do not newly expose hidden
 * content).
 *
 * The account eligibility for hidden nodes is passed in as [hiddenNodesEnabled] rather than
 * resolved here, so the (single) source of truth stays [MonitorHiddenNodesEnabledUseCase] and the
 * potentially expensive per-node scan is skipped entirely when hidden nodes are not enabled.
 */
class GetShareFolderSensitiveWarningTypeUseCase @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val hasSensitiveDescendantUseCase: HasSensitiveDescendantUseCase,
) {
    /**
     * @param nodeIds the folder(s) about to be shared.
     * @param hiddenNodesEnabled whether hidden nodes are enabled for the current account, from
     * [MonitorHiddenNodesEnabledUseCase].
     * @return the warning to show, or [SensitiveNodeShareWarning.None] when none is needed.
     */
    suspend operator fun invoke(
        nodeIds: List<NodeId>,
        hiddenNodesEnabled: Boolean,
    ): SensitiveNodeShareWarning {
        if (!hiddenNodesEnabled) return SensitiveNodeShareWarning.None

        val anySensitive = nodeIds.any { id ->
            val node = getNodeByIdUseCase(id)
            node is FolderNode && !node.isShared &&
                    (node.isMarkedSensitive ||
                            node.isSensitiveInherited ||
                            hasSensitiveDescendantUseCase(node.id))
        }

        return when {
            !anySensitive -> SensitiveNodeShareWarning.None
            nodeIds.size == 1 -> SensitiveNodeShareWarning.Folder
            else -> SensitiveNodeShareWarning.Folders
        }
    }
}
