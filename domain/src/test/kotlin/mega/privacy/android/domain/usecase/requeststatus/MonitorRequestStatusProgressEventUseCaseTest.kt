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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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

    @Test
    fun `test that progress is set to null when number is -1L`() = runTest {
        val event = mock<RequestStatusProgressEvent> {
            on { progress } doReturn -1L
        }
        val eventsFlow: Flow<Event> = flowOf(event)
        whenever(notificationsRepository.monitorEvent()) doReturn (eventsFlow)

        val result = underTest().toList()

        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isNull()
    }
}