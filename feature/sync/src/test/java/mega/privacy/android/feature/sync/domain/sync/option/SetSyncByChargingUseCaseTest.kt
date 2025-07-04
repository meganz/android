package mega.privacy.android.feature.sync.domain.sync.option

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSyncByChargingUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetSyncByChargingUseCaseTest {

    private val syncPreferencesRepository: SyncPreferencesRepository = mock()
    private val underTest = SetSyncByChargingUseCase(syncPreferencesRepository)

    @Test
    fun `test that invoke calls repository setSyncByCharging with true`() = runTest {
        underTest(true)

        verify(syncPreferencesRepository).setSyncByCharging(true)
    }

    @Test
    fun `test that invoke calls repository setSyncByCharging with false`() = runTest {
        underTest(false)

        verify(syncPreferencesRepository).setSyncByCharging(false)
    }
}
