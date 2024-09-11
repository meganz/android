package mega.privacy.android.feature.sync.domain.usecase.worker

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.GetSyncFrequencyUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.StartSyncWorkerUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartSyncWorkerUseCaseTest {

    private val syncRepository: SyncRepository = mock()
    private val getSyncFrequencyUseCase: GetSyncFrequencyUseCase = mock()
    private val getSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()
    private val underTest: StartSyncWorkerUseCase =
        StartSyncWorkerUseCase(syncRepository, getSyncFrequencyUseCase, getSyncByWiFiUseCase)

    @Test
    fun `test that invoke calls sync repository start sync worker method`() = runTest {
        whenever(getSyncFrequencyUseCase()).thenReturn(15)
        whenever(getSyncByWiFiUseCase()).thenReturn(flowOf(true))

        underTest()

        verify(syncRepository).startSyncWorker(15, true)
    }
}