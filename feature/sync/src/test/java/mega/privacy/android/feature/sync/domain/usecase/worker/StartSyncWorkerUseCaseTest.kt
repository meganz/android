package mega.privacy.android.feature.sync.domain.usecase.worker

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.StartSyncWorkerUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartSyncWorkerUseCaseTest {

    private val syncRepository: SyncRepository = mock()
    private val underTest: StartSyncWorkerUseCase = StartSyncWorkerUseCase(syncRepository)

    @Test
    fun `test that invoke calls sync repository start sync worker method`() = runTest {
        underTest()

        verify(syncRepository).startSyncWorker()
    }
}