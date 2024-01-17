package mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import javax.inject.Inject

internal class AddCounterToNodeNameUseCase @Inject constructor(
    private val renameNodeUseCase: RenameNodeUseCase,
) {

    suspend operator fun invoke(
        nodeName: String,
        nodeId: NodeId,
        counter: Int,
    ) {
        val nodeNameWithoutExtension = nodeName.substringBeforeLast(".")
        val nodeExtension =
            nodeName.substringAfterLast(".", missingDelimiterValue = "")
        val fullNodeExtension = if (nodeExtension.isNotEmpty()) {
            ".$nodeExtension"
        } else {
            ""
        }
        renameNodeUseCase(
            nodeId.longValue,
            "$nodeNameWithoutExtension ($counter)$fullNodeExtension"
        )
    }
}