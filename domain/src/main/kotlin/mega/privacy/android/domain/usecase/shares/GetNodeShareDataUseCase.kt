package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.Node
import javax.inject.Inject

class GetNodeShareDataUseCase @Inject constructor(
    private val getUnverifiedIncomingShares: GetUnverifiedIncomingShares,
    private val getUnverifiedOutgoingShares: GetUnverifiedOutgoingShares,
    private val isOutShareUseCase: IsOutShareUseCase
) {
    suspend operator fun invoke(node: Node) = when {
        isOutShareUseCase(node) -> runCatching {
            getUnverifiedOutgoingShares(SortOrder.ORDER_NONE).firstOrNull {
                it.nodeHandle == node.id.longValue
            }
        }.getOrNull()

        node.isIncomingShare -> runCatching {
            getUnverifiedIncomingShares(SortOrder.ORDER_NONE).firstOrNull {
                it.nodeHandle == node.id.longValue
            }
        }.getOrNull()

        else -> null
    }
}