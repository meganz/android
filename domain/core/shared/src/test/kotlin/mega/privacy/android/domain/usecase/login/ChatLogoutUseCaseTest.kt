package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.LoginRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatLogoutUseCaseTest {

    private val loginRepository = mock<LoginRepository> {
        onBlocking { chatLogout() }.thenReturn(Unit)
    }
    private val disableChatApiUseCase = mock<DisableChatApiUseCase>()

    private val underTest = ChatLogoutUseCase(
        loginRepository = loginRepository,
        disableChatApiUseCase = disableChatApiUseCase,
    )

    @AfterEach
    fun tearDown() {
        reset(loginRepository, disableChatApiUseCase)
    }

    @Test
    fun `test that invoke calls chatLogout`() = runTest {
        underTest.invoke(disableChatApi = false)
        verify(loginRepository).chatLogout()
    }

    @Test
    fun `test that invoke with disableChatApi true calls disable chat api on success`() = runTest {
        underTest.invoke(disableChatApi = true)
        verify(disableChatApiUseCase).invoke()
    }

    @Test
    fun `test that invoke with disableChatApi false does not call disable chat api`() = runTest {
        underTest.invoke(disableChatApi = false)
        verify(disableChatApiUseCase, never()).invoke()
    }
}
