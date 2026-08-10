package mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.repository.QASimulationRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetPreviousSimulatedLastActiveDateUseCaseTest {

    private val qaSimulationRepository = mock<QASimulationRepository>()

    private lateinit var underTest: GetPreviousSimulatedLastActiveDateUseCase

    @Before
    fun setUp() {
        underTest = GetPreviousSimulatedLastActiveDateUseCase(qaSimulationRepository)
    }

    @Test
    fun `test that invoke returns acknowledged purge minus purge offset when a purge was acknowledged`() =
        runTest {
            val acknowledgedPurge =
                LAST_ACTIVE_SECONDS + PURGE_OFFSET_SECONDS
            whenever(qaSimulationRepository.getLastPurgeAcknowledged()).thenReturn(acknowledgedPurge)

            assertThat(underTest()).isEqualTo(LAST_ACTIVE_SECONDS)
        }

    @Test
    fun `test that invoke returns null when no purge has been acknowledged`() =
        runTest {
            whenever(qaSimulationRepository.getLastPurgeAcknowledged()).thenReturn(0L)

            assertThat(underTest()).isNull()
        }

    companion object {
        private const val LAST_ACTIVE_SECONDS = 1_700_000_000L
    }
}
