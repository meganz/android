package mega.privacy.android.feature.sync.ui.mapper

import android.content.Context
import com.google.common.truth.Truth
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StalledIssueTypeToStalledIssueStringMapperTest {
    private val context: Context = mock()
    private val underTest = StalledIssueTypeToStalledIssueStringMapper(context)

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
    }

    @ParameterizedTest(name = "is current device: {0}")
    @MethodSource("provideStringParameters")
    fun `test that when provided stalled issue type it returns appropriate string {1}`(
        stallIssueType: StallIssueType,
        stalledIssueTitleRes: Int,
    ) {
        val string = underTest(stallIssueType)
        Truth.assertThat(string).isEqualTo(context.getString(stalledIssueTitleRes))
    }

    private fun provideStringParameters() = Stream.of(
        Arguments.of(
            StallIssueType.NoReason, R.string.sync_stalled_issue_no_reason
        ),
        Arguments.of(
            StallIssueType.FileIssue,
            R.string.sync_stalled_issue_file_issue
        ),
        Arguments.of(
            StallIssueType.MoveOrRenameCannotOccur,
            R.string.sync_stalled_issue_move_or_rename
        ),
        Arguments.of(
            StallIssueType.DeleteOrMoveWaitingOnScanning,
            R.string.sync_stalled_issue_delete_or_move_scanning
        ),
        Arguments.of(
            StallIssueType.DeleteWaitingOnMoves,
            R.string.sync_stalled_issue_delete_or_move
        ),
        Arguments.of(
            StallIssueType.UploadIssue,
            R.string.sync_stalled_issue_upload_issue
        ),
        Arguments.of(
            StallIssueType.DownloadIssue,
            R.string.sync_stalled_issue_download_issue
        ),
        Arguments.of(
            StallIssueType.CannotCreateFolder,
            R.string.sync_stalled_issue_cannot_create_folder
        ),
        Arguments.of(
            StallIssueType.CannotPerformDeletion,
            R.string.sync_stalled_issue_cannot_perform_deletion
        ),
        Arguments.of(
            StallIssueType.SyncItemExceedsSupportedTreeDepth,
            R.string.sync_stalled_issue_cannot_support_tree_depth
        ),
        Arguments.of(
            StallIssueType.FolderMatchedAgainstFile,
            R.string.sync_stalled_issue_folder_matched_against_file
        ),
        Arguments.of(
            StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
            R.string.sync_stalled_issue_local_and_remote_change_last_sync
        ),
        Arguments.of(
            StallIssueType.LocalAndRemotePreviouslyNotSyncedDifferUserMustChoose,
            R.string.sync_stalled_issue_local_and_remote_not_synced
        ),
        Arguments.of(
            StallIssueType.NamesWouldClashWhenSynced,
            R.string.sync_stalled_issue_name_clash
        ),
        Arguments.of(
            StallIssueType.SyncStallReasonLastPlusOne,
            R.string.sync_stalled_issue_last_plus_one
        ),
    )
}