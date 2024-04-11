package mega.privacy.android.shared.sync

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.shared.resources.R
import javax.inject.Inject

/**
 * UI Mapper that retrieves the appropriate Device Folder Error Message from a Device Folder's
 * [SyncError]
 */
class DeviceFolderUINodeErrorMessageMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param errorSubState The corresponding [SyncError]
     * @return A [StringRes] of the specific Error Message
     */
    operator fun invoke(errorSubState: SyncError?): Int? = when (errorSubState) {
        SyncError.NO_SYNC_ERROR -> null
        SyncError.UNKNOWN_ERROR -> R.string.general_sync_message_unknown_error
        SyncError.UNSUPPORTED_FILE_SYSTEM -> R.string.general_sync_unsupported_file_system
        SyncError.INVALID_REMOTE_TYPE -> R.string.general_sync_invalid_remote_type
        SyncError.INVALID_LOCAL_TYPE -> R.string.general_sync_invalid_local_type
        SyncError.INITIAL_SCAN_FAILED -> R.string.general_sync_initial_scan_failed
        SyncError.LOCAL_PATH_TEMPORARY_UNAVAILABLE -> R.string.general_sync_message_cannot_locate_local_drive_now
        SyncError.LOCAL_PATH_UNAVAILABLE -> R.string.general_sync_message_cannot_locate_local_drive
        SyncError.REMOTE_NODE_NOT_FOUND -> R.string.general_sync_remote_node_not_found
        SyncError.STORAGE_OVERQUOTA -> R.string.general_sync_storage_overquota
        SyncError.ACCOUNT_EXPIRED -> R.string.general_sync_account_expired
        SyncError.FOREIGN_TARGET_OVERSTORAGE -> R.string.general_sync_foreign_target_overshare
        SyncError.REMOTE_PATH_HAS_CHANGED -> R.string.general_sync_remote_path_has_changed
        SyncError.SHARE_NON_FULL_ACCESS -> R.string.general_sync_share_non_full_access
        SyncError.LOCAL_FILESYSTEM_MISMATCH -> R.string.general_sync_local_filesystem_mismatch
        SyncError.PUT_NODES_ERROR -> R.string.general_sync_put_nodes_error
        SyncError.ACTIVE_SYNC_BELOW_PATH -> R.string.general_sync_active_sync_below_path
        SyncError.VBOXSHAREDFOLDER_UNSUPPORTED -> R.string.general_sync_vboxsharedfolder_unsupported
        SyncError.ACCOUNT_BLOCKED -> R.string.general_sync_account_blocked
        SyncError.UNKNOWN_TEMPORARY_ERROR -> R.string.general_sync_unknown_temporary_error
        SyncError.TOO_MANY_ACTION_PACKETS -> R.string.general_sync_too_many_action_packets
        SyncError.LOGGED_OUT -> R.string.general_sync_logged_out
        SyncError.BACKUP_MODIFIED -> R.string.general_sync_message_folder_backup_issue_due_to_recent_changes
        SyncError.BACKUP_SOURCE_NOT_BELOW_DRIVE -> R.string.general_sync_backup_source_not_below_drive
        SyncError.SYNC_CONFIG_WRITE_FAILURE -> R.string.general_sync_message_folder_backup_issue
        SyncError.ACTIVE_SYNC_SAME_PATH -> R.string.general_sync_active_sync_same_path
        SyncError.COULD_NOT_MOVE_CLOUD_NODES -> R.string.general_sync_could_not_move_cloud_nodes
        SyncError.COULD_NOT_CREATE_IGNORE_FILE -> R.string.general_sync_could_not_create_ignore_file
        SyncError.SYNC_CONFIG_READ_FAILURE -> R.string.general_sync_config_read_failure
        SyncError.UNKNOWN_DRIVE_PATH -> R.string.general_sync_unknown_drive_path
        SyncError.INVALID_SCAN_INTERVAL -> R.string.general_sync_invalid_scan_interval
        SyncError.NOTIFICATION_SYSTEM_UNAVAILABLE -> R.string.general_sync_notification_system_unavailable
        SyncError.UNABLE_TO_ADD_WATCH -> R.string.general_sync_unable_to_add_watch
        SyncError.INSUFFICIENT_DISK_SPACE -> R.string.general_sync_insufficient_disk_space

        SyncError.UNABLE_TO_RETRIEVE_ROOT_FSID,
        SyncError.FAILURE_ACCESSING_PERSISTENT_STORAGE,
        -> R.string.general_sync_unable_to_retrieve_root_fsid

        SyncError.UNABLE_TO_OPEN_DATABASE,
        SyncError.MISMATCH_OF_ROOT_FSID,
        SyncError.FILESYSTEM_FILE_IDS_ARE_UNSTABLE,
        SyncError.FILESYSTEM_ID_UNAVAILABLE,
        -> R.string.general_sync_message_folder_backup_issue

        SyncError.REMOTE_NODE_MOVED_TO_RUBBISH,
        SyncError.REMOTE_NODE_INSIDE_RUBBISH,
        -> R.string.general_sync_message_node_in_rubbish_bin

        SyncError.ACTIVE_SYNC_ABOVE_PATH,
        SyncError.LOCAL_PATH_SYNC_COLLISION,
        -> R.string.general_sync_message_folder_backup_issue_due_to_being_inside_another_backed_up_folder

        else -> R.string.general_sync_message_unknown_error
    }
}