package mega.privacy.android.domain.usecase.transfers.uploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.file.GetGPSCoordinatesUseCase
import javax.inject.Inject

/**
 * Set node coordinates use case
 *
 * @property nodeRepository
 * @property getGPSCoordinatesUseCase
 * @constructor Create empty Set node coordinates use case
 */
class SetNodeCoordinatesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val getGPSCoordinatesUseCase: GetGPSCoordinatesUseCase,
) {

    /**
     * Invoke
     *
     * @param nodeHandle Node identifier of the file in the cloud.
     * @param uriPath [UriPath] of the file, it will be used as a fallback in case geolocation is not set.
     * @param geolocation Geolocation fetched at upload start when the file is temporary, so can't be checked here.
     */
    suspend operator fun invoke(
        uriPath: UriPath,
        nodeHandle: Long,
        geolocation: TransferAppData.Geolocation? = null,
    ) {
        (geolocation?.let { Pair(it.latitude, it.longitude) } ?: getGPSCoordinatesUseCase(uriPath))
            ?.let { (latitude, longitude) ->
                nodeRepository.setNodeCoordinates(
                    NodeId(nodeHandle),
                    latitude,
                    longitude,
                )
            }
    }
}
