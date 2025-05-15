package mega.privacy.android.app.presentation.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.AnalyticsTestExtension
import mega.privacy.android.app.InstantExecutorExtension
import mega.privacy.android.app.R
import mega.privacy.android.app.middlelayer.installreferrer.InstallReferrerDetails
import mega.privacy.android.app.middlelayer.installreferrer.InstallReferrerHandler
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.model.RkLink
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.CheckRecoveryKeyUseCase
import mega.privacy.android.domain.usecase.account.ClearUserCredentialsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountBlockedUseCase
import mega.privacy.android.domain.usecase.account.MonitorLoggedOutFromAnotherLocationUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.ResendVerificationEmailUseCase
import mega.privacy.android.domain.usecase.account.ResumeCreateAccountUseCase
import mega.privacy.android.domain.usecase.account.SetLoggedOutFromAnotherLocationUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasPreferencesUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.environment.GetHistoricalProcessExitReasonsUseCase
import mega.privacy.android.domain.usecase.environment.IsFirstLaunchUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.ClearLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.login.FastLoginUseCase
import mega.privacy.android.domain.usecase.login.FetchNodesUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.GetLastRegisteredEmailUseCase
import mega.privacy.android.domain.usecase.login.GetSessionUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutUseCase
import mega.privacy.android.domain.usecase.login.LoginUseCase
import mega.privacy.android.domain.usecase.login.LoginWith2FAUseCase
import mega.privacy.android.domain.usecase.login.MonitorEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.domain.usecase.login.SaveEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.notifications.ShouldShowNotificationReminderUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.requeststatus.EnableRequestStatusMonitorUseCase
import mega.privacy.android.domain.usecase.requeststatus.MonitorRequestStatusProgressEventUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.OngoingTransfersExistUseCase
import mega.privacy.android.domain.usecase.transfers.paused.CheckIfTransfersShouldBePausedUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
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
    private val rootNodeExistsUseCase: RootNodeExistsUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase =
        mock { onBlocking { invoke(any()) }.thenReturn(false) }
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase = mock()
    private val getAccountCredentialsUseCase: GetAccountCredentialsUseCase = mock()
    private val getSessionUseCase: GetSessionUseCase = mock()
    private val hasPreferencesUseCase: HasPreferencesUseCase = mock()
    private val hasCameraSyncEnabledUseCase: HasCameraSyncEnabledUseCase = mock()
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase = mock()
    private val querySignupLinkUseCase: QuerySignupLinkUseCase = mock()
    private val cancelTransfersUseCase: CancelTransfersUseCase = mock()
    private val localLogoutUseCase: LocalLogoutUseCase = mock()
    private val loginUseCase: LoginUseCase = mock()
    private val loginWith2FAUseCase: LoginWith2FAUseCase = mock()
    private val fastLoginUseCase: FastLoginUseCase = mock()
    private val fetchNodesUseCase: FetchNodesUseCase = mock()
    private val ongoingTransfersExistUseCase: OngoingTransfersExistUseCase = mock()
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase = mock()
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase = mock()
    private val monitorEphemeralCredentialsUseCase: MonitorEphemeralCredentialsUseCase = mock()
    private val saveEphemeralCredentialsUseCase: SaveEphemeralCredentialsUseCase = mock()
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase = mock()
    private val monitorAccountBlockedUseCase = mock<MonitorAccountBlockedUseCase>()
    private val getTimelinePhotosUseCase = mock<GetTimelinePhotosUseCase>()
    private val establishCameraUploadsSyncHandlesUseCase =
        mock<EstablishCameraUploadsSyncHandlesUseCase>()
    private val getLastRegisteredEmailUseCase = mock<GetLastRegisteredEmailUseCase>()
    private val clearLastRegisteredEmailUseCase = mock<ClearLastRegisteredEmailUseCase>()
    private val installReferrerHandler = mock<InstallReferrerHandler>()
    private val clearUserCredentialsUseCase = mock<ClearUserCredentialsUseCase>()
    private val getHistoricalProcessExitReasonsUseCase =
        mock<GetHistoricalProcessExitReasonsUseCase>()
    private val enableRequestStatusMonitorUseCase = mock<EnableRequestStatusMonitorUseCase>()
    private val requestStatusProgressFakeFlow = MutableSharedFlow<Progress>()
    private val monitorRequestStatusProgressEventUseCase =
        mock<MonitorRequestStatusProgressEventUseCase>()
    private val checkIfTransfersShouldBePausedUseCase =
        mock<CheckIfTransfersShouldBePausedUseCase>()
    private val isFirstLaunchUseCase = mock<IsFirstLaunchUseCase>()
    private val getThemeMode = mock<GetThemeMode>()
    private val resendVerificationEmailUseCase = mock<ResendVerificationEmailUseCase>()
    private val resumeCreateAccountUseCase = mock<ResumeCreateAccountUseCase>()
    private val checkRecoveryKeyUseCase = mock<CheckRecoveryKeyUseCase>()
    private val monitorLoggedOutFromAnotherLocationUseCase: MonitorLoggedOutFromAnotherLocationUseCase =
        mock {
            onBlocking { invoke() }.thenReturn(flowOf(false))
        }
    private val setLoggedOutFromAnotherLocationUseCase: SetLoggedOutFromAnotherLocationUseCase =
        mock()
    private val savedStateHandle = mock<SavedStateHandle>()
    private val shouldShowNotificationReminderUseCase =
        mock<ShouldShowNotificationReminderUseCase>()

    @BeforeEach
    fun setUp() {
        runBlocking {
            stubCommon()
        }
        underTest = LoginViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            resetChatSettingsUseCase = resetChatSettingsUseCase,
            getAccountCredentialsUseCase = getAccountCredentialsUseCase,
            getSessionUseCase = getSessionUseCase,
            hasPreferencesUseCase = hasPreferencesUseCase,
            hasCameraSyncEnabledUseCase = hasCameraSyncEnabledUseCase,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            querySignupLinkUseCase = querySignupLinkUseCase,
            cancelTransfersUseCase = cancelTransfersUseCase,
            localLogoutUseCase = localLogoutUseCase,
            loginUseCase = loginUseCase,
            loginWith2FAUseCase = loginWith2FAUseCase,
            fastLoginUseCase = fastLoginUseCase,
            fetchNodesUseCase = fetchNodesUseCase,
            establishCameraUploadsSyncHandlesUseCase = establishCameraUploadsSyncHandlesUseCase,
            ongoingTransfersExistUseCase = ongoingTransfersExistUseCase,
            monitorFetchNodesFinishUseCase = monitorFetchNodesFinishUseCase,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
            monitorEphemeralCredentialsUseCase = monitorEphemeralCredentialsUseCase,
            saveEphemeralCredentialsUseCase = saveEphemeralCredentialsUseCase,
            clearEphemeralCredentialsUseCase = clearEphemeralCredentialsUseCase,
            monitorAccountBlockedUseCase = monitorAccountBlockedUseCase,
            getTimelinePhotosUseCase = getTimelinePhotosUseCase,
            loginMutex = mock(),
            getLastRegisteredEmailUseCase = getLastRegisteredEmailUseCase,
            clearLastRegisteredEmailUseCase = clearLastRegisteredEmailUseCase,
            installReferrerHandler = installReferrerHandler,
            clearUserCredentialsUseCase = clearUserCredentialsUseCase,
            getHistoricalProcessExitReasonsUseCase = getHistoricalProcessExitReasonsUseCase,
            enableRequestStatusMonitorUseCase = enableRequestStatusMonitorUseCase,
            monitorRequestStatusProgressEventUseCase = monitorRequestStatusProgressEventUseCase,
            checkIfTransfersShouldBePausedUseCase = checkIfTransfersShouldBePausedUseCase,
            isFirstLaunchUseCase = isFirstLaunchUseCase,
            getThemeMode = getThemeMode,
            resendVerificationEmailUseCase = resendVerificationEmailUseCase,
            resumeCreateAccountUseCase = resumeCreateAccountUseCase,
            checkRecoveryKeyUseCase = checkRecoveryKeyUseCase,
            monitorLoggedOutFromAnotherLocationUseCase = monitorLoggedOutFromAnotherLocationUseCase,
            setLoggedOutFromAnotherLocationUseCase = setLoggedOutFromAnotherLocationUseCase,
            savedStateHandle = savedStateHandle,
            shouldShowNotificationReminderUseCase = shouldShowNotificationReminderUseCase,
        )
    }

    private suspend fun stubCommon() {
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(
            requestStatusProgressFakeFlow
        )
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(
            true
        )
        whenever(getSessionUseCase()).thenReturn(null)
        whenever(monitorAccountBlockedUseCase()).thenReturn(emptyFlow())
        whenever(rootNodeExistsUseCase()).thenReturn(false)
        whenever(hasPreferencesUseCase()).thenReturn(false)
        whenever(hasCameraSyncEnabledUseCase()).thenReturn(false)
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
        whenever(isFirstLaunchUseCase()).thenReturn(false)
        whenever(monitorFetchNodesFinishUseCase()).thenReturn(emptyFlow())
        whenever(getThemeMode()).thenReturn(flowOf(ThemeMode.System))
    }

    @AfterEach
    fun resetMocks() {
        reset(
            monitorRequestStatusProgressEventUseCase,
            checkIfTransfersShouldBePausedUseCase,
            isFirstLaunchUseCase,
            resendVerificationEmailUseCase,
            checkRecoveryKeyUseCase,
            setLoggedOutFromAnotherLocationUseCase,
            savedStateHandle,
            shouldShowNotificationReminderUseCase
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
                assertThat(accountConfirmationLink).isNull()
                assertThat(fetchNodesUpdate).isNull()
                assertThat(isFirstTime).isFalse()
                assertThat(isAlreadyLoggedIn).isTrue()
                assertThat(pressedBackWhileLogin).isFalse()
                assertThat(is2FAEnabled).isFalse()
                assertThat(is2FARequired).isFalse()
                assertThat(multiFactorAuthState).isNull()
                assertThat(isAccountConfirmed).isFalse()
                assertThat(rootNodesExists).isFalse()
                assertThat(temporalEmail).isNull()
                assertThat(temporalPassword).isNull()
                assertThat(hasPreferences).isFalse()
                assertThat(hasCUSetting).isFalse()
                assertThat(isCUSettingEnabled).isFalse()
                assertThat(isLocalLogoutInProgress).isFalse()
                assertThat(isLoginRequired).isFalse()
                assertThat(isLoginInProgress).isFalse()
                assertThat(loginException).isNull()
                assertThat(ongoingTransfersExist).isNull()
                assertThat(isPendingToFinishActivity).isFalse()
                assertThat(isPendingToShowFragment).isEqualTo(LoginFragmentType.Tour)
                assertThat(isCheckingSignupLink).isFalse()
                assertThat(snackbarMessage).isInstanceOf(consumed().javaClass)
                assertThat(isFirstTimeLaunch).isFalse()
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
    fun `test that an exception from local logout is not propagated`() = runTest {
        whenever(localLogoutUseCase(any()))
            .thenAnswer { throw MegaException(1, "It's broken") }

        with(underTest) {
            state.map { it.isLocalLogoutInProgress }.distinctUntilChanged().test {
                assertFalse(awaitItem())
                stopLogin()
                assertTrue(awaitItem())
                assertFalse(awaitItem())
            }
        }
    }

    @Test
    fun `test that call correct functions when calling setTemporalEmail`() = runTest {
        val ephemeral = mock<EphemeralCredentials>()
        val newEphemeral = mock<EphemeralCredentials>()
        val temporalEmail = "email"
        whenever(monitorEphemeralCredentialsUseCase()).thenReturn(flowOf(ephemeral))
        whenever(ephemeral.copy(email = temporalEmail)).thenReturn(newEphemeral)
        underTest.setTemporalEmail(temporalEmail)
        advanceUntilIdle()
        verify(clearEphemeralCredentialsUseCase).invoke()
        verify(saveEphemeralCredentialsUseCase).invoke(newEphemeral)
    }

    @Test
    fun `test that call correct functions when calling saveEphemeral`() = runTest {
        val ephemeral = mock<EphemeralCredentials>()
        underTest.saveEphemeral(ephemeral)
        advanceUntilIdle()
        verify(clearEphemeralCredentialsUseCase).invoke()
        verify(saveEphemeralCredentialsUseCase).invoke(ephemeral)
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
            verify(clearLastRegisteredEmailUseCase).invoke()
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
            verifyNoInteractions(installReferrerHandler, clearLastRegisteredEmailUseCase)
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
    fun `test that requestStatusProgress is updated when event is received`() = runTest {
        val newProgress = 0.5f
        requestStatusProgressFakeFlow.emit(Progress(newProgress))
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.requestStatusProgress?.floatValue).isEqualTo(newProgress)
        }
    }

    @Test
    fun `test that requestStatusProgress is set to null when exception is thrown`() = runTest {
        whenever(monitorRequestStatusProgressEventUseCase()).thenReturn(
            flow {
                throw MegaException(1, "error")
            }
        )
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.requestStatusProgress).isNull()
        }
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
    fun `test that loginFailed with LoginLoggedOutFromOtherLocation then fetchNodesUpdate is null`() =
        runTest {
            whenever(fastLoginUseCase(any(), any(), any())).thenThrow(
                LoginLoggedOutFromOtherLocation()
            )
            whenever(getAccountCredentialsUseCase()).thenReturn(
                UserCredentials(
                    email = "email",
                    session = "session",
                    firstName = "firstName",
                    lastName = "lastName",
                    myHandle = "myHandle"
                )
            )
            underTest.fastLogin(false)
            advanceUntilIdle()
            underTest.state.test {
                val item = awaitItem()
                assertThat(item.fetchNodesUpdate).isNull()
                assertThat(item.isLoginInProgress).isFalse()
                assertThat(item.loginException).isInstanceOf(LoginLoggedOutFromOtherLocation::class.java)
            }
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
    fun `test that set handled logged out from another location invokes correctly`() = runTest {
        underTest.setHandledLoggedOutFromAnotherLocation()
        advanceUntilIdle()
        verify(setLoggedOutFromAnotherLocationUseCase).invoke(false)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that should show notification reminder returns correct value`(value: Boolean) =
        runTest {
            whenever(shouldShowNotificationReminderUseCase()) doReturn value

            assertThat(underTest.shouldShowNotificationPermission()).isEqualTo(value)
        }

    @Test
    fun `test that should show notification reminder returns false when use case throws exception`() =
        runTest {
            whenever(shouldShowNotificationReminderUseCase()).thenThrow(RuntimeException())

            assertThat(underTest.shouldShowNotificationPermission()).isFalse()
        }

    companion object {
        private val scheduler = TestCoroutineScheduler()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher(scheduler))

        @JvmField
        @RegisterExtension
        val analyticsExtension = AnalyticsTestExtension()
    }
}
