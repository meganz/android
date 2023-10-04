package mega.privacy.android.feature.sync.domain.entity

import mega.privacy.android.domain.entity.node.NodeId

internal class StalledIssue(
    val nodeId: NodeId,
    val localPath: String,
    val issueType: StallIssueType,
    val conflictName: String,
    val nodeName: String,
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