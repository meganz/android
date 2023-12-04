package mega.privacy.android.feature.sync.domain.usecase.stalledIssue

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.usecase.AddCounterToNodeNameUseCase
import java.io.File
import javax.inject.Inject

internal class RenameNodeWithTheSameNameUseCase @Inject constructor(
    private val addCounterToNodeNameUseCase: AddCounterToNodeNameUseCase,
) {

    suspend operator fun invoke(nodeIdsWithNames: List<Pair<NodeId, String>>) {
        var counter = 1
        nodeIdsWithNames.drop(1).forEach { nodeIdWithName ->
            val nodeNameWithFullPath = nodeIdWithName.second
            val nodeName = nodeNameWithFullPath.substringAfterLast(File.separator)
            addCounterToNodeNameUseCase(nodeName, nodeIdWithName.first, counter)
            counter++
        }
    }
}