package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.SetSecondaryFolderPath
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
