package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.SetSyncLocalPath
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
