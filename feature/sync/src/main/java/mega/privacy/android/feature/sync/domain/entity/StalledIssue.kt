package mega.privacy.android.feature.sync.domain.entity

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Stalled issue entity
 *
 * @property nodeIds List of node ids
 * @property localPaths List of local paths
 * @property issueType Issue type
 * @property conflictName Conflict name
 * @property nodeNames List of node names
 */
data class StalledIssue(
    val nodeIds: List<NodeId>,
    val localPaths: List<String>,
    val issueType: StallIssueType,
    val conflictName: String,
    val nodeNames: List<String>,
)


/**
 * Stall issue type
 *
 * Different types of stalled issues handled
 */
enum class StallIssueType {
    /**
     * No Reason
     */
    NoReason,

    /**
     * File Issue
     */
    FileIssue,

    /**
     * Move or rename cannot be occurred
     */
    MoveOrRenameCannotOccur,

    /**
     * Delete or rename cannot be occurred
     */
    DeleteOrMoveWaitingOnScanning,

    /**
     * Delete waiting on moves
     */
    DeleteWaitingOnMoves,

    /**
     * Upload issue
     */
    UploadIssue,

    /**
     * Download issue
     */
    DownloadIssue,

    /**
     * Cannot create folder
     */
    CannotCreateFolder,

    /**
     * Cannot perform deletion
     */
    CannotPerformDeletion,

    /**
     * Sync item exceeds supported tree depth
     */
    SyncItemExceedsSupportedTreeDepth,

    /**
     * Folder matched against file
     */
    FolderMatchedAgainstFile,

    /**
     * Local and remote changed since last synced state
     * User must choose which should be selected
     */
    LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,

    /**
     * Local and remote previously not synced
     * User must choose which should be selected
     */
    LocalAndRemotePreviouslyNotSyncedDifferUserMustChoose,

    /**
     * Names will clash if synced
     */
    NamesWouldClashWhenSynced,

    /**
     * Sync stall reason last plus one
     */
    SyncStallReasonLastPlusOne
}