package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class MonitorCallsReconnectingStatusUseCaseTest {

    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock()
    private val callId = 1L
    private lateinit var underTest: MonitorCallsReconnectingStatusUseCase

    @BeforeEach
    fun setup() {
        whenever(monitorChatCallUpdatesUseCase()).thenReturn(emptyFlow())
        underTest = MonitorCallsReconnectingStatusUseCase(
            monitorChatCallUpdatesUseCase,
        )
    }

    @Test
    fun `test that flow emit true if previous status is in progress`() = runTest {
        val chatId = 1L

        whenever(monitorChatCallUpdatesUseCase.invoke()).thenReturn(
            flowOf(
                ChatCall(
                    chatId = chatId,
                    callId = callId,
                    status = ChatCallStatus.InProgress,
                    changes = listOf(ChatCallChanges.Status),
                ),
                ChatCall(
                    chatId = chatId,
                    callId = callId,
                    status = ChatCallStatus.Connecting,
                    changes = listOf(ChatCallChanges.Status),
                )
            )
        )

        underTest.invoke().test {
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that flow emit false if previous status is in connecting`() = runTest {
        val chatId = 1L
        whenever(monitorChatCallUpdatesUseCase.invoke()).thenReturn(
            flowOf(
                ChatCall(
                    chatId = chatId,
                    callId = callId,
                    status = ChatCallStatus.InProgress,
                    changes = listOf(ChatCallChanges.Status),
                ),
                ChatCall(
                    chatId = chatId,
                    callId = callId,
                    status = ChatCallStatus.Connecting,
                    changes = listOf(ChatCallChanges.Status),
                ),
                ChatCall(
                    chatId = chatId,
                    callId = callId,
                    status = ChatCallStatus.InProgress,
                    changes = listOf(ChatCallChanges.Status),
                )
            )
        )

        underTest.invoke().test {
            assertThat(awaitItem()).isTrue()
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that flow doesn't emit when getting new destroyed status`() = runTest {
        val chatId = 1L

        whenever(monitorChatCallUpdatesUseCase.invoke()).thenReturn(
            flowOf(
                ChatCall(
                    chatId = chatId,
                    callId = callId,
                    status = ChatCallStatus.Connecting,
                    changes = listOf(ChatCallChanges.Status),
                ),
                ChatCall(
                    chatId = chatId,
                    callId = callId,
                    status = ChatCallStatus.Destroyed,
                    changes = listOf(ChatCallChanges.Status),
                )
            )
        )

        underTest.invoke().test {
            awaitComplete()
        }
    }
}