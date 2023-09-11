package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRequestType
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.chat.InitGuestChatSessionUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever

class CheckChatLinkUseCaseTest {
    private val chatRepository: ChatRepository = mock()
    private val initGuestChatSessionUseCase: InitGuestChatSessionUseCase = mock()

    private lateinit var underTest: CheckChatLinkUseCase

    @Before
    fun setup() {
        underTest = CheckChatLinkUseCase(chatRepository, initGuestChatSessionUseCase)
    }

    @Test
    fun `test that all methods are called in correct order`(): Unit =
        runBlocking {
            val chatLink = "chatLink"
            val chatRequest = ChatRequest(
                type = ChatRequestType.LoadPreview,
                requestString = null,
                tag = 0,
                number = 0,
                numRetry = 0,
                flag = false,
                peersList = null,
                chatHandle = 1L,
                userHandle = 2L,
                privilege = null,
                text = null,
                link = null,
                peersListByChatHandle = null,
                handleList = null,
                paramType = null
            )

            whenever(chatRepository.checkChatLink(chatLink)).thenReturn(chatRequest)

            val result = underTest.invoke(chatLink)

            verify(initGuestChatSessionUseCase).invoke()
            verify(chatRepository).checkChatLink(chatLink)
            assertThat(result == chatRequest)
        }
}
