package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Set sync folder path
 *
 */
class DefaultSetSyncLocalPath @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : SetSyncLocalPath {

    override fun invoke(localPath: String) =
        cameraUploadRepository.setSyncLocalPath(localPath)
}
