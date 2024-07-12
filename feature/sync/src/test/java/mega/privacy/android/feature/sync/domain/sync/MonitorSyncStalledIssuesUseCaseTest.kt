package mega.privacy.android.feature.sync.domain.sync

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncStalledIssuesUseCaseTest {

    private val syncRepository: SyncRepository = mock()

    private val underTest = MonitorSyncStalledIssuesUseCase(
        syncRepository
    )

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

    @AfterEach
    fun resetAndTearDown() {
        reset(
            syncRepository,
        )
    }

    @Test
    fun `test that monitor stalled issues emits flow of stalled issues`() = runTest {
        whenever(syncRepository.monitorStalledIssues()).thenReturn(
            flow {
                emit(stalledIssues)
                awaitCancellation()
            },
        )

        underTest().test {
            val first = awaitItem()
            assertThat(first.size).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }
}