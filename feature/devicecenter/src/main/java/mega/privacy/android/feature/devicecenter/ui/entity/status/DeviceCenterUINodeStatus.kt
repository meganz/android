package mega.privacy.android.feature.devicecenter.ui.entity.status

import mega.privacy.android.core.R as CoreR
import mega.privacy.android.feature.devicecenter.R as DeviceCenterR
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import mega.privacy.android.core.ui.theme.orange_400
import mega.privacy.android.core.ui.theme.red_500

/**
 * Sealed UI Node Status class that enumerates all possible UI Node Statuses with their respective
 * information
 *
 * @property name The Status Name
 * @property icon The Status Icon. If no Icon should be displayed, leave it as null
 * @property color The Status Color to be applied for [name] and [icon]. If the Color should not be
 * changed, leave it as null
 */
sealed class DeviceCenterUINodeStatus(
    @StringRes val name: Int,
    @DrawableRes val icon: Int?,
    val color: Color?,
) {

    /**
     * Represents an Up to Date Status
     */
    object UpToDate : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_up_to_date,
        icon = CoreR.drawable.ic_check_circle,
        color = completed_status_color,
    )

    /**
     * Represents an Initialising Status
     */
    object Initialising : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_initialising,
        icon = CoreR.drawable.ic_sync_02,
        color = in_progress_status_color,
    )

    /**
     * Represents a Scanning Status
     */
    object Scanning : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_scanning,
        icon = CoreR.drawable.ic_sync_02,
        color = in_progress_status_color,
    )

    /**
     * Represents an Updating Status
     */
    object Updating : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_updating,
        icon = CoreR.drawable.ic_sync_02,
        color = in_progress_status_color,
    )

    /**
     * Represents an Updating Status with the Percentage
     *
     * @param progress the Update Progress indicator
     */
    data class UpdatingWithPercentage(val progress: Int) : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_updating_with_progress,
        icon = null,
        color = in_progress_status_color,
    )

    /**
     * Represents a No Camera Uploads Status
     */
    object NoCameraUploads : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_no_camera_uploads,
        icon = CoreR.drawable.ic_alert_circle,
        color = orange_400,
    )

    /**
     * Represents a Disabled Status
     */
    object Disabled : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_disabled,
        icon = CoreR.drawable.ic_alert_circle,
        color = orange_400,
    )

    /**
     * Represents an Offline Status
     */
    object Offline : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_offline,
        icon = CoreR.drawable.ic_cloud_offline,
        color = null,
    )

    /**
     * Represents a Paused Status
     */
    object Paused : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_paused,
        icon = CoreR.drawable.ic_cloud_offline,
        color = null,
    )

    /**
     * Represents a Backup Stopped Status
     */
    object BackupStopped : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_backup_stopped,
        icon = CoreR.drawable.ic_pause,
        color = null,
    )

    /**
     * Represents an Out of Quota Status
     */
    object OutOfQuota : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_out_of_quota,
        icon = CoreR.drawable.ic_alert_circle,
        color = red_500,
    )

    /**
     * Represents an Error Status
     */
    object Error : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_error,
        icon = CoreR.drawable.ic_alert_circle,
        color = red_500,
    )

    /**
     * Represents a Blocked Status
     */
    object Blocked : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_blocked,
        icon = CoreR.drawable.ic_alert_circle,
        color = red_500,
    )
}