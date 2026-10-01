package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorChatRetentionTimeUseCaseTest {
    private lateinit var underTest: MonitorChatRetentionTimeUseCase

    private val retentionTimeUpdateFlow = MutableSharedFlow<Long>()
    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()
    private val monitorChatRetentionTimeUpdateUseCase =
        mock<MonitorChatRetentionTimeUpdateUseCase>()

    private val chatId = 123L

    @BeforeAll
    fun setUp() {
        underTest = MonitorChatRetentionTimeUseCase(
            getChatRoomUseCase = getChatRoomUseCase,
            monitorChatRetentionTimeUpdateUseCase = monitorChatRetentionTimeUpdateUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getChatRoomUseCase, monitorChatRetentionTimeUpdateUseCase)
        whenever(monitorChatRetentionTimeUpdateUseCase(chatId))
            .thenReturn(retentionTimeUpdateFlow)
    }

    @Test
    fun `test that invoke emits the chat room retention time initially`() = runTest {
        stubChatRoomRetentionTime(3600L)

        underTest(chatId).test {
            assertThat(awaitItem()).isEqualTo(3600L)
        }
    }

    @Test
    fun `test that invoke emits null initially when the retention time is disabled`() = runTest {
        stubChatRoomRetentionTime(0L)

        underTest(chatId).test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that invoke emits null initially when the chat room does not exist`() = runTest {
        whenever(getChatRoomUseCase(chatId)).thenReturn(null)

        underTest(chatId).test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that invoke emits the updated retention time when an update is received`() =
        runTest {
            stubChatRoomRetentionTime(3600L)

            underTest(chatId).test {
                assertThat(awaitItem()).isEqualTo(3600L)
                retentionTimeUpdateFlow.emit(7200L)
                assertThat(awaitItem()).isEqualTo(7200L)
            }
        }

    @Test
    fun `test that invoke emits null when an update disables the retention time`() = runTest {
        stubChatRoomRetentionTime(3600L)

        underTest(chatId).test {
            assertThat(awaitItem()).isEqualTo(3600L)
            retentionTimeUpdateFlow.emit(0L)
            assertThat(awaitItem()).isNull()
        }
    }

    private suspend fun stubChatRoomRetentionTime(retentionTime: Long) {
        val chatRoom = mock<ChatRoom> {
            on { this.retentionTime } doReturn retentionTime
        }
        whenever(getChatRoomUseCase(chatId)).thenReturn(chatRoom)
    }
}
