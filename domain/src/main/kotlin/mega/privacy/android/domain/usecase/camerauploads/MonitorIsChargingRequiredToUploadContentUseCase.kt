package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that observes the User's Setting in the DataStore, as to whether or not the Device
 * must be charged for the active Camera Uploads to start uploading content
 *
 * @property cameraUploadRepository Repository containing all Camera Uploads related functions
 */
class MonitorIsChargingRequiredToUploadContentUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {
    /**
     * Invocation function
     *
     * @return A Boolean [Flow] that observes the User's Setting. The [Flow] emits false if the
     * Setting cannot be found
     */
    operator fun invoke(): Flow<Boolean> =
        cameraUploadRepository.monitorIsChargingRequiredToUploadContent().mapNotNull { it ?: false }
}