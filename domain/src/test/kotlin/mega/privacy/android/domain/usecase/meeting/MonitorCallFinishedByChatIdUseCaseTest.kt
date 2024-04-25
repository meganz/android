package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorCallFinishedByChatIdUseCaseTest {

    private lateinit var underTest: MonitorCallFinishedByChatIdUseCase

    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock {
        onBlocking { invoke() } doReturn emptyFlow()
    }
    private val broadcastCallRecordingConsentEventUseCase =
        mock<BroadcastCallRecordingConsentEventUseCase>()

    private val chatId = 123L

    @BeforeAll
    internal fun setUp() {
        underTest = MonitorCallFinishedByChatIdUseCase(
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            broadcastCallRecordingConsentEventUseCase = broadcastCallRecordingConsentEventUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(broadcastCallRecordingConsentEventUseCase)
        wheneverBlocking { monitorChatCallUpdatesUseCase() }.thenReturn(emptyFlow())
    }

    @ParameterizedTest(name = " {0}")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["UserNoPresent", "TerminatingUserParticipation", "Destroyed", "Unknown"]
    )
    fun `test that broadcast call recording consent event is invoked with null when call update has finish status`(
        status: ChatCallStatus,
    ) = runTest {
        val call = mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.status } doReturn status
        }

        whenever(monitorChatCallUpdatesUseCase()).thenReturn(flowOf(call))

        underTest.invoke(chatId)
        verify(broadcastCallRecordingConsentEventUseCase).invoke(null)
    }

    @Test
    fun `test that broadcast call recording consent event is not invoked when the call update does not have the same chat id`() =
        runTest {
            val otherChat = 456L
            val call = mock<ChatCall> {
                on { this.chatId } doReturn chatId
                on { this.status } doReturn ChatCallStatus.UserNoPresent
            }

            whenever(monitorChatCallUpdatesUseCase()).thenReturn(flowOf(call))

            underTest.invoke(otherChat)
            verifyNoInteractions(broadcastCallRecordingConsentEventUseCase)
        }

    @ParameterizedTest(name = " {0}")
    @EnumSource(
        value = ChatCallStatus::class,
        names = ["Initial", "Connecting", "WaitingRoom", "Joining", "InProgress", "GenericNotification"]
    )
    fun `test that broadcast call recording consent event is not invoked when the call update has not finished status`(
        status: ChatCallStatus,
    ) = runTest {
        val call = mock<ChatCall> {
            on { this.chatId } doReturn chatId
            on { this.status } doReturn status
        }

        whenever(monitorChatCallUpdatesUseCase()).thenReturn(flowOf(call))

        underTest.invoke(chatId)
        verifyNoInteractions(broadcastCallRecordingConsentEventUseCase)
    }
}