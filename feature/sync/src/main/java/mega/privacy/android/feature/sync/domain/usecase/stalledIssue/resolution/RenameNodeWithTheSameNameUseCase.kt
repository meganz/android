package mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import javax.inject.Inject

internal class RenameNodeWithTheSameNameUseCase @Inject constructor(
    private val addCounterToNodeNameUseCase: AddCounterToNodeNameUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) {

    suspend operator fun invoke(nodeIds: List<NodeId>) {
        var counter = 1
        val nodes = if (nodeIds.size > 1) {
            nodeIds.drop(1)
        } else {
            nodeIds
        }
        nodes.forEach { id ->
            val nodeName = getNodeByIdUseCase(id)?.name ?: return@forEach
            addCounterToNodeNameUseCase(nodeName, id, counter)
            counter++
        }
    }
}
