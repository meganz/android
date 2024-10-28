package mega.privacy.android.domain.usecase.requeststatus

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.NormalEvent
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
        val event1 = mock<NormalEvent> {
            on { type } doReturn EventType.RequestStatusProgress
        }
        val event2 = mock<NormalEvent> {
            on { type } doReturn EventType.AccountConfirmation
        }
        val eventsFlow: Flow<Event> = flowOf(event1, event2)
        whenever(notificationsRepository.monitorEvent()) doReturn (eventsFlow)

        val result = underTest().toList()

        assertThat(result.size).isEqualTo(1)
        assertThat(result[0].type).isEqualTo(EventType.RequestStatusProgress)
    }
}