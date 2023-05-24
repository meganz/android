package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import javax.inject.Inject

/**
 * Check the availability of camera upload local secondary folder
 *
 * If it's a path in internal storage, check its existence
 * If it's a path in SD card, check the corresponding DocumentFile's existence
 *
 * @return true, if secondary folder is available
 */
class DefaultIsLocalSecondaryFolderSet @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val fileSystemRepository: FileSystemRepository,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
) : IsLocalSecondaryFolderSet {
    override suspend fun invoke() = if (isSecondaryFolderEnabled()) {
        if (cameraUploadRepository.isSecondaryFolderInSDCard()) {
            val uriPath = cameraUploadRepository.getSecondaryFolderSDCardUriPath()
            fileSystemRepository.isFolderInSDCardAvailable(uriPath)
        } else {
            fileSystemRepository.doesFolderExists(getSecondaryFolderPathUseCase())
        }
    } else {
        cameraUploadRepository.setSecondaryEnabled(false)
        true
    }
}
