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
class DefaultUpdateTimeStamp @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) :
    UpdateTimeStamp {
    override fun invoke(
        timestamp: Long?,
        timestampType: SyncTimeStamp,
    ) {
        when (timestampType) {
            SyncTimeStamp.PRIMARY -> {
                val timeStampPrimary = timestamp ?: cameraUploadRepository.getMaxTimestamp(
                    false,
                    SyncRecordType.TYPE_PHOTO.value)
                val currentTimeStamp =
                    cameraUploadRepository.getSyncTimeStamp(SyncTimeStamp.PRIMARY)
                if (timeStampPrimary > currentTimeStamp) {
                    Timber.d("Update Primary Photo Timestamp with: %s", timeStampPrimary)
                    cameraUploadRepository.setSyncTimeStamp(timeStampPrimary, SyncTimeStamp.PRIMARY)
                } else {
                    Timber.d("Primary Photo Timestamp is: %s", currentTimeStamp)
                }

            }
            SyncTimeStamp.PRIMARY_VIDEO -> {
                val timeStampPrimaryVideo =
                    timestamp ?: cameraUploadRepository.getMaxTimestamp(
                        false,
                        SyncRecordType.TYPE_VIDEO.value)
                val currentVideoTimeStamp =
                    cameraUploadRepository.getSyncTimeStamp(SyncTimeStamp.PRIMARY_VIDEO)
                if (timeStampPrimaryVideo > currentVideoTimeStamp) {
                    Timber.d("Update Primary Video Timestamp with: %s", timeStampPrimaryVideo)
                    cameraUploadRepository.setSyncTimeStamp(timeStampPrimaryVideo,
                        SyncTimeStamp.PRIMARY_VIDEO)
                } else {
                    Timber.d("Primary Video Timestamp is: %s", currentVideoTimeStamp)
                }
            }
            SyncTimeStamp.SECONDARY -> {
                val timeStampSecondary = timestamp ?: cameraUploadRepository.getMaxTimestamp(
                    true,
                    SyncRecordType.TYPE_PHOTO.value)
                val secondaryTimeStamp =
                    cameraUploadRepository.getSyncTimeStamp(SyncTimeStamp.SECONDARY)
                if (timeStampSecondary > secondaryTimeStamp) {
                    Timber.d("Update Secondary Photo Timestamp with: %s", timeStampSecondary)
                    cameraUploadRepository.setSyncTimeStamp(timeStampSecondary,
                        SyncTimeStamp.SECONDARY)
                } else {
                    Timber.d("Secondary Photo Timestamp is: %s", secondaryTimeStamp)
                }
            }
            SyncTimeStamp.SECONDARY_VIDEO -> {
                val timeStampSecondaryVideo = timestamp ?: cameraUploadRepository.getMaxTimestamp(
                    true,
                    SyncRecordType.TYPE_VIDEO.value)
                val secondaryVideoTimeStamp =
                    cameraUploadRepository.getSyncTimeStamp(SyncTimeStamp.SECONDARY_VIDEO)
                if (timeStampSecondaryVideo > secondaryVideoTimeStamp) {
                    Timber.d("Update Secondary Video Timestamp with: %s", timeStampSecondaryVideo)
                    cameraUploadRepository.setSyncTimeStamp(timeStampSecondaryVideo,
                        SyncTimeStamp.SECONDARY_VIDEO)
                } else {
                    Timber.d("Secondary Video Timestamp is: %s", secondaryVideoTimeStamp)
                }
            }
        }
    }
}
