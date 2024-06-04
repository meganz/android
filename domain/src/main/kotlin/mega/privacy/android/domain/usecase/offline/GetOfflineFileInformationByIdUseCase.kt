package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.node.NodeId
import javax.inject.Inject

/**
 * Get OfflineFileInformation by Node Id
 *
 */
class GetOfflineFileInformationByIdUseCase @Inject constructor(
    private val getOfflineNodeInformationByIdUseCase: GetOfflineNodeInformationByIdUseCase,
    private val getOfflineFileInformationUseCase: GetOfflineFileInformationUseCase,
) {
    /**
     * Invoke
     * @param nodeId [NodeId]
     */
    suspend operator fun invoke(nodeId: NodeId) =
        getOfflineNodeInformationByIdUseCase(nodeId)?.let {
            getOfflineFileInformationUseCase(it)
        }
}