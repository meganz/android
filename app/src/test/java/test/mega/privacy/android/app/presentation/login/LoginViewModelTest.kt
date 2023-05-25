package test.mega.privacy.android.app.presentation.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.logging.LegacyLoggingSettings
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.model.LoginError
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasPreferencesUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.ClearEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.FastLoginUseCase
import mega.privacy.android.domain.usecase.login.FetchNodesUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.GetSessionUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutUseCase
import mega.privacy.android.domain.usecase.login.LoginUseCase
import mega.privacy.android.domain.usecase.login.LoginWith2FAUseCase
import mega.privacy.android.domain.usecase.login.MonitorEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.SaveEphemeralCredentialsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import mega.privacy.android.domain.usecase.transfer.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.OngoingTransfersExistUseCase
import mega.privacy.android.domain.usecase.workers.ScheduleCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadUseCase
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
internal class LoginViewModelTest {

    private lateinit var underTest: LoginViewModel

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val scheduler = TestCoroutineScheduler()

    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val rootNodeExistsUseCase: RootNodeExistsUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase =
        mock { onBlocking { invoke(any()) }.thenReturn(false) }
    private val loggingSettings: LegacyLoggingSettings = mock()
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase = mock()
    private val saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase = mock()
    private val getAccountCredentialsUseCase: GetAccountCredentialsUseCase = mock()
    private val getSessionUseCase: GetSessionUseCase = mock()
    private val hasPreferencesUseCase: HasPreferencesUseCase = mock()
    private val hasCameraSyncEnabledUseCase: HasCameraSyncEnabledUseCase = mock()
    private val isCameraSyncEnabledUseCase: IsCameraSyncEnabledUseCase = mock()
    private val querySignupLinkUseCase: QuerySignupLinkUseCase = mock()
    private val cancelTransfersUseCase: CancelTransfersUseCase = mock()
    private val localLogoutUseCase: LocalLogoutUseCase = mock()
    private val loginUseCase: LoginUseCase = mock()
    private val loginWith2FAUseCase: LoginWith2FAUseCase = mock()
    private val fastLoginUseCase: FastLoginUseCase = mock()
    private val fetchNodesUseCase: FetchNodesUseCase = mock()
    private val ongoingTransfersExistUseCase: OngoingTransfersExistUseCase = mock()
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase = mock()
    private val scheduleCameraUploadUseCase: ScheduleCameraUploadUseCase = mock()
    private val stopCameraUploadUseCase: StopCameraUploadUseCase = mock()
    private val monitorEphemeralCredentialsUseCase: MonitorEphemeralCredentialsUseCase = mock()
    private val saveEphemeralCredentialsUseCase: SaveEphemeralCredentialsUseCase = mock()
    private val clearEphemeralCredentialsUseCase: ClearEphemeralCredentialsUseCase = mock()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))

        underTest = LoginViewModel(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            loggingSettings = loggingSettings,
            resetChatSettingsUseCase = resetChatSettingsUseCase,
            saveAccountCredentialsUseCase = saveAccountCredentialsUseCase,
            getAccountCredentialsUseCase = getAccountCredentialsUseCase,
            getSessionUseCase = getSessionUseCase,
            hasPreferencesUseCase = hasPreferencesUseCase,
            hasCameraSyncEnabledUseCase = hasCameraSyncEnabledUseCase,
            isCameraSyncEnabledUseCase = isCameraSyncEnabledUseCase,
            querySignupLinkUseCase = querySignupLinkUseCase,
            cancelTransfersUseCase = cancelTransfersUseCase,
            localLogoutUseCase = localLogoutUseCase,
            loginUseCase = loginUseCase,
            loginWith2FAUseCase = loginWith2FAUseCase,
            fastLoginUseCase = fastLoginUseCase,
            fetchNodesUseCase = fetchNodesUseCase,
            ongoingTransfersExistUseCase = ongoingTransfersExistUseCase,
            monitorFetchNodesFinishUseCase = monitorFetchNodesFinishUseCase,
            scheduleCameraUploadUseCase = scheduleCameraUploadUseCase,
            stopCameraUploadUseCase = stopCameraUploadUseCase,
            monitorEphemeralCredentialsUseCase = monitorEphemeralCredentialsUseCase,
            saveEphemeralCredentialsUseCase = saveEphemeralCredentialsUseCase,
            clearEphemeralCredentialsUseCase = clearEphemeralCredentialsUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
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
                assertThat(isPendingToShowFragment).isNull()
                assertThat(enabledFlags).isEmpty()
                assertThat(isCheckingSignupLink).isFalse()
                assertThat(snackbarMessage).isInstanceOf(consumed<Int>().javaClass)
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
            whenever(monitorConnectivityUseCase()).thenReturn(MutableStateFlow(false))

            with(underTest) {
                state.map { it.snackbarMessage }.distinctUntilChanged()
                    .test {
                        assertThat(awaitItem()).isInstanceOf(consumed<Int>().javaClass)
                        onEmailChanged("test@test.com")
                        assertThat(awaitItem()).isInstanceOf(consumed<Int>().javaClass)
                        onPasswordChanged("Password")
                        assertThat(awaitItem()).isInstanceOf(consumed<Int>().javaClass)
                        onLoginClicked(false)
                        assertThat(awaitItem()).isInstanceOf(triggered(R.string.error_server_connection_problem).javaClass)
                    }
            }
        }

    @Test
    fun `test that performLogin is invoked when onLoginClick and there are no errors`() = runTest {
        whenever(ongoingTransfersExistUseCase()).thenReturn(false)
        whenever(monitorConnectivityUseCase()).thenReturn(MutableStateFlow(true))

        with(underTest) {
            state.test {
                onEmailChanged("test@test.com")
                onPasswordChanged("Password")
                onLoginClicked(false)
                advanceUntilIdle()
                assertThat(awaitItem().emailError).isNull()
                assertThat(awaitItem().passwordError).isNull()
                assertThat(awaitItem().ongoingTransfersExist).isNull()
                assertThat(awaitItem().snackbarMessage).isInstanceOf(consumed<Int>().javaClass)
            }
        }
    }

    @Test
    fun `test that an exception from local logout is not propagated`() = runTest {
        whenever(localLogoutUseCase(any(), any()))
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
}