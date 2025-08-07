package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.backup.GetDeviceIdUseCase
import mega.privacy.android.domain.usecase.backup.GetDeviceNameUseCase
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.usecase.SetOnboardingShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.MonitorSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.ResolveStalledIssueUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.ui.mapper.stalledissue.StalledIssueItemMapper
import mega.privacy.android.feature.sync.ui.synclist.SyncListViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncListViewModelTest {

    private lateinit var underTest: SyncListViewModel
    private val setOnboardingShownUseCase: SetOnboardingShownUseCase = mock()
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val resolveStalledIssueUseCase: ResolveStalledIssueUseCase = mock()
    private val stalledIssueItemMapper: StalledIssueItemMapper = mock()
    private val monitorSyncSolvedIssuesUseCase: MonitorSyncSolvedIssuesUseCase = mock()
    private val getDeviceIdUseCase: GetDeviceIdUseCase = mock()
    private val getDeviceNameUseCase: GetDeviceNameUseCase = mock()

    private val stalledIssues = listOf(
        StalledIssue(
            syncId = 3L,
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
            id = "1_1_0"
        )
    )

    private val solvedIssues = listOf(
        SolvedIssue(
            syncId = 1L, nodeIds = listOf(), localPaths = listOf(), resolutionExplanation = ""
        )
    )

    @AfterEach
    fun resetAndTearDown() {
        reset(
            setOnboardingShownUseCase,
            monitorSyncStalledIssuesUseCase,
            resolveStalledIssueUseCase,
            stalledIssueItemMapper,
            monitorSyncSolvedIssuesUseCase,
            getDeviceIdUseCase,
            getDeviceNameUseCase,
        )
    }

    @Test
    fun `test that view model initialization sets onboarding shown to true`() = runTest {
        initViewModel()

        verify(setOnboardingShownUseCase).invoke(true)
    }

    @Test
    fun `test that stalled issues count is updated when stalled issues are fetched`() = runTest {
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flow {
            emit(stalledIssues)
            awaitCancellation()
        })
        whenever(monitorSyncSolvedIssuesUseCase()).thenReturn(flow {
            emit(solvedIssues)
            awaitCancellation()
        })
        initViewModel()

        underTest.state.test {
            assertThat(awaitItem().stalledIssuesCount).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when monitorSyncSolvedIssuesUseCase emit list then it updates shouldShowCleanSolvedIssueMenuItem to true`() =
        runTest {
            whenever(monitorSyncSolvedIssuesUseCase()).thenReturn(flow {
                emit(solvedIssues)
                awaitCancellation()
            })
            initViewModel()
            underTest.state.test {
                assertThat(awaitItem().shouldShowCleanSolvedIssueMenuItem).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun initViewModel() {
        underTest = SyncListViewModel(
            setOnboardingShownUseCase = setOnboardingShownUseCase,
            monitorSyncStalledIssuesUseCase = monitorSyncStalledIssuesUseCase,
            monitorSyncSolvedIssuesUseCase = monitorSyncSolvedIssuesUseCase,
            getDeviceIdUseCase = getDeviceIdUseCase,
            getDeviceNameUseCase = getDeviceNameUseCase,
        )
    }
}
