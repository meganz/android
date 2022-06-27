package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default is camera upload running
 *
 * @property cameraUploadRepository
 */
class DefaultIsCameraUploadRunning @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) :
    IsCameraUploadRunning {
    override fun invoke(): Flow<Boolean> = cameraUploadRepository.isCameraUploadRunning()
}
