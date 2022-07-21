package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import javax.inject.Inject

/**
 * Set sync record pending by local path
 *
 */
class DefaultSetSyncRecordPendingByPath @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : SetSyncRecordPendingByPath {

    override fun invoke(localPath: String?, isSecondary: Boolean) =
        cameraUploadRepository.updateSyncRecordStatusByLocalPath(SyncStatus.STATUS_PENDING.value,
            localPath,
            isSecondary)
}
