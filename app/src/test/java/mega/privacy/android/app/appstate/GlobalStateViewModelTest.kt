package mega.privacy.android.app.appstate

import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.appstate.global.GlobalStateViewModel
import mega.privacy.android.app.appstate.global.mapper.BlockedStateMapper
import mega.privacy.android.app.appstate.global.model.GlobalState
import mega.privacy.android.app.appstate.initialisation.GlobalInitialiser
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.account.HandleBlockedStateSessionUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GlobalStateViewModelTest {
    private lateinit var underTest: GlobalStateViewModel
    private val monitorThemeModeUseCase = mock<MonitorThemeModeUseCase>()
    private val monitorUserCredentialsUseCase = mock<MonitorUserCredentialsUseCase>()
    private val monitorAccountBlockedUseCase = mock<MonitorAccountBlockedUseCase>()
    private val handleBlockedStateSessionUseCase = mock<HandleBlockedStateSessionUseCase>()
    private val snackbarEventQueue = mock<SnackbarEventQueue>()
    private val navigationEventQueue = mock<NavigationEventQueue>()

    private val globalInitialiser = mock<GlobalInitialiser>()

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
        underTest = GlobalStateViewModel(
            monitorThemeModeUseCase = monitorThemeModeUseCase,
            monitorUserCredentialsUseCase = monitorUserCredentialsUseCase,
            globalInitialiser = globalInitialiser,
            monitorAccountBlockedUseCase = monitorAccountBlockedUseCase,
            blockedStateMapper = BlockedStateMapper(),
            handleBlockedStateSessionUseCase = handleBlockedStateSessionUseCase,
            snackbarEventQueue = snackbarEventQueue,
            navigationEventQueue = navigationEventQueue,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            monitorThemeModeUseCase,
            monitorUserCredentialsUseCase,
            globalInitialiser,
            handleBlockedStateSessionUseCase,
            snackbarEventQueue,
            navigationEventQueue,
        )
    }

    @Test
    fun `test that app start initializers are called during initialization`() = runTest {
        verify(globalInitialiser).onAppStart()
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

        stubNotBlockedState()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(GlobalState.RequireLogin::class.java)

            // Verify pre-login initializers were called with null session
            verify(globalInitialiser).onPreLogin(null)

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
            assertThat(state).isInstanceOf(GlobalState.LoggedIn::class.java)

            // Verify pre-login initializers were called with existing session
            verify(globalInitialiser).onPreLogin("existing-session")

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

        stubNotBlockedState()

        underTest.state.filterIsInstance<GlobalState.LoggedIn>().test {
            val state = awaitItem()
            assertThat(state.themeMode).isEqualTo(themeMode)
            assertThat(state.session).isEqualTo(credentials.session)

            // Verify post-login initializers were called with the session
            verify(globalInitialiser).onPostLogin("test-session")

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

        stubNotBlockedState()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(GlobalState.RequireLogin::class.java)

            // Verify post-login initializers were NOT called
            verify(globalInitialiser, never()).onPostLogin(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(emptyFlow<UserCredentials>())
        }
        assertThat(underTest.state.value).isEqualTo(GlobalState.Loading(ThemeMode.System))
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
            assertThat(state).isInstanceOf(GlobalState.RequireLogin::class.java)
            assertThat((state as GlobalState.RequireLogin).themeMode).isEqualTo(themeMode)
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

            stubNotBlockedState()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(GlobalState.RequireLogin::class.java)
                assertThat((state as GlobalState.RequireLogin).themeMode).isEqualTo(themeMode)
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

            stubNotBlockedState()

            underTest.state.filterIsInstance<GlobalState.LoggedIn>().test {
                val state = awaitItem()
                assertThat(state.themeMode).isEqualTo(themeMode)
                assertThat(state.session).isEqualTo(credentials.session)
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

        stubNotBlockedState()

        underTest.state.test {
            // Initial state should be loading
            val initialState = awaitItem()
            assertThat(initialState).isInstanceOf(GlobalState.RequireLogin::class.java)

            // Emit credentials
            credentialsFlow.emit(credentials)

            // State should transition to logged in
            val loggedInState = awaitItem()
            assertThat(loggedInState).isInstanceOf(GlobalState.LoggedIn::class.java)
            assertThat((loggedInState as GlobalState.LoggedIn).session).isEqualTo(credentials.session)

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
            stubNotBlockedState()

            underTest.state.test {
                // Initial state should be logged in
                val initialState = awaitItem()
                assertThat(initialState).isInstanceOf(GlobalState.LoggedIn::class.java)

                // Emit null credentials
                credentialsFlow.emit(null)

                // State should transition to loading
                val loadingState = awaitItem()
                assertThat(loadingState).isInstanceOf(GlobalState.RequireLogin::class.java)

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

        underTest.state.filterIsInstance<GlobalState.LoggedIn>().test {
            val initialState = awaitItem()
            assertThat(initialState.themeMode).isEqualTo(initialThemeMode)

            // Change theme mode
            themeModeFlow.emit(ThemeMode.Dark)

            val updatedState = awaitItem()
            assertThat(updatedState.themeMode).isEqualTo(newThemeMode)
            assertThat(updatedState.session).isEqualTo(credentials.session)

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
            assertThat(state).isInstanceOf(GlobalState.Loading::class.java)
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

        stubNotBlockedState()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(GlobalState.LoggedIn::class.java)
            assertThat((state as GlobalState.LoggedIn).themeMode).isEqualTo(ThemeMode.System)
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

        stubNotBlockedState()

        underTest.state.test {
            // Should still emit the initial loading state even with errors
            val state = awaitItem()
            assertThat(state).isInstanceOf(GlobalState.RequireLogin::class.java)
            assertThat((state as GlobalState.RequireLogin).themeMode).isEqualTo(ThemeMode.System)
            credentialsFlow.emit(credentials)
            val newState = awaitItem()
            assertThat(newState).isInstanceOf(GlobalState.LoggedIn::class.java)
            assertThat((newState as GlobalState.LoggedIn).themeMode).isEqualTo(
                expectedThemeMode
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that when blocked state is not blocked and a session exists, auth state is logged in `() =
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
            stubNotBlockedState()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(GlobalState.LoggedIn::class.java)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when blocked state is not blocked and no session exists, auth state is not logged in `() =
        runTest {
            val themeMode = ThemeMode.Light
            monitorThemeModeUseCase.stub {
                on { invoke() }.thenReturn(MutableStateFlow(themeMode))
            }
            monitorUserCredentialsUseCase.stub {
                on { invoke() }.thenReturn(MutableStateFlow(null))
            }
            stubNotBlockedState()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(GlobalState.RequireLogin::class.java)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest
    @MethodSource("provideBlockedStates")
    fun `test that all blocked states sets state to not logged in`(blockedEvent: AccountBlockedEvent) =
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
            monitorAccountBlockedUseCase.stub {
                on { invoke() }.thenReturn(flow {
                    emit(blockedEvent)
                    awaitCancellation()
                })
            }
            underTest.state.test {
                val state = awaitItem()
                assertThat(state).isInstanceOf(GlobalState.RequireLogin::class.java)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun provideBlockedStates() =
        AccountBlockedType.entries.filterNot { it == AccountBlockedType.NOT_BLOCKED }.map {
            AccountBlockedEvent(
                handle = it.ordinal.toLong(),
                type = it,
                text = it.name
            )
        }

    @Test
    fun `test that state transitions from logged in to require login when account becomes blocked`() =
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

            val blockedFlow = MutableStateFlow(notBlockedEvent)
            monitorAccountBlockedUseCase.stub {
                on { invoke() }.thenReturn(blockedFlow)
            }

            underTest.state.test {
                // Initial state should be logged in
                val initialState = awaitItem()
                assertThat(initialState).isInstanceOf(GlobalState.LoggedIn::class.java)

                // Emit null credentials
                blockedFlow.emit(
                    AccountBlockedEvent(
                        handle = -1L,
                        type = AccountBlockedType.VERIFICATION_EMAIL,
                        text = ""
                    )
                )

                // State should transition to loading
                val loadingState = awaitItem()
                assertThat(loadingState).isInstanceOf(GlobalState.RequireLogin::class.java)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that state transitions from require login to logged in when account becomes blocked`() =
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

            val blockedFlow = MutableStateFlow(
                AccountBlockedEvent(
                    handle = -1L,
                    type = AccountBlockedType.VERIFICATION_EMAIL,
                    text = ""
                )
            )
            monitorAccountBlockedUseCase.stub {
                on { invoke() }.thenReturn(blockedFlow)
            }

            underTest.state.test {
                // Initial state should be logged in
                val initialState = awaitItem()
                assertThat(initialState).isInstanceOf(GlobalState.RequireLogin::class.java)

                // Emit null credentials
                blockedFlow.emit(
                    notBlockedEvent
                )

                // State should transition to loading
                val loadingState = awaitItem()
                assertThat(loadingState).isInstanceOf(GlobalState.LoggedIn::class.java)
                assertThat((loadingState as GlobalState.LoggedIn).session).isEqualTo(credentials.session)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that post login initialisers are not called again if credentials are updated, but session is the same`() =
        runTest {
            val session = "test-session"
            val initialCredentials = UserCredentials(
                email = "test@example.com",
                session = session,
                firstName = "John",
                lastName = "Doe",
                myHandle = "123456789"
            )

            val updatedCredentials = UserCredentials(
                email = "test@example.com",
                session = session,
                firstName = "Jane",
                lastName = "Doe",
                myHandle = "123456789"
            )

            val themeMode = ThemeMode.Light
            monitorThemeModeUseCase.stub {
                on { invoke() }.thenReturn(MutableStateFlow(themeMode))
            }

            val credentialsFlow = MutableStateFlow<UserCredentials?>(initialCredentials)
            monitorUserCredentialsUseCase.stub {
                on { invoke() }.thenReturn(credentialsFlow)
            }
            stubNotBlockedState()

            underTest.state.test {
                // Initial state should be logged in
                val initialState = awaitItem()
                assertThat(initialState).isInstanceOf(GlobalState.LoggedIn::class.java)

                // Emit new credentials
                credentialsFlow.emit(updatedCredentials)

                cancelAndIgnoreRemainingEvents()
            }

            verify(globalInitialiser, times(1)).onPreLogin(session)
        }

    @Test
    fun `test that post login initialisers are not called again if ui state is resubscribed`() =
        runTest {
            val session = "test-session"
            val credentials = UserCredentials(
                email = "test@example.com",
                session = session,
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
            stubNotBlockedState()
            underTest.state.test { cancelAndIgnoreRemainingEvents() }
            advanceTimeBy(6_000) // Advance time past ui state flow timeout
            underTest.state.test { cancelAndIgnoreRemainingEvents() }
            verify(globalInitialiser, times(1)).onPreLogin(session)
        }

    @Test
    fun `test that session is retained if ui state is resubscribed`() =
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
            stubNotBlockedState()
            underTest.state.test {
                assertThat(awaitItem()).isInstanceOf(GlobalState.LoggedIn::class.java)
                cancelAndIgnoreRemainingEvents()
            }
            advanceTimeBy(6_000) // Advance time past ui state flow timeout
            underTest.state.test {
                assertThat(awaitItem()).isInstanceOf(GlobalState.LoggedIn::class.java)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that new session values are retained if ui state is resubscribed`() =
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
            stubNotBlockedState()
            underTest.state.test {
                assertThat(awaitItem()).isInstanceOf(GlobalState.LoggedIn::class.java)
                cancelAndIgnoreRemainingEvents()
            }
            advanceTimeBy(6_000) // Advance time past ui state flow timeout
            credentialsFlow.emit(null)
            underTest.state.test {
                assertThat(awaitItem()).isInstanceOf(GlobalState.RequireLogin::class.java)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that pre login initialisers are not called again if credentials are updated, but session is the same`() =
        runTest {
            val session = "test-session"
            val initialCredentials = UserCredentials(
                email = "test@example.com",
                session = session,
                firstName = "John",
                lastName = "Doe",
                myHandle = "123456789"
            )

            val updatedCredentials = UserCredentials(
                email = "test@example.com",
                session = session,
                firstName = "Jane",
                lastName = "Doe",
                myHandle = "123456789"
            )

            val themeMode = ThemeMode.Light
            monitorThemeModeUseCase.stub {
                on { invoke() }.thenReturn(MutableStateFlow(themeMode))
            }

            val credentialsFlow = MutableStateFlow<UserCredentials?>(initialCredentials)
            monitorUserCredentialsUseCase.stub {
                on { invoke() }.thenReturn(credentialsFlow)
            }
            stubNotBlockedState()

            underTest.state.test {
                // Initial state should be logged in
                val initialState = awaitItem()
                assertThat(initialState).isInstanceOf(GlobalState.LoggedIn::class.java)

                // Emit new credentials
                credentialsFlow.emit(updatedCredentials)

                cancelAndIgnoreRemainingEvents()
            }

            verify(globalInitialiser, times(1)).onPostLogin(session)
        }

    @Test
    fun `test that pre login initialisers are not called again if ui state is resubscribed`() =
        runTest {
            val session = "test-session"
            val credentials = UserCredentials(
                email = "test@example.com",
                session = session,
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
            stubNotBlockedState()
            underTest.state.test { cancelAndIgnoreRemainingEvents() }
            advanceTimeBy(6_000) // Advance time past ui state flow timeout
            underTest.state.test { cancelAndIgnoreRemainingEvents() }
            verify(globalInitialiser, times(1)).onPostLogin(session)
        }

    @Test
    fun `test that handle blocked state is called when a blocked state is returned`() = runTest {
        stubNotBlockedState()
        monitorThemeModeUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(ThemeMode.Light))
        }
        monitorUserCredentialsUseCase.stub {
            on { invoke() }.thenReturn(MutableStateFlow(null))
        }

        underTest.state.test { cancelAndIgnoreRemainingEvents() }
        verify(handleBlockedStateSessionUseCase).invoke(notBlockedEvent)
    }

    @Test
    fun `test that queueMessage enqueues the message to snackbarEventQueue`() = runTest {
        val message = "something important"
        underTest.queueMessage(message)

        verify(snackbarEventQueue).queueMessage(message)
    }

    @Test
    fun `test that addNavKeysToEventQueue enqueues the navigation to navigationEventQueue`() =
        runTest {

            val navKeys = (0..3).map { mock<NavKey>() }

            underTest.addNavKeysToEventQueue(navKeys)

            navKeys.forEach {
                verify(navigationEventQueue).emit(it)
            }
        }

    private val notBlockedEvent = AccountBlockedEvent(
        handle = -1L,
        type = AccountBlockedType.NOT_BLOCKED,
        text = ""
    )

    private fun stubNotBlockedState() {
        monitorAccountBlockedUseCase.stub {
            on { invoke() }.thenReturn(flow {
                emit(
                    notBlockedEvent
                )
                awaitCancellation()
            })
        }
    }

}