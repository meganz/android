package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.ResolveStalledIssueUseCase
import mega.privacy.android.feature.sync.domain.usecase.SetOnboardingShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.SetSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissues.ClearSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissues.MonitorSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.ui.mapper.StalledIssueItemMapper
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.model.SyncOption
import mega.privacy.android.feature.sync.ui.synclist.SyncListAction
import mega.privacy.android.feature.sync.ui.synclist.SyncListViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncListViewModelTest {

    private lateinit var underTest: SyncListViewModel
    private val setOnboardingShownUseCase: SetOnboardingShownUseCase = mock()
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val resolveStalledIssueUseCase: ResolveStalledIssueUseCase = mock()
    private val stalledIssueItemMapper: StalledIssueItemMapper = mock()
    private val monitorSyncSolvedIssuesUseCase: MonitorSyncSolvedIssuesUseCase = mock()
    private val clearSyncSolvedIssuesUseCase: ClearSyncSolvedIssuesUseCase = mock()
    private val setSyncByWiFiUseCase: SetSyncByWiFiUseCase = mock()
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()

    private val stalledIssues = listOf(
        StalledIssue(
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
        )
    )

    private val solvedIssues = listOf(
        SolvedIssue(
            nodeIds = listOf(), localPaths = listOf(), resolutionExplanation = ""
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
            setOnboardingShownUseCase,
            monitorSyncStalledIssuesUseCase,
            resolveStalledIssueUseCase,
            stalledIssueItemMapper,
            monitorSyncSolvedIssuesUseCase
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
            assertThat(awaitItem().snackbarMessage).isEqualTo("Conflict resolved")
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

    @Test
    fun `test that state changes when sync option is selected`() = runTest {
        val monitorSyncByWiFiStateFlow = MutableStateFlow(false)
        whenever(monitorSyncByWiFiUseCase()).thenReturn(monitorSyncByWiFiStateFlow)
        initViewModel()

        underTest.state.test {
            assertThat(awaitItem().selectedSyncOption).isEqualTo(SyncOption.WI_FI_OR_MOBILE_DATA)
        }

        underTest.handleAction(SyncListAction.SyncOptionsSelected(SyncOption.WI_FI_ONLY))
        monitorSyncByWiFiStateFlow.emit(true)

        underTest.state.test {
            assertThat(awaitItem().selectedSyncOption).isEqualTo(SyncOption.WI_FI_ONLY)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that set sync by wifi usecase is invoked when sync option is selected`() = runTest {
        initViewModel()

        underTest.handleAction(SyncListAction.SyncOptionsSelected(SyncOption.WI_FI_ONLY))

        verify(setSyncByWiFiUseCase).invoke(true)
    }

    private fun initViewModel() {
        underTest = SyncListViewModel(
            setOnboardingShownUseCase = setOnboardingShownUseCase,
            monitorSyncStalledIssuesUseCase = monitorSyncStalledIssuesUseCase,
            resolveStalledIssueUseCase = resolveStalledIssueUseCase,
            stalledIssueItemMapper = stalledIssueItemMapper,
            monitorSyncSolvedIssuesUseCase = monitorSyncSolvedIssuesUseCase,
            clearSyncSolvedIssuesUseCase = clearSyncSolvedIssuesUseCase,
            setSyncByWiFiUseCase = setSyncByWiFiUseCase,
            monitorSyncByWiFiUseCase = monitorSyncByWiFiUseCase
        )
    }
}