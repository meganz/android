package mega.privacy.android.feature.sync.domain.usecase.progress

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineSignals
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncEngineSignalsUseCaseTest {

    private lateinit var underTest: MonitorSyncEngineSignalsUseCase

    private val syncRepository = mock<SyncRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorSyncEngineSignalsUseCase(syncRepository = syncRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncRepository)
    }

    @Test
    fun `test that invoke returns the repository signals`() = runTest {
        val waiting = SyncEngineSignals(isWaiting = true, isSyncStalled = false)
        whenever(syncRepository.monitorSyncEngineSignals()).thenReturn(flowOf(waiting))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(waiting)
            awaitComplete()
        }
    }
}
