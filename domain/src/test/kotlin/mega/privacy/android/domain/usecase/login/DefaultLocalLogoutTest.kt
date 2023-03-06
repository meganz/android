package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.ClearPsa
import mega.privacy.android.domain.usecase.LocalLogoutApp
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultLocalLogoutTest {

    private lateinit var underTest: LocalLogout

    private val loginRepository = mock<LoginRepository>()
    private val localLogoutApp = mock<LocalLogoutApp>()
    private val chatLogout = mock<ChatLogout>()

    @Before
    fun setUp() {
        underTest = DefaultLocalLogout(
            loginRepository = loginRepository,
            localLogoutApp = localLogoutApp,
            chatLogout = chatLogout
        )
    }

    @Test
    fun `test that local logout invokes ChatLogout`() = runTest {
        val disableChatApi = mock<DisableChatApi>()
        underTest.invoke(disableChatApi, mock())
        verify(chatLogout).invoke(disableChatApi)
    }

    @Test
    fun `test that local logout invokes LocalLogoutApp on success`() = runTest {
        whenever(loginRepository.localLogout()).thenReturn(Unit)
        val clearPsa = mock<ClearPsa>()
        underTest.invoke(mock(), clearPsa)
        verify(localLogoutApp).invoke(clearPsa)
    }
}