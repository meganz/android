package mega.privacy.android.app.presentation.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.test.AnalyticsTestExtension
import mega.privacy.android.app.InstantExecutorExtension
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.content.navigation.FetchNodeProvider
import mega.privacy.android.app.middlelayer.installreferrer.InstallReferrerDetails
import mega.privacy.android.app.middlelayer.installreferrer.InstallReferrerHandler
import mega.privacy.android.app.presentation.login.mapper.AccountBlockedTypeStringMapper
import mega.privacy.android.app.presentation.login.model.AccountBlockedUiState
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginScreen
import mega.privacy.android.app.presentation.login.model.RkLink
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.login.GoogleSignInResult
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.exception.LoginBlockedAccount
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginMultiFactorAuthRequired
import mega.privacy.android.domain.exception.LoginRequireValidation
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.account.CheckRecoveryKeyUseCase
import mega.privacy.android.domain.usecase.account.ClearUserCredentialsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.account.ResendVerificationEmailUseCase
import mega.privacy.android.domain.usecase.account.CreateAccountUseCase
import mega.privacy.android.domain.usecase.account.ResumeCreateAccountUseCase
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.domain.usecase.environment.GetHistoricalProcessExitReasonsUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.DecodeGoogleIdTokenUseCase
import mega.privacy.android.domain.usecase.login.GetLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutUseCase
import mega.privacy.android.domain.usecase.login.LoginUseCase
import mega.privacy.android.domain.usecase.login.LoginWith2FAUseCase
import mega.privacy.android.domain.usecase.login.MonitorEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.domain.usecase.login.SaveEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.setting.GetMiscFlagsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorMiscLoadedUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.OngoingTransfersExistUseCase
import mega.privacy.android.domain.usecase.transfers.ResumeTransfersForNotLoggedInInstanceUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(InstantExecutorExtension::class)
@ExperimentalCoroutinesApi
internal class LoginViewModelTest {

    private lateinit var underTest: LoginViewModel

    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase = mock()
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase = mock()
    private val monitorUserCredentialsFlow = MutableSharedFlow<UserCredentials?>()
    private val monitorUserCredentialsUseCase: MonitorUserCredentialsUseCase = mock()
    private val querySignupLinkUseCase: QuerySignupLinkUseCase = mock()
    private val cancelTransfersUseCase: CancelTransfersUseCase = mock()
    private val localLogoutUseCase: LocalLogoutUseCase = mock()
    private val loginUseCase: LoginUseCase = mock()
    private val loginWith2FAUseCase: LoginWith2FAUseCase = mock()
    private val ongoingTransfersExistUseCase: OngoingTransfersExistUseCase = mock()
    private val monitorEphemeralCredentialsUseCase: MonitorEphemeralCredentialsUseCase = mock()
    private val saveEphemeralCredentialsUseCase: SaveEphemeralCredentialsUseCase = mock()
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase = mock()
    private val monitorAccountBlockedUseCase = mock<MonitorAccountBlockedUseCase>()
    private val accountBlockedTypeStringMapper = mock<AccountBlockedTypeStringMapper>()
    private val getLastRegisteredEmailUseCase = mock<GetLastRegisteredEmailUseCase>()
    private val installReferrerHandler = mock<InstallReferrerHandler>()
    private val clearUserCredentialsUseCase = mock<ClearUserCredentialsUseCase>()
    private val getHistoricalProcessExitReasonsUseCase =
        mock<GetHistoricalProcessExitReasonsUseCase>()
    private val monitorThemeModeUseCase = mock<MonitorThemeModeUseCase>()
    private val resendVerificationEmailUseCase = mock<ResendVerificationEmailUseCase>()
    private val resumeCreateAccountUseCase = mock<ResumeCreateAccountUseCase>()
    private val checkRecoveryKeyUseCase = mock<CheckRecoveryKeyUseCase>()
    private val savedStateHandle = mock<SavedStateHandle>()
    private val ephemeralCredentialManager = mock<EphemeralCredentialManager>()
    private val resumeTransfersForNotLoggedInInstanceUseCase =
        mock<ResumeTransfersForNotLoggedInInstanceUseCase>()
    private val getMiscFlagsUseCase = mock<GetMiscFlagsUseCase>()
    private val getDomainNameUseCase = mock<GetDomainNameUseCase>()
    private val monitorMiscLoadedUseCase = mock<MonitorMiscLoadedUseCase>()
    private val monitorMiscLoadedFlow = MutableSharedFlow<Boolean>()
    private val decodeGoogleIdTokenUseCase: DecodeGoogleIdTokenUseCase = mock()
    private val createAccountUseCase: CreateAccountUseCase = mock()

