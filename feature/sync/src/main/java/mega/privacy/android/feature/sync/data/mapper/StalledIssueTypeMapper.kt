package mega.privacy.android.feature.sync.data.mapper

import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import nz.mega.sdk.MegaSyncStall
import nz.mega.sdk.MegaSyncStall.SyncStallReason.CannotCreateFolder
import nz.mega.sdk.MegaSyncStall.SyncStallReason.CannotPerformDeletion
import nz.mega.sdk.MegaSyncStall.SyncStallReason.DeleteOrMoveWaitingOnScanning
import nz.mega.sdk.MegaSyncStall.SyncStallReason.DeleteWaitingOnMoves
import nz.mega.sdk.MegaSyncStall.SyncStallReason.DownloadIssue
import nz.mega.sdk.MegaSyncStall.SyncStallReason.FileIssue
import nz.mega.sdk.MegaSyncStall.SyncStallReason.FolderMatchedAgainstFile
import nz.mega.sdk.MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose
import nz.mega.sdk.MegaSyncStall.SyncStallReason.LocalAndRemotePreviouslyUnsyncedDiffer_userMustChoose
import nz.mega.sdk.MegaSyncStall.SyncStallReason.MoveOrRenameCannotOccur
import nz.mega.sdk.MegaSyncStall.SyncStallReason.NamesWouldClashWhenSynced
import nz.mega.sdk.MegaSyncStall.SyncStallReason.NoReason
import nz.mega.sdk.MegaSyncStall.SyncStallReason.SyncItemExceedsSupportedTreeDepth
import nz.mega.sdk.MegaSyncStall.SyncStallReason.SyncStallReason_LastPlusOne
import nz.mega.sdk.MegaSyncStall.SyncStallReason.UploadIssue
import javax.inject.Inject

internal class StalledIssueTypeMapper @Inject constructor() {

    operator fun invoke(reason: MegaSyncStall.SyncStallReason): StallIssueType = when (reason) {
        NoReason -> StallIssueType.NoReason
        FileIssue -> StallIssueType.FileIssue
        MoveOrRenameCannotOccur -> StallIssueType.MoveOrRenameCannotOccur
        DeleteOrMoveWaitingOnScanning -> StallIssueType.DeleteOrMoveWaitingOnScanning
        DeleteWaitingOnMoves -> StallIssueType.DeleteWaitingOnMoves
        UploadIssue -> StallIssueType.UploadIssue
        DownloadIssue -> StallIssueType.DownloadIssue
        CannotCreateFolder -> StallIssueType.CannotCreateFolder
        CannotPerformDeletion -> StallIssueType.CannotPerformDeletion
        SyncItemExceedsSupportedTreeDepth -> StallIssueType.SyncItemExceedsSupportedTreeDepth
        FolderMatchedAgainstFile -> StallIssueType.FolderMatchedAgainstFile
        LocalAndRemoteChangedSinceLastSyncedState_userMustChoose -> StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose
        LocalAndRemotePreviouslyUnsyncedDiffer_userMustChoose -> StallIssueType.LocalAndRemotePreviouslyNotSyncedDifferUserMustChoose
        NamesWouldClashWhenSynced -> StallIssueType.NamesWouldClashWhenSynced
        SyncStallReason_LastPlusOne -> StallIssueType.SyncStallReasonLastPlusOne
        else -> StallIssueType.NoReason
    }
}