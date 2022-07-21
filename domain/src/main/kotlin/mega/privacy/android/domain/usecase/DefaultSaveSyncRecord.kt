package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.usecase.SaveSyncRecord
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
