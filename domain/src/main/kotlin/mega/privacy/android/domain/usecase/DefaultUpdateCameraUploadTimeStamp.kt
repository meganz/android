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
        if (timestampType.isPrimary || isSecondaryFolderEnabled()) {
            setMaxTimeStamp(
                timestamp,
                timestampType.isPrimary,
                timestampType.syncRecordType,
                timestampType
            )
        }
    }

    private suspend fun setMaxTimeStamp(
        timestamp: Long?,
        isPrimary: Boolean,
        syncRecordType: SyncRecordType,
        timestampType: SyncTimeStamp,
    ) {
        val maxTimeStamp =
            timestamp ?: cameraUploadRepository.getMaxTimestamp(
                isSecondary = !isPrimary,
                syncRecordType
            )
        updateTimeStamp(maxTimeStamp, timestampType)
    }

    private suspend fun updateTimeStamp(
        newTimeStamp: Long,
        timestampType: SyncTimeStamp,
    ) {
        val currentTimeStamp =
            cameraUploadRepository.getSyncTimeStamp(timestampType) ?: 0L
        if (newTimeStamp > currentTimeStamp) {
            cameraUploadRepository.setSyncTimeStamp(newTimeStamp, timestampType)
        }
    }

    private val SyncTimeStamp.isPrimary: Boolean
        get() = when (this) {
            SyncTimeStamp.PRIMARY_PHOTO -> true
            SyncTimeStamp.PRIMARY_VIDEO -> true
            SyncTimeStamp.SECONDARY_PHOTO -> false
            SyncTimeStamp.SECONDARY_VIDEO -> false
        }

    private val SyncTimeStamp.syncRecordType: SyncRecordType
        get() = when (this) {
            SyncTimeStamp.PRIMARY_PHOTO -> SyncRecordType.TYPE_PHOTO
            SyncTimeStamp.PRIMARY_VIDEO -> SyncRecordType.TYPE_VIDEO
            SyncTimeStamp.SECONDARY_PHOTO -> SyncRecordType.TYPE_PHOTO
            SyncTimeStamp.SECONDARY_VIDEO -> SyncRecordType.TYPE_VIDEO
        }
}
