package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.ClearPsa
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class LocalLogoutUseCaseTest {

    private lateinit var underTest: LocalLogoutUseCase

    private val loginRepository = mock<LoginRepository>()
    private val localLogoutAppUseCase = mock<LocalLogoutAppUseCase>()
    private val chatLogoutUseCase = mock<ChatLogoutUseCase>()

    @Before
    fun setUp() {
        underTest = LocalLogoutUseCase(
            loginRepository = loginRepository,
            localLogoutAppUseCase = localLogoutAppUseCase,
            chatLogoutUseCase = chatLogoutUseCase
        )
    }

    @Test
    fun `test that local logout invokes ChatLogout`() = runTest {
        val disableChatApiUseCase = mock<DisableChatApiUseCase>()
        underTest.invoke(disableChatApiUseCase, mock())
        verify(chatLogoutUseCase).invoke(disableChatApiUseCase)
    }

    @Test
    fun `test that local logout invokes LocalLogoutApp on success`() = runTest {
        whenever(loginRepository.localLogout()).thenReturn(Unit)
        val clearPsa = mock<ClearPsa>()
        underTest.invoke(mock(), clearPsa)
        verify(localLogoutAppUseCase).invoke(clearPsa)
    }
}