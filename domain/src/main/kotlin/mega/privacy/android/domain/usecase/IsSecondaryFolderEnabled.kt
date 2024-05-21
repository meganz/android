package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Check preferences if secondary media folder is enabled
 *
 * @return true, if secondary enabled
 */
class IsSecondaryFolderEnabled @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invoke
     *
     * @return if secondary is enabled
     */
    suspend operator fun invoke() = cameraUploadsRepository.isSecondaryMediaFolderEnabled() ?: false
}
