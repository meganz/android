package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that checks whether the Node Handle of the Primary Folder is valid or not
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 * @property getSecondarySyncHandleUseCase [GetSecondarySyncHandleUseCase]
 * @property isMediaUploadsEnabledUseCase [IsMediaUploadsEnabledUseCase]
 */
class IsPrimaryFolderNodeValidUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase,
) {
    /**
     * Invocation function
     *
     * @param nodeHandle The Node Handle of the Primary Folder, which can be null
     * @return true if the Primary Folder Node Handle is valid
     */
    suspend operator fun invoke(nodeHandle: Long?) = nodeHandle?.let { primaryHandle ->
        if (isMediaUploadsEnabledUseCase()) {
            primaryHandle.isNotAnInvalidHandle() && primaryHandle != getSecondarySyncHandleUseCase()
        } else {
            primaryHandle.isNotAnInvalidHandle()
        }
    } ?: false

    private fun Long?.isNotAnInvalidHandle() = this != cameraUploadsRepository.getInvalidHandle()
}