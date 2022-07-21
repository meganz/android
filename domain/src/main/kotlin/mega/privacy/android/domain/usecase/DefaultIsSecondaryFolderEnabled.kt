package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
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
