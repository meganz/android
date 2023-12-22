package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import javax.inject.Inject

/**
 * Get the default path where node should be downloaded
 */
class GetDownloadLocationForNodeIdUseCase @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getDownloadLocationForNodeUseCase: GetDownloadLocationForNodeUseCase,
) {

    /**
     * Get the default path where node should be downloaded
     * @param nodeId of the node that will be downloaded
     * @return a string representing the destination path
     */
    suspend operator fun invoke(nodeId: NodeId) =
        getNodeByIdUseCase(nodeId)?.let { node ->
            getDownloadLocationForNodeUseCase(node)
        }
}