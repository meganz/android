package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.logout.LogoutTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
internal class LogoutUseCaseTest {
    private lateinit var underTest: LogoutUseCase

    private val loginRepository = mock<LoginRepository>()

    private val logoutTask1 = mock<LogoutTask>()
    private val logoutTask2 = mock<LogoutTask>()

    @BeforeEach
    internal fun setUp() {
        underTest = LogoutUseCase(
            loginRepository = loginRepository,
            logoutTasks = setOf(logoutTask1, logoutTask2),
        )
    }

    @Test
    internal fun `test that logout flag is set to true`() = runTest {
        underTest()
        verify(loginRepository).setLogoutInProgressFlag(true)
    }

    @Test
    internal fun `test that logout tasks are called`() = runTest {
        underTest()
        verify(logoutTask1).invoke()
        verify(logoutTask2).invoke()
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

        verify(loginRepository).setLogoutInProgressFlag(false)
    }
}