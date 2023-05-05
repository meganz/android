package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that sets the new Primary Folder path
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property setPrimaryFolderLocalPathUseCase [SetPrimaryFolderPathUseCase]
 */
class SetPrimaryFolderPathUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val setPrimaryFolderLocalPathUseCase: SetPrimaryFolderLocalPathUseCase,
) {

    /**
     * Invocation function
     *
     * @param newFolderPath The new Primary Folder path
     * @param isPrimaryFolderInSDCard true if the Primary Folder is now located in the SD Card, and
     * false if otherwise
     */
    suspend operator fun invoke(newFolderPath: String, isPrimaryFolderInSDCard: Boolean) {
        cameraUploadRepository.let {
            it.setPrimaryFolderInSDCard(isPrimaryFolderInSDCard)
            if (isPrimaryFolderInSDCard) {
                it.setPrimaryFolderSDCardUriPath(newFolderPath)
                setPrimaryFolderLocalPathUseCase("")
            } else {
                it.setPrimaryFolderSDCardUriPath("")
                setPrimaryFolderLocalPathUseCase(newFolderPath)
            }
        }
    }
}