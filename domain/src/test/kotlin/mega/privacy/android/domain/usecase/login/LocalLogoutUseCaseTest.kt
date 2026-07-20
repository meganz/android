package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.LoginRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalLogoutUseCaseTest {

    private lateinit var underTest: LocalLogoutUseCase

    private val loginRepository = mock<LoginRepository>()
    private val localLogoutAppUseCase = mock<LocalLogoutAppUseCase>()
    private val chatLogoutUseCase = mock<ChatLogoutUseCase>()

    @AfterEach
    fun tearDown() {
        reset(loginRepository, localLogoutAppUseCase, chatLogoutUseCase)
    }

    @BeforeEach
    fun setUp() {
        underTest = LocalLogoutUseCase(
            loginRepository = loginRepository,
            localLogoutAppUseCase = localLogoutAppUseCase,
            chatLogoutUseCase = chatLogoutUseCase,
        )
    }

    @Test
    fun `test that invoke calls chatLogoutUseCase`() = runTest {
        underTest.invoke(disableChatApi = true)
        verify(chatLogoutUseCase).invoke(disableChatApi = true)
    }

    @Test
    fun `test that invoke calls localLogoutAppUseCase on success`() = runTest {
        whenever(loginRepository.localLogout()).thenReturn(Unit)
        underTest.invoke(disableChatApi = true)
        verify(localLogoutAppUseCase).invoke()
    }
}
