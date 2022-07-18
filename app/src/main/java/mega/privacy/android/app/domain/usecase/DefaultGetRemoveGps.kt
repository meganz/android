package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Get remove GPS setting
 *
 */
class DefaultGetRemoveGps @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : GetRemoveGps {
    override fun invoke(): Boolean = cameraUploadRepository.getRemoveGpsDefault()
}
