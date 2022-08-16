package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Update camera upload time stamp if newer
 *
 * @property cameraUploadRepository
 */
class DefaultUpdateCameraUploadTimeStamp @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
) :
    UpdateCameraUploadTimeStamp {
    override suspend fun invoke(
        timestamp: Long?,
        timestampType: SyncTimeStamp,
    ) {
        when (timestampType) {
            SyncTimeStamp.PRIMARY_PHOTO -> {
                val timeStampPrimary = timestamp ?: cameraUploadRepository.getMaxTimestamp(
                    false,
                    SyncRecordType.TYPE_PHOTO.value)
                updateTimeStamp(timeStampPrimary, timestampType)
            }
            SyncTimeStamp.PRIMARY_VIDEO -> {
                val timeStampPrimaryVideo = timestamp ?: cameraUploadRepository.getMaxTimestamp(
                    false,
                    SyncRecordType.TYPE_VIDEO.value)
                updateTimeStamp(timeStampPrimaryVideo, timestampType)
            }
            SyncTimeStamp.SECONDARY_PHOTO -> {
                if (isSecondaryFolderEnabled()) {
                    val timeStampSecondary = timestamp ?: cameraUploadRepository.getMaxTimestamp(
                        true,
                        SyncRecordType.TYPE_PHOTO.value)
                    updateTimeStamp(timeStampSecondary, timestampType)
                }
            }
            SyncTimeStamp.SECONDARY_VIDEO -> {
                if (isSecondaryFolderEnabled()) {
                    val timeStampSecondaryVideo =
                        timestamp ?: cameraUploadRepository.getMaxTimestamp(
                            true,
                            SyncRecordType.TYPE_VIDEO.value)
                    updateTimeStamp(timeStampSecondaryVideo, timestampType)
                }
            }
        }
    }

    private suspend fun updateTimeStamp(
        newTimeStamp: Long,
        timestampType: SyncTimeStamp,
    ) {
        val currentTimeStamp = cameraUploadRepository.getSyncTimeStamp(timestampType)
        if (newTimeStamp > currentTimeStamp) {
            cameraUploadRepository.setSyncTimeStamp(newTimeStamp, timestampType)
        }
    }
}
