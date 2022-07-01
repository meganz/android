package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.data.repository.DefaultCameraUploadRepository.SyncTimeStamp
import mega.privacy.android.app.domain.entity.SyncRecordType
import mega.privacy.android.app.domain.repository.CameraUploadRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Update camera upload time stamp if newer
 *
 * @property cameraUploadRepository
 */
class DefaultUpdateCameraUploadTimeStamp @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) :
    UpdateCameraUploadTimeStamp {
    override fun invoke(
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
                val timeStampSecondary = timestamp ?: cameraUploadRepository.getMaxTimestamp(
                    true,
                    SyncRecordType.TYPE_PHOTO.value)
                updateTimeStamp(timeStampSecondary, timestampType)
            }
            SyncTimeStamp.SECONDARY_VIDEO -> {
                val timeStampSecondaryVideo = timestamp ?: cameraUploadRepository.getMaxTimestamp(
                    true,
                    SyncRecordType.TYPE_VIDEO.value)
                updateTimeStamp(timeStampSecondaryVideo, timestampType)
            }
        }
    }

    private fun updateTimeStamp(
        newTimeStamp: Long,
        timestampType: SyncTimeStamp,
    ) {
        val currentTimeStamp = cameraUploadRepository.getSyncTimeStamp(timestampType)
        if (newTimeStamp > currentTimeStamp) {
            Timber.d("Update %s Timestamp with: %s", timestampType.toString(), newTimeStamp)
            cameraUploadRepository.setSyncTimeStamp(newTimeStamp, timestampType)
        } else {
            Timber.d("%s Timestamp is: %s", timestampType.toString(), currentTimeStamp)
        }
    }
}
