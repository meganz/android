package mega.privacy.android.feature.devicecenter.ui.mapper

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.backup.BackupInfoSubState
import mega.privacy.android.feature.devicecenter.R
import javax.inject.Inject

/**
 * UI Mapper that retrieves the appropriate Device Folder Error Message from a Device Folder's
 * [BackupInfoSubState]
 */
internal class DeviceFolderUINodeErrorMessageMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param errorSubState The corresponding [BackupInfoSubState]
     * @return A [StringRes] of the specific Error Message
     */
    operator fun invoke(errorSubState: BackupInfoSubState): Int = when (errorSubState) {
        BackupInfoSubState.NO_SYNC_ERROR -> R.string.device_center_list_view_item_sub_state_no_sync_error
        BackupInfoSubState.UNKNOWN_ERROR -> R.string.device_center_list_view_item_sub_state_message_unknown_error
        BackupInfoSubState.UNSUPPORTED_FILE_SYSTEM -> R.string.device_center_list_view_item_sub_state_unsupported_file_system
        BackupInfoSubState.INVALID_REMOTE_TYPE -> R.string.device_center_list_view_item_sub_state_invalid_remote_type
        BackupInfoSubState.INVALID_LOCAL_TYPE -> R.string.device_center_list_view_item_sub_state_invalid_local_type
        BackupInfoSubState.INITIAL_SCAN_FAILED -> R.string.device_center_list_view_item_sub_state_initial_scan_failed
        BackupInfoSubState.LOCAL_PATH_TEMPORARY_UNAVAILABLE -> R.string.device_center_list_view_item_sub_state_message_cannot_locate_local_drive_now
        BackupInfoSubState.LOCAL_PATH_UNAVAILABLE -> R.string.device_center_list_view_item_sub_state_message_cannot_locate_local_drive
        BackupInfoSubState.REMOTE_NODE_NOT_FOUND -> R.string.device_center_list_view_item_sub_state_remote_node_not_found
        BackupInfoSubState.STORAGE_OVERQUOTA -> R.string.device_center_list_view_item_sub_state_storage_overquota
        BackupInfoSubState.ACCOUNT_EXPIRED -> R.string.device_center_list_view_item_sub_state_account_expired
        BackupInfoSubState.FOREIGN_TARGET_OVERSTORAGE -> R.string.device_center_list_view_item_sub_state_foreign_target_overshare
        BackupInfoSubState.REMOTE_PATH_HAS_CHANGED -> R.string.device_center_list_view_item_sub_state_remote_path_has_changed
        BackupInfoSubState.SHARE_NON_FULL_ACCESS -> R.string.device_center_list_view_item_sub_state_share_non_full_access
        BackupInfoSubState.PUT_NODES_ERROR -> R.string.device_center_list_view_item_sub_state_put_nodes_error
        BackupInfoSubState.ACTIVE_SYNC_BELOW_PATH -> R.string.device_center_list_view_item_sub_state_active_sync_below_path
        BackupInfoSubState.VBOXSHAREDFOLDER_UNSUPPORTED -> R.string.device_center_list_view_item_sub_state_vboxsharedfolder_unsupported
        BackupInfoSubState.ACCOUNT_BLOCKED -> R.string.device_center_list_view_item_sub_state_account_blocked
        BackupInfoSubState.UNKNOWN_TEMPORARY_ERROR -> R.string.device_center_list_view_item_sub_state_unknown_temporary_error
        BackupInfoSubState.TOO_MANY_ACTION_PACKETS -> R.string.device_center_list_view_item_sub_state_too_many_action_packets
        BackupInfoSubState.LOGGED_OUT -> R.string.device_center_list_view_item_sub_state_logged_out
        BackupInfoSubState.MISSING_PARENT_NODE -> R.string.device_center_list_view_item_sub_state_missing_parent_node
        BackupInfoSubState.BACKUP_MODIFIED -> R.string.device_center_list_view_item_sub_state_message_folder_backup_issue_due_to_recent_changes
        BackupInfoSubState.BACKUP_SOURCE_NOT_BELOW_DRIVE -> R.string.device_center_list_view_item_sub_state_backup_source_not_below_drive
        BackupInfoSubState.SYNC_CONFIG_WRITE_FAILURE -> R.string.device_center_list_view_item_sub_state_message_folder_backup_issue
        BackupInfoSubState.ACTIVE_SYNC_SAME_PATH -> R.string.device_center_list_view_item_sub_state_active_sync_same_path
        BackupInfoSubState.COULD_NOT_MOVE_CLOUD_NODES -> R.string.device_center_list_view_item_sub_state_could_not_move_cloud_nodes
        BackupInfoSubState.COULD_NOT_CREATE_IGNORE_FILE -> R.string.device_center_list_view_item_sub_state_could_not_create_ignore_file
        BackupInfoSubState.SYNC_CONFIG_READ_FAILURE -> R.string.device_center_list_view_item_sub_state_sync_config_read_failure
        BackupInfoSubState.UNKNOWN_DRIVE_PATH -> R.string.device_center_list_view_item_sub_state_unknown_drive_path
        BackupInfoSubState.INVALID_SCAN_INTERVAL -> R.string.device_center_list_view_item_sub_state_invalid_scan_interval
        BackupInfoSubState.NOTIFICATION_SYSTEM_UNAVAILABLE -> R.string.device_center_list_view_item_sub_state_notification_system_unavailable
        BackupInfoSubState.UNABLE_TO_ADD_WATCH -> R.string.device_center_list_view_item_sub_state_unable_to_add_watch
        BackupInfoSubState.UNABLE_TO_RETRIEVE_ROOT_FSID -> R.string.device_center_list_view_item_sub_state_unable_to_retrieve_root_fsid
        BackupInfoSubState.UNABLE_TO_OPEN_DATABASE -> R.string.device_center_list_view_item_sub_state_unable_to_open_database
        BackupInfoSubState.INSUFFICIENT_DISK_SPACE -> R.string.device_center_list_view_item_sub_state_insufficient_disk_space
        BackupInfoSubState.FAILURE_ACCESSING_PERSISTENT_STORAGE -> R.string.device_center_list_view_item_sub_state_failure_accessing_persistent_storage
        BackupInfoSubState.LOCAL_FILESYSTEM_MISMATCH,
        BackupInfoSubState.MISMATCH_OF_ROOT_FSID,
        BackupInfoSubState.FILESYSTEM_FILE_IDS_ARE_UNSTABLE,
        BackupInfoSubState.FILESYSTEM_ID_UNAVAILABLE,
        -> {
            R.string.device_center_list_view_item_sub_state_message_something_went_wrong
        }

        BackupInfoSubState.REMOTE_NODE_MOVED_TO_RUBBISH,
        BackupInfoSubState.REMOTE_NODE_INSIDE_RUBBISH,
        -> {
            R.string.device_center_list_view_item_sub_state_message_node_in_rubbish_bin
        }

        BackupInfoSubState.ACTIVE_SYNC_ABOVE_PATH,
        BackupInfoSubState.LOCAL_PATH_SYNC_COLLISION,
        -> {
            R.string.device_center_list_view_item_sub_state_message_folder_backup_issue_due_to_being_inside_another_backed_up_folder
        }

        else -> R.string.device_center_list_view_item_sub_state_message_unknown_error
    }
}