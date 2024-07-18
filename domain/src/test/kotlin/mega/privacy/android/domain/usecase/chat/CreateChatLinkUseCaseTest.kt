package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRequestType
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateChatLinkUseCaseTest {

    private lateinit var underTest: CreateChatLinkUseCase

    private val chatRepository: ChatRepository = mock()

    private val chatId = 1L

    @BeforeEach
    fun setUp() {
        underTest = CreateChatLinkUseCase(
            chatRepository = chatRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(chatRepository)
    }

    @Test
    fun `test that the correct chat request is returned`() = runTest {
        val charRequest = ChatRequest(
            type = ChatRequestType.LoadPreview,
            requestString = null,
            tag = 0,
            number = 0,
            numRetry = 0,
            flag = false,
            peersList = null,
            chatHandle = chatId,
            userHandle = 2L,
            privilege = 0,
            text = null,
            link = null,
            peersListByChatHandle = null,
            handleList = null,
            paramType = null

        )
        whenever(chatRepository.createChatLink(chatId = chatId)) doReturn charRequest

        val actual = underTest(chatId = chatId)

        assertThat(actual).isEqualTo(charRequest)
    }
}
