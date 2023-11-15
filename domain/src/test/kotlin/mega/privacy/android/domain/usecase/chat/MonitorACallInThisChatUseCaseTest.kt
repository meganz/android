package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorACallInThisChatUseCaseTest {
    private lateinit var underTest: MonitorACallInThisChatUseCase
    private val sharedFlow = MutableSharedFlow<ChatCall>()
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock {
        on { invoke() } doReturn sharedFlow
    }
    private val hasACallInThisChatByChatIdUseCase: HasACallInThisChatByChatIdUseCase = mock()


    @BeforeAll
    fun setup() {
        underTest =
            MonitorACallInThisChatUseCase(monitorChatCallUpdatesUseCase, hasACallInThisChatByChatIdUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(hasACallInThisChatByChatIdUseCase)
    }

    @ParameterizedTest(name = "returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that flow emits correct value when HasACallInThisChatByChatIdUseCase`(hasACall: Boolean) =
        runTest {
            val chatId = 1L
            whenever(hasACallInThisChatByChatIdUseCase(chatId)).thenReturn(hasACall)
            Truth.assertThat(underTest(chatId).first()).isEqualTo(hasACall)
        }

    @Test
    fun `test that flow emits true when MonitorChatCallUpdates emit a call with Initial status`() =
        runTest {
            val chatId = 1L
            whenever(hasACallInThisChatByChatIdUseCase(chatId)).thenReturn(false)
            underTest(chatId).test {
                Truth.assertThat(awaitItem()).isEqualTo(false)
                val call = mock<ChatCall> {
                    on { this.chatId } doReturn chatId
                    on { this.status } doReturn ChatCallStatus.Initial
                }
                sharedFlow.emit(call)
                Truth.assertThat(awaitItem()).isEqualTo(true)
            }
        }

    @Test
    fun `test that flow emits false when MonitorChatCallUpdates emit a call with Destroyed status`() =
        runTest {
            val chatId = 1L
            whenever(hasACallInThisChatByChatIdUseCase(chatId)).thenReturn(true)
            underTest(chatId).test {
                Truth.assertThat(awaitItem()).isEqualTo(true)
                val call = mock<ChatCall> {
                    on { this.chatId } doReturn chatId
                    on { this.status } doReturn ChatCallStatus.Destroyed
                }
                sharedFlow.emit(call)
                Truth.assertThat(awaitItem()).isEqualTo(false)
            }
        }
}