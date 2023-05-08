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
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasPreferencesUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.FastLoginUseCase
import mega.privacy.android.domain.usecase.login.FetchNodesUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.GetSessionUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutUseCase
import mega.privacy.android.domain.usecase.login.LoginUseCase
import mega.privacy.android.domain.usecase.login.LoginWith2FAUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
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
import org.mockito.kotlin.whenever

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
}