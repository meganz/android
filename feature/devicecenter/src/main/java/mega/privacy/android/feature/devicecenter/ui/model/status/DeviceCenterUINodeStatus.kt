package mega.privacy.android.feature.devicecenter.ui.model.status

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.core.R as CoreR
import mega.privacy.android.feature.devicecenter.R as DeviceCenterR
import mega.privacy.android.shared.resources.R as SharedR
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import mega.privacy.android.shared.original.core.ui.controls.status.StatusColor
import mega.privacy.android.feature.devicecenter.R

/**
 * Sealed UI Node Status class that enumerates all possible UI Node Statuses with their respective information
 *
 * @property name The generalized Status Name
 * @property localizedErrorMessage The specific Error Message for Error Statuses such as [Blocked] or [Overquota]
 * @property icon The Status Icon. If no Icon should be displayed, leave it as null
 * @property color The Status Color to be applied for [name] and [icon]. If the Color should not be changed, leave it as null
 */
sealed class DeviceCenterUINodeStatus(
    @StringRes val name: Int,
    @StringRes val localizedErrorMessage: Int?,
    @DrawableRes val icon: Int?,
    val color: StatusColor?,
) {

    /**
     * Represents an Unknown Status. This is the default Status assigned when no matching Status
     * is found
     */
    data object Unknown : DeviceCenterUINodeStatus(
        name = R.string.device_center_list_view_item_status_unknown_status,
        localizedErrorMessage = null,
        icon = iconPackR.drawable.ic_help_circle_medium_regular_outline,
        color = null,
    )

    /**
     * Represents an Up to Date Status
     */
    data object UpToDate : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_up_to_date,
        localizedErrorMessage = null,
        icon = CoreR.drawable.ic_check_circle,
        color = StatusColor.Success,
    )

    /**
     * Represents an Initializing Status
     */
    data object Initializing : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_initialising,
        localizedErrorMessage = null,
        icon = CoreR.drawable.ic_sync_02,
        color = StatusColor.Info,
    )

    /**
     * Represents a Scanning Status
     */
    data object Scanning : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_scanning,
        localizedErrorMessage = null,
        icon = CoreR.drawable.ic_sync_02,
        color = StatusColor.Info,
    )

    /**
     * Represents a Syncing Status
     */
    data object Syncing : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_updating,
        localizedErrorMessage = null,
        icon = CoreR.drawable.ic_sync_02,
        color = StatusColor.Info,
    )

    /**
     * Represents a Syncing Status with the Percentage
     *
     * @param progress the Update Progress indicator
     */
    data class SyncingWithPercentage(val progress: Int) : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_updating_with_progress,
        localizedErrorMessage = null,
        icon = null,
        color = StatusColor.Info,
    )

    /**
     * Represents a Camera Uploads Disabled Status
     */
    data object CameraUploadsDisabled : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_camera_uploads_disabled,
        localizedErrorMessage = null,
        icon = CoreR.drawable.ic_info,
        color = StatusColor.Warning,
    )

    /**
     * Represents a Nothing Set Up Status
     */
    data object NothingSetUp : DeviceCenterUINodeStatus(
        name = SharedR.string.device_center_list_view_item_status_nothing_setup,
        localizedErrorMessage = null,
        icon = CoreR.drawable.ic_info,
        color = null,
    )

    /**
     * Represents a Disabled Status
     */
    data object Disabled : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_disabled,
        localizedErrorMessage = null,
        icon = CoreR.drawable.ic_alert_triangle,
        color = StatusColor.Warning,
    )

    /**
     * Represents an Offline Status
     */
    data object Offline : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_offline,
        localizedErrorMessage = null,
        icon = CoreR.drawable.ic_cloud_offline,
        color = null,
    )

    /**
     * Represents a Paused Status
     */
    data object Paused : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_paused,
        localizedErrorMessage = null,
        icon = CoreR.drawable.ic_pause,
        color = null,
    )

    /**
     * Represents a Stopped Status
     */
    data object Stopped : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_backup_stopped,
        localizedErrorMessage = null,
        icon = CoreR.drawable.ic_x_circle,
        color = null,
    )

    /**
     * Represents an Overquota Error Status
     *
     * @property specificErrorMessage The specific Error Message which may or may not exist
     */
    data class Overquota(val specificErrorMessage: Int?) : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_out_of_quota,
        localizedErrorMessage = specificErrorMessage,
        icon = iconPackR.drawable.ic_alert_circle_regular_medium_outline,
        color = StatusColor.Error,
    )

    /**
     * Represents a non-specific Error Status
     *
     * @property specificErrorMessage The specific Error Message which may or may not exist
     */
    data class Error(val specificErrorMessage: Int?) : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_error,
        localizedErrorMessage = specificErrorMessage,
        icon = CoreR.drawable.ic_x_circle,
        color = StatusColor.Error,
    )

    /**
     * Represents a Blocked Error Status
     *
     * @property specificErrorMessage The specific Error Message which may or may not exist
     */
    data class Blocked(val specificErrorMessage: Int?) : DeviceCenterUINodeStatus(
        name = DeviceCenterR.string.device_center_list_view_item_status_blocked,
        localizedErrorMessage = specificErrorMessage,
        icon = CoreR.drawable.ic_minus_circle,
        color = StatusColor.Error,
    )
}