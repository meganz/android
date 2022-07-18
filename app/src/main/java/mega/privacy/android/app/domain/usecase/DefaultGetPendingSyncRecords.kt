package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecord
import javax.inject.Inject

/**
 * Get pending sync records
 *
 */
class DefaultGetPendingSyncRecords @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : GetPendingSyncRecords {

    override fun invoke(): List<SyncRecord> = cameraUploadRepository.getPendingSyncRecords()
}
