package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.logout.LogoutTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

internal class LogoutUseCaseTest {
    private lateinit var underTest: LogoutUseCase

    private val loginRepository = mock<LoginRepository>()
    private val setLogoutInProgressFlagUseCase = mock<SetLogoutInProgressFlagUseCase>()

    private val logoutTask1 = mock<LogoutTask>()
    private val logoutTask2 = mock<LogoutTask>()

    @BeforeEach
    internal fun setUp() {
        underTest = LogoutUseCase(
            loginRepository = loginRepository,
            setLogoutInProgressFlagUseCase = setLogoutInProgressFlagUseCase,
            logoutTasks = setOf(logoutTask1, logoutTask2),
        )
    }

    @Test
    internal fun `test that logout flag is set to true`() = runTest {
        underTest()
        verify(setLogoutInProgressFlagUseCase).invoke(true)
    }

    @Test
    internal fun `test that logout tasks on success methods are called if logged out successfully`() =
        runTest {
            underTest()
            inOrder(loginRepository, logoutTask1, logoutTask2) {
                verify(loginRepository).logout()
                verify(logoutTask1).onLogoutSuccess()
                verify(logoutTask2).onLogoutSuccess()
            }
        }

    @Test
    internal fun `test that logout is called`() = runTest {
        underTest()
        verify(loginRepository).logout()
    }

    @Test
    internal fun `test that logout flag is set to false if exception is thrown`() = runTest {
        loginRepository.stub {
            onBlocking { logout() }.thenAnswer { throw Exception("Logout failed") }
        }

        assertThrows<Exception> { underTest() }

        verify(setLogoutInProgressFlagUseCase).invoke(false)
    }

    @Test
    fun `test that logout tasks on pre logout method is called before logout`() = runTest {
        underTest()

        inOrder(loginRepository, logoutTask1, logoutTask2) {
            verify(logoutTask1).onPreLogout()
            verify(logoutTask2).onPreLogout()
            verify(loginRepository).logout()
        }
    }

    @Test
    fun `test that logout tasks on logout failure methods are called on logout failure`() =
        runTest {
            val exception = Exception("Logout failed")
            loginRepository.stub {
                onBlocking { logout() }.thenAnswer { throw exception }
            }

            assertThrows<Exception> { underTest() }

            verify(logoutTask1).onLogoutFailed(exception)
            verify(logoutTask2).onLogoutFailed(exception)
        }
}