package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
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
    private val callRepository = mock<CallRepository>()


    @BeforeAll
    fun setup() {
        underTest =
            MonitorACallInThisChatUseCase(monitorChatCallUpdatesUseCase, callRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(callRepository)
    }

    @ParameterizedTest(name = "returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that flow emits correct value when HasACallInThisChatByChatIdUseCase`(hasACall: Boolean) =
        runTest {
            val chatId = 1L
            val call = if (hasACall) mock<ChatCall>() else null
            whenever(callRepository.getChatCall(chatId)).thenReturn(call)
            Truth.assertThat(underTest(chatId).first()).isEqualTo(call)
        }

    @Test
    fun `test that flow emits call when MonitorChatCallUpdates emit a call with Initial status`() =
        runTest {
            val chatId = 1L
            val call = mock<ChatCall> {
                on { this.chatId } doReturn chatId
                on { this.status } doReturn ChatCallStatus.Initial
            }
            whenever(callRepository.getChatCall(chatId)).thenReturn(null).thenReturn(call)
            underTest(chatId).test {
                Truth.assertThat(awaitItem()).isEqualTo(null)
                sharedFlow.emit(call)
                Truth.assertThat(awaitItem()).isEqualTo(call)
            }
        }

    @Test
    fun `test that flow emits null when MonitorChatCallUpdates emit a call with Destroyed status`() =
        runTest {
            val chatId = 1L
            val call = mock<ChatCall>()
            whenever(callRepository.getChatCall(chatId)).thenReturn(call)
            underTest(chatId).test {
                Truth.assertThat(awaitItem()).isEqualTo(call)
                val newCall = mock<ChatCall> {
                    on { this.chatId } doReturn chatId
                    on { this.status } doReturn ChatCallStatus.Destroyed
                }
                sharedFlow.emit(newCall)
                Truth.assertThat(awaitItem()).isEqualTo(null)
            }
        }
}