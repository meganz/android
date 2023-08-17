package mega.privacy.android.feature.devicecenter.ui.mapper

import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import javax.inject.Inject

/**
 * UI Mapper class that converts [DeviceCenterNodeStatus] into its UI equivalent
 * [DeviceCenterUINodeStatus]
 */
internal class DeviceCenterUINodeStatusMapper @Inject constructor() {

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
        is DeviceCenterNodeStatus.Blocked -> DeviceCenterUINodeStatus.Blocked
        DeviceCenterNodeStatus.Overquota -> DeviceCenterUINodeStatus.Overquota
        DeviceCenterNodeStatus.Paused -> DeviceCenterUINodeStatus.Paused
        DeviceCenterNodeStatus.Initializing -> DeviceCenterUINodeStatus.Initializing
        is DeviceCenterNodeStatus.Syncing -> {
            if (status.progress > 0) {
                DeviceCenterUINodeStatus.SyncingWithPercentage(status.progress)
            } else {
                DeviceCenterUINodeStatus.Syncing
            }
        }

        DeviceCenterNodeStatus.NoCameraUploads -> DeviceCenterUINodeStatus.NoCameraUploads
        else -> DeviceCenterUINodeStatus.Unknown
    }
}