package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Should keep file names
 *
 */
class DefaultKeepFileNames @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : KeepFileNames {

    override fun invoke(): Boolean = cameraUploadRepository.getKeepFileNames()
}
