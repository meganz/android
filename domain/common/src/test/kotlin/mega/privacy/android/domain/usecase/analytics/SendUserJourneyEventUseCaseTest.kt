package mega.privacy.android.domain.usecase.analytics

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.StatisticsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
internal class SendUserJourneyEventUseCaseTest {
    private lateinit var underTest: SendUserJourneyEventUseCase

    private val statisticsRepository = mock<StatisticsRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = SendUserJourneyEventUseCase(statisticsRepository = statisticsRepository)
    }

    @Test
    internal fun `test that add journey id is set to true`() = runTest {
        underTest(1, "message", "viewId")
        verify(statisticsRepository).sendEvent(
            eventId = any(),
            message = any(),
            addJourneyId = eq(true),
            viewId = any(),
        )
    }
}