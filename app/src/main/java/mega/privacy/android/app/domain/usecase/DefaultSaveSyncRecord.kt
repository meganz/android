package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecord
import javax.inject.Inject

/**
 * Save sync record
 *
 */
class DefaultSaveSyncRecord @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : SaveSyncRecord {

    override fun invoke(syncRecord: SyncRecord) = cameraUploadRepository.saveSyncRecord(syncRecord)
}
