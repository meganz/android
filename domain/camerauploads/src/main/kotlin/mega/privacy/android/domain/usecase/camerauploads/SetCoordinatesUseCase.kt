package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use case for setting the GPS coordinates of image files as a node attribute.
 */
class SetCoordinatesUseCase @Inject constructor(private val cameraUploadsRepository: CameraUploadsRepository) {

    /**
     * invoke
     * @param nodeId
     * @param latitude
     * @param longitude
     */
    suspend operator fun invoke(nodeId: NodeId, latitude: Double, longitude: Double) =
        cameraUploadsRepository.setCoordinates(nodeId, latitude, longitude)
}
