package mega.privacy.android.feature.sync.domain.usecase.progress

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEpisodeProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseToggle
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorUserPausedSyncIdsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncPauseToggleUseCaseTest {

    private lateinit var underTest: MonitorSyncPauseToggleUseCase

    private val monitorSyncRunSnapshotUseCase = mock<MonitorSyncRunSnapshotUseCase>()
    private val monitorUserPausedSyncIdsUseCase = mock<MonitorUserPausedSyncIdsUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorSyncPauseToggleUseCase(
            monitorSyncRunSnapshotUseCase = monitorSyncRunSnapshotUseCase,
            monitorUserPausedSyncIdsUseCase = monitorUserPausedSyncIdsUseCase,
            getSyncPauseToggleUseCase = GetSyncPauseToggleUseCase(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(monitorSyncRunSnapshotUseCase, monitorUserPausedSyncIdsUseCase)
    }

    @Test
    fun `test that invoke emits nothing until the sync run snapshot is seeded`() = runTest {
        val runSnapshot = MutableStateFlow(SyncRunSnapshot.NotSeeded)
        val userPausedSyncIds = MutableStateFlow(setOf(1L))
        stubInputs(runSnapshot, userPausedSyncIds)

        underTest().test {
            expectNoEvents()

            runSnapshot.value = seeded(sync(1, SyncRunState.Suspended))

            assertThat(awaitItem()).isEqualTo(SyncPauseToggle.ResumeAll)
        }
    }

    @Test
    fun `test that invoke emits resume all once the user pause rows and the suspended states agree`() =
        runTest {
            val runSnapshot = MutableStateFlow(seeded(sync(1, SyncRunState.Running)))
            val userPausedSyncIds = MutableStateFlow(emptySet<Long>())
            stubInputs(runSnapshot, userPausedSyncIds)

            underTest().test {
                assertThat(awaitItem()).isEqualTo(SyncPauseToggle.PauseAll)

                userPausedSyncIds.value = setOf(1L)
                expectNoEvents()

                runSnapshot.value = seeded(sync(1, SyncRunState.Suspended))
                assertThat(awaitItem()).isEqualTo(SyncPauseToggle.ResumeAll)
            }
        }

    @Test
    fun `test that invoke emits pause all again when a paused sync resumes`() = runTest {
        val runSnapshot = MutableStateFlow(seeded(sync(1, SyncRunState.Suspended)))
        val userPausedSyncIds = MutableStateFlow(setOf(1L))
        stubInputs(runSnapshot, userPausedSyncIds)

        underTest().test {
            assertThat(awaitItem()).isEqualTo(SyncPauseToggle.ResumeAll)

            userPausedSyncIds.value = emptySet()
            runSnapshot.value = seeded(sync(1, SyncRunState.Running))

            assertThat(awaitItem()).isEqualTo(SyncPauseToggle.PauseAll)
            expectNoEvents()
        }
    }

    @Test
    fun `test that invoke emits resume all when only the user pause ids change`() = runTest {
        val runSnapshot = MutableStateFlow(seeded(sync(1, SyncRunState.Suspended)))
        val userPausedSyncIds = MutableStateFlow(emptySet<Long>())
        stubInputs(runSnapshot, userPausedSyncIds)

        underTest().test {
            assertThat(awaitItem()).isEqualTo(SyncPauseToggle.PauseAll)

            userPausedSyncIds.value = setOf(1L)

            assertThat(awaitItem()).isEqualTo(SyncPauseToggle.ResumeAll)
        }
    }

    @Test
    fun `test that invoke emits pause all for a seeded run snapshot with no syncs`() = runTest {
        stubInputs(MutableStateFlow(seeded()), MutableStateFlow(emptySet()))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(SyncPauseToggle.PauseAll)
        }
    }

    @Test
    fun `test that invoke emits distinct values only`() = runTest {
        val runSnapshot = MutableStateFlow(seeded(sync(1, SyncRunState.Running, pending = 0)))
        val userPausedSyncIds = MutableStateFlow(emptySet<Long>())
        stubInputs(runSnapshot, userPausedSyncIds)

        underTest().test {
            assertThat(awaitItem()).isEqualTo(SyncPauseToggle.PauseAll)

            (1..3).forEach { pending ->
                runSnapshot.value = seeded(sync(1, SyncRunState.Running, pending = pending))
            }

            expectNoEvents()
        }
    }

    private fun stubInputs(
        runSnapshot: MutableStateFlow<SyncRunSnapshot>,
        userPausedSyncIds: MutableStateFlow<Set<Long>>,
    ) {
        whenever(monitorSyncRunSnapshotUseCase()).thenReturn(runSnapshot)
        whenever(monitorUserPausedSyncIdsUseCase()).thenReturn(userPausedSyncIds)
    }

    private fun seeded(vararg syncs: SyncWorkState) = SyncRunSnapshot(
        isSeeded = true,
        syncs = syncs.toList(),
        progress = SyncEpisodeProgress.None,
    )

    private fun sync(id: Long, runState: SyncRunState, pending: Int? = null) = SyncWorkState(
        syncId = id,
        runState = runState,
        isStorageOverQuota = false,
        stats = pending?.let {
            SyncStatsSample(
                isScanning = false,
                isSyncing = true,
                pendingUploads = it,
                pendingDownloads = 0,
            )
        },
        runningSinceMillis = null,
    )
}
