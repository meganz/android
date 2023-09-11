package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.security.LoginRepository
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class InitGuestChatSessionUseCaseTest {
    private val loginRepository: LoginRepository = mock(LoginRepository::class.java)
    private val chatRepository: ChatRepository = mock(ChatRepository::class.java)
    private val accountRepository: AccountRepository = mock(AccountRepository::class.java)

    private lateinit var underTest: InitGuestChatSessionUseCase

    @Before
    fun setup() {
        underTest = InitGuestChatSessionUseCase(loginRepository, chatRepository, accountRepository)
    }

    @Test
    fun `test that chatLogout and initAnonymousChat are called when user is not logged in and chat state is not anonymous`(): Unit =
        runBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(false)
            whenever(chatRepository.getChatInitState()).thenReturn(ChatInitState.WAITING_NEW_SESSION)

            underTest.invoke(true)

            verify(loginRepository).chatLogout()
            verify(chatRepository).initAnonymousChat()
        }

    @Test
    fun `test that chatLogout and initMegaChat are called when user is not logged in, chat state is not anonymous and initAnonymous is false`() =
        runBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(false)
            whenever(chatRepository.getChatInitState()).thenReturn(ChatInitState.WAITING_NEW_SESSION)

            underTest.invoke(false)

            verify(loginRepository).chatLogout()
            verify(loginRepository).initMegaChat()
        }

    @Test
    fun `test that chatLogout and initAnonymousChat are not called when user is logged in`() =
        runBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)

            underTest.invoke(true)

            verifyNoMoreInteractions(loginRepository)
        }

    @Test
    fun `test that chatLogout and initAnonymousChat are not called when chat state is anonymous`() =
        runBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(false)
            whenever(chatRepository.getChatInitState()).thenReturn(ChatInitState.ANONYMOUS)

            underTest.invoke(true)

            verifyNoMoreInteractions(loginRepository)
        }
}
