package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Set secondary folder path
 *
 */
class DefaultSetSecondaryFolderPath @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : SetSecondaryFolderPath {

    override fun invoke(folderPath: String) =
        cameraUploadRepository.setSecondaryFolderPath(folderPath)
}
