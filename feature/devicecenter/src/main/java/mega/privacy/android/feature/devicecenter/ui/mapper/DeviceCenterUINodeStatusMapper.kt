package mega.privacy.android.feature.devicecenter.ui.mapper

import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceStatus
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import javax.inject.Inject

/**
 * UI Mapper class that retrieves the appropriate [DeviceCenterUINodeStatus] based on several
 * parameters
 *
 * @property deviceFolderUINodeErrorMessageMapper [DeviceFolderUINodeErrorMessageMapper]
 */
internal class DeviceCenterUINodeStatusMapper @Inject constructor(
    private val deviceFolderUINodeErrorMessageMapper: DeviceFolderUINodeErrorMessageMapper,
) {

    /**
     * Invocation function
     *
     * @param type The [BackupInfoType]
     * @param status The [DeviceFolderStatus]
     * @return the corresponding [DeviceCenterUINodeStatus]
     */
    operator fun invoke(type: BackupInfoType, status: DeviceFolderStatus) =
        when (status) {
            DeviceFolderStatus.Inactive -> DeviceCenterUINodeStatus.Inactive

            is DeviceFolderStatus.Error -> DeviceCenterUINodeStatus.Error(
                specificErrorMessage = status.errorSubState?.let { errorSubState ->
                    deviceFolderUINodeErrorMessageMapper(errorSubState)
                })

            DeviceFolderStatus.Paused -> DeviceCenterUINodeStatus.Paused

            DeviceFolderStatus.Disabled -> DeviceCenterUINodeStatus.Disabled

            is DeviceFolderStatus.Updating -> when (type) {
                BackupInfoType.CAMERA_UPLOADS, BackupInfoType.MEDIA_UPLOADS -> {
                    if (status.progress > 0) {
                        DeviceCenterUINodeStatus.UploadingWithPercentage(status.progress)
                    } else {
                        DeviceCenterUINodeStatus.Uploading
                    }
                }

                else -> {
                    if (status.progress > 0) {
                        DeviceCenterUINodeStatus.UpdatingWithPercentage(status.progress)
                    } else {
                        DeviceCenterUINodeStatus.Updating
                    }
                }
            }

            DeviceFolderStatus.UpToDate -> DeviceCenterUINodeStatus.UpToDate
            else -> DeviceCenterUINodeStatus.Unknown
        }

    /**
     * Invocation function
     *
     * @param status The [DeviceFolderStatus]
     * @return the corresponding [DeviceCenterUINodeStatus]
     */
    operator fun invoke(status: DeviceStatus) = when (status) {
        DeviceStatus.Inactive -> DeviceCenterUINodeStatus.Inactive
        DeviceStatus.AttentionNeeded -> DeviceCenterUINodeStatus.AttentionNeeded
        DeviceStatus.Updating -> DeviceCenterUINodeStatus.Updating
        DeviceStatus.UpToDate -> DeviceCenterUINodeStatus.UpToDate
        DeviceStatus.NothingSetUp -> DeviceCenterUINodeStatus.NothingSetUp
        else -> DeviceCenterUINodeStatus.Unknown
    }
}