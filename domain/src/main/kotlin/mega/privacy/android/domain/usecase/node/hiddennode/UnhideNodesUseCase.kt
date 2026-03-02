package mega.privacy.android.domain.usecase.node.hiddennode

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.HideNodesResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.node.HideNodesException
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class UnhideNodesUseCase @Inject constructor(
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isBusinessStatusUseCase: GetBusinessStatusUseCase,
) {
    suspend operator fun invoke(nodes: Set<NodeId>): HideNodesResult {
        if (isPaid() && !isBusinessAccountExpired()) {
            return unhideNodes(nodes)
        }

        throw HideNodesException.Unauthorized()
    }

    private suspend fun unhideNodes(nodes: Set<NodeId>): HideNodesResult {
        val success = AtomicInteger(0)
        val failed = AtomicInteger(0)

        nodes.mapAsync { nodeId ->
            runCatching {
                updateNodeSensitiveUseCase(
                    nodeId = nodeId,
                    isSensitive = false
                )
            }.onFailure {
                failed.getAndIncrement()
            }.onSuccess {
                success.getAndIncrement()
            }
        }

        return HideNodesResult(success = success.get(), failed = failed.get())
    }

    private suspend fun isPaid(): Boolean =
        monitorAccountDetailUseCase()
            .first()
            .levelDetail
            ?.accountPlanDetail
            ?.accountType
            ?.isPaid == true

    private suspend fun isBusinessAccountExpired(): Boolean =
        isBusinessStatusUseCase() == BusinessAccountStatus.Expired
}