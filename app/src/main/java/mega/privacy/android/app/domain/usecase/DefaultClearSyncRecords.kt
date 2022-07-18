package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecordType
import javax.inject.Inject

/**
 * Clear sync records if necessary
 *
 */
class DefaultClearSyncRecords @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : ClearSyncRecords {

    override fun invoke() {
        if (cameraUploadRepository.shouldClearSyncRecords()) {
            cameraUploadRepository.deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            cameraUploadRepository.shouldClearSyncRecords(false)
        }
    }
}
