package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Check preferences if secondary media folder is enabled
 *
 * @return true, if secondary enabled
 */
class DefaultIsSecondaryFolderEnabled @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : IsSecondaryFolderEnabled {
    override fun invoke(): Boolean = cameraUploadRepository.isSecondaryMediaFolderEnabled()
}
