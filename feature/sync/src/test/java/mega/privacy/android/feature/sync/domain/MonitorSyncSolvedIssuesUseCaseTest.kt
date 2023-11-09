package mega.privacy.android.feature.sync.domain

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
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.repository.SyncSolvedIssuesRepository
import mega.privacy.android.feature.sync.domain.usecase.solvedissues.GetSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissues.MonitorSyncSolvedIssuesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorSyncSolvedIssuesUseCaseTest {

    private val getSyncSolvedIssuesUseCase: GetSyncSolvedIssuesUseCase = mock()
    private val syncSolvedIssuesRepository: SyncSolvedIssuesRepository = mock()

    private val underTest = MonitorSyncSolvedIssuesUseCase(
        getSyncSolvedIssuesUseCase,
        syncSolvedIssuesRepository
    )

    private val solvedIssues = listOf(
        SolvedIssue(
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            resolutionExplanation = "Folders were merged",
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
            getSyncSolvedIssuesUseCase,
            syncSolvedIssuesRepository,
        )
    }

    @Test
    fun `test that solved issues are refetched every time solved issues change`() = runTest {
        whenever(getSyncSolvedIssuesUseCase()).thenReturn(solvedIssues)
        whenever(syncSolvedIssuesRepository.monitorSolvedIssuesCountChanged()).thenReturn(
            flow {
                emit(Unit)
                emit(Unit)
                awaitCancellation()
            })

        underTest().test { cancelAndIgnoreRemainingEvents() }

        verify(getSyncSolvedIssuesUseCase, times(3)).invoke()
    }

    @Test
    fun `test that correct solved issues are returned`() = runTest {
        whenever(getSyncSolvedIssuesUseCase()).thenReturn(solvedIssues)
        whenever(syncSolvedIssuesRepository.monitorSolvedIssuesCountChanged()).thenReturn(
            flow {
                awaitCancellation()
            })

        underTest()
            .test {
                Truth.assertThat(awaitItem()).isEqualTo(solvedIssues)
            }
    }
}