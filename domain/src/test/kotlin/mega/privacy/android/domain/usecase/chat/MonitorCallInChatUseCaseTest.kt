package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.isCallFinished
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorCallInChatUseCaseTest {

    private lateinit var underTest: MonitorCallInChatUseCase

    private val sharedFlow = MutableSharedFlow<ChatCall>()

    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock {
        on { invoke() } doReturn sharedFlow
    }
    private val callRepository = mock<CallRepository>()

    private val chatId = 1L


    @BeforeAll
    fun setup() {
        underTest =
            MonitorCallInChatUseCase(monitorChatCallUpdatesUseCase, callRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(callRepository)
    }

    @ParameterizedTest(name = " when there is a call {0}, initial call status {1} and updated call status is {2}")
    @ArgumentsSource(MonitorCallInChatArgumentsProvider::class)
    fun `test that monitor call in chat returns correctly`(
        hasCall: Boolean,
        initialCallStatus: ChatCallStatus?,
        finalCallStatus: ChatCallStatus,
    ) = runTest {
        val initialCall =
            if (hasCall && initialCallStatus != ChatCallStatus.Destroyed && initialCallStatus != ChatCallStatus.Unknown) {
                mock<ChatCall> {
                    on { this.chatId } doReturn chatId
                    on { this.status } doReturn initialCallStatus
                }
            } else {
                null
            }
        val updatedCall = mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.status } doReturn finalCallStatus
        }
        val finalCall = if (!finalCallStatus.isCallFinished()) updatedCall else null
        whenever(callRepository.getChatCall(chatId)).thenReturn(initialCall).thenReturn(updatedCall)
        underTest(chatId).test {
            Truth.assertThat(awaitItem()).isEqualTo(initialCall)
            sharedFlow.emit(updatedCall)
            Truth.assertThat(awaitItem()).isEqualTo(finalCall)
        }
    }
}

internal class MonitorCallInChatArgumentsProvider : ArgumentsProvider {

