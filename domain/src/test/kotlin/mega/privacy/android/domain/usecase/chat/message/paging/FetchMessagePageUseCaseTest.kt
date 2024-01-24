package mega.privacy.android.domain.usecase.chat.message.paging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.paging.FetchMessagePageResponse
import mega.privacy.android.domain.usecase.chat.message.GetMessageListUseCase
import mega.privacy.android.domain.usecase.chat.message.MonitorChatRoomMessagesUseCase
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FetchMessagePageUseCaseTest {
    private lateinit var underTest: FetchMessagePageUseCase

    private val loadMessagesUseCase = mock<LoadMessagesUseCase>()
    private val getMessageListUseCase = mock<GetMessageListUseCase>()
    private val monitorChatRoomMessagesUseCase = mock<MonitorChatRoomMessagesUseCase>()

    @BeforeEach
    internal fun prepare() {
        reset(
            loadMessagesUseCase,
            getMessageListUseCase,
            monitorChatRoomMessagesUseCase,
        )
    }

    @BeforeAll
    internal fun setUp() {
        underTest = FetchMessagePageUseCase(
            loadMessagesUseCase = loadMessagesUseCase,
            getMessageListUseCase = getMessageListUseCase,
            monitorChatRoomMessagesUseCase = monitorChatRoomMessagesUseCase,
        )
    }

    @Test
    internal fun `test that response contains correct values`() = runTest {
        val expectedStatus = ChatHistoryLoadStatus.REMOTE
        val expectedMessages = listOf(mock<ChatMessage>())

        monitorChatRoomMessagesUseCase.stub {
            onBlocking { invoke(any()) } doReturn emptyFlow()
        }

        loadMessagesUseCase.stub {
            onBlocking { invoke(any()) } doReturn expectedStatus
        }

        getMessageListUseCase.stub {
            onBlocking { invoke(any()) } doReturn expectedMessages
        }

        assertThat(underTest.invoke(0)).isEqualTo(
            FetchMessagePageResponse(
                chatId = 0,
                messages = expectedMessages,
                loadResponse = expectedStatus,
            )
        )
    }
}