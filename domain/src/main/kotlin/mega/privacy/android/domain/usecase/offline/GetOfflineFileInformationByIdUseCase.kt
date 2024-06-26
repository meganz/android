package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.node.NodeId
import javax.inject.Inject

/**
 * Get OfflineFileInformation by Node Id
 *
 */
class GetOfflineFileInformationByIdUseCase @Inject constructor(
    private val getOfflineNodeInformationByNodeIdUseCase: GetOfflineNodeInformationByNodeIdUseCase,
    private val getOfflineFileInformationUseCase: GetOfflineFileInformationUseCase,
) {
    /**
     * Invoke
     *
     * @param nodeId [NodeId]
     * @param useOriginalImageAsThumbnail [Boolean] use original image file as thumbnail
     */
    suspend operator fun invoke(
        nodeId: NodeId, useOriginalImageAsThumbnail: Boolean = false,
    ) = getOfflineNodeInformationByNodeIdUseCase(nodeId = nodeId)?.let {
        getOfflineFileInformationUseCase(
            offlineNodeInformation = it,
            useOriginalImageAsThumbnail = useOriginalImageAsThumbnail
        )
    }
}