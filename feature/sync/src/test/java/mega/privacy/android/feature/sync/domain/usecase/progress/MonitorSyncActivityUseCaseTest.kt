package mega.privacy.android.feature.sync.domain.usecase.progress

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.progress.SyncActivity
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEpisodeProgress
import mega.privacy.android.feature.sync.domain.repository.SyncProgressRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncActivityUseCaseTest {

    private lateinit var underTest: MonitorSyncActivityUseCase

    private val syncProgressRepository = mock<SyncProgressRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorSyncActivityUseCase(syncProgressRepository = syncProgressRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncProgressRepository)
    }

    @Test
    fun `test that invoke returns the sync activity from the repository`() = runTest {
        val seeded = SyncActivity(
            isSeeded = true,
            syncs = emptyList(),
            progress = SyncEpisodeProgress.None,
        )
        whenever(syncProgressRepository.monitorSyncActivity())
            .thenReturn(flowOf(SyncActivity.NotSeeded, seeded))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(SyncActivity.NotSeeded)
            assertThat(awaitItem()).isEqualTo(seeded)
            awaitComplete()
        }
    }
}
