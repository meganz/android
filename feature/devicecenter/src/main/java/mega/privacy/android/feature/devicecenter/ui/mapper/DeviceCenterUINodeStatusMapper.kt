package mega.privacy.android.feature.devicecenter.ui.mapper

import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
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
     * @param status The [DeviceCenterNodeStatus]
     * @return the corresponding [DeviceCenterUINodeStatus]
     */
    operator fun invoke(status: DeviceCenterNodeStatus) = when (status) {
        DeviceCenterNodeStatus.Stopped -> DeviceCenterUINodeStatus.Stopped
        DeviceCenterNodeStatus.Disabled -> DeviceCenterUINodeStatus.Disabled
        DeviceCenterNodeStatus.Offline -> DeviceCenterUINodeStatus.Offline
        DeviceCenterNodeStatus.UpToDate -> DeviceCenterUINodeStatus.UpToDate

        is DeviceCenterNodeStatus.Error -> DeviceCenterUINodeStatus.Error(
            specificErrorMessage = status.errorSubState?.let { errorSubState ->
                deviceFolderUINodeErrorMessageMapper(errorSubState)
            }
        )

        is DeviceCenterNodeStatus.Blocked -> DeviceCenterUINodeStatus.Blocked(
            specificErrorMessage = status.errorSubState?.let { errorSubState ->
                deviceFolderUINodeErrorMessageMapper(errorSubState)
            }
        )

        is DeviceCenterNodeStatus.Overquota -> DeviceCenterUINodeStatus.Overquota(
            specificErrorMessage = status.errorSubState?.let { errorSubState ->
                deviceFolderUINodeErrorMessageMapper(errorSubState)
            }
        )

        DeviceCenterNodeStatus.Paused -> DeviceCenterUINodeStatus.Paused
        DeviceCenterNodeStatus.Initializing -> DeviceCenterUINodeStatus.Initializing
        DeviceCenterNodeStatus.Scanning -> DeviceCenterUINodeStatus.Scanning
        is DeviceCenterNodeStatus.Syncing -> {
            if (status.progress > 0) {
                DeviceCenterUINodeStatus.SyncingWithPercentage(status.progress)
            } else {
                DeviceCenterUINodeStatus.Syncing
            }
        }

        DeviceCenterNodeStatus.NoCameraUploads -> DeviceCenterUINodeStatus.CameraUploadsDisabled

        DeviceCenterNodeStatus.Stalled -> DeviceCenterUINodeStatus.Blocked(null)

        else -> DeviceCenterUINodeStatus.Unknown
    }
}