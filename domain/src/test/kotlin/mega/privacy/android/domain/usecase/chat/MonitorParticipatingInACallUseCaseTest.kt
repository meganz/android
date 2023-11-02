package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.usecase.meeting.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorParticipatingInACallUseCaseTest {
    private val monitorChatCallUpdates: MonitorChatCallUpdates = mock()
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase = mock()
    private lateinit var underTest: MonitorParticipatingInACallUseCase

    @BeforeAll
    fun setup() {
        underTest = MonitorParticipatingInACallUseCase(
            monitorChatCallUpdates,
            isParticipatingInChatCallUseCase,
        )
    }

    @ParameterizedTest(name = "chat call status is {0} and isParticipatingInChatCall is {1}")
    @MethodSource("provideParameters")
    fun `test that user is participating in a call when`(
        chatCallStatus: ChatCallStatus,
        isParticipatingInChatCall: Boolean,
    ) = runTest {
        // GIVEN
        val flow = MutableSharedFlow<ChatCall>()
        whenever(isParticipatingInChatCallUseCase()).thenReturn(isParticipatingInChatCall)
        whenever(monitorChatCallUpdates()).thenReturn(flow)
        underTest.invoke().test {
            Truth.assertThat(awaitItem()).isEqualTo(isParticipatingInChatCall)
            val call = mock<ChatCall> {
                on { this.status } doReturn chatCallStatus
            }
            flow.emit(call)
            Truth.assertThat(awaitItem()).isEqualTo(
                call.status == ChatCallStatus.Initial
                        || call.status == ChatCallStatus.WaitingRoom || isParticipatingInChatCall
            )
        }
    }

    @Test
    fun `test that no user present status returns no value`() = runTest{
        val flow = MutableSharedFlow<ChatCall>()
        whenever(isParticipatingInChatCallUseCase()).thenReturn(false)
        whenever(monitorChatCallUpdates()).thenReturn(flow)
        underTest().test {
            Truth.assertThat(awaitItem()).isFalse()
            val call = mock<ChatCall> {
                on { this.status } doReturn ChatCallStatus.UserNoPresent
            }
            flow.emit(call)
            Truth.assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorChatCallUpdates,
            isParticipatingInChatCallUseCase,
        )
    }

    companion object {
        @JvmStatic
        private fun provideParameters(): Stream<Arguments> = Stream.of(
            Arguments.of(ChatCallStatus.Initial, true),
            Arguments.of(ChatCallStatus.WaitingRoom, true),
            Arguments.of(ChatCallStatus.Destroyed, true),
            Arguments.of(ChatCallStatus.TerminatingUserParticipation, true),
            Arguments.of(ChatCallStatus.Initial, false),
            Arguments.of(ChatCallStatus.WaitingRoom, false),
            Arguments.of(ChatCallStatus.Destroyed, false),
            Arguments.of(ChatCallStatus.TerminatingUserParticipation, false),
        )
    }
}