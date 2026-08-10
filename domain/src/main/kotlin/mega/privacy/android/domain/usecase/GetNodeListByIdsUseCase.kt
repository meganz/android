package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

class GetNodeListByIdsUseCase @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(nodeIds: List<NodeId>): List<TypedNode> =
        withContext(ioDispatcher) {
            nodeIds.mapAsync { getNodeByIdUseCase(it) }.filterNotNull()
        }
}