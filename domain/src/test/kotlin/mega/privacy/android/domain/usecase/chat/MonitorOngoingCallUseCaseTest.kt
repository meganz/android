package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.usecase.call.GetChatCallInProgress
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorOngoingCallUseCaseTest {
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock()
    private val getChatCallInProgress: GetChatCallInProgress = mock()
    private val updateFlow = MutableSharedFlow<ChatCall>()
    private lateinit var underTest: MonitorOngoingCallUseCase

    @BeforeAll
    fun setup() {
        underTest = MonitorOngoingCallUseCase(
            monitorChatCallUpdatesUseCase,
            getChatCallInProgress
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getChatCallInProgress
        )
        whenever(monitorChatCallUpdatesUseCase()).thenReturn(updateFlow)
    }

    @Test
    fun `test that return flow of ChatCall with the call in progress`() = runTest {
        val chatCall: ChatCall = mock()
        whenever(getChatCallInProgress()).thenReturn(chatCall)
        underTest().test {
            val result = awaitItem()
            assertThat(result).isEqualTo(chatCall)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that return flow of ChatCall with null`() = runTest {
        whenever(getChatCallInProgress()).thenReturn(null)
        underTest().test {
            val result = awaitItem()
            assertThat(result).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that return flow of ChatCall when monitor chat call update emit`() = runTest {
        val chatCall = ChatCall(
            chatId = 1,
            callId = 1
        )
        whenever(getChatCallInProgress()).thenReturn(null)
            .thenReturn(chatCall)
        underTest().test {
            val result = awaitItem()
            assertThat(result).isNull()
            updateFlow.emit(chatCall)
            testScheduler.advanceUntilIdle()
            val result2 = awaitItem()
            assertThat(result2).isEqualTo(chatCall)
        }
    }
}