package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.statistics.DisableSoundNotification
import mega.privacy.android.domain.entity.statistics.EnableSoundNotification
import mega.privacy.android.domain.entity.statistics.EndCallEmptyCall
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.entity.statistics.EndedEmptyCallTimeout
import mega.privacy.android.domain.entity.statistics.MeetingsStatisticsEvents
import mega.privacy.android.domain.entity.statistics.StayOnCallEmptyCall
import mega.privacy.android.domain.repository.StatisticsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import java.util.stream.Stream

/**
 * Test class for [SendStatisticsMeetingsUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendStatisticsMeetingsUseCaseTest {

    private lateinit var underTest: SendStatisticsMeetingsUseCase

    private val statisticsRepository = mock<StatisticsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SendStatisticsMeetingsUseCase(
            statisticsRepository = statisticsRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(statisticsRepository)
    }

    @ParameterizedTest(name = "test that send event for \"{0}\" statistics is fired")
    @MethodSource("provideParameters")
    fun `test that send event for meetings statistics is fired`(
        name: String,
        event: MeetingsStatisticsEvents,
    ) {
        runTest {
            underTest(event)
            verify(statisticsRepository).sendEvent(event.id, event.message)
        }
    }

    companion object {
        @JvmStatic
        private fun provideParameters(): Stream<Arguments?>? {
            return Stream.of(
                Arguments.of(EnableSoundNotification().message, EnableSoundNotification()),
                Arguments.of(DisableSoundNotification().message, DisableSoundNotification()),
                Arguments.of(StayOnCallEmptyCall().message, StayOnCallEmptyCall()),
                Arguments.of(EndCallEmptyCall().message, EndCallEmptyCall()),
                Arguments.of(EndCallForAll().message, EndCallForAll()),
                Arguments.of(EndedEmptyCallTimeout().message, EndedEmptyCallTimeout())
            )
        }
    }
}