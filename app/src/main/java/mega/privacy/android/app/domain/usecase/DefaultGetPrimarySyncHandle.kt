package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * @return the primary camera upload folder's handle
 */
class DefaultGetPrimarySyncHandle
@Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : GetPrimarySyncHandle {
    override suspend fun invoke(): Long {
        return cameraUploadRepository.getPrimarySyncHandle() ?: MegaApiJava.INVALID_HANDLE
    }
}
