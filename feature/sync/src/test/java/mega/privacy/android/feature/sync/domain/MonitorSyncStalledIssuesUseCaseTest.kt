package mega.privacy.android.feature.sync.domain

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
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.GetSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncStalledIssuesUseCase
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
internal class MonitorSyncStalledIssuesUseCaseTest {

    private val syncRepository: SyncRepository = mock()
    private val getSyncStalledIssuesUseCase: GetSyncStalledIssuesUseCase = mock()

    private val underTest = MonitorSyncStalledIssuesUseCase(
        getSyncStalledIssuesUseCase,
        syncRepository
    )

    private val stalledIssues = listOf(
        StalledIssue(
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("/storage/emulated/0/DCIM"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
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
            getSyncStalledIssuesUseCase,
            syncRepository,
        )
    }

    @Test
    fun `test that syncs are refetched every time syncs change`() = runTest {
        whenever(getSyncStalledIssuesUseCase()).thenReturn(stalledIssues)
        whenever(syncRepository.monitorSyncChanges()).thenReturn(
            flow {
                emit(Unit)
                emit(Unit)
                awaitCancellation()
            })

        underTest().test { cancelAndIgnoreRemainingEvents() }

        verify(getSyncStalledIssuesUseCase, times(3)).invoke()
    }

    @Test
    fun `test that correct stalled issues are returned`() = runTest {
        whenever(getSyncStalledIssuesUseCase()).thenReturn(stalledIssues)
        whenever(syncRepository.monitorSyncChanges()).thenReturn(
            flow {
                awaitCancellation()
            })

        underTest()
            .test {
                assertThat(awaitItem()).isEqualTo(stalledIssues)
            }
    }
}