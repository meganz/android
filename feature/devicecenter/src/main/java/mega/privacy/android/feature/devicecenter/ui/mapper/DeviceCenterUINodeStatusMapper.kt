package mega.privacy.android.feature.devicecenter.ui.mapper

import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
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
     * @param isDevice true if the node is a Device Node, and false if otherwise
     * @param status The [DeviceCenterNodeStatus]
     * @return the corresponding [DeviceCenterUINodeStatus]
     */
    operator fun invoke(isDevice: Boolean, status: DeviceCenterNodeStatus) = when (status) {
        DeviceCenterNodeStatus.Stopped -> DeviceCenterUINodeStatus.Stopped
        DeviceCenterNodeStatus.Disabled -> DeviceCenterUINodeStatus.Disabled
        DeviceCenterNodeStatus.Offline -> DeviceCenterUINodeStatus.Offline
        DeviceCenterNodeStatus.UpToDate -> DeviceCenterUINodeStatus.UpToDate
        is DeviceCenterNodeStatus.Blocked -> {
            if (isDevice || status.errorSubState == null) {
                DeviceCenterUINodeStatus.Blocked
            } else {
                DeviceCenterUINodeStatus.FolderError(
                    errorMessage = deviceFolderUINodeErrorMessageMapper(
                        errorSubState = status.errorSubState,
                    )
                )
            }
        }

        is DeviceCenterNodeStatus.Overquota -> {
            if (isDevice || status.errorSubState == null) {
                DeviceCenterUINodeStatus.Overquota
            } else {
                DeviceCenterUINodeStatus.FolderError(
                    errorMessage = deviceFolderUINodeErrorMessageMapper(
                        errorSubState = status.errorSubState,
                    )
                )
            }
        }

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
        else -> DeviceCenterUINodeStatus.Unknown
    }
}