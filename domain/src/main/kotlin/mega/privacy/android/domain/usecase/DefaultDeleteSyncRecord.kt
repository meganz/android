package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.DeleteSyncRecord
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
