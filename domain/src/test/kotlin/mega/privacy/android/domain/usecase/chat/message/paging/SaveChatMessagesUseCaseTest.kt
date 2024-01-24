package mega.privacy.android.domain.usecase.chat.message.paging

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.messages.paging.FetchMessagePageResponse
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveChatMessagesUseCaseTest {
    private lateinit var underTest: SaveChatMessagesUseCase

    private val chatRepository = mock<ChatRepository>()
    private val createSaveMessageRequestUseCase = mock<CreateSaveMessageRequestUseCase>()
    private val myUserHandle = 123L

    @BeforeAll
    internal fun setUp() {
        underTest = SaveChatMessagesUseCase(
            chatRepository = chatRepository,
            createSaveMessageRequestUseCase = createSaveMessageRequestUseCase,
        )
    }

    @AfterEach
    internal fun tearDown() {
        reset(chatRepository, createSaveMessageRequestUseCase)
    }

    @Test
    internal fun `test that next message handle is not passed if no next message is returned`() =
        runTest {
            chatRepository.stub {
                onBlocking { getMyUserHandle() } doReturn myUserHandle
                onBlocking { getNextMessagePagingInfo(any(), any()) } doReturn null
            }

            underTest(
                FetchMessagePageResponse(
                    chatId = 123L,
                    messages = emptyList(),
                    loadResponse = ChatHistoryLoadStatus.REMOTE
                )
            )

            verify(createSaveMessageRequestUseCase).invoke(
                chatMessages = emptyList(),
                currentUserHandle = myUserHandle,
                nextMessageUserHandle = null
            )
        }
}