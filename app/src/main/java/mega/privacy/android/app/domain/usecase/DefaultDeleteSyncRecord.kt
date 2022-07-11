package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Delete camera upload sync record
 *
 */
class DefaultDeleteSyncRecord @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : DeleteSyncRecord {

    override fun invoke(path: String, isSecondary: Boolean) =
        cameraUploadRepository.deleteSyncRecord(path, isSecondary)
}
