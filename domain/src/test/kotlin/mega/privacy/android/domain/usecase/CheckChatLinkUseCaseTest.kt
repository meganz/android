package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRequestType
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.chat.InitAnonymousChatModeUseCase
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever

class CheckChatLinkUseCaseTest {
    private val chatRepository: ChatRepository = mock()
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase = mock()
    private val initAnonymousChatModeUseCase: InitAnonymousChatModeUseCase = mock()

    private lateinit var underTest: CheckChatLinkUseCase

    private val chatLink = "chatLink"
    private val chatRequest = ChatRequest(
        type = ChatRequestType.LoadPreview,
        requestString = null,
        tag = 0,
        number = 0,
        numRetry = 0,
        flag = false,
        peersList = null,
        chatHandle = 1L,
        userHandle = 2L,
        privilege = 0,
        text = null,
        link = null,
        peersListByChatHandle = null,
        handleList = null,
        paramType = null
    )

    @Before
    fun setup() {
        underTest = CheckChatLinkUseCase(
            chatRepository,
            isUserLoggedInUseCase,
            initAnonymousChatModeUseCase,
        )
    }

    @Test
    fun `test that all methods are called in correct order when user is not logged in`(): Unit =
        runBlocking {
            whenever(isUserLoggedInUseCase()).thenReturn(false)
            whenever(chatRepository.checkChatLink(chatLink)).thenReturn(chatRequest)

            val result = underTest.invoke(chatLink)

            verify(initAnonymousChatModeUseCase).invoke()
            verify(chatRepository).checkChatLink(chatLink)
            assertThat(result).isEqualTo(chatRequest)
        }

    @Test
    fun `test that ChatNotInitializedErrorStatus is thrown if initAnonymousChatModeUseCase throws it`() =
        runTest {
            val exception = ChatNotInitializedErrorStatus()
            whenever(isUserLoggedInUseCase()) doReturn false
            whenever(initAnonymousChatModeUseCase()) doThrow exception

            runCatching { underTest.invoke(chatLink) }
                .onFailure { assertThat(it).isEqualTo(exception) }
        }

    @Test
    fun `test that initAnonymousChatModeUseCase is not called when user is logged in`(): Unit =
        runBlocking {
            whenever(isUserLoggedInUseCase()).thenReturn(true)
            whenever(chatRepository.checkChatLink(chatLink)).thenReturn(chatRequest)

            val result = underTest.invoke(chatLink)

            verify(chatRepository).checkChatLink(chatLink)
            assertThat(result).isEqualTo(chatRequest)
        }
}
