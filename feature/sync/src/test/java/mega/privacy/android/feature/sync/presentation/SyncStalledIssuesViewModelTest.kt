package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.ResolveStalledIssueUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.ui.mapper.stalledissue.StalledIssueItemMapper
import mega.privacy.android.feature.sync.ui.model.StalledIssueDetailedInfo
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.SyncListAction
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesState
import mega.privacy.android.feature.sync.ui.synclist.stalledissues.SyncStalledIssuesViewModel
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncStalledIssuesViewModelTest {

    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val stalledIssueItemMapper: StalledIssueItemMapper = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val resolveStalledIssueUseCase: ResolveStalledIssueUseCase = mock()

    private lateinit var underTest: SyncStalledIssuesViewModel

    private val stalledIssues = listOf(
        StalledIssue(
            syncId = 1L,
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
            id = "1_3_0",
        )
    )

    private val stalledIssuesUiItems = listOf(
        StalledIssueUiItem(
            syncId = 1L,
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
            icon = 0,
            detailedInfo = StalledIssueDetailedInfo("", ""),
            actions = emptyList(),
            displayedName = "Camera",
            displayedPath = "/storage/emulated/0/DCIM",
            id = "1_3_0",
        )
    )

    @AfterEach
    fun resetAndTearDown() {
        reset(
            monitorSyncStalledIssuesUseCase,
            stalledIssueItemMapper,
            getNodeByHandleUseCase,
            resolveStalledIssueUseCase
        )
    }

    @Test
    fun `test that view model fetches all stalled issues and updates the state on init`() =
        runTest {
            whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flow {
                emit(stalledIssues)
                awaitCancellation()
            })
            val node: FolderNode = mock {
                on { name } doReturn "Camera"
                on { isIncomingShare } doReturn true
            }
            whenever(
                stalledIssueItemMapper(
                    stalledIssues.first(),
                    listOf(node),
                )
            ).thenReturn(
                stalledIssuesUiItems.first()
            )
            whenever(getNodeByHandleUseCase(stalledIssues.first().nodeIds.first().longValue))
                .thenReturn(node)
            initViewModel()

            underTest.state.test {
                assertThat(awaitItem())
                    .isEqualTo(SyncStalledIssuesState(stalledIssuesUiItems))
            }
        }

    @Test
    fun `test that snackbar is shown when issues are resolved`() = runTest {
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flow {
            emit(stalledIssues)
            awaitCancellation()
        })
        val selectedResolution = StalledIssueResolutionAction(
            resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE,
            actionName = "Choose remote file",
        )
        val stalledIssueUiItem: StalledIssueUiItem = mock()
        val stalledIssue: StalledIssue = mock()
        whenever(stalledIssueItemMapper(stalledIssueUiItem)).thenReturn(stalledIssue)
        initViewModel()

        underTest.handleAction(
            SyncListAction.ResolveStalledIssue(
                stalledIssueUiItem, selectedResolution
            )
        )

        verify(resolveStalledIssueUseCase).invoke(
            selectedResolution, stalledIssue
        )
        underTest.state.test {
            assertThat(awaitItem().snackbarMessageContent).isEqualTo(triggered(sharedR.string.sync_stalled_issue_resolved))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that apply to all resolves multiple issues with same resolution action`() = runTest {
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flow {
            emit(stalledIssues)
            awaitCancellation()
        })
        val selectedResolution = StalledIssueResolutionAction(
            resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE,
            actionName = "Choose local file",
        )
        val stalledIssueUiItem = stalledIssuesUiItems.first().copy(
            actions = listOf(selectedResolution)
        )
        val node: FolderNode = mock {
            on { name } doReturn "Camera"
            on { isIncomingShare } doReturn true
        }
        whenever(
            stalledIssueItemMapper(
                stalledIssues.first(),
                listOf(node),
            )
        ).thenReturn(
            stalledIssueUiItem
        )
        whenever(getNodeByHandleUseCase(stalledIssues.first().nodeIds.first().longValue))
            .thenReturn(node)
        val stalledIssue: StalledIssue = mock()
        whenever(stalledIssueItemMapper(stalledIssueUiItem)).thenReturn(stalledIssue)

        initViewModel()
        underTest.handleAction(
            SyncListAction.ResolveStalledIssue(
                uiItem = stalledIssueUiItem,
                selectedResolution = selectedResolution,
                isApplyToAll = true
            )
        )

        underTest.state.test {
            verify(resolveStalledIssueUseCase).invoke(selectedResolution, stalledIssue)
            assertThat(awaitItem().snackbarMessageContent).isEqualTo(triggered(sharedR.string.sync_stalled_issues_resolved))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that snackbar message is cleared when SnackBarShown action is handled`() = runTest {
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flow {
            emit(emptyList())
            awaitCancellation()
        })
        initViewModel()

        // First set a snackbar message
        val selectedResolution = StalledIssueResolutionAction(
            resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE,
            actionName = "Choose local file",
        )
        val stalledIssueUiItem: StalledIssueUiItem = mock()
        val stalledIssue: StalledIssue = mock()
        whenever(stalledIssueItemMapper(stalledIssueUiItem)).thenReturn(stalledIssue)

        underTest.handleAction(
            SyncListAction.ResolveStalledIssue(
                stalledIssueUiItem, selectedResolution
            )
        )
        underTest.handleAction(SyncListAction.SnackBarShown)

        underTest.state.test {
            assertThat(awaitItem().snackbarMessageContent).isEqualTo(consumed())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that distinctUntilChanged prevents duplicate emissions when stalled issues are the same`() =
        runTest {
            val mutableFlow = MutableSharedFlow<List<StalledIssue>>()
            whenever(monitorSyncStalledIssuesUseCase()).thenReturn(mutableFlow)

            val node: FolderNode = mock {
                on { name } doReturn "Camera"
                on { isIncomingShare } doReturn true
            }
            whenever(stalledIssueItemMapper(stalledIssues.first(), listOf(node)))
                .thenReturn(stalledIssuesUiItems.first())
            whenever(getNodeByHandleUseCase(stalledIssues.first().nodeIds.first().longValue))
                .thenReturn(node)

            initViewModel()

            underTest.state.test {
                // Wait for initial empty state
                val initialState = awaitItem()
                assertThat(initialState.stalledIssues).isEmpty()
                // Emit the first list
                mutableFlow.emit(stalledIssues)
                val firstEmission = awaitItem()
                assertThat(firstEmission.stalledIssues).isEqualTo(stalledIssuesUiItems)
                // Emit the same list again - should not trigger a new emission due to distinctUntilChanged
                mutableFlow.emit(stalledIssues)
                // Verify no additional emissions occur
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun initViewModel() {
        underTest = SyncStalledIssuesViewModel(
            monitorSyncStalledIssuesUseCase = monitorSyncStalledIssuesUseCase,
            stalledIssueItemMapper = stalledIssueItemMapper,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            resolveStalledIssueUseCase = resolveStalledIssueUseCase
        )
    }
}
