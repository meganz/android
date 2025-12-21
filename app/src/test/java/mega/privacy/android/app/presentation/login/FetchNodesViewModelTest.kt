package mega.privacy.android.app.presentation.login

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.appstate.global.model.RefreshEvent
import mega.privacy.android.app.appstate.initialisation.GlobalInitialiser
import mega.privacy.android.app.triggeredContent
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.entity.login.TemporaryWaitingError
import mega.privacy.android.domain.exception.LoginRequireValidation
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.login.FetchNodesErrorAccess
import mega.privacy.android.domain.exception.login.FetchNodesUnknownStatus
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.GetUserDataUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.chat.IsMegaApiLoggedInUseCase
import mega.privacy.android.domain.usecase.login.FastLoginUseCase
import mega.privacy.android.domain.usecase.login.FetchNodesUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.requeststatus.EnableRequestStatusMonitorUseCase
import mega.privacy.android.domain.usecase.requeststatus.MonitorRequestStatusProgressEventUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import nz.mega.sdk.MegaError
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FetchNodesViewModelTest {
    private lateinit var underTest: FetchNodesViewModel
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val rootNodeExistsUseCase = mock<RootNodeExistsUseCase>()
    private val fastLoginUseCase = mock<FastLoginUseCase>()
    private val fetchNodesUseCase = mock<FetchNodesUseCase>()
    private val loginMutex = mock<Mutex>()
    private val enableRequestStatusMonitorUseCase = mock<EnableRequestStatusMonitorUseCase>()
    private val monitorRequestStatusProgressEventUseCase =
        mock<MonitorRequestStatusProgressEventUseCase>()
    private val ephemeralCredentialManager = mock<EphemeralCredentialManager>()
    private val resetChatSettingsUseCase = mock<ResetChatSettingsUseCase>()
    private val monitorAccountBlockedUseCase = mock<MonitorAccountBlockedUseCase>()
    private val isMegaApiLoggedInUseCase = mock<IsMegaApiLoggedInUseCase>()
    private val fetchNodesException = mock<FetchNodesUnknownStatus> {
        on { megaException }
            .thenReturn(
                MegaException(
                    MegaError.API_EACCESS,
                    "Access error"
                )
            )
    }
    private val getUserDataUseCase: GetUserDataUseCase = mock()
    private val globalInitialiser = mock<GlobalInitialiser>()
    private val applicationScope = CoroutineScope(UnconfinedTestDispatcher())

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
        // Reset mocks before each test
        reset(
            isConnectedToInternetUseCase,
            rootNodeExistsUseCase,
            fastLoginUseCase,
            fetchNodesUseCase,
            loginMutex,
            enableRequestStatusMonitorUseCase,
            monitorRequestStatusProgressEventUseCase,
            ephemeralCredentialManager,
            resetChatSettingsUseCase,
            monitorAccountBlockedUseCase,
            isMegaApiLoggedInUseCase,
            getUserDataUseCase,
            globalInitialiser
        )
    }

    private fun initViewModel(
        args: FetchNodesViewModel.Args = FetchNodesViewModel.Args(
            session = "test-session",
            isFromLogin = false,
            refreshEvent = null
        ),
    ) {
        underTest = FetchNodesViewModel(
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            fastLoginUseCase = fastLoginUseCase,
            fetchNodesUseCase = fetchNodesUseCase,
            loginMutex = loginMutex,
            enableRequestStatusMonitorUseCase = enableRequestStatusMonitorUseCase,
            monitorRequestStatusProgressEventUseCase = monitorRequestStatusProgressEventUseCase,
            ephemeralCredentialManager = ephemeralCredentialManager,
            resetChatSettingsUseCase = resetChatSettingsUseCase,
            monitorAccountBlockedUseCase = monitorAccountBlockedUseCase,
            isMegaApiLoggedInUseCase = isMegaApiLoggedInUseCase,
            getUserDataUseCase = getUserDataUseCase,
            globalInitialiser = globalInitialiser,
            args = args,
            applicationScope = applicationScope,
        )
    }


    @Test
    fun `test that initializers are called during initialization`() = runTest {
        // Setup default mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        wheneverBlocking { isMegaApiLoggedInUseCase() }.thenReturn(true)

        initViewModel()

        verify(enableRequestStatusMonitorUseCase).invoke()
        verify(resetChatSettingsUseCase).invoke()
        verify(monitorAccountBlockedUseCase).invoke()
    }

    @Test
    fun `test that fast login is performed when user is not logged in`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        whenever(isMegaApiLoggedInUseCase()).thenReturn(false)
        val loginStatusFlow = MutableStateFlow<LoginStatus>(LoginStatus.LoginStarted)
        whenever(fastLoginUseCase.invoke(any(), any(), any())).thenReturn(loginStatusFlow)
        whenever(fetchNodesUseCase()).thenReturn(emptyFlow())

        initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isFastLoginInProgress).isTrue()
            assertThat(state.isFastLogin).isTrue()

            verify(fastLoginUseCase).invoke(any(), any(), any())

            // Emit login success to trigger onPostLogin
            loginStatusFlow.emit(LoginStatus.LoginSucceed)
            advanceUntilIdle()

            verify(globalInitialiser).onPostLogin("test-session", true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that fetch nodes is performed when user is already logged in`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        whenever(isMegaApiLoggedInUseCase()).thenReturn(true)
        whenever(fetchNodesUseCase()).thenReturn(emptyFlow())

        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isFastLoginInProgress).isFalse()
            assertThat(state.isFastLogin).isFalse()

            verify(fetchNodesUseCase).invoke()
            verify(fastLoginUseCase, never()).invoke(any(), any(), any())
            verify(globalInitialiser).onPostLogin("test-session", false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that login waiting status shows temporary error`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        whenever(isMegaApiLoggedInUseCase()).thenReturn(false)
        val loginStatusFlow = MutableStateFlow<LoginStatus>(LoginStatus.LoginStarted)
        whenever(fastLoginUseCase.invoke(any(), any(), any())).thenReturn(loginStatusFlow)

        initViewModel()

        underTest.state.test {
            // Initial state
            awaitItem()

            // Login waiting with error
            val waitingError = TemporaryWaitingError.ConnectivityIssues
            loginStatusFlow.emit(LoginStatus.LoginWaiting(waitingError))
            val waitingState = awaitItem()
            assertThat(waitingState.loginTemporaryError).isEqualTo(waitingError)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that login exception is handled correctly`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        whenever(isMegaApiLoggedInUseCase()).thenReturn(false)
        val loginException = LoginRequireValidation()
        whenever(fastLoginUseCase.invoke(any(), any(), any())).thenReturn(
            flow { throw loginException }
        )

        initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.snackbarMessage.triggeredContent()).isNotNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that fetch nodes progress updates are handled correctly`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        whenever(isMegaApiLoggedInUseCase()).thenReturn(true)
        val fetchNodesFlow = MutableStateFlow(FetchNodesUpdate())
        whenever(fetchNodesUseCase()).thenReturn(fetchNodesFlow)

        initViewModel()

        underTest.state.test {
            // Initial state
            val initialState = awaitItem()
            assertThat(initialState.fetchNodesUpdate).isNotNull()

            // Progress update
            val progressUpdate = FetchNodesUpdate(progress = Progress(0.5f))
            fetchNodesFlow.emit(progressUpdate)
            val progressState = awaitItem()
            assertThat(progressState.fetchNodesUpdate?.progress?.floatValue).isEqualTo(0.5f)

            // Completion
            val completionUpdate = FetchNodesUpdate(progress = Progress(1.0f))
            fetchNodesFlow.emit(completionUpdate)
            val completionState = awaitItem()
            assertThat(completionState.fetchNodesUpdate?.progress?.floatValue).isEqualTo(1.0f)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that fetch nodes exception is handled correctly`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        whenever(isMegaApiLoggedInUseCase()).thenReturn(true)
        whenever(fetchNodesUseCase()).thenReturn(
            flow { throw fetchNodesException }
        )

        initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.snackbarMessage.triggeredContent()).isNotNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that fetch nodes error access is handled without snackbar`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        whenever(isMegaApiLoggedInUseCase()).thenReturn(true)
        val fetchNodesErrorAccess = mock<FetchNodesErrorAccess>()
        whenever(fetchNodesUseCase()).thenReturn(
            flow { throw fetchNodesErrorAccess }
        )

        initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.snackbarMessage.triggeredContent()).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that request status progress updates are handled correctly`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        val requestStatusFlow = MutableStateFlow<Progress?>(null)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(requestStatusFlow)
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        wheneverBlocking { isMegaApiLoggedInUseCase() }.thenReturn(true)

        initViewModel()

        underTest.state.test {
            // Initial state
            val initialState = awaitItem()
            assertThat(initialState.requestStatusProgress).isNull()

            // Progress update
            val progress = Progress(0.3f)
            requestStatusFlow.emit(progress)
            val progressState = awaitItem()
            assertThat(progressState.requestStatusProgress).isEqualTo(progress)

            // Clear progress
            requestStatusFlow.emit(null)
            val clearState = awaitItem()
            assertThat(clearState.requestStatusProgress).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that account blocked events stop fetch nodes`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(isMegaApiLoggedInUseCase()).thenReturn(true)
        val fetchNodesFlow = MutableStateFlow(FetchNodesUpdate())
        whenever(fetchNodesUseCase()).thenReturn(fetchNodesFlow)
        val accountBlockedFlow = MutableSharedFlow<AccountBlockedEvent>()
        whenever(monitorAccountBlockedUseCase()).thenReturn(accountBlockedFlow)

        initViewModel()

        underTest.state.test {
            // Initial state
            awaitItem()

            // Emit blocked account event
            val blockedEvent = mock<AccountBlockedEvent>()
            accountBlockedFlow.emit(blockedEvent)

            // Verify fetch nodes was called initially
            verify(fetchNodesUseCase).invoke()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that isConnected returns correct value`() {
        // Setup mocks
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        wheneverBlocking { isMegaApiLoggedInUseCase() }.thenReturn(true)

        initViewModel()

        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        assertThat(underTest.isConnected).isTrue()

        whenever(isConnectedToInternetUseCase()).thenReturn(false)
        assertThat(underTest.isConnected).isFalse()
    }

    @Test
    fun `test that request status progress error hides progress bar`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(
            flow { throw RuntimeException("Test error") }
        )
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        wheneverBlocking { isMegaApiLoggedInUseCase() }.thenReturn(true)

        initViewModel()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.requestStatusProgress).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that isFromLogin is passed correctly to state`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        wheneverBlocking { isMegaApiLoggedInUseCase() }.thenReturn(true)
        whenever(fetchNodesUseCase()).thenReturn(emptyFlow())

        initViewModel(
            args = FetchNodesViewModel.Args(
                session = "test-session",
                isFromLogin = true,
                refreshEvent = null
            )
        )

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isFromLogin).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that refresh event Refresh triggers fetch nodes with refresh session`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        wheneverBlocking { isMegaApiLoggedInUseCase() }.thenReturn(true)
        val fetchNodesFlow = MutableStateFlow(FetchNodesUpdate())
        whenever(fetchNodesUseCase()).thenReturn(fetchNodesFlow)

        initViewModel(
            args = FetchNodesViewModel.Args(
                session = "test-session",
                isFromLogin = false,
                refreshEvent = RefreshEvent.ManualRefresh
            )
        )
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            // When refresh event is Refresh, it should call fetchNodes with isRefreshSession = true
            // which resets the fetchNodesUpdate to clean state
            assertThat(state.fetchNodesUpdate).isNotNull()

            verify(fetchNodesUseCase).invoke()
            // When refreshEvent is Refresh, handlePostLogin is not called
            verify(globalInitialiser, never()).onPostLogin(any(), any())
            verify(fastLoginUseCase, never()).invoke(any(), any(), any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that refresh event ChangeEnvironment triggers fast login with refreshChatUrl true`() =
        runTest {
            // Setup mocks
            whenever(isConnectedToInternetUseCase()).thenReturn(true)
            whenever(loginMutex.isLocked).thenReturn(false)
            whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
            whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
            wheneverBlocking { isMegaApiLoggedInUseCase() }.thenReturn(true)
            val loginStatusFlow = MutableStateFlow<LoginStatus>(LoginStatus.LoginStarted)
            whenever(fastLoginUseCase.invoke(any(), any(), any())).thenReturn(loginStatusFlow)
            whenever(fetchNodesUseCase()).thenReturn(emptyFlow())

            initViewModel(
                args = FetchNodesViewModel.Args(
                    session = "test-session",
                    isFromLogin = false,
                    refreshEvent = RefreshEvent.ChangeEnvironment
                )
            )
            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isFastLoginInProgress).isTrue()
                assertThat(state.isFastLogin).isTrue()

                verify(fastLoginUseCase).invoke(eq("test-session"), eq(true), any())
                verify(fetchNodesUseCase, never()).invoke()
                verify(globalInitialiser, never()).onPostLogin(any(), any())

                loginStatusFlow.emit(LoginStatus.LoginSucceed)
                advanceUntilIdle()

                verify(globalInitialiser).onPostLogin("test-session", true)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that null refresh event triggers normal fetch nodes flow when logged in`() = runTest {
        // Setup mocks
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        whenever(loginMutex.isLocked).thenReturn(false)
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(emptyFlow())
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        wheneverBlocking { isMegaApiLoggedInUseCase() }.thenReturn(true)
        whenever(fetchNodesUseCase()).thenReturn(emptyFlow())

        initViewModel(
            args = FetchNodesViewModel.Args(
                session = "test-session",
                isFromLogin = false,
                refreshEvent = null
            )
        )
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.isFastLoginInProgress).isFalse()
            assertThat(state.isFastLogin).isFalse()

            verify(fetchNodesUseCase).invoke()
            verify(globalInitialiser).onPostLogin("test-session", false)
            verify(fastLoginUseCase, never()).invoke(any(), any(), any())

            cancelAndIgnoreRemainingEvents()
        }
    }

} 