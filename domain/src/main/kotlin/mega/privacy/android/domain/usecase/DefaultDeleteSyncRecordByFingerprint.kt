package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import javax.inject.Inject

/**
 * Delete camera upload sync record by fingerprint
 *
 */
class DefaultDeleteSyncRecordByFingerprint @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : DeleteSyncRecordByFingerprint {

    override fun invoke(originalPrint: String, newPrint: String, isSecondary: Boolean) {
        cameraUploadRepository.deleteSyncRecordByFingerprint(originalPrint, newPrint, isSecondary)
    }
}
