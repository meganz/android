package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.KeepFileNames
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
