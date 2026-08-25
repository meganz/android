package mega.privacy.android.app.appstate.global.call

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.chat.MonitorOngoingCallUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class OngoingCallBannerViewModelTest {

    private lateinit var underTest: OngoingCallBannerViewModel

    private val monitorOngoingCallUseCase: MonitorOngoingCallUseCase = mock()
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase = mock()

    @BeforeEach
    fun resetMocks() {
        reset(monitorOngoingCallUseCase, monitorThemeModeUseCase)
    }

    private fun initTestClass() {
        underTest = OngoingCallBannerViewModel(
            monitorOngoingCallUseCase = monitorOngoingCallUseCase,
            monitorThemeModeUseCase = monitorThemeModeUseCase,
        )
    }

    @Test
    fun `test that uiState currentCall reflects the ongoing call from the use case`() = runTest {
        val call = ChatCall(chatId = 123L, callId = 1L)
        whenever(monitorOngoingCallUseCase()).thenReturn(flowOf(call))
        whenever(monitorThemeModeUseCase()).thenReturn(flowOf(ThemeMode.System))
        initTestClass()

        underTest.uiState.test {
            assertThat(awaitItem().currentCall).isEqualTo(call)
        }
    }

    @Test
    fun `test that uiState currentCall is null when there is no ongoing call`() = runTest {
        whenever(monitorOngoingCallUseCase()).thenReturn(flowOf(null))
        whenever(monitorThemeModeUseCase()).thenReturn(flowOf(ThemeMode.System))
        initTestClass()

        underTest.uiState.test {
            assertThat(awaitItem().currentCall).isNull()
        }
    }

    @Test
    fun `test that uiState themeMode reflects the theme mode from the use case`() = runTest {
        whenever(monitorOngoingCallUseCase()).thenReturn(flowOf(null))
        whenever(monitorThemeModeUseCase()).thenReturn(flowOf(ThemeMode.Dark))
        initTestClass()

        underTest.uiState.test {
            assertThat(awaitItem().themeMode).isEqualTo(ThemeMode.Dark)
        }
    }

    @Test
    fun `test that uiState falls back to defaults when the use cases emit nothing`() = runTest {
        whenever(monitorOngoingCallUseCase()).thenReturn(emptyFlow())
        whenever(monitorThemeModeUseCase()).thenReturn(emptyFlow())
        initTestClass()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.currentCall).isNull()
            assertThat(state.themeMode).isEqualTo(ThemeMode.System)
        }
    }
}
