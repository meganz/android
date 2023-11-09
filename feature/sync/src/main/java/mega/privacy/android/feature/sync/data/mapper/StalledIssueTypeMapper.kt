package mega.privacy.android.feature.sync.data.mapper

import mega.privacy.android.feature.sync.data.mock.MegaSyncStall
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.CannotCreateFolder
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.CannotPerformDeletion
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.DeleteOrMoveWaitingOnScanning
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.DeleteWaitingOnMoves
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.DownloadIssue
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.FileIssue
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.FolderMatchedAgainstFile
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.LocalAndRemotePreviouslyUnsyncedDiffer_userMustChoose
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.MoveOrRenameCannotOccur
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.NamesWouldClashWhenSynced
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.NoReason
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.SyncItemExceedsSupportedTreeDepth
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.SyncStallReason_LastPlusOne
import mega.privacy.android.feature.sync.data.mock.MegaSyncStall.SyncStallReason.UploadIssue
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
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