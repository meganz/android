package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Save sync records
 *
 */
class SaveSyncRecordsUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(syncRecords: List<SyncRecord>) =
        cameraUploadRepository.saveSyncRecords(syncRecords)
}
