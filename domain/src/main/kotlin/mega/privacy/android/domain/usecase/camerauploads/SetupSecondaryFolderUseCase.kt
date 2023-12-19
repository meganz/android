package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import javax.inject.Inject

/**
 * Setup Secondary Folder for Camera Upload
 *
 */
class SetupSecondaryFolderUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val setSecondarySyncHandle: SetSecondarySyncHandle,
) {

    /**
     * Invoke
     *
     * @param secondaryHandle
     */
    suspend operator fun invoke(secondaryHandle: Long) {
        cameraUploadRepository.setupSecondaryFolder(secondaryHandle)
            .takeIf { it != cameraUploadRepository.getInvalidHandle() }
            ?.let {
                setSecondarySyncHandle(it)
            }
    }
}

