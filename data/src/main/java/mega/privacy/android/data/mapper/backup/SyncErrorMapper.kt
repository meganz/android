package mega.privacy.android.data.mapper.backup

import mega.privacy.android.domain.entity.sync.SyncError
import nz.mega.sdk.MegaSync.Error
import javax.inject.Inject

/**
 * Mapper that converts the [Int] value of [nz.mega.sdk.MegaBackupInfo.substate] into a corresponding
 * [SyncError]
 */
class SyncErrorMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param sdkSubState The [Int] value from [nz.mega.sdk.MegaBackupInfo.substate]. The values can
     * be any of the enumerations found in [Error]
     * @return a corresponding [SyncError]
     */
    operator fun invoke(sdkSubState: Int) = when (sdkSubState) {
        Error.NO_SYNC_ERROR.swigValue() -> SyncError.NO_SYNC_ERROR
        Error.UNKNOWN_ERROR.swigValue() -> SyncError.UNKNOWN_ERROR
        Error.UNSUPPORTED_FILE_SYSTEM.swigValue() -> SyncError.UNSUPPORTED_FILE_SYSTEM
        Error.INVALID_REMOTE_TYPE.swigValue() -> SyncError.INVALID_REMOTE_TYPE
        Error.INVALID_LOCAL_TYPE.swigValue() -> SyncError.INVALID_LOCAL_TYPE
        Error.INITIAL_SCAN_FAILED.swigValue() -> SyncError.INITIAL_SCAN_FAILED
        Error.LOCAL_PATH_TEMPORARY_UNAVAILABLE.swigValue() -> SyncError.LOCAL_PATH_TEMPORARY_UNAVAILABLE
        Error.LOCAL_PATH_UNAVAILABLE.swigValue() -> SyncError.LOCAL_PATH_UNAVAILABLE
        Error.REMOTE_NODE_NOT_FOUND.swigValue() -> SyncError.REMOTE_NODE_NOT_FOUND
        Error.STORAGE_OVERQUOTA.swigValue() -> SyncError.STORAGE_OVERQUOTA
        Error.ACCOUNT_EXPIRED.swigValue() -> SyncError.ACCOUNT_EXPIRED
        Error.FOREIGN_TARGET_OVERSTORAGE.swigValue() -> SyncError.FOREIGN_TARGET_OVERSTORAGE
        Error.REMOTE_PATH_HAS_CHANGED.swigValue() -> SyncError.REMOTE_PATH_HAS_CHANGED
        Error.SHARE_NON_FULL_ACCESS.swigValue() -> SyncError.SHARE_NON_FULL_ACCESS
        Error.LOCAL_FILESYSTEM_MISMATCH.swigValue() -> SyncError.LOCAL_FILESYSTEM_MISMATCH
        Error.PUT_NODES_ERROR.swigValue() -> SyncError.PUT_NODES_ERROR
        Error.ACTIVE_SYNC_BELOW_PATH.swigValue() -> SyncError.ACTIVE_SYNC_BELOW_PATH
        Error.ACTIVE_SYNC_ABOVE_PATH.swigValue() -> SyncError.ACTIVE_SYNC_ABOVE_PATH
        Error.REMOTE_NODE_MOVED_TO_RUBBISH.swigValue() -> SyncError.REMOTE_NODE_MOVED_TO_RUBBISH
        Error.REMOTE_NODE_INSIDE_RUBBISH.swigValue() -> SyncError.REMOTE_NODE_INSIDE_RUBBISH
        Error.VBOXSHAREDFOLDER_UNSUPPORTED.swigValue() -> SyncError.VBOXSHAREDFOLDER_UNSUPPORTED
        Error.LOCAL_PATH_SYNC_COLLISION.swigValue() -> SyncError.LOCAL_PATH_SYNC_COLLISION
        Error.ACCOUNT_BLOCKED.swigValue() -> SyncError.ACCOUNT_BLOCKED
        Error.UNKNOWN_TEMPORARY_ERROR.swigValue() -> SyncError.UNKNOWN_TEMPORARY_ERROR
        Error.TOO_MANY_ACTION_PACKETS.swigValue() -> SyncError.TOO_MANY_ACTION_PACKETS
        Error.LOGGED_OUT.swigValue() -> SyncError.LOGGED_OUT
        Error.BACKUP_MODIFIED.swigValue() -> SyncError.BACKUP_MODIFIED
        Error.BACKUP_SOURCE_NOT_BELOW_DRIVE.swigValue() -> SyncError.BACKUP_SOURCE_NOT_BELOW_DRIVE
        Error.SYNC_CONFIG_WRITE_FAILURE.swigValue() -> SyncError.SYNC_CONFIG_WRITE_FAILURE
        Error.ACTIVE_SYNC_SAME_PATH.swigValue() -> SyncError.ACTIVE_SYNC_SAME_PATH
        Error.COULD_NOT_MOVE_CLOUD_NODES.swigValue() -> SyncError.COULD_NOT_MOVE_CLOUD_NODES
        Error.COULD_NOT_CREATE_IGNORE_FILE.swigValue() -> SyncError.COULD_NOT_CREATE_IGNORE_FILE
        Error.SYNC_CONFIG_READ_FAILURE.swigValue() -> SyncError.SYNC_CONFIG_READ_FAILURE
        Error.UNKNOWN_DRIVE_PATH.swigValue() -> SyncError.UNKNOWN_DRIVE_PATH
        Error.INVALID_SCAN_INTERVAL.swigValue() -> SyncError.INVALID_SCAN_INTERVAL
        Error.NOTIFICATION_SYSTEM_UNAVAILABLE.swigValue() -> SyncError.NOTIFICATION_SYSTEM_UNAVAILABLE
        Error.UNABLE_TO_ADD_WATCH.swigValue() -> SyncError.UNABLE_TO_ADD_WATCH
        Error.UNABLE_TO_RETRIEVE_ROOT_FSID.swigValue() -> SyncError.UNABLE_TO_RETRIEVE_ROOT_FSID
        Error.UNABLE_TO_OPEN_DATABASE.swigValue() -> SyncError.UNABLE_TO_OPEN_DATABASE
        Error.INSUFFICIENT_DISK_SPACE.swigValue() -> SyncError.INSUFFICIENT_DISK_SPACE
        Error.FAILURE_ACCESSING_PERSISTENT_STORAGE.swigValue() -> SyncError.FAILURE_ACCESSING_PERSISTENT_STORAGE
        Error.MISMATCH_OF_ROOT_FSID.swigValue() -> SyncError.MISMATCH_OF_ROOT_FSID
        Error.FILESYSTEM_FILE_IDS_ARE_UNSTABLE.swigValue() -> SyncError.FILESYSTEM_FILE_IDS_ARE_UNSTABLE
        Error.FILESYSTEM_ID_UNAVAILABLE.swigValue() -> SyncError.FILESYSTEM_ID_UNAVAILABLE
        else -> SyncError.UNKNOWN_BACKUP_INFO_SUB_STATE
    }
}