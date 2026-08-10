package mega.privacy.android.domain.usecase.requeststatus

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountConfirmationEvent
import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.RequestStatusProgressEvent
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

class MonitorRequestStatusProgressEventUseCaseTest {

    private val notificationsRepository: NotificationsRepository = mock()
    private lateinit var underTest: MonitorRequestStatusProgressEventUseCase

    @BeforeEach
    fun setUp() {
        underTest = MonitorRequestStatusProgressEventUseCase(notificationsRepository)
    }

    @Test
    fun `test that RequestStatusProgress events are filtered when invoked`() = runTest {
        val event1 = mock<RequestStatusProgressEvent> {
            on { progress } doReturn 500L
        }
        val event2 = mock<AccountConfirmationEvent>()
        val eventsFlow: Flow<Event> = flowOf(event1, event2)
        whenever(notificationsRepository.monitorEvent()) doReturn (eventsFlow)

        val result = underTest().toList()

        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]?.floatValue).isEqualTo(0.5f)
    }

    @ParameterizedTest
    @MethodSource("progressTestArguments")
    fun `test that progress is handled correctly for various values`(
        progressValue: Long,
        expectedProgress: Float?
    ) = runTest {
        val event = mock<RequestStatusProgressEvent> {
            on { progress } doReturn progressValue
        }
        val eventsFlow: Flow<Event> = flowOf(event)
        whenever(notificationsRepository.monitorEvent()) doReturn (eventsFlow)

        val result = underTest().toList()

        assertThat(result.size).isEqualTo(1)
        if (expectedProgress == null) {
            assertThat(result[0]).isNull()
        } else {
            assertThat(result[0]?.floatValue).isEqualTo(expectedProgress)
        }
    }

    companion object {
        @JvmStatic
        fun progressTestArguments(): Stream<Arguments> = Stream.of(
            Arguments.of(-1L, null),
            Arguments.of(0L, 0.0f),
            Arguments.of(100L, 0.1f),
            Arguments.of(995L, null)
        )
    }
}