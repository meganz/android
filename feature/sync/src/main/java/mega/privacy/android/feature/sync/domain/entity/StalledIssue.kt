package mega.privacy.android.feature.sync.domain.entity

import mega.privacy.android.domain.entity.node.NodeId

internal data class StalledIssue(
    val nodeIds: List<NodeId>,
    val localPaths: List<String>,
    val issueType: StallIssueType,
    val conflictName: String,
    val nodeNames: List<String>,
)

internal enum class StallIssueType {
    NoReason,
    FileIssue,
    MoveOrRenameCannotOccur,
    DeleteOrMoveWaitingOnScanning,
    DeleteWaitingOnMoves,
    UploadIssue,
    DownloadIssue,
    CannotCreateFolder,
    CannotPerformDeletion,
    SyncItemExceedsSupportedTreeDepth,
    FolderMatchedAgainstFile,
    LocalAndRemoteChangedSinceLastSyncedState_userMustChoose,
    LocalAndRemotePreviouslyUnsyncedDiffer_userMustChoose,
    NamesWouldClashWhenSynced,
    SyncStallReason_LastPlusOne
}