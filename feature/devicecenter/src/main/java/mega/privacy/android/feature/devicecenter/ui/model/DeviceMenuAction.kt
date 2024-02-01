package mega.privacy.android.feature.devicecenter.ui.model

import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithoutIcon
import mega.privacy.android.feature.devicecenter.R

/**
 * Device menu action.
 */
sealed interface DeviceMenuAction : MenuAction {

    /**
     * Rename
     */
    object Rename : MenuActionWithoutIcon(
        descriptionRes = R.string.device_center_device_top_menu_option_rename,
        testTag = TEST_TAG_RENAME_ACTION
    ), DeviceMenuAction

    /**
     * Info
     */
    object Info : MenuActionWithoutIcon(
        descriptionRes = R.string.device_center_device_top_menu_option_info,
        testTag = TEST_TAG_INFO_ACTION
    ), DeviceMenuAction

    /**
     * Camera Uploads
     */
    object CameraUploads : MenuActionWithoutIcon(
        descriptionRes = R.string.device_center_device_top_menu_option_camera_uploads,
        testTag = TEST_TAG_CAMERA_UPLOADS_ACTION
    ), DeviceMenuAction

    companion object {
        /**
         * Test Tag Rename Action
         */
        const val TEST_TAG_RENAME_ACTION = "device_center_screen:action_rename_device"

        /**
         * Test Tag Info Action
         */
        const val TEST_TAG_INFO_ACTION = "device_center_screen:action_device_info"

        /**
         * Test Tag Camera Uploads Action
         */
        const val TEST_TAG_CAMERA_UPLOADS_ACTION = "device_center_screen:action_camera_uploads"
    }
}