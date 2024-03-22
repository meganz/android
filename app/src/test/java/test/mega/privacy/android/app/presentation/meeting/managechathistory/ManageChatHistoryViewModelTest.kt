package test.mega.privacy.android.app.presentation.meeting.managechathistory

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.meeting.managechathistory.ManageChatHistoryViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.chat.MonitorChatRetentionTimeUpdateUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import test.mega.privacy.android.app.AnalyticsTestExtension

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ManageChatHistoryViewModelTest {

    private lateinit var underTest: ManageChatHistoryViewModel

    private val monitorChatRetentionTimeUpdateUseCase =
        mock<MonitorChatRetentionTimeUpdateUseCase>()

    private val chatId = 123L

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)

        @JvmField
        @RegisterExtension
        val analyticsTestExtension = AnalyticsTestExtension()
    }

    @BeforeAll
    fun setUp() {
        underTest = ManageChatHistoryViewModel(monitorChatRetentionTimeUpdateUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        wheneverBlocking { monitorChatRetentionTimeUpdateUseCase(chatId) } doReturn emptyFlow()
    }

    @Test
    fun `test that retention time in state is updated when retention time update is received`() =
        runTest {
            val retentionTime = 100L
            whenever(monitorChatRetentionTimeUpdateUseCase(chatId)).thenReturn(flowOf(retentionTime))
            underTest.monitorChatRetentionTimeUpdate(chatId)
            underTest.state.test {
                Truth.assertThat(awaitItem().retentionTimeUpdate).isEqualTo(retentionTime)
            }
        }

    @Test
    fun `test that retention time in state is updated as null when update is consumed`() = runTest {
        val retentionTime = 100L
        whenever(monitorChatRetentionTimeUpdateUseCase(chatId)).thenReturn(flowOf(retentionTime))
        underTest.monitorChatRetentionTimeUpdate(chatId)
        underTest.onRetentionTimeUpdateConsumed()
        underTest.state.test {
            Truth.assertThat(awaitItem().retentionTimeUpdate).isNull()
        }
    }
}