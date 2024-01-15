package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetCurrentCallIdsInOtherChatsUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorParticipatingInACallInOtherChatsUseCaseTest {

    private lateinit var underTest: MonitorParticipatingInACallInOtherChatsUseCase

    private val monitorChatCallUpdates: MonitorChatCallUpdatesUseCase = mock()
    private val getCurrentCallIdsInOtherChatsUseCase: GetCurrentCallIdsInOtherChatsUseCase = mock()
    private val getChatCallUseCase: GetChatCallUseCase = mock()

    private val currentChatId = 1234L
    private val otherChatId = 4321L
    private val updateFlow = MutableSharedFlow<ChatCall>()

    @BeforeAll
    fun setup() {
        underTest = MonitorParticipatingInACallInOtherChatsUseCase(
            monitorChatCallUpdatesUseCase = monitorChatCallUpdates,
            getCurrentCallIdsInOtherChatsUseCase = getCurrentCallIdsInOtherChatsUseCase,
            getChatCallUseCase = getChatCallUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getCurrentCallIdsInOtherChatsUseCase,
            getChatCallUseCase
        )
        wheneverBlocking { monitorChatCallUpdates() }.thenReturn(updateFlow)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @ParameterizedTest(name = "initial call status is {0} with chat id {2} and updated call status is {1} with chat id {3}")
    @MethodSource("provideParameters")
    fun `test that user has a call in other chat when`(
        initialChatCallStatus: ChatCallStatus,
        updatedChatCallStatus: ChatCallStatus,
        initialCallChatId: Long,
        updatedCallChatId: Long,
    ) = runTest {
        // GIVEN
        val initialCall = mock<ChatCall> {
            on { this.status } doReturn initialChatCallStatus
            on { this.chatId } doReturn initialCallChatId
        }
        val updatedCall = mock<ChatCall> {
            on { this.status } doReturn updatedChatCallStatus
            on { this.chatId } doReturn updatedCallChatId
        }
        val initialList = when {
            initialCallChatId == currentChatId || !initialChatCallStatus.isCurrentCall() -> emptyList()
            else -> listOf(initialCallChatId)
        }
        val updatedList = when {
            ((initialCallChatId != currentChatId && initialChatCallStatus.isCurrentCall())
                    || (initialCallChatId == currentChatId))
                    && updatedCallChatId != currentChatId && updatedChatCallStatus.isCurrentCall() ->
                listOf(updatedCallChatId)

            initialCallChatId != currentChatId && initialChatCallStatus.isCurrentCall()
                    && ((updatedCallChatId != currentChatId && !updatedChatCallStatus.isCurrentCall())
                    || (updatedCallChatId == currentChatId)) ->
                listOf(initialCallChatId)

            else -> emptyList()
        }
        val expectedInitialList =
            if (initialList.isEmpty()) {
                emptyList()
            } else {
                whenever(getChatCallUseCase(initialCallChatId)).thenReturn(initialCall)
                listOf(initialCall)
            }
        val expectedUpdatedList = if (updatedList.isEmpty()) {
            emptyList()
        } else {
            val call = if (updatedList[0] == initialCallChatId) initialCall else updatedCall
            whenever(getChatCallUseCase(updatedList[0])).thenReturn(call)
            listOf(call)
        }

        whenever(getCurrentCallIdsInOtherChatsUseCase(currentChatId))
            .thenReturn(initialList, updatedList)
        whenever(monitorChatCallUpdates()).thenReturn(updateFlow)

        underTest.invoke(currentChatId).test {
            assertThat(awaitItem()).isEqualTo(expectedInitialList.toList())
            updateFlow.emit(updatedCall)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(expectedUpdatedList.toList())
        }
    }

    private fun ChatCallStatus.isCurrentCall() =
        this != ChatCallStatus.WaitingRoom
                && this != ChatCallStatus.Destroyed
                && this != ChatCallStatus.TerminatingUserParticipation
                && this != ChatCallStatus.Unknown

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.Initial, currentChatId, otherChatId),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.Initial,
            currentChatId,
            currentChatId
        ),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.Initial,
            currentChatId,
            otherChatId
        ),
        Arguments.of(
            ChatCallStatus.Destroyed,
            ChatCallStatus.Initial,
            currentChatId,
            currentChatId
        ),
        Arguments.of(ChatCallStatus.Destroyed, ChatCallStatus.Initial, currentChatId, otherChatId),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.Initial,
            currentChatId,
            currentChatId,
        ),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.Initial,
            currentChatId,
            otherChatId,
        ),
        Arguments.of(
            ChatCallStatus.Initial,
            ChatCallStatus.Connecting,
            currentChatId,
            currentChatId
        ),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.Connecting, currentChatId, otherChatId),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.Connecting,
            currentChatId,
            currentChatId
        ),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.Connecting,
            currentChatId,
            otherChatId
        ),
        Arguments.of(
            ChatCallStatus.Destroyed,
            ChatCallStatus.Connecting,
            currentChatId,
            currentChatId
        ),
        Arguments.of(
            ChatCallStatus.Destroyed,
            ChatCallStatus.Connecting,
            currentChatId,
            otherChatId
        ),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.Connecting,
            currentChatId,
            currentChatId
        ),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.Connecting,
            currentChatId,
            otherChatId
        ),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.Joining, currentChatId, currentChatId),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.Joining, currentChatId, otherChatId),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.Joining,
            currentChatId,
            currentChatId
        ),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.Joining,
            currentChatId,
            otherChatId
        ),
        Arguments.of(
            ChatCallStatus.Destroyed,
            ChatCallStatus.Joining,
            currentChatId,
            currentChatId
        ),
        Arguments.of(ChatCallStatus.Destroyed, ChatCallStatus.Joining, currentChatId, otherChatId),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.Joining,
            currentChatId,
            currentChatId,
        ),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.Joining,
            currentChatId,
            otherChatId,
        ),
        Arguments.of(
            ChatCallStatus.Initial,
            ChatCallStatus.InProgress,
            currentChatId,
            currentChatId
        ),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.InProgress, currentChatId, otherChatId),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.InProgress,
            currentChatId,
            currentChatId
        ),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.InProgress,
            currentChatId,
            otherChatId
        ),
        Arguments.of(
            ChatCallStatus.Destroyed,
            ChatCallStatus.InProgress,
            currentChatId,
            currentChatId
        ),
        Arguments.of(
            ChatCallStatus.Destroyed,
            ChatCallStatus.InProgress,
            currentChatId,
            otherChatId
        ),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.InProgress,
            currentChatId,
            currentChatId,
        ),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.InProgress,
            currentChatId,
            otherChatId,
        ),
        Arguments.of(ChatCallStatus.WaitingRoom, ChatCallStatus.Initial, otherChatId, otherChatId),
        Arguments.of(ChatCallStatus.Destroyed, ChatCallStatus.Initial, otherChatId, otherChatId),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.Initial,
            otherChatId,
            otherChatId,
        ),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.Connecting, otherChatId, otherChatId),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.Connecting,
            otherChatId,
            otherChatId
        ),
        Arguments.of(ChatCallStatus.Destroyed, ChatCallStatus.Connecting, otherChatId, otherChatId),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.Connecting,
            otherChatId,
            otherChatId,
        ),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.Joining, otherChatId, otherChatId),
        Arguments.of(ChatCallStatus.WaitingRoom, ChatCallStatus.Joining, otherChatId, otherChatId),
        Arguments.of(ChatCallStatus.Destroyed, ChatCallStatus.Joining, otherChatId, otherChatId),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.Joining,
            otherChatId,
            otherChatId,
        ),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.InProgress, otherChatId, otherChatId),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.InProgress,
            otherChatId,
            otherChatId
        ),
        Arguments.of(ChatCallStatus.Destroyed, ChatCallStatus.InProgress, otherChatId, otherChatId),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.InProgress,
            otherChatId,
            otherChatId,
        ),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.Initial,
            otherChatId,
            currentChatId
        ),
        Arguments.of(ChatCallStatus.Destroyed, ChatCallStatus.Initial, otherChatId, currentChatId),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.Initial,
            otherChatId,
            currentChatId,
        ),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.Connecting, otherChatId, currentChatId),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.Connecting,
            otherChatId,
            currentChatId
        ),
        Arguments.of(
            ChatCallStatus.Destroyed,
            ChatCallStatus.Connecting,
            otherChatId,
            currentChatId
        ),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.Connecting,
            otherChatId,
            currentChatId,
        ),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.Joining, otherChatId, currentChatId),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.Joining,
            otherChatId,
            currentChatId
        ),
        Arguments.of(ChatCallStatus.Destroyed, ChatCallStatus.Joining, otherChatId, currentChatId),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.Joining,
            otherChatId,
            currentChatId,
        ),
        Arguments.of(ChatCallStatus.Initial, ChatCallStatus.InProgress, otherChatId, currentChatId),
        Arguments.of(
            ChatCallStatus.WaitingRoom,
            ChatCallStatus.InProgress,
            otherChatId,
            currentChatId
        ),
        Arguments.of(
            ChatCallStatus.Destroyed,
            ChatCallStatus.InProgress,
            otherChatId,
            currentChatId
        ),
        Arguments.of(
            ChatCallStatus.TerminatingUserParticipation,
            ChatCallStatus.InProgress,
            otherChatId,
            currentChatId,
        ),
    )
}