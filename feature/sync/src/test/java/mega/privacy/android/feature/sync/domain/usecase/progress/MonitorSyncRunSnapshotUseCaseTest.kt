package mega.privacy.android.feature.sync.domain.usecase.progress

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEpisodeProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.repository.SyncProgressRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncRunSnapshotUseCaseTest {

    private lateinit var underTest: MonitorSyncRunSnapshotUseCase

    private val syncProgressRepository = mock<SyncProgressRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorSyncRunSnapshotUseCase(syncProgressRepository = syncProgressRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncProgressRepository)
    }

    @Test
    fun `test that invoke returns the sync run snapshot from the repository`() = runTest {
        val seeded = SyncRunSnapshot(
            isSeeded = true,
            syncs = emptyList(),
            progress = SyncEpisodeProgress.None,
        )
        whenever(syncProgressRepository.monitorSyncRunSnapshot())
            .thenReturn(flowOf(SyncRunSnapshot.NotSeeded, seeded))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(SyncRunSnapshot.NotSeeded)
            assertThat(awaitItem()).isEqualTo(seeded)
            awaitComplete()
        }
    }
}
