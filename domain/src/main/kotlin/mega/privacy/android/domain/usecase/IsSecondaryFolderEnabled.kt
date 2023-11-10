package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Check preferences if secondary media folder is enabled
 *
 * @return true, if secondary enabled
 */
class IsSecondaryFolderEnabled @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invoke
     *
     * @return if secondary is enabled
     */
    suspend operator fun invoke() = cameraUploadRepository.isSecondaryMediaFolderEnabled() ?: false
}