    override fun provideArguments(context: ExtensionContext): Stream<out Arguments>? {
        return Stream.of(
            Arguments.of(false, null, ChatCallStatus.Initial),
            Arguments.of(false, null, ChatCallStatus.UserNoPresent),
            Arguments.of(false, null, ChatCallStatus.Connecting),
            Arguments.of(false, null, ChatCallStatus.WaitingRoom),
            Arguments.of(false, null, ChatCallStatus.Joining),
            Arguments.of(false, null, ChatCallStatus.InProgress),
            Arguments.of(false, null, ChatCallStatus.TerminatingUserParticipation),
            Arguments.of(false, null, ChatCallStatus.Destroyed),
            Arguments.of(false, null, ChatCallStatus.Unknown),
            Arguments.of(true, ChatCallStatus.UserNoPresent, ChatCallStatus.Initial),
            Arguments.of(true, ChatCallStatus.Connecting, ChatCallStatus.Initial),
            Arguments.of(true, ChatCallStatus.WaitingRoom, ChatCallStatus.Initial),
            Arguments.of(true, ChatCallStatus.Joining, ChatCallStatus.Initial),
            Arguments.of(true, ChatCallStatus.InProgress, ChatCallStatus.Initial),
            Arguments.of(true, ChatCallStatus.TerminatingUserParticipation, ChatCallStatus.Initial),
            Arguments.of(true, ChatCallStatus.Destroyed, ChatCallStatus.Initial),
            Arguments.of(true, ChatCallStatus.Unknown, ChatCallStatus.Initial),
            Arguments.of(true, ChatCallStatus.Initial, ChatCallStatus.UserNoPresent),
            Arguments.of(true, ChatCallStatus.Connecting, ChatCallStatus.UserNoPresent),
            Arguments.of(true, ChatCallStatus.WaitingRoom, ChatCallStatus.UserNoPresent),
            Arguments.of(true, ChatCallStatus.Joining, ChatCallStatus.UserNoPresent),
            Arguments.of(true, ChatCallStatus.InProgress, ChatCallStatus.UserNoPresent),
            Arguments.of(
                true,
                ChatCallStatus.TerminatingUserParticipation,
                ChatCallStatus.UserNoPresent
            ),
            Arguments.of(true, ChatCallStatus.Destroyed, ChatCallStatus.UserNoPresent),
            Arguments.of(true, ChatCallStatus.Unknown, ChatCallStatus.UserNoPresent),
            Arguments.of(true, ChatCallStatus.Initial, ChatCallStatus.Connecting),
            Arguments.of(true, ChatCallStatus.UserNoPresent, ChatCallStatus.Connecting),
            Arguments.of(true, ChatCallStatus.WaitingRoom, ChatCallStatus.Connecting),
            Arguments.of(true, ChatCallStatus.Joining, ChatCallStatus.Connecting),
            Arguments.of(true, ChatCallStatus.InProgress, ChatCallStatus.Connecting),
            Arguments.of(
                true,
                ChatCallStatus.TerminatingUserParticipation,
                ChatCallStatus.Connecting
            ),
            Arguments.of(true, ChatCallStatus.Destroyed, ChatCallStatus.Connecting),
            Arguments.of(true, ChatCallStatus.Unknown, ChatCallStatus.Connecting),
            Arguments.of(true, ChatCallStatus.Initial, ChatCallStatus.WaitingRoom),
            Arguments.of(true, ChatCallStatus.UserNoPresent, ChatCallStatus.WaitingRoom),
            Arguments.of(true, ChatCallStatus.Connecting, ChatCallStatus.WaitingRoom),
            Arguments.of(true, ChatCallStatus.Joining, ChatCallStatus.WaitingRoom),
            Arguments.of(true, ChatCallStatus.InProgress, ChatCallStatus.WaitingRoom),
            Arguments.of(
                true,
                ChatCallStatus.TerminatingUserParticipation,
                ChatCallStatus.WaitingRoom
            ),
            Arguments.of(true, ChatCallStatus.Destroyed, ChatCallStatus.WaitingRoom),
            Arguments.of(true, ChatCallStatus.Unknown, ChatCallStatus.WaitingRoom),
            Arguments.of(true, ChatCallStatus.Initial, ChatCallStatus.Joining),
            Arguments.of(true, ChatCallStatus.UserNoPresent, ChatCallStatus.Joining),
            Arguments.of(true, ChatCallStatus.Connecting, ChatCallStatus.Joining),
            Arguments.of(true, ChatCallStatus.WaitingRoom, ChatCallStatus.Joining),
            Arguments.of(true, ChatCallStatus.InProgress, ChatCallStatus.Joining),
            Arguments.of(true, ChatCallStatus.TerminatingUserParticipation, ChatCallStatus.Joining),
            Arguments.of(true, ChatCallStatus.Destroyed, ChatCallStatus.Joining),
            Arguments.of(true, ChatCallStatus.Unknown, ChatCallStatus.Joining),
            Arguments.of(true, ChatCallStatus.Initial, ChatCallStatus.InProgress),
            Arguments.of(true, ChatCallStatus.UserNoPresent, ChatCallStatus.InProgress),
            Arguments.of(true, ChatCallStatus.Connecting, ChatCallStatus.InProgress),
            Arguments.of(true, ChatCallStatus.WaitingRoom, ChatCallStatus.InProgress),
            Arguments.of(true, ChatCallStatus.Joining, ChatCallStatus.InProgress),
            Arguments.of(
                true,
                ChatCallStatus.TerminatingUserParticipation,
                ChatCallStatus.InProgress
            ),
            Arguments.of(true, ChatCallStatus.Destroyed, ChatCallStatus.InProgress),
            Arguments.of(true, ChatCallStatus.Unknown, ChatCallStatus.InProgress),
            Arguments.of(true, ChatCallStatus.Initial, ChatCallStatus.TerminatingUserParticipation),
            Arguments.of(
                true,
                ChatCallStatus.UserNoPresent,
                ChatCallStatus.TerminatingUserParticipation
            ),
            Arguments.of(
                true,
                ChatCallStatus.Connecting,
                ChatCallStatus.TerminatingUserParticipation
            ),
            Arguments.of(
                true,
                ChatCallStatus.WaitingRoom,
                ChatCallStatus.TerminatingUserParticipation
            ),
            Arguments.of(true, ChatCallStatus.Joining, ChatCallStatus.TerminatingUserParticipation),
            Arguments.of(
                true,
                ChatCallStatus.InProgress,
                ChatCallStatus.TerminatingUserParticipation
            ),
            Arguments.of(
                true,
                ChatCallStatus.Destroyed,
                ChatCallStatus.TerminatingUserParticipation
            ),
            Arguments.of(true, ChatCallStatus.Unknown, ChatCallStatus.TerminatingUserParticipation),
            Arguments.of(true, ChatCallStatus.Initial, ChatCallStatus.Destroyed),
            Arguments.of(true, ChatCallStatus.UserNoPresent, ChatCallStatus.Destroyed),
            Arguments.of(true, ChatCallStatus.Connecting, ChatCallStatus.Destroyed),
            Arguments.of(true, ChatCallStatus.WaitingRoom, ChatCallStatus.Destroyed),
            Arguments.of(true, ChatCallStatus.Joining, ChatCallStatus.Destroyed),
            Arguments.of(true, ChatCallStatus.InProgress, ChatCallStatus.Destroyed),
            Arguments.of(
                true,
                ChatCallStatus.TerminatingUserParticipation,
                ChatCallStatus.Destroyed
            ),
            Arguments.of(true, ChatCallStatus.Unknown, ChatCallStatus.Destroyed),
            Arguments.of(true, ChatCallStatus.Initial, ChatCallStatus.Unknown),
            Arguments.of(true, ChatCallStatus.UserNoPresent, ChatCallStatus.Unknown),
            Arguments.of(true, ChatCallStatus.Connecting, ChatCallStatus.Unknown),
            Arguments.of(true, ChatCallStatus.WaitingRoom, ChatCallStatus.Unknown),
            Arguments.of(true, ChatCallStatus.Joining, ChatCallStatus.Unknown),
            Arguments.of(true, ChatCallStatus.InProgress, ChatCallStatus.Unknown),
            Arguments.of(true, ChatCallStatus.TerminatingUserParticipation, ChatCallStatus.Unknown),
            Arguments.of(true, ChatCallStatus.Destroyed, ChatCallStatus.Unknown),
        )
    }
}