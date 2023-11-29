package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth
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
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.usecase.solvedissues.MonitorSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.ui.mapper.SolvedIssueItemMapper
import mega.privacy.android.feature.sync.ui.model.SolvedIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesState
import mega.privacy.android.feature.sync.ui.synclist.solvedissues.SyncSolvedIssuesViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncSolvedIssuesViewModelTest {

    private val monitorSyncSolvedIssuesUseCase: MonitorSyncSolvedIssuesUseCase = mock()
    private val solvedIssueItemMapper: SolvedIssueItemMapper = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()

    private lateinit var underTest: SyncSolvedIssuesViewModel

    private val solvedIssues = listOf(
        SolvedIssue(
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            resolutionExplanation = "folders merged",
        )
    )

    private val stalledIssuesUiItems = listOf(
        SolvedIssueUiItem(
            nodeIds = listOf(NodeId(3L)),
            nodeNames = listOf("DCIM"),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            resolutionExplanation = "folders merged",
            icon = 0,
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
            monitorSyncSolvedIssuesUseCase,
            solvedIssueItemMapper,
            getNodeByHandleUseCase,
        )
    }

    @Test
    fun `test that view model fetches all solved issues and updates the state on init`() =
        runTest {
            whenever(monitorSyncSolvedIssuesUseCase()).thenReturn(flow {
                emit(solvedIssues)
                awaitCancellation()
            })
            val node: FolderNode = mock()
            whenever(getNodeByHandleUseCase(solvedIssues.first().nodeIds.first().longValue)).thenReturn(
                node
            )
            whenever(solvedIssueItemMapper(solvedIssues.first(), listOf(node))).thenReturn(
                stalledIssuesUiItems.first()
            )
            initViewModel()

            underTest.state.test {
                Truth.assertThat(awaitItem())
                    .isEqualTo(SyncSolvedIssuesState(stalledIssuesUiItems))
            }
        }

    private fun initViewModel() {
        underTest = SyncSolvedIssuesViewModel(
            monitorSyncSolvedIssuesUseCase,
            solvedIssueItemMapper,
            getNodeByHandleUseCase
        )
    }
}