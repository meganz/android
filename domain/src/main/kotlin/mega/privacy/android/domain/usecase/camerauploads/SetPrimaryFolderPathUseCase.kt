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
     */
    suspend operator fun invoke(newFolderPath: String) {
        cameraUploadRepository.let {
            val isInSDCard = it.isPrimaryFolderInSDCard()
            it.setPrimaryFolderInSDCard(isInSDCard)
            setPrimaryFolderLocalPathUseCase(
                if (isInSDCard) {
                    it.getPrimaryFolderSDCardUriPath()
                } else {
                    newFolderPath
                }
            )
        }
    }
}