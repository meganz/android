package mega.privacy.android.feature.devicecenter.ui.model.status

import mega.privacy.android.core.R as CoreR
import mega.privacy.android.feature.devicecenter.R as DeviceCenterR
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import mega.privacy.android.core.ui.theme.blue_500
import mega.privacy.android.core.ui.theme.jade_400
import mega.privacy.android.core.ui.theme.orange_400
import mega.privacy.android.core.ui.theme.red_500
import mega.privacy.android.feature.devicecenter.R

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
     * Represents an Unknown Status. This is the default Status assigned when no matching Status
     * is found
     */
    data object Unknown : DeviceCenterUINodeStatus(
        name = R.string.device_center_list_view_item_status_unknown_status,
        icon = CoreR.drawable.ic_help_circle,
        color = null,
    )

    /**
     * Represents an Up to Date Status
     */
    data object UpToDate : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_up_to_date,
        icon = CoreR.drawable.ic_check_circle,
        color = jade_400,
    )

    /**
     * Represents an Initializing Status
     */
    data object Initializing : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_initialising,
        icon = CoreR.drawable.ic_sync_02,
        color = blue_500,
    )

    /**
     * Represents a Scanning Status
     */
    data object Scanning : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_scanning,
        icon = CoreR.drawable.ic_sync_02,
        color = blue_500,
    )

    /**
     * Represents a Syncing Status
     */
    data object Syncing : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_updating,
        icon = CoreR.drawable.ic_sync_02,
        color = blue_500,
    )

    /**
     * Represents a Syncing Status with the Percentage
     *
     * @param progress the Update Progress indicator
     */
    data class SyncingWithPercentage(val progress: Int) : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_updating_with_progress,
        icon = null,
        color = blue_500,
    )

    /**
     * Represents a No Camera Uploads Status
     */
    data object NoCameraUploads : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_no_camera_uploads,
        icon = CoreR.drawable.ic_info,
        color = orange_400,
    )

    /**
     * Represents a Disabled Status
     */
    data object Disabled : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_disabled,
        icon = CoreR.drawable.ic_alert_triangle,
        color = orange_400,
    )

    /**
     * Represents an Offline Status
     */
    data object Offline : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_offline,
        icon = CoreR.drawable.ic_cloud_offline,
        color = null,
    )

    /**
     * Represents a Paused Status
     */
    data object Paused : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_paused,
        icon = CoreR.drawable.ic_pause,
        color = null,
    )

    /**
     * Represents a Stopped Status
     */
    data object Stopped : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_backup_stopped,
        icon = CoreR.drawable.ic_x_circle,
        color = null,
    )

    /**
     * Represents an Overquota Status
     */
    data object Overquota : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_out_of_quota,
        icon = CoreR.drawable.ic_alert_circle,
        color = red_500,
    )

    /**
     * Represents an Error Status
     */
    data object Error : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_error,
        icon = CoreR.drawable.ic_x_circle,
        color = red_500,
    )

    /**
     * Represents an Error Status for Device Folders
     *
     * @param errorMessage The Error Message
     */
    data class FolderError(@StringRes val errorMessage: Int) : DeviceCenterUINodeStatus(
        name = errorMessage,
        icon = null,
        color = red_500,
    )

    /**
     * Represents a Blocked Status
     */
    data object Blocked : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_blocked,
        icon = CoreR.drawable.ic_minus_circle,
        color = red_500,
    )
}