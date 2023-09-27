package mega.privacy.android.feature.sync.ui.model

internal data class StalledIssueUiItem(
    val id: Long,
    val nodeId: Long,
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