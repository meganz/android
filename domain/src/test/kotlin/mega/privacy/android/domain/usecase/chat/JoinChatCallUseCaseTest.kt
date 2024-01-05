package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRequestType
import mega.privacy.android.domain.entity.chat.ChatPreview
import mega.privacy.android.domain.usecase.chat.link.JoinPublicChatUseCase
import mega.privacy.android.domain.usecase.chat.link.LoadChatPreviewUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JoinChatCallUseCaseTest {
    private lateinit var underTest: JoinChatCallUseCase

    private val loadChatPreviewUseCase = mock<LoadChatPreviewUseCase>()
    private val joinPublicChatUseCase = mock<JoinPublicChatUseCase>()

    @BeforeAll
    fun setup() {
        underTest = JoinChatCallUseCase(
            loadChatPreviewUseCase = loadChatPreviewUseCase,
            joinPublicChatUseCase = joinPublicChatUseCase,
        )
    }

    @BeforeEach
    fun reset() {
        reset(loadChatPreviewUseCase, joinPublicChatUseCase)
    }

    @Test
    fun `test that all methods are called in correct order`() = runTest {
        val chatLink = "chatLink"
        val chatId = 1L
        val chatPublicHandle = 2L

        val chatRequest = ChatRequest(
            type = ChatRequestType.LoadPreview,
            requestString = null,
            tag = 0,
            number = 0,
            numRetry = 0,
            flag = false,
            peersList = null,
            chatHandle = chatId,
            userHandle = chatPublicHandle,
            privilege = 0,
            text = null,
            link = null,
            peersListByChatHandle = null,
            handleList = null,
            paramType = null
        )

        val chatPreview = ChatPreview(chatRequest, false)

        whenever(loadChatPreviewUseCase(chatLink)).thenReturn(chatPreview)

        underTest.invoke(chatLink)

        inOrder(loadChatPreviewUseCase, joinPublicChatUseCase) {
            verify(loadChatPreviewUseCase).invoke(chatLink)
            verify(joinPublicChatUseCase).invoke(chatId, chatPublicHandle, true)
        }
    }

}
