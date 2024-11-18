package mega.privacy.android.domain.usecase.psa

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.psa.Psa
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.stub
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorPsaUseCaseTest {
    lateinit var underTest: MonitorPsaUseCase

    private val fetchPsaUseCase = mock<FetchPsaUseCase?>()

    @BeforeEach
    fun setUp() {
        underTest = MonitorPsaUseCase(
            fetchPsaUseCase = fetchPsaUseCase,
            psaCheckFrequency = 10.seconds
        )
    }

    @Test
    fun `test that fetch use case is called`() = runTest {
        val currentTime: Long = 0
        underTest.invoke { currentTime }.test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(fetchPsaUseCase).invoke(currentTime)
    }

    @Test
    fun `test that fetch use case is called after every check frequency`() = runTest {
        fetchPsaUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(getPsa(1))
        }

        val initial = testScheduler.currentTime
        underTest.invoke { testScheduler.currentTime - initial }.test {
            testScheduler.advanceTimeBy(50.seconds)
            val events: List<Event<Psa>> = cancelAndConsumeRemainingEvents()
            assertThat(events.filterIsInstance<Event.Item<Psa>>().size).isEqualTo(5)
        }
    }

    private fun getPsa(id: Int) = Psa(
        id = id,
        title = "title",
        text = "description",
        imageUrl = null,
        positiveText = null,
        positiveLink = null,
        url = null
    )
}