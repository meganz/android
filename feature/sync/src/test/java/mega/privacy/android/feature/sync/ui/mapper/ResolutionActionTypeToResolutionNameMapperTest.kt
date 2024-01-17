package mega.privacy.android.feature.sync.ui.mapper

import android.content.Context
import com.google.common.truth.Truth
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.ui.mapper.stalledissue.ResolutionActionTypeToResolutionNameMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolutionActionTypeToResolutionNameMapperTest {
    private val context: Context = mock()
    private val underTest = ResolutionActionTypeToResolutionNameMapper(context)

    @BeforeEach
    fun provideStrings() {
        whenever(context.getString(R.string.sync_resolve_rename_all_items)).thenReturn("Rename all items")
        whenever(context.getString(R.string.sync_stalled_issue_merge_folders)).thenReturn("Merge folders")
        whenever(context.getString(R.string.sync_stalled_issue_choose_local_file)).thenReturn("Choose local file")
        whenever(context.getString(R.string.sync_stalled_issue_choose_remote_file)).thenReturn("Choose remote file")
        whenever(context.getString(R.string.sync_stalled_issue_choose_latest_modified_time)).thenReturn(
            "Choose the one with the latest modified time"
        )
    }

    @ParameterizedTest(name = "Test Action {0} returns strings")
    @MethodSource("resolutionActionTypeProvider")
    fun `test that for each action type gets proper string`(
        type: StalledIssueResolutionActionType,
        actual: Int,
    ) {
        val expected = underTest(type)
        Truth.assertThat(expected).isEqualTo(context.getString(actual))
    }

    private fun resolutionActionTypeProvider() = Stream.of(
        Arguments.of(
            StalledIssueResolutionActionType.RENAME_ALL_ITEMS,
            R.string.sync_resolve_rename_all_items
        ),
        Arguments.of(
            StalledIssueResolutionActionType.MERGE_FOLDERS,
            R.string.sync_stalled_issue_merge_folders
        ),
        Arguments.of(
            StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE,
            R.string.sync_stalled_issue_choose_local_file
        ),
        Arguments.of(
            StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE,
            R.string.sync_stalled_issue_choose_remote_file
        ),
        Arguments.of(
            StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME,
            R.string.sync_stalled_issue_choose_latest_modified_time
        )
    )
}