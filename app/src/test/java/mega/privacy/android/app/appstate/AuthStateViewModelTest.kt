package mega.privacy.android.app.appstate

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.appstate.initialisation.AuthInitialiser
import mega.privacy.android.app.appstate.model.AuthState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthStateViewModelTest {
    private lateinit var underTest: AuthStateViewModel
    private val monitorThemeModeUseCase = mock<MonitorThemeModeUseCase>()
    private val monitorUserCredentialsUseCase = mock<MonitorUserCredentialsUseCase>()
    private val authInitialiser = mock<AuthInitialiser>()

    @BeforeAll
    fun initialisation() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun cleanUp() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun setUp() {
        underTest = AuthStateViewModel(
            monitorThemeModeUseCase = monitorThemeModeUseCase,
            monitorUserCredentialsUseCase = monitorUserCredentialsUseCase,
            authInitialiser = authInitialiser,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(monitorThemeModeUseCase, monitorUserCredentialsUseCase, authInitialiser)
    }

    @Test
    fun `test that app start initializers are called during initialization`() = runTest {
        verify(authInitialiser).onAppStart()
    }

    @Test
    fun `test that pre login initializers are called when checking existing session`() = runTest {

        val themeMode = ThemeMode.Light
        monitorThemeModeUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(themeMode))
        }
        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(null))
        }

        underTest.state.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(AuthState.RequireLogin::class.java)

            // Verify pre-login initializers were called with null session
            verify(authInitialiser).onPreLogin(null)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that pre login initializers are called with existing session`() = runTest {

        val credentials = UserCredentials(
            email = "test@example.com",
            session = "existing-session",
            firstName = "John",
            lastName = "Doe",
            myHandle = "123456789"
        )
        val themeMode = ThemeMode.Light
        monitorThemeModeUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(themeMode))
        }
        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(credentials))
        }

        underTest.state.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(AuthState.LoggedIn::class.java)

            // Verify pre-login initializers were called with existing session
            verify(authInitialiser).onPreLogin("existing-session")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that post login initializers are called when user logs in`() = runTest {

        val credentials = UserCredentials(
            email = "test@example.com",
            session = "test-session",
            firstName = "John",
            lastName = "Doe",
            myHandle = "123456789"
        )
        val themeMode = ThemeMode.Light
        monitorThemeModeUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(themeMode))
        }
        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(credentials))
        }

        underTest.state.filterIsInstance<AuthState.LoggedIn>().test {
            val state = awaitItem()
            assertThat(state.themeMode).isEqualTo(themeMode)
            assertThat(state.credentials).isEqualTo(credentials)

            // Verify post-login initializers were called with the session
            verify(authInitialiser).onPostLogin("test-session")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that post login initializers are not called when user is not logged in`() = runTest {

        val themeMode = ThemeMode.Light
        monitorThemeModeUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(themeMode))
        }
        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(null))
        }

        underTest.state.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(AuthState.RequireLogin::class.java)

            // Verify post-login initializers were NOT called
            verify(authInitialiser, never()).onPostLogin(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(emptyFlow<UserCredentials>())
        }
        assertThat(underTest.state.value).isEqualTo(AuthState.Loading(ThemeMode.System))
    }

    @Test
    fun `test that state is require login when credentials are null`() = runTest {
        val themeMode = ThemeMode.Light
        monitorThemeModeUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(themeMode))
        }
        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(null))
        }

        underTest.state.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(AuthState.RequireLogin::class.java)
            assertThat((state as AuthState.RequireLogin).themeMode).isEqualTo(themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that state is require login when credentials are not null but session is null`() =
        runTest {
            val credentials = UserCredentials(
                email = "test@example.com",
                session = null,
                firstName = "John",
                lastName = "Doe",
                myHandle = "123456789"
            )
            val themeMode = ThemeMode.Light
            monitorThemeModeUseCase.stub {
                on { invoke() }.thenReturn(MutableStateFlow(themeMode))
            }
            monitorUserCredentialsUseCase.stub {
                on { invoke() }.thenReturn(MutableStateFlow(credentials))
            }
            underTest.state.test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(AuthState.RequireLogin::class.java)
                assertThat((state as AuthState.RequireLogin).themeMode).isEqualTo(themeMode)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that state is logged in when credentials are available and session is not null`() =
        runTest {
            val credentials = UserCredentials(
                email = "test@example.com",
                session = "test-session",
                firstName = "John",
                lastName = "Doe",
                myHandle = "123456789"
            )
            val themeMode = ThemeMode.Light
            monitorThemeModeUseCase.stub {
                on { invoke() }.thenReturn(MutableStateFlow(themeMode))
            }
            monitorUserCredentialsUseCase.stub {
                on { invoke() }.thenReturn(MutableStateFlow(credentials))
            }

            underTest.state.filterIsInstance<AuthState.LoggedIn>().test {
                val state = awaitItem()
                assertThat(state.themeMode).isEqualTo(themeMode)
                assertThat(state.credentials).isEqualTo(credentials)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that state transitions from require login to logged in`() = runTest {
        val credentials = UserCredentials(
            email = "test@example.com",
            session = "test-session",
            firstName = "John",
            lastName = "Doe",
            myHandle = "123456789"
        )

        val themeMode = ThemeMode.Light
        monitorThemeModeUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(themeMode))
        }
        val credentialsFlow = MutableStateFlow<UserCredentials?>(null)
        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(credentialsFlow)
        }

        underTest.state.test {
            // Initial state should be loading
            val initialState = awaitItem()
            assertThat(initialState).isInstanceOf(AuthState.RequireLogin::class.java)

            // Emit credentials
            credentialsFlow.emit(credentials)

            // State should transition to logged in
            val loggedInState = awaitItem()
            assertThat(loggedInState).isInstanceOf(AuthState.LoggedIn::class.java)
            assertThat((loggedInState as AuthState.LoggedIn).credentials).isEqualTo(credentials)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that state transitions from logged in to require login when credentials become null`() =
        runTest {
            val credentials = UserCredentials(
                email = "test@example.com",
                session = "test-session",
                firstName = "John",
                lastName = "Doe",
                myHandle = "123456789"
            )

            val themeMode = ThemeMode.Light
            monitorThemeModeUseCase.stub {
                on { invoke() }.thenReturn(MutableStateFlow(themeMode))
            }
            val credentialsFlow = MutableStateFlow<UserCredentials?>(credentials)
            monitorUserCredentialsUseCase.stub {
                on { invoke() }.thenReturn(credentialsFlow)
            }


            underTest.state.test {
                // Initial state should be logged in
                val initialState = awaitItem()
                assertThat(initialState).isInstanceOf(AuthState.LoggedIn::class.java)

                // Emit null credentials
                credentialsFlow.emit(null)

                // State should transition to loading
                val loadingState = awaitItem()
                assertThat(loadingState).isInstanceOf(AuthState.RequireLogin::class.java)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that theme mode changes are reflected in state`() = runTest {
        val credentials = UserCredentials(
            email = "test@example.com",
            session = "test-session",
            firstName = "John",
            lastName = "Doe",
            myHandle = "123456789"
        )

        val initialThemeMode = ThemeMode.Light
        val newThemeMode = ThemeMode.Dark

        val themeModeFlow = MutableStateFlow(initialThemeMode)
        monitorThemeModeUseCase.stub {
            on { invoke() }.thenReturn(themeModeFlow)
        }
        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(credentials))
        }

        underTest.state.filterIsInstance<AuthState.LoggedIn>().test {
            val initialState = awaitItem()
            assertThat(initialState.themeMode).isEqualTo(initialThemeMode)

            // Change theme mode
            themeModeFlow.emit(ThemeMode.Dark)

            val updatedState = awaitItem()
            assertThat(updatedState.themeMode).isEqualTo(newThemeMode)
            assertThat(updatedState.credentials).isEqualTo(credentials)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that error handling works correctly`() = runTest {
        // Simulate an error by making the flow throw an exception
        whenever(monitorUserCredentialsUseCase()).thenReturn(
            flow { throw RuntimeException("Test error") }
        )

        underTest.state.test {
            // Should still emit the initial loading state even with errors
            val state = awaitItem()
            assertThat(state).isInstanceOf(AuthState.Loading::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that system theme mode is returned if monitor fails`() = runTest {
        val credentials = UserCredentials(
            email = "test@example.com",
            session = "test-session",
            firstName = "John",
            lastName = "Doe",
            myHandle = "123456789"
        )
        monitorThemeModeUseCase.stub {
            on { invoke() }.thenReturn(
                flow { throw RuntimeException("Test error") },
            )
        }

        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(credentials))
        }

        underTest.state.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(AuthState.LoggedIn::class.java)
            assertThat((state as AuthState.LoggedIn).themeMode).isEqualTo(ThemeMode.System)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that change in login status retries failed theme mode call`() = runTest {
        val credentials = UserCredentials(
            email = "test@example.com",
            session = "test-session",
            firstName = "John",
            lastName = "Doe",
            myHandle = "123456789"
        )

        val expectedThemeMode = ThemeMode.Light
        monitorThemeModeUseCase.stub {
            on { invoke() }.thenReturn(
                flow { throw RuntimeException("Test error") },
                MutableStateFlow(expectedThemeMode)
            )
        }

        val credentialsFlow = MutableStateFlow<UserCredentials?>(null)
        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(credentialsFlow)
        }

        underTest.state.test {
            // Should still emit the initial loading state even with errors
            val state = awaitItem()
            assertThat(state).isInstanceOf(AuthState.RequireLogin::class.java)
            assertThat((state as AuthState.RequireLogin).themeMode).isEqualTo(ThemeMode.System)
            credentialsFlow.emit(credentials)
            val newState = awaitItem()
            assertThat(newState).isInstanceOf(AuthState.LoggedIn::class.java)
            assertThat((newState as AuthState.LoggedIn).themeMode).isEqualTo(
                expectedThemeMode
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

}