package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * @return the secondary camera upload folder's handle
 */
class DefaultGetSecondarySyncHandle
@Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : GetSecondarySyncHandle {
    override suspend fun invoke(): Long {
        return cameraUploadRepository.getSecondarySyncHandle() ?: MegaApiJava.INVALID_HANDLE
    }
}
