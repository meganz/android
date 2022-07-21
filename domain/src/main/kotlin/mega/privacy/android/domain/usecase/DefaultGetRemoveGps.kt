package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.GetRemoveGps
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
