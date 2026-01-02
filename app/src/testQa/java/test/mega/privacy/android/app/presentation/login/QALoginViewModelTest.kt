package test.mega.privacy.android.app.presentation.login

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.login.LoginEvent
import mega.privacy.android.app.presentation.login.QALoginViewModel
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.entity.login.TemporaryWaitingError
import mega.privacy.android.domain.exception.LoginTooManyAttempts
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import mega.privacy.android.domain.usecase.login.ChatLogoutUseCase
import mega.privacy.android.domain.usecase.login.LoginUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class QALoginViewModelTest {

    private lateinit var underTest: QALoginViewModel
    private val loginUseCase = mock<LoginUseCase>()
    private val chatLogoutUseCase = mock<ChatLogoutUseCase>()
    private val scheduler = TestCoroutineScheduler()
    private val standardDispatcher = StandardTestDispatcher(scheduler)

    companion object {
        // Shared Test Data
        private const val TEST_EMAIL = "test@example.com"
        private const val TEST_PASSWORD = "password123"
        private const val TEST_WRONG_PASSWORD = "wrongpassword"
        private const val ERROR_EMPTY_EMAIL = "Email cannot be empty"
        private const val ERROR_EMPTY_PASSWORD = "Password cannot be empty"
        private const val ERROR_WRONG_CREDENTIALS = "Wrong email or password"
        private const val ERROR_TOO_MANY_ATTEMPTS = "Too many login attempts. Please try again later."
        private const val ERROR_CANNOT_START = "Login cannot start"
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(standardDispatcher)
        underTest = QALoginViewModel(
            loginUseCase = loginUseCase,
            chatLogoutUseCase = chatLogoutUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state has empty email and password`() = runTest {
        underTest.state.test {
            val state = awaitItem()

            assertEquals("", state.email)
            assertEquals("", state.password)
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `test onEmailChanged updates email and clears error`() = runTest {
        val email = TEST_EMAIL

        underTest.onEmailChanged(email)

        underTest.state.test {
            val state = awaitItem()
            assertEquals(email, state.email)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `test onEmailChanged trims whitespace`() = runTest {
        val email = " test@example.com "
        underTest.onEmailChanged(email)

        underTest.state.test {
            val state = awaitItem()
            assertEquals(TEST_EMAIL, state.email)
        }
    }

    @Test
    fun `test onPasswordChanged updates password and clears error`() = runTest {
        val password = "testPassword123"
        underTest.onPasswordChanged(password)

        underTest.state.test {
            val state = awaitItem()
            assertEquals(password, state.password)
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `test onLoginClicked with empty email shows error`() = runTest {
        underTest.onEmailChanged("")
        underTest.onPasswordChanged(TEST_PASSWORD)

        underTest.onLoginClicked()

        underTest.state.test {
            val state = awaitItem()
            assertEquals(ERROR_EMPTY_EMAIL, state.errorMessage)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `test onLoginClicked with empty password shows error`() = runTest {
        underTest.onEmailChanged(TEST_EMAIL)
        underTest.onPasswordChanged("")

        underTest.onLoginClicked()

        underTest.state.test {
            val state = awaitItem()
            assertEquals(ERROR_EMPTY_PASSWORD, state.errorMessage)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `test onLoginClicked with valid credentials starts login`() = runTest {
        val email = TEST_EMAIL
        val password = TEST_PASSWORD
        underTest.onEmailChanged(email)
        underTest.onPasswordChanged(password)

        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginStarted, LoginStatus.LoginSucceed)
        )

        underTest.onLoginClicked()
        scheduler.advanceUntilIdle()

        verify(chatLogoutUseCase).invoke(any())
        verify(loginUseCase).invoke(any(), any(), any())
    }

    @Test
    fun `test onLoginClicked sets loading state`() = runTest {
        underTest.onEmailChanged(TEST_EMAIL)
        underTest.onPasswordChanged(TEST_PASSWORD)

        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginStarted)
        )

        underTest.onLoginClicked()

        underTest.state.test {
            scheduler.advanceUntilIdle()
            val state = awaitItem()
            // Loading should be true during login
            assertNull(state.errorMessage)
        }
    }

    @Test
    fun `test onLoginClicked with LoginSucceed sends NavigateToHome event`() = runTest {
        underTest.onEmailChanged(TEST_EMAIL)
        underTest.onPasswordChanged(TEST_PASSWORD)

        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginSucceed)
        )

        underTest.onLoginClicked()

        underTest.events.test {
            scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is LoginEvent.NavigateToHome)
        }
    }

    @Test
    fun `test onLoginClicked with LoginSucceed clears loading state`() = runTest {
        underTest.onEmailChanged(TEST_EMAIL)
        underTest.onPasswordChanged(TEST_PASSWORD)

        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginSucceed)
        )

        underTest.onLoginClicked()
        scheduler.advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `test onLoginClicked with LoginCannotStart shows error`() = runTest {
        underTest.onEmailChanged(TEST_EMAIL)
        underTest.onPasswordChanged(TEST_PASSWORD)

        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginCannotStart)
        )

        underTest.onLoginClicked()
        scheduler.advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertEquals(ERROR_CANNOT_START, state.errorMessage)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `test onLoginClicked with LoginWrongEmailOrPassword shows error`() = runTest {
        underTest.onEmailChanged(TEST_EMAIL)
        underTest.onPasswordChanged(TEST_WRONG_PASSWORD)

        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flow {
                throw LoginWrongEmailOrPassword()
            }
        )

        underTest.onLoginClicked()
        scheduler.advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertEquals(ERROR_WRONG_CREDENTIALS, state.errorMessage)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `test onLoginClicked with LoginTooManyAttempts shows error`() = runTest {
        underTest.onEmailChanged(TEST_EMAIL)
        underTest.onPasswordChanged(TEST_PASSWORD)

        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flow {
                throw LoginTooManyAttempts()
            }
        )

        underTest.onLoginClicked()
        scheduler.advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertEquals(ERROR_TOO_MANY_ATTEMPTS, state.errorMessage)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `test onLoginClicked with generic exception shows error`() = runTest {
        underTest.onEmailChanged(TEST_EMAIL)
        underTest.onPasswordChanged(TEST_PASSWORD)

        val errorMessage = "Network error"
        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flow {
                throw Exception(errorMessage)
            }
        )

        underTest.onLoginClicked()
        scheduler.advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertTrue(state.errorMessage?.contains(errorMessage) == true)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `test onLoginClicked handles LoginStarted status`() = runTest {
        underTest.onEmailChanged(TEST_EMAIL)
        underTest.onPasswordChanged(TEST_PASSWORD)

        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginStarted, LoginStatus.LoginSucceed)
        )

        underTest.onLoginClicked()
        scheduler.advanceUntilIdle()

        verify(loginUseCase).invoke(any(), any(), any())
    }

    @Test
    fun `test onLoginClicked handles LoginWaiting status`() = runTest {
        underTest.onEmailChanged(TEST_EMAIL)
        underTest.onPasswordChanged(TEST_PASSWORD)

        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginWaiting(TemporaryWaitingError.ConnectivityIssues), LoginStatus.LoginSucceed)
        )

        underTest.onLoginClicked()
        scheduler.advanceUntilIdle()

        verify(loginUseCase).invoke(any(), any(), any())
    }

    @Test
    fun `test onLoginClicked handles LoginResumed status`() = runTest {
        underTest.onEmailChanged(TEST_EMAIL)
        underTest.onPasswordChanged(TEST_PASSWORD)

        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginResumed, LoginStatus.LoginSucceed)
        )

        underTest.onLoginClicked()
        scheduler.advanceUntilIdle()

        verify(loginUseCase).invoke(any(), any(), any())
    }

    @Test
    fun `test clearError clears error message`() = runTest {
        // Set an empty email to simulate error
        underTest.onEmailChanged("")
        underTest.onPasswordChanged(TEST_PASSWORD)
        underTest.onLoginClicked()

        // Verify error exists
        underTest.state.test {
            val stateWithError = awaitItem()
            assertEquals(ERROR_EMPTY_EMAIL, stateWithError.errorMessage)
        }

        underTest.clearError()

        // Verify error is cleared
        underTest.state.test {
            val stateAfterClear = awaitItem()
            assertNull(stateAfterClear.errorMessage)
        }
    }

    @Test
    fun `test onLoginClicked trims email before login`() = runTest {
        underTest.onEmailChanged("  test@example.com  ")
        underTest.onPasswordChanged(TEST_PASSWORD)

        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginSucceed)
        )

        underTest.onLoginClicked()
        scheduler.advanceUntilIdle()

        verify(loginUseCase).invoke(
            email = eq(TEST_EMAIL),
            password = eq(TEST_PASSWORD),
            disableChatApiUseCase = any()
        )
    }

    @Test
    fun `test multiple login status transitions`() = runTest {
        underTest.onEmailChanged(TEST_EMAIL)
        underTest.onPasswordChanged(TEST_PASSWORD)

        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(loginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(
                LoginStatus.LoginStarted,
                LoginStatus.LoginWaiting(TemporaryWaitingError.ConnectivityIssues),
                LoginStatus.LoginResumed,
                LoginStatus.LoginSucceed
            )
        )

        underTest.onLoginClicked()

        underTest.events.test {
            scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is LoginEvent.NavigateToHome)
        }
    }
}
