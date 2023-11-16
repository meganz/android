package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.testutils.hotFlow
import mega.privacy.android.domain.usecase.meeting.GetCurrentChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorParticipatingInACallUseCaseTest {
    private lateinit var underTest: MonitorParticipatingInACallUseCase

    private val monitorChatCallUpdates: MonitorChatCallUpdatesUseCase = mock()
    private val getCurrentChatCallUseCase: GetCurrentChatCallUseCase = mock()

    @BeforeAll
    fun setup() {
        underTest = MonitorParticipatingInACallUseCase(
            monitorChatCallUpdatesUseCase = monitorChatCallUpdates,
            getCurrentChatCallUseCase = getCurrentChatCallUseCase,
        )
    }

    @ParameterizedTest(name = "chat call status is {0} and getCurrentChatCallUseCase is {1}")
    @MethodSource("provideParameters")
    fun `test that user is participating in a call when`(
        chatCallStatus: ChatCallStatus,
        currentChatCall: Long?,
    ) = runTest {
        // GIVEN
        val callId = 1234L
        whenever(getCurrentChatCallUseCase()).thenReturn(currentChatCall)
        val call = mock<ChatCall> {
            on { this.status } doReturn chatCallStatus
            on { this.chatId } doReturn (callId)
        }
        whenever(monitorChatCallUpdates()).thenReturn(hotFlow(call))


        underTest.invoke().test {
            assertThat(awaitItem()).isEqualTo(currentChatCall)
            assertThat(awaitItem()).isEqualTo(
                if (call.status == ChatCallStatus.Initial
                    || call.status == ChatCallStatus.WaitingRoom
                ) callId else currentChatCall
            )
        }
    }

    @ParameterizedTest(name = "test that {0} status returns no events")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["UserNoPresent", "Connecting", "Joining", "InProgress", "Unknown"]
    )
    fun `test that non monitored status returns no value`(status: ChatCallStatus) = runTest {
        val flow = MutableSharedFlow<ChatCall>()
        whenever(getCurrentChatCallUseCase()).thenReturn(null)
        whenever(monitorChatCallUpdates()).thenReturn(flow)
        underTest().test {
            assertThat(awaitItem()).isNull()
            val call = mock<ChatCall> {
                on { this.status } doReturn status
            }
            flow.emit(call)
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }


    @BeforeEach
    fun resetMocks() {
        reset(
            monitorChatCallUpdates,
            getCurrentChatCallUseCase,
        )
    }

    companion object {
        private const val currentCallId = 9876L

        @JvmStatic
        private fun provideParameters(): Stream<Arguments> = Stream.of(
            Arguments.of(ChatCallStatus.Initial, currentCallId),
            Arguments.of(ChatCallStatus.WaitingRoom, currentCallId),
            Arguments.of(ChatCallStatus.Destroyed, currentCallId),
            Arguments.of(ChatCallStatus.TerminatingUserParticipation, currentCallId),
            Arguments.of(ChatCallStatus.Initial, null),
            Arguments.of(ChatCallStatus.WaitingRoom, null),
            Arguments.of(ChatCallStatus.Destroyed, null),
            Arguments.of(ChatCallStatus.TerminatingUserParticipation, null),
        )
    }
}