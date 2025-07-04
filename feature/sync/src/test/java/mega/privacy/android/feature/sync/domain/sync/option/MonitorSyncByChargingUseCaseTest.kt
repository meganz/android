package mega.privacy.android.feature.sync.domain.sync.option

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByChargingUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorSyncByChargingUseCaseTest {

    private val syncPreferencesRepository: SyncPreferencesRepository = mock()
    private val underTest = MonitorSyncByChargingUseCase(syncPreferencesRepository)

    @Test
    fun `test that invoke returns true when repository returns true`() = runTest {
        whenever(syncPreferencesRepository.monitorSyncByCharging()).thenReturn(flowOf(true))

        val result = underTest().toList()

        assertEquals(listOf(true), result)
    }

    @Test
    fun `test that invoke returns false when repository returns false`() = runTest {
        whenever(syncPreferencesRepository.monitorSyncByCharging()).thenReturn(flowOf(false))

        val result = underTest().toList()

        assertEquals(listOf(false), result)
    }

    @Test
    fun `test that invoke returns false when repository returns null`() = runTest {
        whenever(syncPreferencesRepository.monitorSyncByCharging()).thenReturn(flowOf(null))

        val result = underTest().toList()

        assertEquals(listOf(false), result)
    }
}
