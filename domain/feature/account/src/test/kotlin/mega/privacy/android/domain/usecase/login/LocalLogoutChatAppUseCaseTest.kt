package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.LoginRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalLogoutChatAppUseCaseTest {

    private lateinit var underTest: LocalLogoutChatAppUseCase

    private val loginRepository = mock<LoginRepository>()
    private val localLogoutAppUseCase = mock<LocalLogoutAppUseCase>()
    private val chatLocalLogoutUseCase = mock<ChatLocalLogoutUseCase>()

    @AfterEach
    fun tearDown() {
        reset(loginRepository, localLogoutAppUseCase, chatLocalLogoutUseCase)
    }

    @BeforeEach
    fun setUp() {
        underTest = LocalLogoutChatAppUseCase(
            loginRepository = loginRepository,
            localLogoutAppUseCase = localLogoutAppUseCase,
            chatLocalLogoutUseCase = chatLocalLogoutUseCase,
        )
    }

    @Test
    fun `test that invoke calls chatLocalLogoutUseCase`() = runTest {
        underTest.invoke(disableChatApi = true)
        verify(chatLocalLogoutUseCase).invoke(disableChatApi = true)
    }

    @Test
    fun `test that invoke calls loginRepository localLogout`() = runTest {
        whenever(loginRepository.localLogout()).thenReturn(Unit)
        underTest.invoke(disableChatApi = true)
        verify(loginRepository).localLogout()
    }

    @Test
    fun `test that invoke calls localLogoutAppUseCase on success`() = runTest {
        whenever(loginRepository.localLogout()).thenReturn(Unit)
        underTest.invoke(disableChatApi = true)
        verify(localLogoutAppUseCase).invoke()
    }

    @Test
    fun `test that invoke does not call localLogoutAppUseCase when localLogout fails`() = runTest {
        whenever(loginRepository.localLogout()).thenThrow(RuntimeException("Local logout failed"))
        assertThrows<RuntimeException> {
            underTest.invoke(disableChatApi = true)
        }
        verifyNoInteractions(localLogoutAppUseCase)
    }

    @Test
    fun `test that invoke executes operations in correct order`() = runTest {
        whenever(loginRepository.localLogout()).thenReturn(Unit)
        underTest.invoke(disableChatApi = true)
        val inOrder = inOrder(chatLocalLogoutUseCase, loginRepository, localLogoutAppUseCase)
        inOrder.verify(chatLocalLogoutUseCase).invoke(disableChatApi = true)
        inOrder.verify(loginRepository).localLogout()
        inOrder.verify(localLogoutAppUseCase).invoke()
    }
}
