package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.security.LoginRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatAnonymousLogoutUseCaseTest {

    private lateinit var underTest: ChatAnonymousLogoutUseCase

    private val loginRepository = mock<LoginRepository>()
    private val chatRepository = mock<ChatRepository>()

    @BeforeEach
    fun setUp() {
        underTest = ChatAnonymousLogoutUseCase(
            loginRepository = loginRepository,
            chatRepository = chatRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(loginRepository, chatRepository)
    }

    @Test
    fun `test that chatLogout is invoked when chat init state is anonymous`() = runTest {
        whenever(chatRepository.getChatInitState()).thenReturn(ChatInitState.ANONYMOUS)
        underTest.invoke()

        verify(chatRepository).getChatInitState()
        verify(loginRepository).chatLogout()
    }

    @Test
    fun `test that chatLogout is not invoked when chat init state is not anonymous`() = runTest {
        whenever(chatRepository.getChatInitState()).thenReturn(ChatInitState.NOT_DONE)
        underTest.invoke()

        verify(chatRepository).getChatInitState()
        verifyNoInteractions(loginRepository)
    }

    @Test
    fun `test that chatLogout is not invoked when chat init state is initialized`() = runTest {
        whenever(chatRepository.getChatInitState()).thenReturn(ChatInitState.NOT_DONE)
        underTest.invoke()

        verify(chatRepository).getChatInitState()
        verifyNoInteractions(loginRepository)
    }

    @Test
    fun `test that chatLogout is not invoked when chat init state is error`() = runTest {
        whenever(chatRepository.getChatInitState()).thenReturn(ChatInitState.ERROR)
        underTest.invoke()

        verify(chatRepository).getChatInitState()
        verifyNoInteractions(loginRepository)
    }
}