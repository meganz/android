package mega.privacy.android.domain.entity.backup

/**
 * Enum class representing the different Backup Sub States mapped from
 * nz.mega.sdk.MegaBackupInfo.substate, which also references nz.mega.sdk.MegaSync.Error
 */
enum class BackupInfoSubState {

    /**
     * Represents a Sub State that has no Error
     */
    NO_SYNC_ERROR,

    /**
     * Represents an Unknown Error Sub State
     */
    UNKNOWN_ERROR,

    /**
     * Represents a Sub State in which the File System type is not supported
     */
    UNSUPPORTED_FILE_SYSTEM,

    /**
     * Represents a Sub State in which the Remote Type is not a Folder that can be synced
     */
    INVALID_REMOTE_TYPE,

    /**
     * Represents a Sub State in which the Local Path does not refer to a Folder
     */
    INVALID_LOCAL_TYPE,

    /**
     * Represents an Initial Scan Failed Sub State
     */
    INITIAL_SCAN_FAILED,

    /**
     * Represents a Local Path Temporary Unavailable Sub State
     */
    LOCAL_PATH_TEMPORARY_UNAVAILABLE,

    /**
     * Represents a Local Path Unavailable Sub State
     */
    LOCAL_PATH_UNAVAILABLE,

    /**
     * Represents a Sub State in which the Remote Node no longer exists
     */
    REMOTE_NODE_NOT_FOUND,

    /**
     * Represents a Sub State in which the Account hit a Storage Overquota
     */
    STORAGE_OVERQUOTA,

    /**
     * Represents an Account Expired Sub State
     */
    ACCOUNT_EXPIRED,

    /**
     * Represents a Sub State in which the Sync Transfer fails (Upload into Incoming Shares whose
     * Account is Overquota)
     */
    FOREIGN_TARGET_OVERSTORAGE,

    /**
     * Represents a Remote Path has Changed Sub State
     */
    REMOTE_PATH_HAS_CHANGED,

    /**
     * Represents a Sub State in which the existing Incoming Share Sync or part thereof lost full
     * access
     */
    SHARE_NON_FULL_ACCESS,

    /**
     * Represents a Sub State in which the File System Fingerprint does not match the one stored for
     * synchronization
     */
    LOCAL_FILESYSTEM_MISMATCH,

    /**
     * Represents a Sub State in which an error occurred when processing PUT Nodes
     */
    PUT_NODES_ERROR,

    /**
     * Represents a Sub State in which there is a Synced Node below the path to be synced
     */
    ACTIVE_SYNC_BELOW_PATH,

    /**
     * Represents a Sub State in which there is a Synced Node above the path to be synced
     */
    ACTIVE_SYNC_ABOVE_PATH,

    /**
     * Represents a Remote Node Moved to Rubbish Sub State
     */
    REMOTE_NODE_MOVED_TO_RUBBISH,

    /**
     * Represents a Sub State in which the Remote Node was attempted to be added in Rubbish
     */
    REMOTE_NODE_INSIDE_RUBBISH,

    /**
     * Represents a Sub State in which an unsupported VBoxSharedFolderFS was found
     */
    VBOXSHAREDFOLDER_UNSUPPORTED,

    /**
     * Represents a Sub State in which the Local Path includes a Synced Path or is included in one
     */
    LOCAL_PATH_SYNC_COLLISION,

    /**
     * Represents an Account Blocked Sub State
     */
    ACCOUNT_BLOCKED,

    /**
     * Represents an Unknown Temporary Error Sub State
     */
    UNKNOWN_TEMPORARY_ERROR,

    /**
     * Represents a Sub State in which too many Account changes were found and the Local State is
     * discarded
     */
    TOO_MANY_ACTION_PACKETS,

    /**
     * Represents a Logged Out Sub State
     */
    LOGGED_OUT,

    /**
     * Represents a Sub State in which setting a new Parent to a Parent whose Local Node is missing
     * its corresponding Node cross reference
     */
    MISSING_PARENT_NODE,

    /**
     * Represents a Sub State in which the Backup has been externally modified
     */
    BACKUP_MODIFIED,

    /**
     * Represents a Backup Source Not Below Drive Sub State
     */
    BACKUP_SOURCE_NOT_BELOW_DRIVE,

    /**
     * Represents a Sub State in which the Sync Config cannot be written to the Disk
     */
    SYNC_CONFIG_WRITE_FAILURE,

    /**
     * Represents a Sub State in which there is a synced Node at the path to be synced
     */
    ACTIVE_SYNC_SAME_PATH,

    /**
     * Represents a Sub State in which the rename() functionality failed
     */
    COULD_NOT_MOVE_CLOUD_NODES,

    /**
     * Represents a Sub State in which a Sync's initial ignore file cannot be created
     */
    COULD_NOT_CREATE_IGNORE_FILE,

    /**
     * Represents a Sub State in which the Sync Configurations from disk cannot be read
     */
    SYNC_CONFIG_READ_FAILURE,

    /**
     * Represents a Sub State in which the Sync Drive Path is unknown
     */
    UNKNOWN_DRIVE_PATH,

    /**
     * Represents a Sub State in which the User specified an invalid scan interval
     */
    INVALID_SCAN_INTERVAL,

    /**
     * Represents a Sub State in which the File System Notification subsystem has encountered an
     * unrecoverable error
     */
    NOTIFICATION_SYSTEM_UNAVAILABLE,

    /**
     * Represents a Sub State in which the File System Watch cannot be added
     */
    UNABLE_TO_ADD_WATCH,

    /**
     * Represents a Sub State in which the Sync Root FSID cannot be retrieved
     */
    UNABLE_TO_RETRIEVE_ROOT_FSID,

    /**
     * Represents a Sub State in which the State Cache Database cannot be opened
     */
    UNABLE_TO_OPEN_DATABASE,

    /**
     * Represents a Sub State in which there is insufficient Disk Space for downloads
     */
    INSUFFICIENT_DISK_SPACE,

    /**
     * Represents a Failure Accessing to Persistent Storage Sub State
     */
    FAILURE_ACCESSING_PERSISTENT_STORAGE,

    /**
     * Represents a Sub State in which the Sync Root FSID has changed
     */
    MISMATCH_OF_ROOT_FSID,

    /**
     * Represents a Sub State in which the FSID of a File in an exFAT drive changes spontaneously
     * and frequently
     */
    FILESYSTEM_FILE_IDS_ARE_UNSTABLE,

    /**
     * Represents a Sub State in which the FSID of a File in an exFAT drive is unavailable
     */
    FILESYSTEM_ID_UNAVAILABLE,

    /**
     * Represents an Unknown Sub State. This is the default value assigned when there is no matching
     * SDK value
     */
    UNKNOWN_BACKUP_INFO_SUB_STATE,
}