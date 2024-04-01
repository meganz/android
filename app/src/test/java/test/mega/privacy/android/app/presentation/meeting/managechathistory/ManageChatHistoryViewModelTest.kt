package test.mega.privacy.android.app.presentation.meeting.managechathistory

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.managechathistory.ManageChatHistoryViewModel
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRetentionTimeUpdateUseCase
import mega.privacy.android.domain.usecase.chat.SetChatRetentionTimeUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManageChatHistoryViewModelTest {

    private lateinit var underTest: ManageChatHistoryViewModel

    private val monitorChatRetentionTimeUpdateUseCase =
        mock<MonitorChatRetentionTimeUpdateUseCase>()
    private val clearChatHistoryUseCase = mock<ClearChatHistoryUseCase>()
    private val setChatRetentionTimeUseCase = mock<SetChatRetentionTimeUseCase>()
    private val snackBarHandler = mock<SnackBarHandler>()

    private val chatId = 123L

    @BeforeAll
    fun setUp() {
        underTest = ManageChatHistoryViewModel(
            monitorChatRetentionTimeUpdateUseCase = monitorChatRetentionTimeUpdateUseCase,
            clearChatHistoryUseCase = clearChatHistoryUseCase,
            setChatRetentionTimeUseCase = setChatRetentionTimeUseCase,
            snackBarHandler = snackBarHandler
        )
    }

    @BeforeEach
    fun resetMocks() {
        wheneverBlocking { monitorChatRetentionTimeUpdateUseCase(chatId) } doReturn emptyFlow()
        reset(
            monitorChatRetentionTimeUpdateUseCase,
            clearChatHistoryUseCase,
            snackBarHandler
        )
    }

    @Test
    fun `test that retention time in state is updated when retention time update is received`() =
        runTest {
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatId)).thenReturn(flowOf(retentionTime))
            underTest.monitorChatRetentionTimeUpdate(chatId)
            underTest.uiState.test {
                assertThat(awaitItem().retentionTimeUpdate).isEqualTo(retentionTime)
            }
        }

    @Test
    fun `test that retention time in state is updated as null when update is consumed`() = runTest {
        val retentionTime = 100L
        whenever(monitorChatRetentionTimeUpdateUseCase(chatId)).thenReturn(flowOf(retentionTime))
        underTest.monitorChatRetentionTimeUpdate(chatId)
        underTest.onRetentionTimeUpdateConsumed()
        underTest.uiState.test {
            assertThat(awaitItem().retentionTimeUpdate).isNull()
        }
    }

    @Test
    fun `test that the chat's history is cleared with the correct chat room ID`() = runTest {
        underTest.clearChatHistory(chatId)

        verify(clearChatHistoryUseCase).invoke(chatId)
    }

    @Test
    fun `test that the clear chat history visibility state is true`() = runTest {
        underTest.showClearChatConfirmation()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().shouldShowClearChatConfirmation).isTrue()
        }
    }

    @Test
    fun `test that the clear chat history visibility state is false when dismissed`() = runTest {
        underTest.showClearChatConfirmation()
        underTest.dismissClearChatConfirmation()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().shouldShowClearChatConfirmation).isFalse()
        }
    }

    @Test
    fun `test that the correct snack bar message is shown after successfully clearing the chat history`() =
        runTest {
            underTest.clearChatHistory(chatId)

            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.clear_history_success,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }

    @Test
    fun `test that the correct snack bar message is shown when clearing the chat history fails`() =
        runTest {
            whenever(clearChatHistoryUseCase(chatId)).thenThrow(RuntimeException())

            underTest.clearChatHistory(chatId)

            verify(snackBarHandler).postSnackbarMessage(
                resId = R.string.clear_history_error,
                snackbarDuration = MegaSnackbarDuration.Long
            )
        }

    @Test
    fun `test that chat's retention time use case is invoked with the correct parameters`() =
        runTest {
            val period = 321L
            underTest.setChatRetentionTime(chatId = chatId, period = period)

            verify(setChatRetentionTimeUseCase).invoke(chatId, period)
        }
}