    @BeforeEach
    fun setUp() = runTest {
        stubCommon()
        initViewModel()
    }

    private fun initViewModel() {
        underTest = LoginViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            resetChatSettingsUseCase = resetChatSettingsUseCase,
            monitorUserCredentialsUseCase = monitorUserCredentialsUseCase,
            querySignupLinkUseCase = querySignupLinkUseCase,
            cancelTransfersUseCase = cancelTransfersUseCase,
            localLogoutUseCase = localLogoutUseCase,
            loginUseCase = loginUseCase,
            loginWith2FAUseCase = loginWith2FAUseCase,
            ongoingTransfersExistUseCase = ongoingTransfersExistUseCase,
            monitorEphemeralCredentialsUseCase = monitorEphemeralCredentialsUseCase,
            saveEphemeralCredentialsUseCase = saveEphemeralCredentialsUseCase,
            clearEphemeralCredentialsUseCase = clearEphemeralCredentialsUseCase,
            monitorAccountBlockedUseCase = monitorAccountBlockedUseCase,
            accountBlockedTypeStringMapper = accountBlockedTypeStringMapper,
            loginMutex = mock(),
            getLastRegisteredEmailUseCase = getLastRegisteredEmailUseCase,
            installReferrerHandler = installReferrerHandler,
            clearUserCredentialsUseCase = clearUserCredentialsUseCase,
            getHistoricalProcessExitReasonsUseCase = getHistoricalProcessExitReasonsUseCase,
            monitorThemeModeUseCase = monitorThemeModeUseCase,
            resendVerificationEmailUseCase = resendVerificationEmailUseCase,
            resumeCreateAccountUseCase = resumeCreateAccountUseCase,
            checkRecoveryKeyUseCase = checkRecoveryKeyUseCase,
            savedStateHandle = savedStateHandle,
            ephemeralCredentialManager = ephemeralCredentialManager,
            resumeTransfersForNotLoggedInInstanceUseCase = resumeTransfersForNotLoggedInInstanceUseCase,
            getMiscFlagsUseCase = getMiscFlagsUseCase,
            getDomainNameUseCase = getDomainNameUseCase,
            monitorMiscLoadedUseCase = monitorMiscLoadedUseCase,
            fetchNodeProvider = mock(),
            decodeGoogleIdTokenUseCase = decodeGoogleIdTokenUseCase,
            createAccountUseCase = createAccountUseCase,
        )
    }

    private suspend fun stubCommon() {
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        whenever(monitorEphemeralCredentialsUseCase()).thenReturn(emptyFlow())
        whenever(monitorUserCredentialsUseCase()).thenReturn(monitorUserCredentialsFlow)
        // Emit initial value to match test expectations (isAlreadyLoggedIn = true)
        monitorUserCredentialsFlow.emit(
            UserCredentials(
                email = null,
                session = "initialSession",
                firstName = null,
                lastName = null,
                myHandle = null
            )
        )
        whenever(monitorThemeModeUseCase()).thenReturn(flowOf(ThemeMode.System))
        whenever(monitorMiscLoadedUseCase()).thenReturn(monitorMiscLoadedFlow)
        whenever(getDomainNameUseCase()).thenReturn("mega.foo")
        whenever(savedStateHandle.get<Int>(any<String>())).thenReturn(null)
        whenever(savedStateHandle.get<String>(any<String>())).thenReturn(null)
    }

    @AfterEach
    fun resetMocks() {
        reset(
            resendVerificationEmailUseCase,
            checkRecoveryKeyUseCase,
            savedStateHandle,
            resumeTransfersForNotLoggedInInstanceUseCase,
            getMiscFlagsUseCase,
            getDomainNameUseCase,
            monitorMiscLoadedUseCase,
            decodeGoogleIdTokenUseCase,
            createAccountUseCase,
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            with(awaitItem()) {
                assertThat(intentState).isNull()
                assertThat(accountSession).isNull()
                assertThat(emailError).isNull()
                assertThat(password).isNull()
                assertThat(passwordError).isNull()
                assertThat(is2FARequired).isFalse()
                assertThat(multiFactorAuthState).isNull()
                assertThat(isAccountConfirmed).isFalse()
                assertThat(temporalEmail).isNull()
                assertThat(isLoginRequired).isFalse()
                assertThat(isLoginInProgress).isFalse()
                assertThat(loginException).isNull()
                assertThat(ongoingTransfersExist).isNull()
                assertThat(isPendingToShowFragment).isEqualTo(consumed())
                assertThat(snackbarMessage).isInstanceOf(consumed().javaClass)
            }
        }
    }

    @Test
    fun `test that emailError is updated when onLoginClicked and email is null`() = runTest {
        with(underTest) {
            state.map { it.emailError }.test {
                assertThat(awaitItem()).isNull()
                onLoginClicked(false)
                assertThat(awaitItem()).isEqualTo(LoginError.EmptyEmail)
            }
        }
    }

    @Test
    fun `test that emailError is updated when onLoginClicked and email is not correct`() = runTest {
        with(underTest) {
            state.map { it.emailError }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isNull()
                    onEmailChanged("wrongEmail")
                    onLoginClicked(false)
                    assertThat(awaitItem()).isEqualTo(LoginError.NotValidEmail)
                }
        }
    }

    @Test
    fun `test that passwordError is updated when onLoginClicked and password is null`() = runTest {
        with(underTest) {
            state.map { it.passwordError }.test {
                assertThat(awaitItem()).isNull()
                onLoginClicked(false)
                assertThat(awaitItem()).isEqualTo(LoginError.EmptyPassword)
            }
        }
    }

    @Test
    fun `test that setPendingFragmentToShow clears emailError and passwordError when showing the login screen`() =
        runTest {
            with(underTest) {
                onLoginClicked(false)
                state.test {
                    awaitItem().let {
                        assertThat(it.emailError).isEqualTo(LoginError.EmptyEmail)
                        assertThat(it.passwordError).isEqualTo(LoginError.EmptyPassword)
                    }
                    setPendingFragmentToShow(LoginScreen.LoginScreen)
                    awaitItem().let {
                        assertThat(it.emailError).isNull()
                        assertThat(it.passwordError).isNull()
                    }
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

    @Test
    fun `test that setPendingFragmentToShow does not clear emailError and passwordError when showing another screen`() =
        runTest {
            with(underTest) {
                onLoginClicked(false)
                state.test {
                    awaitItem().let {
                        assertThat(it.emailError).isEqualTo(LoginError.EmptyEmail)
                        assertThat(it.passwordError).isEqualTo(LoginError.EmptyPassword)
                    }
                    setPendingFragmentToShow(LoginScreen.CreateAccount)
                    awaitItem().let {
                        assertThat(it.emailError).isEqualTo(LoginError.EmptyEmail)
                        assertThat(it.passwordError).isEqualTo(LoginError.EmptyPassword)
                    }
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

    @Test
    fun `test that ongoingTransfersExist is updated when onLoginClicked and there are transfers in progress`() =
        runTest {
            whenever(ongoingTransfersExistUseCase()).thenReturn(true)

            with(underTest) {
                state.map { it.ongoingTransfersExist }.distinctUntilChanged()
                    .test {
                        onEmailChanged("test@test.com")
                        onPasswordChanged("Password")
                        assertThat(awaitItem()).isNull()
                        onLoginClicked(false)
                        assertThat(awaitItem()).isTrue()
                    }
            }
        }

    @Test
    fun `test that snackbarMessage is updated when onLoginClicked and there is no network connection`() =
        runTest {
            whenever(ongoingTransfersExistUseCase()).thenReturn(false)
            whenever(isConnectedToInternetUseCase()).thenReturn(false)

            with(underTest) {
                state.map { it.snackbarMessage }
                    .test {
                        assertThat(awaitItem()).isInstanceOf(consumed().javaClass)
                        onEmailChanged("test@test.com")
                        assertThat(awaitItem()).isInstanceOf(consumed().javaClass)
                        onPasswordChanged("Password")
                        assertThat(awaitItem()).isInstanceOf(consumed().javaClass)
                        onLoginClicked(false)
                        assertThat(awaitItem()).isInstanceOf(triggered(R.string.error_server_connection_problem).javaClass)
                        cancelAndIgnoreRemainingEvents()
                    }
            }
        }

    @Test
    fun `test that performLogin is invoked when onLoginClick and there are no errors`() = runTest {
        whenever(ongoingTransfersExistUseCase()).thenReturn(false)
        whenever(isConnectedToInternetUseCase()).thenReturn(true)

        with(underTest) {
            state.test {
                onEmailChanged("test@test.com")
                onPasswordChanged("Password")
                onLoginClicked(false)
                advanceUntilIdle()
                assertThat(awaitItem().emailError).isNull()
                assertThat(awaitItem().passwordError).isNull()
                assertThat(awaitItem().ongoingTransfersExist).isNull()
                assertThat(awaitItem().snackbarMessage).isInstanceOf(consumed().javaClass)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `test that clearEphemeralCredentialsUseCase invoke when calling clearEphemeral`() =
        runTest {
            underTest.clearEphemeral()
            advanceUntilIdle()
            verify(clearEphemeralCredentialsUseCase).invoke()
        }

    @Test
    fun `test that sendAnalyticsEventIfFirstTimeLogin sends event when logged email matched with last registered email`() =
        runTest {
            val email = "test@example.com"
            val details = InstallReferrerDetails(
                referrerUrl = "referrerUrl",
                referrerClickTime = 123L,
                appInstallTime = 456L
            )
            whenever(getLastRegisteredEmailUseCase()).thenReturn(email)
            whenever(installReferrerHandler.getDetails()).thenReturn(details)

            underTest.sendAnalyticsEventIfFirstTimeLogin(email)
            advanceUntilIdle()

            assertThat(analyticsExtension.events).hasSize(1)
            verify(installReferrerHandler).getDetails()
        }

    @Test
    fun `test that sendAnalyticsEventIfFirstTimeLogin does not send event when emails do not match`() =
        runTest {
            val email = "test@example.com"
            val lastRegisteredEmail = "lastRegistered@example.com"

            whenever(getLastRegisteredEmailUseCase()).thenReturn(lastRegisteredEmail)

            underTest.sendAnalyticsEventIfFirstTimeLogin(email)
            advanceUntilIdle()

            assertThat(analyticsExtension.events).isEmpty()
        }

    @Test
    fun `test that clear user credentials invoke correctly`() = runTest {
        underTest.clearUserCredentials()
        advanceUntilIdle()
        verify(clearUserCredentialsUseCase).invoke()
    }

    @Test
    fun `test that getHistoricalProcessExitReasonsUseCase invoke correctly`() = runTest {
        verify(getHistoricalProcessExitReasonsUseCase).invoke()
    }

@Test
    fun `test that resend Verification Email UseCase should be triggered when resend email is clicked`() =
        runTest {
            underTest.resendVerificationEmail()
            advanceUntilIdle()
            verify(resendVerificationEmailUseCase).invoke()
        }

    @Test
    fun `test that resend email success event is triggered when resend email use case returns success`() =
        runTest {
            whenever(resendVerificationEmailUseCase()).thenReturn(Unit)
            underTest.resendVerificationEmail()
            advanceUntilIdle()
            underTest.state.test {
                val item = awaitItem()
                assertThat(item.resendVerificationEmailEvent).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
                if (item.resendVerificationEmailEvent is StateEventWithContentTriggered) {
                    assertThat((item.resendVerificationEmailEvent as StateEventWithContentTriggered<Boolean>).content).isTrue()
                }
            }
        }

    @Test
    fun `test that resend email failure event is triggered when resend email use case throws error`() =
        runTest {
            whenever(resendVerificationEmailUseCase()).thenThrow(RuntimeException())
            underTest.resendVerificationEmail()
            advanceUntilIdle()
            underTest.state.test {
                val item = awaitItem()
                assertThat(item.resendVerificationEmailEvent).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
                if (item.resendVerificationEmailEvent is StateEventWithContentTriggered) {
                    assertThat((item.resendVerificationEmailEvent as StateEventWithContentTriggered<Boolean>).content).isFalse()
                }
            }
        }

    @Test
    fun `test that resumeCreateAccount is invoked when calling resumeCreateAccount`() = runTest {
        underTest.resumeCreateAccount("session")
        advanceUntilIdle()
        verify(resumeCreateAccountUseCase).invoke("session")
    }

    @Test
    fun `test that checkRecoveryKey triggers success event when use case succeeds`() = runTest {
        val link = "https://example.com/recovery"
        val recoveryKey = "validRecoveryKey"
        whenever(checkRecoveryKeyUseCase(link, recoveryKey)).thenReturn(Unit)

        underTest.checkRecoveryKey(link, recoveryKey)
        advanceUntilIdle()

        underTest.state.test {
            val item = awaitItem()
            assertThat(item.checkRecoveryKeyEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            if (item.checkRecoveryKeyEvent is StateEventWithContentTriggered) {
                val result =
                    (item.checkRecoveryKeyEvent as StateEventWithContentTriggered<Result<RkLink>>).content
                assertThat(result.isSuccess).isTrue()
                assertThat(result.getOrNull()).isEqualTo(RkLink(link, recoveryKey))
            }
        }
    }

    @Test
    fun `test that checkRecoveryKey triggers failure event when use case throws error`() = runTest {
        val link = "https://example.com/recovery"
        val recoveryKey = "invalidRecoveryKey"
        val exception = RuntimeException("Invalid recovery key")
        whenever(checkRecoveryKeyUseCase(link, recoveryKey)).thenThrow(exception)

        underTest.checkRecoveryKey(link, recoveryKey)
        advanceUntilIdle()

        underTest.state.test {
            val item = awaitItem()
            assertThat(item.checkRecoveryKeyEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            if (item.checkRecoveryKeyEvent is StateEventWithContentTriggered) {
                val result =
                    (item.checkRecoveryKeyEvent as StateEventWithContentTriggered<Result<RkLink>>).content
                assertThat(result.isFailure).isTrue()
                assertThat(result.exceptionOrNull()).isEqualTo(exception)
            }
        }
    }

    @Test
    fun `test that onCheckRecoveryKeyEventConsumed resets checkRecoveryKeyEvent`() = runTest {
        underTest.onCheckRecoveryKeyEventConsumed()
        advanceUntilIdle()

        underTest.state.test {
            val item = awaitItem()
            assertThat(item.checkRecoveryKeyEvent).isInstanceOf(consumed().javaClass)
        }
    }

    @Test
    fun `test that resumeTransfersForNotLoggedInInstanceUseCase is invoked when there is no session`() =
        runTest {
            monitorUserCredentialsFlow.emit(null)
            advanceUntilIdle()

            verify(resumeTransfersForNotLoggedInInstanceUseCase).invoke()
        }

    @Test
    fun `test that resumeTransfersForNotLoggedInInstanceUseCase is not invoked when there is session`() =
        runTest {
            monitorUserCredentialsFlow.emit(
                UserCredentials(
                    email = null,
                    session = "session",
                    firstName = null,
                    lastName = null,
                    myHandle = null
                )
            )
            advanceUntilIdle()

            verifyNoInteractions(resumeTransfersForNotLoggedInInstanceUseCase)
        }

    @Test
    fun `test that checkTemporalCredentials returns true and performs login when valid credentials exist`() =
        runTest {
            val email = "test@example.com"
            val password = "password123"
            val ephemeralCredentials = EphemeralCredentials(
                email = email,
                password = password,
                session = "session",
                firstName = "John",
                lastName = "Doe"
            )

            whenever(ephemeralCredentialManager.getEphemeralCredential()).thenReturn(
                ephemeralCredentials
            )

            val result = underTest.checkTemporalCredentials()

            assertTrue(result)
            advanceUntilIdle()
            verify(loginUseCase).invoke(
                eq(email),
                eq(password),
                any()
            )
        }

    @Test
    fun `test that checkTemporalCredentials returns false when ephemeral credentials are null`() =
        runTest {
            whenever(ephemeralCredentialManager.getEphemeralCredential()).thenReturn(null)

            val result = underTest.checkTemporalCredentials()

            assertFalse(result)
            verifyNoInteractions(loginUseCase)
        }

    @ParameterizedTest(name = "test that checkTemporalCredentials returns false when credentials are invalid: {0}")
    @MethodSource("provideInvalidCredentials")
    fun `test that checkTemporalCredentials returns false when credentials are invalid`(
        credentials: EphemeralCredentials,
        description: String,
    ) = runTest {
        whenever(ephemeralCredentialManager.getEphemeralCredential()).thenReturn(credentials)

        val result = underTest.checkTemporalCredentials()

        assertFalse(result)
        verifyNoInteractions(loginUseCase)
    }

    @Test
    fun `test that openRecoveryUrlEvent is not triggered until the feature flags are loaded`() =
        runTest {
            underTest.state.test {
                val item = awaitItem()
                assertThat(item.miscFlagLoaded).isFalse()
                assertThat(item.openUrlEvent).isInstanceOf(
                    StateEventWithContentConsumed::class.java
                )
                underTest.onForgotPassword()
                this.expectNoEvents()
                monitorMiscLoadedFlow.emit(true)
                advanceUntilIdle()
                assertThat(awaitItem().miscFlagLoaded).isTrue()
                assertThat(awaitItem().openUrlEvent).isInstanceOf(
                    StateEventWithContentTriggered::class.java
                )
            }
        }

    @Test
    fun `test that the account blocked event model is successfully mapped to account blocker ui state`() =
        runTest {
            val handle = 123L
            val type = AccountBlockedType.VERIFICATION_EMAIL
            val text = "text"
            val accountBlockedEvent = AccountBlockedEvent(
                handle = handle,
                type = type,
                text = text
            )

            underTest.triggerAccountBlockedEvent(accountBlockedEvent = accountBlockedEvent)

            underTest.state.test {
                assertThat(expectMostRecentItem().accountBlockedEvent).isEqualTo(
                    triggered(
                        AccountBlockedUiState(
                            type = type,
                            text = text
                        )
                    )
                )
            }
        }

    @Test
    fun `test that monitorAccountBlockedUseCase emits then state receives event with text from mapper`() =
        runTest {
            val mappedText = "mapped message"
            whenever(accountBlockedTypeStringMapper(any())).thenReturn(mappedText)
            val accountBlockedFlow = MutableSharedFlow<AccountBlockedEvent>()
            whenever(monitorAccountBlockedUseCase()).thenReturn(accountBlockedFlow)
            initViewModel()
            advanceUntilIdle()

            val emittedEvent = AccountBlockedEvent(
                handle = -1L,
                type = AccountBlockedType.TOS_COPYRIGHT,
                text = "original"
            )
            accountBlockedFlow.emit(emittedEvent)
            advanceUntilIdle()

            val event =
                (underTest.state.value.accountBlockedEvent as? StateEventWithContentTriggered)?.content
            assertThat(event).isNotNull()
            assertThat(event?.type).isEqualTo(AccountBlockedType.TOS_COPYRIGHT)
            assertThat(event?.text).isEqualTo(mappedText)
            verify(accountBlockedTypeStringMapper).invoke(any())
        }

    // region Google Sign-In tests

    private val googleResult = GoogleSignInResult(
        email = "google.user@example.com",
        sub = "1234567890",
        firstName = "Google",
        lastName = "User",
    )

    @Test
    fun `test that onGoogleSignIn logs in via existing flow when account exists`() = runTest {
        whenever(decodeGoogleIdTokenUseCase("fake.jwt.token")).thenReturn(googleResult)
        whenever(loginUseCase(eq(googleResult.email), eq(googleResult.sub), any()))
            .thenReturn(flowOf(LoginStatus.LoginSucceed))

        underTest.onGoogleSignIn("fake.jwt.token")
        advanceUntilIdle()

        verify(loginUseCase).invoke(eq(googleResult.email), eq(googleResult.sub), any())
        verifyNoInteractions(createAccountUseCase)
    }

    @Test
    fun `test that onGoogleSignIn navigates to ConfirmEmail after auto-creating a new account`() =
        runTest {
            val ephemeralCredentials = EphemeralCredentials(
                email = googleResult.email,
                password = googleResult.sub,
                session = "session",
                firstName = googleResult.firstName,
                lastName = googleResult.lastName,
            )
            whenever(decodeGoogleIdTokenUseCase("fake.jwt.token")).thenReturn(googleResult)
            whenever(loginUseCase(eq(googleResult.email), eq(googleResult.sub), any()))
                .thenReturn(flow { throw LoginWrongEmailOrPassword() })
            whenever(
                createAccountUseCase(
                    email = googleResult.email,
                    password = googleResult.sub,
                    firstName = googleResult.firstName.orEmpty(),
                    lastName = googleResult.lastName.orEmpty(),
                )
            ).thenReturn(ephemeralCredentials)

            underTest.onGoogleSignIn("fake.jwt.token")
            advanceUntilIdle()

            verify(loginUseCase, times(1)).invoke(
                eq(googleResult.email),
                eq(googleResult.sub),
                any()
            )
            verify(createAccountUseCase).invoke(
                email = googleResult.email,
                password = googleResult.sub,
                firstName = googleResult.firstName.orEmpty(),
                lastName = googleResult.lastName.orEmpty(),
            )
            verify(ephemeralCredentialManager).setEphemeralCredential(ephemeralCredentials)
            underTest.state.test {
                val state = awaitItem()
                val event = state.isPendingToShowFragment as? StateEventWithContentTriggered
                assertThat(event?.content).isEqualTo(LoginScreen.ConfirmEmail)
            }
        }

    @Test
    fun `test that onGoogleSignIn shows email-exists snackbar when account already registered with different password`() =
        runTest {
            whenever(decodeGoogleIdTokenUseCase("fake.jwt.token")).thenReturn(googleResult)
            whenever(loginUseCase(eq(googleResult.email), eq(googleResult.sub), any()))
                .thenReturn(flow { throw LoginWrongEmailOrPassword() })
            whenever(
                createAccountUseCase(
                    email = googleResult.email,
                    password = googleResult.sub,
                    firstName = googleResult.firstName.orEmpty(),
                    lastName = googleResult.lastName.orEmpty(),
                )
            ).thenAnswer { throw CreateAccountException.AccountAlreadyExists }

            underTest.onGoogleSignIn("fake.jwt.token")
            advanceUntilIdle()

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.snackbarMessage)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat(state.isLoginInProgress).isFalse()
                assertThat(state.isLoginRequired).isTrue()
            }
        }

    @Test
    fun `test that onGoogleSignIn shows snackbar when JWT decode fails`() = runTest {
        whenever(decodeGoogleIdTokenUseCase("fake.jwt.token"))
            .thenThrow(RuntimeException("malformed"))

        underTest.onGoogleSignIn("fake.jwt.token")
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.snackbarMessage)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
        }
        verifyNoInteractions(loginUseCase)
    }

    @Test
    fun `test that onGoogleSignIn navigates to ConfirmEmail when account exists but not validated`() =
        runTest {
            whenever(decodeGoogleIdTokenUseCase("fake.jwt.token")).thenReturn(googleResult)
            whenever(loginUseCase(eq(googleResult.email), eq(googleResult.sub), any()))
                .thenReturn(flow { throw LoginRequireValidation() })

            underTest.onGoogleSignIn("fake.jwt.token")
            advanceUntilIdle()

            verifyNoInteractions(createAccountUseCase)
            verify(ephemeralCredentialManager).setEphemeralCredential(
                EphemeralCredentials(
                    email = googleResult.email,
                    password = googleResult.sub,
                    session = null,
                    firstName = googleResult.firstName,
                    lastName = googleResult.lastName,
                )
            )
            underTest.state.test {
                val state = awaitItem()
                val event = state.isPendingToShowFragment as? StateEventWithContentTriggered
                assertThat(event?.content).isEqualTo(LoginScreen.ConfirmEmail)
            }
        }

    @Test
    fun `test that onGoogleSignIn switches to 2FA state when MEGA login requires it`() = runTest {
        whenever(decodeGoogleIdTokenUseCase("fake.jwt.token")).thenReturn(googleResult)
        whenever(loginUseCase(eq(googleResult.email), eq(googleResult.sub), any()))
            .thenReturn(flow { throw LoginMultiFactorAuthRequired() })

        underTest.onGoogleSignIn("fake.jwt.token")
        advanceUntilIdle()

        verifyNoInteractions(createAccountUseCase)
        verify(savedStateHandle)["is_2fa_required"] = true
        verify(savedStateHandle)["pending_2fa_email"] = googleResult.email
        verify(savedStateHandle)["pending_2fa_password"] = googleResult.sub
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.is2FARequired).isTrue()
            assertThat(state.isLoginRequired).isFalse()
            assertThat(state.isLoginInProgress).isFalse()
        }
    }

    @Test
    fun `test that onGoogleSignIn falls through to loginFailed for a generic LoginException`() =
        runTest {
            whenever(decodeGoogleIdTokenUseCase("fake.jwt.token")).thenReturn(googleResult)
            whenever(loginUseCase(eq(googleResult.email), eq(googleResult.sub), any()))
                .thenReturn(flow { throw LoginBlockedAccount() })

            underTest.onGoogleSignIn("fake.jwt.token")
            advanceUntilIdle()

            verifyNoInteractions(createAccountUseCase)
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.password).isNull()
                assertThat(state.accountSession?.email).isNull()
                assertThat(state.isLoginInProgress).isFalse()
                assertThat(state.isLoginRequired).isTrue()
                assertThat(state.loginException).isInstanceOf(LoginBlockedAccount::class.java)
            }
        }

    @Test
    fun `test that onGoogleSignInError triggers snackbar`() = runTest {
        underTest.onGoogleSignInError(RuntimeException("boom"))
        advanceUntilIdle()

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.snackbarMessage)
                .isInstanceOf(StateEventWithContentTriggered::class.java)
        }
    }

    // endregion

    companion object {
        private val scheduler = TestCoroutineScheduler()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher(scheduler))

        @JvmField
        @RegisterExtension
        val analyticsExtension = AnalyticsTestExtension()

        @JvmStatic
        fun provideInvalidCredentials() = listOf(
            Arguments.of(
                EphemeralCredentials(
                    email = null,
                    password = "password123",
                    session = "session",
                    firstName = "John",
                    lastName = "Doe"
                ),
                "email is null"
            ),
            Arguments.of(
                EphemeralCredentials(
                    email = "",
                    password = "password123",
                    session = "session",
                    firstName = "John",
                    lastName = "Doe"
                ),
                "email is empty"
            ),
            Arguments.of(
                EphemeralCredentials(
                    email = "test@example.com",
                    password = null,
                    session = "session",
                    firstName = "John",
                    lastName = "Doe"
                ),
                "password is null"
            ),
            Arguments.of(
                EphemeralCredentials(
                    email = "test@example.com",
                    password = "",
                    session = "session",
                    firstName = "John",
                    lastName = "Doe"
                ),
                "password is empty"
            )
        )
    }
}
