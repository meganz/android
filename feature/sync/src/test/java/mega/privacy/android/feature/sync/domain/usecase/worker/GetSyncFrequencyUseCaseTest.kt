package mega.privacy.android.feature.sync.domain.usecase.worker

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.GetSyncFrequencyUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSyncFrequencyUseCaseTest {

    private val syncPreferencesRepository: SyncPreferencesRepository = mock()
    private val underTest: GetSyncFrequencyUseCase =
        GetSyncFrequencyUseCase(syncPreferencesRepository)

    @Test
    fun `test that get sync frequency fetches data from repository`() = runTest {
        whenever(syncPreferencesRepository.getSyncFrequencyMinutes()).thenReturn(15)

        val result = underTest()

        assertThat(result).isEqualTo(15)
    }
}