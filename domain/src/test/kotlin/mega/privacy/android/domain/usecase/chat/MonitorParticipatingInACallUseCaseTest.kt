package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.testutils.hotFlow
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
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
    private val getChatCallUseCase: GetChatCallUseCase = mock()

    private val chatId = 9876L

    @BeforeAll
    fun setup() {
        underTest = MonitorParticipatingInACallUseCase(
            monitorChatCallUpdatesUseCase = monitorChatCallUpdates,
            getCurrentChatCallUseCase = getCurrentChatCallUseCase,
            getChatCallUseCase = getChatCallUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorChatCallUpdates,
            getCurrentChatCallUseCase,
            getChatCallUseCase
        )
    }

    @ParameterizedTest(name = "chat call status is {0} and getChatCallUseCase is {1}")
    @MethodSource("provideParameters")
    fun `test that user is participating in a call when`(
        chatCallStatus: ChatCallStatus,
        currentChatCallStatus: ChatCallStatus?,
    ) = runTest {
        // GIVEN
        val currentCall = currentChatCallStatus?.let {
            mock<ChatCall> {
                on { this.status } doReturn currentChatCallStatus
                on { this.chatId } doReturn chatId
            }
        }
        val currentCallChatChatId = currentCall?.chatId
        whenever(getCurrentChatCallUseCase()).thenReturn(currentCallChatChatId)
        currentCallChatChatId?.let {
            whenever(getChatCallUseCase(it)).thenReturn(currentCall)
        }
        val call = mock<ChatCall> {
            on { this.status } doReturn chatCallStatus
            on { this.chatId } doReturn (1234L)
        }
        whenever(monitorChatCallUpdates()).thenReturn(hotFlow(call))
        val lastUpdateExpected =
            if (chatCallStatus == ChatCallStatus.Initial || chatCallStatus == ChatCallStatus.WaitingRoom) {
                call
            } else {
                whenever(getCurrentChatCallUseCase()).thenReturn(null)
                null
            }
        val firstUpdateExpected =
            if (chatCallStatus == ChatCallStatus.Initial || chatCallStatus == ChatCallStatus.WaitingRoom) {
                currentCall
            } else {
                null
            }
        underTest.invoke().test {
            assertThat(awaitItem()).isEqualTo(firstUpdateExpected)
            assertThat(awaitItem()).isEqualTo(lastUpdateExpected)
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

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.Initial),
        Arguments.of(ChatCallStatus.WaitingRoom, ChatCallStatus.Initial),
        Arguments.of(ChatCallStatus.Destroyed, ChatCallStatus.Initial),
        Arguments.of(ChatCallStatus.TerminatingUserParticipation, ChatCallStatus.Initial),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.Connecting),
        Arguments.of(ChatCallStatus.WaitingRoom, ChatCallStatus.Connecting),
        Arguments.of(ChatCallStatus.Destroyed, ChatCallStatus.Connecting),
        Arguments.of(ChatCallStatus.TerminatingUserParticipation, ChatCallStatus.Connecting),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.Joining),
        Arguments.of(ChatCallStatus.WaitingRoom, ChatCallStatus.Joining),
        Arguments.of(ChatCallStatus.Destroyed, ChatCallStatus.Joining),
        Arguments.of(ChatCallStatus.TerminatingUserParticipation, ChatCallStatus.Joining),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.InProgress),
        Arguments.of(ChatCallStatus.WaitingRoom, ChatCallStatus.InProgress),
        Arguments.of(ChatCallStatus.Destroyed, ChatCallStatus.InProgress),
        Arguments.of(ChatCallStatus.TerminatingUserParticipation, ChatCallStatus.InProgress),
        Arguments.of(ChatCallStatus.Initial, null),
        Arguments.of(ChatCallStatus.WaitingRoom, null),
        Arguments.of(ChatCallStatus.Destroyed, null),
        Arguments.of(ChatCallStatus.TerminatingUserParticipation, null),
    )
}