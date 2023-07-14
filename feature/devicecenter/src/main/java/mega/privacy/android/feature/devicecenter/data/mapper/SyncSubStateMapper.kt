package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.data.entity.SyncSubState
import nz.mega.sdk.MegaSync.Error
import javax.inject.Inject

/**
 * Mapper that converts the [Int] value of [nz.mega.sdk.MegaBackupInfo.substate] into a corresponding
 * [SyncSubState]
 */
internal class SyncSubStateMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param sdkSubState The [Int] value from [nz.mega.sdk.MegaBackupInfo.substate]. The values can
     * be any of the enumerations found in [Error]
     * @return a corresponding [SyncSubState]
     */
    operator fun invoke(sdkSubState: Int) = when (sdkSubState) {
        Error.NO_SYNC_ERROR.swigValue() -> SyncSubState.NO_SYNC_ERROR
        Error.UNKNOWN_ERROR.swigValue() -> SyncSubState.UNKNOWN_ERROR
        Error.UNSUPPORTED_FILE_SYSTEM.swigValue() -> SyncSubState.UNSUPPORTED_FILE_SYSTEM
        Error.INVALID_REMOTE_TYPE.swigValue() -> SyncSubState.INVALID_REMOTE_TYPE
        Error.INVALID_LOCAL_TYPE.swigValue() -> SyncSubState.INVALID_LOCAL_TYPE
        Error.INITIAL_SCAN_FAILED.swigValue() -> SyncSubState.INITIAL_SCAN_FAILED
        Error.LOCAL_PATH_TEMPORARY_UNAVAILABLE.swigValue() -> SyncSubState.LOCAL_PATH_TEMPORARY_UNAVAILABLE
        Error.LOCAL_PATH_UNAVAILABLE.swigValue() -> SyncSubState.LOCAL_PATH_UNAVAILABLE
        Error.REMOTE_NODE_NOT_FOUND.swigValue() -> SyncSubState.REMOTE_NODE_NOT_FOUND
        Error.STORAGE_OVERQUOTA.swigValue() -> SyncSubState.STORAGE_OVERQUOTA
        Error.ACCOUNT_EXPIRED.swigValue() -> SyncSubState.ACCOUNT_EXPIRED
        Error.FOREIGN_TARGET_OVERSTORAGE.swigValue() -> SyncSubState.FOREIGN_TARGET_OVERSTORAGE
        Error.REMOTE_PATH_HAS_CHANGED.swigValue() -> SyncSubState.REMOTE_PATH_HAS_CHANGED
        Error.SHARE_NON_FULL_ACCESS.swigValue() -> SyncSubState.SHARE_NON_FULL_ACCESS
        Error.LOCAL_FILESYSTEM_MISMATCH.swigValue() -> SyncSubState.LOCAL_FILESYSTEM_MISMATCH
        Error.PUT_NODES_ERROR.swigValue() -> SyncSubState.PUT_NODES_ERROR
        Error.ACTIVE_SYNC_BELOW_PATH.swigValue() -> SyncSubState.ACTIVE_SYNC_BELOW_PATH
        Error.ACTIVE_SYNC_ABOVE_PATH.swigValue() -> SyncSubState.ACTIVE_SYNC_ABOVE_PATH
        Error.REMOTE_NODE_MOVED_TO_RUBBISH.swigValue() -> SyncSubState.REMOTE_NODE_MOVED_TO_RUBBISH
        Error.REMOTE_NODE_INSIDE_RUBBISH.swigValue() -> SyncSubState.REMOTE_NODE_INSIDE_RUBBISH
        Error.VBOXSHAREDFOLDER_UNSUPPORTED.swigValue() -> SyncSubState.VBOXSHAREDFOLDER_UNSUPPORTED
        Error.LOCAL_PATH_SYNC_COLLISION.swigValue() -> SyncSubState.LOCAL_PATH_SYNC_COLLISION
        Error.ACCOUNT_BLOCKED.swigValue() -> SyncSubState.ACCOUNT_BLOCKED
        Error.UNKNOWN_TEMPORARY_ERROR.swigValue() -> SyncSubState.UNKNOWN_TEMPORARY_ERROR
        Error.TOO_MANY_ACTION_PACKETS.swigValue() -> SyncSubState.TOO_MANY_ACTION_PACKETS
        Error.LOGGED_OUT.swigValue() -> SyncSubState.LOGGED_OUT
        Error.WHOLE_ACCOUNT_REFETCHED.swigValue() -> SyncSubState.WHOLE_ACCOUNT_REFETCHED
        Error.MISSING_PARENT_NODE.swigValue() -> SyncSubState.MISSING_PARENT_NODE
        Error.BACKUP_MODIFIED.swigValue() -> SyncSubState.BACKUP_MODIFIED
        Error.BACKUP_SOURCE_NOT_BELOW_DRIVE.swigValue() -> SyncSubState.BACKUP_SOURCE_NOT_BELOW_DRIVE
        Error.SYNC_CONFIG_WRITE_FAILURE.swigValue() -> SyncSubState.SYNC_CONFIG_WRITE_FAILURE
        Error.ACTIVE_SYNC_SAME_PATH.swigValue() -> SyncSubState.ACTIVE_SYNC_SAME_PATH
        Error.COULD_NOT_MOVE_CLOUD_NODES.swigValue() -> SyncSubState.COULD_NOT_MOVE_CLOUD_NODES
        Error.COULD_NOT_CREATE_IGNORE_FILE.swigValue() -> SyncSubState.COULD_NOT_CREATE_IGNORE_FILE
        Error.SYNC_CONFIG_READ_FAILURE.swigValue() -> SyncSubState.SYNC_CONFIG_READ_FAILURE
        Error.UNKNOWN_DRIVE_PATH.swigValue() -> SyncSubState.UNKNOWN_DRIVE_PATH
        Error.INVALID_SCAN_INTERVAL.swigValue() -> SyncSubState.INVALID_SCAN_INTERVAL
        Error.NOTIFICATION_SYSTEM_UNAVAILABLE.swigValue() -> SyncSubState.NOTIFICATION_SYSTEM_UNAVAILABLE
        Error.UNABLE_TO_ADD_WATCH.swigValue() -> SyncSubState.UNABLE_TO_ADD_WATCH
        Error.UNABLE_TO_RETRIEVE_ROOT_FSID.swigValue() -> SyncSubState.UNABLE_TO_RETRIEVE_ROOT_FSID
        Error.UNABLE_TO_OPEN_DATABASE.swigValue() -> SyncSubState.UNABLE_TO_OPEN_DATABASE
        Error.INSUFFICIENT_DISK_SPACE.swigValue() -> SyncSubState.INSUFFICIENT_DISK_SPACE
        Error.FAILURE_ACCESSING_PERSISTENT_STORAGE.swigValue() -> SyncSubState.FAILURE_ACCESSING_PERSISTENT_STORAGE
        Error.MISMATCH_OF_ROOT_FSID.swigValue() -> SyncSubState.MISMATCH_OF_ROOT_FSID
        Error.FILESYSTEM_FILE_IDS_ARE_UNSTABLE.swigValue() -> SyncSubState.FILESYSTEM_FILE_IDS_ARE_UNSTABLE
        else -> throw IllegalArgumentException("The sync sub state value $sdkSubState is invalid")
    }
}