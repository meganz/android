package mega.privacy.android.domain.usecase.createaccount

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorAccountConfirmationUseCaseTest {

    private lateinit var underTest: MonitorAccountConfirmationUseCase

    private lateinit var notificationsRepository: NotificationsRepository

    @BeforeAll
    fun setup() {
        notificationsRepository = mock()
        underTest = MonitorAccountConfirmationUseCase(notificationsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(notificationsRepository)
    }

    @ParameterizedTest(
        name = " {1} when monitor event returns event with type {0}"
    )
    @MethodSource("provideParameters")
    fun `test that monitor account confirmation returns `(
        eventType: EventType,
        expectedResult: Boolean?,
    ) = runTest {
        val event = mock<Event> {
            on { type }.thenReturn(eventType)
        }
        whenever(notificationsRepository.monitorEvent()).thenReturn(flowOf(event))
        underTest().test {
            expectedResult?.let {
                val result = awaitItem()
                awaitComplete()
                Truth.assertThat(result).isEqualTo(it)
            } ?: awaitComplete()
        }
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(EventType.AccountBlocked, null),
        Arguments.of(EventType.AccountConfirmation, true),
        Arguments.of(EventType.Unknown, null),
        Arguments.of(EventType.Storage, null)
    )
}