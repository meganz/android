package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
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
