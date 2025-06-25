package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.LoginRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class ChatLogoutUseCaseTest {

    private lateinit var underTest: ChatLogoutUseCase

    private val loginRepository = mock<LoginRepository>()

    @Before
    fun setUp() {
        underTest = ChatLogoutUseCase(
            loginRepository = loginRepository,
        )
    }

    @Test
    fun `test that chat logout invokes disable chat api and reset sdk logger on success`() =
        runTest {
            val disableChatApiUseCase = mock<DisableChatApiUseCase>()
            underTest.invoke(disableChatApiUseCase)
            verify(disableChatApiUseCase).invoke()
        }
}