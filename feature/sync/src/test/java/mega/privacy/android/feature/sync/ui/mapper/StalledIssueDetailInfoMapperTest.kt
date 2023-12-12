package mega.privacy.android.feature.sync.ui.mapper

import android.content.Context
import com.google.common.truth.Truth
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StalledIssueDetailInfoMapperTest {
    private val context: Context = mock()
    private val underTest = StalledIssueDetailInfoMapper(context)

    @BeforeEach
    fun getStrings() {
        whenever(context.getString(R.string.sync_stalled_issue_no_reason)).thenReturn("No Reason")
        whenever(context.getString(R.string.sync_stalled_issue_file_issue)).thenReturn("File Issue")
        whenever(context.getString(R.string.sync_stalled_issue_move_or_rename)).thenReturn("Move Or Rename Cannot Occur")
        whenever(context.getString(R.string.sync_stalled_issue_delete_or_move_scanning)).thenReturn(
            "Delete Or Move Waiting On Scanning"
        )
        whenever(context.getString(R.string.sync_stalled_issue_delete_or_move)).thenReturn("Delete Waiting On Moves")
        whenever(context.getString(R.string.sync_stalled_issue_upload_issue)).thenReturn("Upload Issue")
        whenever(context.getString(R.string.sync_stalled_issue_download_issue)).thenReturn("Download Issue")
        whenever(context.getString(R.string.sync_stalled_issue_cannot_create_folder)).thenReturn("Cannot Create Folder")
        whenever(context.getString(R.string.sync_stalled_issue_cannot_perform_deletion)).thenReturn(
            "Cannot Perform Deletion"
        )
        whenever(context.getString(R.string.sync_stalled_issue_cannot_support_tree_depth)).thenReturn(
            "Sync Item Exceeds Supported Tree Depth"
        )
        whenever(context.getString(R.string.sync_stalled_issue_folder_matched_against_file)).thenReturn(
            "Folder Matched Against File"
        )
        whenever(context.getString(R.string.sync_stalled_issue_local_and_remote_change_last_sync)).thenReturn(
            "Local And Remote Changed Since Last Sync"
        )
        whenever(context.getString(R.string.sync_stalled_issue_local_and_remote_not_synced)).thenReturn(
            "Local And Remote Previously Not Synced"
        )
        whenever(context.getString(R.string.sync_stalled_issue_name_clash)).thenReturn(
            "Names Would Clash When Synced"
        )
        whenever(context.getString(R.string.sync_stalled_issue_last_plus_one)).thenReturn(
            "Last Plus One"
        )
        whenever(context.getString(R.string.sync_stalled_issue_file_issue_detail)).thenReturn(
            "File Issue Detail"
        )
        whenever(context.getString(R.string.sync_stalled_issues_move_or_rename_message_mega)).thenReturn(
            "Move Or Rename Message Mega"
        )
        whenever(context.getString(R.string.sync_stalled_issues_move_or_rename_message_local)).thenReturn(
            "Move Or Rename Message Local"
        )
        whenever(context.getString(R.string.sync_stalled_issue_delete_or_move_scanning_detail)).thenReturn(
            "Delete Or Move Scanning Detail"
        )
        whenever(context.getString(R.string.sync_stalled_issue_delete_or_move_detail)).thenReturn(
            "Delete Or Move Detail"
        )
        whenever(context.getString(R.string.sync_stalled_issue_upload_issue_detail)).thenReturn(
            "Upload Issue Detail"
        )
        whenever(context.getString(R.string.sync_stalled_issue_download_issue_detail)).thenReturn(
            "Download Issue Detail"
        )
        whenever(context.getString(R.string.sync_stalled_issue_cannot_create_folder_detail)).thenReturn(
            "Cannot Create Folder Detail"
        )
        whenever(context.getString(R.string.sync_stalled_issue_cannot_perform_deletion_detail)).thenReturn(
            "Cannot Perform Deletion Detail"
        )
        whenever(context.getString(R.string.sync_stalled_issue_cannot_support_tree_depth_detail)).thenReturn(
            "Sync Item Exceeds Supported Tree Depth Detail"
        )
        whenever(context.getString(R.string.sync_stalled_issue_folder_matched_against_file_detail)).thenReturn(
            "Folder Matched Against File Detail"
        )
        whenever(context.getString(R.string.sync_stalled_issue_local_and_remote_change_last_sync_detail)).thenReturn(
            "Local And Remote Changed Since Last Sync Detail"
        )
        whenever(context.getString(R.string.sync_stalled_issue_local_and_remote_not_synced_detail)).thenReturn(
            "Local And Remote Previously Not Synced Detail"
        )
        whenever(context.getString(R.string.sync_stalled_issue_name_clash_detail)).thenReturn(
            "Names Would Clash When Synced Detail"
        )
        whenever(context.getString(0)).thenReturn("")
    }

    @ParameterizedTest(name = "is current device: {0}")
    @MethodSource("provideStringParameters")
    fun `test that when provided stalled issue type it returns appropriate string {1}`(
        stalledIssue: StalledIssue,
        stalledIssueTitleRes: Int,
        stalledIssueDetailRes: Int,
    ) {
        val stalledIssueDetailedInfo = underTest(stalledIssue)
        Truth.assertThat(stalledIssueDetailedInfo.title)
            .isEqualTo(context.getString(stalledIssueTitleRes))
        Truth.assertThat(stalledIssueDetailedInfo.explanation)
            .isEqualTo(context.getString(stalledIssueDetailRes))
    }

    private fun provideStringParameters() = Stream.of(
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.NoReason,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_no_reason, 0,
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.FileIssue,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_file_issue,
            R.string.sync_stalled_issue_file_issue_detail,
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.MoveOrRenameCannotOccur,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = listOf("name1", "name2")
            ),
            R.string.sync_stalled_issue_move_or_rename,
            R.string.sync_stalled_issues_move_or_rename_message_mega,
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.MoveOrRenameCannotOccur,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_move_or_rename,
            R.string.sync_stalled_issues_move_or_rename_message_local
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.DeleteOrMoveWaitingOnScanning,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_delete_or_move_scanning,
            R.string.sync_stalled_issue_delete_or_move_scanning_detail
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.DeleteWaitingOnMoves,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_delete_or_move,
            R.string.sync_stalled_issue_delete_or_move_detail
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.UploadIssue,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_upload_issue,
            R.string.sync_stalled_issue_upload_issue_detail
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.DownloadIssue,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_download_issue,
            R.string.sync_stalled_issue_download_issue_detail
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.CannotCreateFolder,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_cannot_create_folder,
            R.string.sync_stalled_issue_cannot_create_folder_detail
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.CannotPerformDeletion,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_cannot_perform_deletion,
            R.string.sync_stalled_issue_cannot_perform_deletion_detail
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.SyncItemExceedsSupportedTreeDepth,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_cannot_support_tree_depth,
            R.string.sync_stalled_issue_cannot_support_tree_depth_detail,
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.FolderMatchedAgainstFile,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_folder_matched_against_file,
            R.string.sync_stalled_issue_folder_matched_against_file_detail,
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_local_and_remote_change_last_sync,
            R.string.sync_stalled_issue_local_and_remote_change_last_sync_detail
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.LocalAndRemotePreviouslyNotSyncedDifferUserMustChoose,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_local_and_remote_not_synced,
            R.string.sync_stalled_issue_local_and_remote_not_synced_detail
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_name_clash,
            R.string.sync_stalled_issue_name_clash_detail,
        ),
        Arguments.of(
            StalledIssue(
                issueType = StallIssueType.SyncStallReasonLastPlusOne,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                conflictName = "",
                nodeNames = emptyList()
            ),
            R.string.sync_stalled_issue_last_plus_one,
            0,
        ),
    )
}