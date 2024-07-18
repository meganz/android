package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.domain.usecase.SetOnboardingShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.ClearSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.MonitorSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.ResolveStalledIssueUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSyncByWiFiUseCase
import mega.privacy.android.feature.sync.ui.mapper.stalledissue.StalledIssueItemMapper
import mega.privacy.android.feature.sync.ui.model.StalledIssueUiItem
import mega.privacy.android.feature.sync.ui.synclist.SyncListAction
import mega.privacy.android.feature.sync.ui.synclist.SyncListViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
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
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()
    private val getAccountTypeUseCase: GetAccountTypeUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()

    private val stalledIssues = listOf(
        StalledIssue(
            syncId = 3L,
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
        )
    )

    private val solvedIssues = listOf(
        SolvedIssue(
            syncId = 1L, nodeIds = listOf(), localPaths = listOf(), resolutionExplanation = ""
        )
    )

    @BeforeEach
    fun setupMock(): Unit = runBlocking {
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.PRO_I
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(
            flowOf(accountDetail)
        )
    }

    @AfterEach
    fun resetAndTearDown() {
        reset(
            setOnboardingShownUseCase,
            monitorSyncStalledIssuesUseCase,
            resolveStalledIssueUseCase,
            stalledIssueItemMapper,
            monitorSyncSolvedIssuesUseCase,
            getAccountTypeUseCase,
            monitorAccountDetailUseCase
        )
    }

    @Test
    fun `test that view model initialization sets onboarding shown to true`() = runTest {
        val monitorSyncByWiFiStateFlow = MutableStateFlow(false)
        whenever(monitorSyncByWiFiUseCase()).thenReturn(monitorSyncByWiFiStateFlow)
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
        val monitorSyncByWiFiStateFlow = MutableStateFlow(false)
        whenever(monitorSyncByWiFiUseCase()).thenReturn(monitorSyncByWiFiStateFlow)
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
            assertThat(awaitItem().snackbarMessage).isEqualTo(R.string.sync_stalled_issue_conflict_resolved)
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
            val monitorSyncByWiFiStateFlow = MutableStateFlow(false)
            whenever(monitorSyncByWiFiUseCase()).thenReturn(monitorSyncByWiFiStateFlow)
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
            resolveStalledIssueUseCase = resolveStalledIssueUseCase,
            stalledIssueItemMapper = stalledIssueItemMapper,
            monitorSyncSolvedIssuesUseCase = monitorSyncSolvedIssuesUseCase,
            clearSyncSolvedIssuesUseCase = clearSyncSolvedIssuesUseCase,
            monitorSyncByWiFiUseCase = monitorSyncByWiFiUseCase,
            getAccountTypeUseCase = getAccountTypeUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        )
    }
}
