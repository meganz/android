package test.mega.privacy.android.app.main.view

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.main.view.OngoingCallViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.chat.MonitorOngoingCallUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.test.Test

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OngoingCallViewModelTest {
    private val monitorOngoingCallUseCase: MonitorOngoingCallUseCase = mock()
    private val getThemeMode: GetThemeMode = mock()
    private lateinit var underTest: OngoingCallViewModel

    @BeforeAll
    fun setup() {
        underTest = OngoingCallViewModel(monitorOngoingCallUseCase, getThemeMode)
    }

    @BeforeEach
    fun reset() {
        reset(monitorOngoingCallUseCase, getThemeMode)
    }

    @Test
    fun `test that set show update state correctly`() = runTest {
        underTest.setShow(true)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.isShown).isTrue()
        }
    }

    @Test
    fun `test that chat call is updated correctly`() = runTest {
        val chatCallFlow = MutableSharedFlow<ChatCall>()
        whenever(monitorOngoingCallUseCase()).thenReturn(chatCallFlow)
        initViewModel()
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.currentCall).isNull()
        }
        val chatCall = ChatCall(chatId = 1L, callId = 1L)
        chatCallFlow.emit(chatCall)
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.currentCall).isEqualTo(chatCall)
        }
    }

    private fun initViewModel() {
        underTest = OngoingCallViewModel(monitorOngoingCallUseCase, getThemeMode)
    }
}