package mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.repository.QASimulationRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class SimulateUserLastActiveDateUseCaseTest {

    private val qaSimulationRepository = mock<QASimulationRepository>()

    private lateinit var underTest: SimulateUserLastActiveDateUseCase

    @Before
    fun setUp() {
        underTest = SimulateUserLastActiveDateUseCase(qaSimulationRepository)
    }

    @Test
    fun `test that invoke writes the purge schedule derived from the selected last active date`() =
        runTest {
            underTest(LAST_ACTIVE_SECONDS)

            verify(qaSimulationRepository).setDevOptForPurge(
                purgeTimestamp = LAST_ACTIVE_SECONDS + PURGE_OFFSET_SECONDS,
                reason = PURGE_REASON_INACTIVE,
                warningTimestamp = LAST_ACTIVE_SECONDS + WARNING_OFFSET_SECONDS,
                lastActiveTimestamp = LAST_ACTIVE_SECONDS,
            )
        }

    @Test
    fun `test that invoke derives positive timestamps with purge scheduled after warning`() =
        runTest {
            val purge = argumentCaptor<Long>()
            val reason = argumentCaptor<Int>()
            val warning = argumentCaptor<Long>()
            val lastActive = argumentCaptor<Long>()

            underTest(LAST_ACTIVE_SECONDS)

            verify(qaSimulationRepository).setDevOptForPurge(
                purge.capture(),
                reason.capture(),
                warning.capture(),
                lastActive.capture(),
            )
            assertThat(purge.firstValue).isGreaterThan(0L)
            assertThat(warning.firstValue).isGreaterThan(0L)
            assertThat(purge.firstValue).isGreaterThan(warning.firstValue)
        }

    companion object {
        /** A safely positive epoch-seconds value (~2024) for the selected last active date. */
        private val LAST_ACTIVE_SECONDS = TimeUnit.DAYS.toSeconds(20_000)

        // Todo: Will be replaced with MegaApiJava constants in the next version
        private const val PURGE_REASON_INACTIVE = 4
    }
}
