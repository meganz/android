package mega.privacy.android.feature.sync.ui.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import javax.inject.Inject

internal class StalledIssueTypeToStalledIssueStringMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    operator fun invoke(type: StallIssueType): String {
        return when (type) {
            StallIssueType.NoReason -> context.getString(R.string.sync_stalled_issue_no_reason)
            StallIssueType.FileIssue -> context.getString(R.string.sync_stalled_issue_file_issue)
            StallIssueType.MoveOrRenameCannotOccur -> context.getString(R.string.sync_stalled_issue_move_or_rename)
            StallIssueType.DeleteOrMoveWaitingOnScanning -> context.getString(R.string.sync_stalled_issue_delete_or_move_scanning)
            StallIssueType.DeleteWaitingOnMoves -> context.getString(R.string.sync_stalled_issue_delete_or_move)
            StallIssueType.UploadIssue -> context.getString(R.string.sync_stalled_issue_upload_issue)
            StallIssueType.DownloadIssue -> context.getString(R.string.sync_stalled_issue_download_issue)
            StallIssueType.CannotCreateFolder -> context.getString(R.string.sync_stalled_issue_cannot_create_folder)
            StallIssueType.CannotPerformDeletion -> context.getString(R.string.sync_stalled_issue_cannot_perform_deletion)
            StallIssueType.SyncItemExceedsSupportedTreeDepth -> context.getString(R.string.sync_stalled_issue_cannot_support_tree_depth)
            StallIssueType.FolderMatchedAgainstFile -> context.getString(R.string.sync_stalled_issue_folder_matched_against_file)
            StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose -> context.getString(
                R.string.sync_stalled_issue_local_and_remote_change_last_sync
            )

            StallIssueType.LocalAndRemotePreviouslyNotSyncedDifferUserMustChoose -> context.getString(
                R.string.sync_stalled_issue_local_and_remote_not_synced
            )

            StallIssueType.NamesWouldClashWhenSynced -> context.getString(R.string.sync_stalled_issue_name_clash)
            StallIssueType.SyncStallReasonLastPlusOne -> context.getString(R.string.sync_stalled_issue_last_plus_one)
        }
    }
}