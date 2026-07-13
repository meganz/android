package mega.privacy.android.domain.usecase.node.hiddennode

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SensitiveNodeShareWarning
import javax.inject.Inject

/**
 * Resolves the hidden/sensitive-node warning to show before sharing the given folder(s) with
 * contacts, sourcing the account eligibility from [MonitorHiddenNodesEnabledUseCase] so callers do
 * not have to resolve and thread it themselves.
 *
 * Callers remain responsible for restricting this to the Compose contact-picker path: the legacy
 * picker performs its own hidden-node check, so warning here as well would double-warn.
 */
class GetShareFolderSensitiveWarningUseCase @Inject constructor(
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val getShareFolderSensitiveWarningTypeUseCase: GetShareFolderSensitiveWarningTypeUseCase,
) {
    /**
     * @param nodeIds the folder(s) about to be shared.
     * @return the warning to show, or [SensitiveNodeShareWarning.None] when none is needed.
     */
    suspend operator fun invoke(nodeIds: List<NodeId>): SensitiveNodeShareWarning =
        getShareFolderSensitiveWarningTypeUseCase(
            nodeIds = nodeIds,
            hiddenNodesEnabled = monitorHiddenNodesEnabledUseCase().first(),
        )
}
