package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Clear sync records if necessary
 *
 */
class DefaultClearSyncRecords @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : ClearSyncRecords {

    override suspend fun invoke() {
        if (cameraUploadRepository.shouldClearSyncRecords()) {
            cameraUploadRepository.deleteAllSyncRecords(SyncRecordType.TYPE_ANY)
            cameraUploadRepository.saveShouldClearCamSyncRecords(false)
        }
    }
}
