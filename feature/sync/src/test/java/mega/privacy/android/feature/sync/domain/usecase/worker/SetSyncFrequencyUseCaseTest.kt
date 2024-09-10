package mega.privacy.android.feature.sync.domain.usecase.worker

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.SetSyncFrequencyUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetSyncFrequencyUseCaseTest {

    private val syncPreferencesRepository: SyncPreferencesRepository = mock()
    private val underTest: SetSyncFrequencyUseCase =
        SetSyncFrequencyUseCase(syncPreferencesRepository)

    @Test
    fun `test that set sync frequency fetches data from repository`() = runTest {
        underTest(15)

        verify(syncPreferencesRepository).setSyncFrequencyInMinutes(15)
    }
}