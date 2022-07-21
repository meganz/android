package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.usecase.GetSyncRecordByFingerprint
import javax.inject.Inject

/**
 * Get sync record by fingerprint
 *
 */
class DefaultGetSyncRecordByFingerprint @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : GetSyncRecordByFingerprint {

    override fun invoke(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopyOnly: Boolean,
    ): SyncRecord? =
        cameraUploadRepository.getSyncRecordByFingerprint(fingerprint, isSecondary, isCopyOnly)
}
