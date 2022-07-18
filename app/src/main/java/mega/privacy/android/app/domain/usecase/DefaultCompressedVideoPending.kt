package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncStatus
import javax.inject.Inject

/**
 * If compressed video is pending
 *
 */
class DefaultCompressedVideoPending @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : CompressedVideoPending {

    override fun invoke(): Boolean {
        return cameraUploadRepository.getVideoSyncRecordsByStatus(SyncStatus.STATUS_TO_COMPRESS.value)
            .isNotEmpty() && SettingsConstants.VIDEO_QUALITY_ORIGINAL.toString() != cameraUploadRepository.getVideoQuality()
    }
}
