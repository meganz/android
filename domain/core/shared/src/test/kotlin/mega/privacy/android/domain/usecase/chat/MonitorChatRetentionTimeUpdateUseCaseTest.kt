package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorChatRetentionTimeUpdateUseCaseTest {

    private lateinit var underTest: MonitorChatRetentionTimeUpdateUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorChatRetentionTimeUpdateUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that monitor chat retention time update returns retention time`() = runTest {
        val chatId = 123L
        val retentionTime = 100L
        val chatUpdate = mock<ChatRoom> {
            on { hasChanged(ChatRoomChange.RetentionTime) }.thenReturn(true)
            on { this.retentionTime }.thenReturn(retentionTime)
        }
        whenever(chatRepository.monitorChatRoomUpdates(chatId)).thenReturn(
            flowOf(chatUpdate)
        )

        underTest(chatId).test {
            val actual = awaitItem()
            awaitComplete()
            Truth.assertThat(actual).isEqualTo(retentionTime)
        }
    }
}