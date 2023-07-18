package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoSubState
import nz.mega.sdk.MegaSync.Error
import javax.inject.Inject

/**
 * Mapper that converts the [Int] value of [nz.mega.sdk.MegaBackupInfo.substate] into a corresponding
 * [BackupInfoSubState]
 */
internal class BackupInfoSubStateMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param sdkSubState The [Int] value from [nz.mega.sdk.MegaBackupInfo.substate]. The values can
     * be any of the enumerations found in [Error]
     * @return a corresponding [BackupInfoSubState]
     */
    operator fun invoke(sdkSubState: Int) = when (sdkSubState) {
        Error.NO_SYNC_ERROR.swigValue() -> BackupInfoSubState.NO_SYNC_ERROR
        Error.UNKNOWN_ERROR.swigValue() -> BackupInfoSubState.UNKNOWN_ERROR
        Error.UNSUPPORTED_FILE_SYSTEM.swigValue() -> BackupInfoSubState.UNSUPPORTED_FILE_SYSTEM
        Error.INVALID_REMOTE_TYPE.swigValue() -> BackupInfoSubState.INVALID_REMOTE_TYPE
        Error.INVALID_LOCAL_TYPE.swigValue() -> BackupInfoSubState.INVALID_LOCAL_TYPE
        Error.INITIAL_SCAN_FAILED.swigValue() -> BackupInfoSubState.INITIAL_SCAN_FAILED
        Error.LOCAL_PATH_TEMPORARY_UNAVAILABLE.swigValue() -> BackupInfoSubState.LOCAL_PATH_TEMPORARY_UNAVAILABLE
        Error.LOCAL_PATH_UNAVAILABLE.swigValue() -> BackupInfoSubState.LOCAL_PATH_UNAVAILABLE
        Error.REMOTE_NODE_NOT_FOUND.swigValue() -> BackupInfoSubState.REMOTE_NODE_NOT_FOUND
        Error.STORAGE_OVERQUOTA.swigValue() -> BackupInfoSubState.STORAGE_OVERQUOTA
        Error.ACCOUNT_EXPIRED.swigValue() -> BackupInfoSubState.ACCOUNT_EXPIRED
        Error.FOREIGN_TARGET_OVERSTORAGE.swigValue() -> BackupInfoSubState.FOREIGN_TARGET_OVERSTORAGE
        Error.REMOTE_PATH_HAS_CHANGED.swigValue() -> BackupInfoSubState.REMOTE_PATH_HAS_CHANGED
        Error.SHARE_NON_FULL_ACCESS.swigValue() -> BackupInfoSubState.SHARE_NON_FULL_ACCESS
        Error.LOCAL_FILESYSTEM_MISMATCH.swigValue() -> BackupInfoSubState.LOCAL_FILESYSTEM_MISMATCH
        Error.PUT_NODES_ERROR.swigValue() -> BackupInfoSubState.PUT_NODES_ERROR
        Error.ACTIVE_SYNC_BELOW_PATH.swigValue() -> BackupInfoSubState.ACTIVE_SYNC_BELOW_PATH
        Error.ACTIVE_SYNC_ABOVE_PATH.swigValue() -> BackupInfoSubState.ACTIVE_SYNC_ABOVE_PATH
        Error.REMOTE_NODE_MOVED_TO_RUBBISH.swigValue() -> BackupInfoSubState.REMOTE_NODE_MOVED_TO_RUBBISH
        Error.REMOTE_NODE_INSIDE_RUBBISH.swigValue() -> BackupInfoSubState.REMOTE_NODE_INSIDE_RUBBISH
        Error.VBOXSHAREDFOLDER_UNSUPPORTED.swigValue() -> BackupInfoSubState.VBOXSHAREDFOLDER_UNSUPPORTED
        Error.LOCAL_PATH_SYNC_COLLISION.swigValue() -> BackupInfoSubState.LOCAL_PATH_SYNC_COLLISION
        Error.ACCOUNT_BLOCKED.swigValue() -> BackupInfoSubState.ACCOUNT_BLOCKED
        Error.UNKNOWN_TEMPORARY_ERROR.swigValue() -> BackupInfoSubState.UNKNOWN_TEMPORARY_ERROR
        Error.TOO_MANY_ACTION_PACKETS.swigValue() -> BackupInfoSubState.TOO_MANY_ACTION_PACKETS
        Error.LOGGED_OUT.swigValue() -> BackupInfoSubState.LOGGED_OUT
        Error.WHOLE_ACCOUNT_REFETCHED.swigValue() -> BackupInfoSubState.WHOLE_ACCOUNT_REFETCHED
        Error.MISSING_PARENT_NODE.swigValue() -> BackupInfoSubState.MISSING_PARENT_NODE
        Error.BACKUP_MODIFIED.swigValue() -> BackupInfoSubState.BACKUP_MODIFIED
        Error.BACKUP_SOURCE_NOT_BELOW_DRIVE.swigValue() -> BackupInfoSubState.BACKUP_SOURCE_NOT_BELOW_DRIVE
        Error.SYNC_CONFIG_WRITE_FAILURE.swigValue() -> BackupInfoSubState.SYNC_CONFIG_WRITE_FAILURE
        Error.ACTIVE_SYNC_SAME_PATH.swigValue() -> BackupInfoSubState.ACTIVE_SYNC_SAME_PATH
        Error.COULD_NOT_MOVE_CLOUD_NODES.swigValue() -> BackupInfoSubState.COULD_NOT_MOVE_CLOUD_NODES
        Error.COULD_NOT_CREATE_IGNORE_FILE.swigValue() -> BackupInfoSubState.COULD_NOT_CREATE_IGNORE_FILE
        Error.SYNC_CONFIG_READ_FAILURE.swigValue() -> BackupInfoSubState.SYNC_CONFIG_READ_FAILURE
        Error.UNKNOWN_DRIVE_PATH.swigValue() -> BackupInfoSubState.UNKNOWN_DRIVE_PATH
        Error.INVALID_SCAN_INTERVAL.swigValue() -> BackupInfoSubState.INVALID_SCAN_INTERVAL
        Error.NOTIFICATION_SYSTEM_UNAVAILABLE.swigValue() -> BackupInfoSubState.NOTIFICATION_SYSTEM_UNAVAILABLE
        Error.UNABLE_TO_ADD_WATCH.swigValue() -> BackupInfoSubState.UNABLE_TO_ADD_WATCH
        Error.UNABLE_TO_RETRIEVE_ROOT_FSID.swigValue() -> BackupInfoSubState.UNABLE_TO_RETRIEVE_ROOT_FSID
        Error.UNABLE_TO_OPEN_DATABASE.swigValue() -> BackupInfoSubState.UNABLE_TO_OPEN_DATABASE
        Error.INSUFFICIENT_DISK_SPACE.swigValue() -> BackupInfoSubState.INSUFFICIENT_DISK_SPACE
        Error.FAILURE_ACCESSING_PERSISTENT_STORAGE.swigValue() -> BackupInfoSubState.FAILURE_ACCESSING_PERSISTENT_STORAGE
        Error.MISMATCH_OF_ROOT_FSID.swigValue() -> BackupInfoSubState.MISMATCH_OF_ROOT_FSID
        Error.FILESYSTEM_FILE_IDS_ARE_UNSTABLE.swigValue() -> BackupInfoSubState.FILESYSTEM_FILE_IDS_ARE_UNSTABLE
        else -> throw IllegalArgumentException("The backup sub state value $sdkSubState is invalid")
    }
}