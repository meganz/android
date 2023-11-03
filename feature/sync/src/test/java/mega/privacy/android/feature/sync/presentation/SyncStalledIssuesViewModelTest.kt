package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.ui.mapper.StalledIssueItemMapper
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesState
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncStalledIssuesViewModelTest {

    private val monitorStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val stalledIssueItemMapper: StalledIssueItemMapper = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()

    private lateinit var underTest: SyncStalledIssuesViewModel

    private val stalledIssues = listOf(
        StalledIssue(
            nodeIds = listOf( NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
        )
    )

    private val stalledIssuesUiItems = listOf(
        StalledIssueUiItem(
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
            icon = 0,
            detailedInfo = StalledIssueDetailedInfo("", ""),
            actions = emptyList()
        )
    )


    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun resetAndTearDown() {
        Dispatchers.resetMain()
        reset(
            monitorStalledIssuesUseCase,
            stalledIssueItemMapper,
            getNodeByHandleUseCase,
        )
    }

    @Test
    fun `test that view model fetches all stalled issues and updates the state on init`() =
        runTest {
            whenever(monitorStalledIssuesUseCase()).thenReturn(flow {
                emit(stalledIssues)
                awaitCancellation()
            })
            whenever(stalledIssueItemMapper(stalledIssues.first(), isFolder = true)).thenReturn(
                stalledIssuesUiItems.first()
            )
            val node: FolderNode = mock()
            whenever(getNodeByHandleUseCase(stalledIssues.first().nodeIds.first().longValue)).thenReturn(node)
            initViewModel()

            underTest.state.test {
                assertThat(awaitItem())
                    .isEqualTo(SyncStalledIssuesState(stalledIssuesUiItems))
            }
        }

    private fun initViewModel() {
        underTest = SyncStalledIssuesViewModel(
            monitorStalledIssuesUseCase,
            stalledIssueItemMapper,
            getNodeByHandleUseCase
        )
    }
}