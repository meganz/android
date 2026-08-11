package mega.privacy.android.feature.sync.domain.sync.option

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetPauseSyncOnBatterySaverUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetPauseSyncOnBatterySaverUseCaseTest {

    private val syncPreferencesRepository: SyncPreferencesRepository = mock()
    private val underTest = SetPauseSyncOnBatterySaverUseCase(syncPreferencesRepository)

    @Test
    fun `test that invoke calls repository setPauseSyncOnBatterySaver with true`() = runTest {
        underTest(true)

        verify(syncPreferencesRepository).setPauseSyncOnBatterySaver(true)
    }

    @Test
    fun `test that invoke calls repository setPauseSyncOnBatterySaver with false`() = runTest {
        underTest(false)

        verify(syncPreferencesRepository).setPauseSyncOnBatterySaver(false)
    }
}
