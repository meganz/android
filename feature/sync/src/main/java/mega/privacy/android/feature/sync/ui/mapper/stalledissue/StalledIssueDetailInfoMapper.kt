package mega.privacy.android.feature.sync.ui.mapper.stalledissue

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import javax.inject.Inject

internal class StalledIssueDetailInfoMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    operator fun invoke(stalledIssue: StalledIssue): StalledIssueDetailedInfo {
        val issue = when (stalledIssue.issueType) {
            StallIssueType.FileIssue -> Pair(
                context.getString(R.string.sync_stalled_issue_file_issue),
                context.getString(R.string.sync_stalled_issue_file_issue_detail)
            )

            StallIssueType.MoveOrRenameCannotOccur -> Pair(
                context.getString(R.string.sync_stalled_issue_move_or_rename),
                if (stalledIssue.nodeNames.size > stalledIssue.localPaths.size) {
                    context.getString(R.string.sync_stalled_issues_move_or_rename_message_mega)
                } else {
                    context.getString(R.string.sync_stalled_issues_move_or_rename_message_local)
                }
            )

            StallIssueType.DeleteOrMoveWaitingOnScanning -> Pair(
                context.getString(R.string.sync_stalled_issue_delete_or_move_scanning),
                context.getString(R.string.sync_stalled_issue_delete_or_move_scanning_detail)
            )

            StallIssueType.DeleteWaitingOnMoves -> Pair(
                context.getString(R.string.sync_stalled_issue_delete_or_move),
                context.getString(R.string.sync_stalled_issue_delete_or_move_detail)
            )

            StallIssueType.UploadIssue -> Pair(
                context.getString(R.string.sync_stalled_issue_upload_issue),
                context.getString(R.string.sync_stalled_issue_upload_issue_detail)
            )

            StallIssueType.DownloadIssue -> Pair(
                context.getString(R.string.sync_stalled_issue_download_issue),
                context.getString(R.string.sync_stalled_issue_download_issue_detail)
            )

            StallIssueType.CannotCreateFolder -> Pair(
                context.getString(R.string.sync_stalled_issue_cannot_create_folder),
                context.getString(R.string.sync_stalled_issue_cannot_create_folder_detail)
            )

            StallIssueType.CannotPerformDeletion -> Pair(
                context.getString(R.string.sync_stalled_issue_cannot_perform_deletion),
                context.getString(R.string.sync_stalled_issue_cannot_perform_deletion_detail)
            )

            StallIssueType.SyncItemExceedsSupportedTreeDepth -> Pair(
                context.getString(R.string.sync_stalled_issue_cannot_support_tree_depth),
                context.getString(R.string.sync_stalled_issue_cannot_support_tree_depth_detail)
            )

            StallIssueType.FolderMatchedAgainstFile -> Pair(
                context.getString(R.string.sync_stalled_issue_folder_matched_against_file),
                context.getString(R.string.sync_stalled_issue_folder_matched_against_file_detail)
            )

            StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose -> Pair(
                context.getString(R.string.sync_stalled_issue_local_and_remote_change_last_sync),
                context.getString(R.string.sync_stalled_issue_local_and_remote_change_last_sync_detail)
            )

            StallIssueType.LocalAndRemotePreviouslyNotSyncedDifferUserMustChoose -> Pair(
                context.getString(R.string.sync_stalled_issue_local_and_remote_not_synced),
                context.getString(R.string.sync_stalled_issue_local_and_remote_not_synced_detail)
            )

            StallIssueType.NamesWouldClashWhenSynced -> Pair(
                context.getString(R.string.sync_stalled_issue_name_clash),
                context.getString(R.string.sync_stalled_issue_name_clash_detail)
            )

            StallIssueType.SyncStallReasonLastPlusOne -> Pair(
                context.getString(R.string.sync_stalled_issue_last_plus_one),
                ""
            )

            else -> Pair(
                context.getString(R.string.sync_stalled_issue_no_reason),
                ""
            )
        }
        return StalledIssueDetailedInfo(
            title = issue.first,
            explanation = issue.second
        )
    }
}