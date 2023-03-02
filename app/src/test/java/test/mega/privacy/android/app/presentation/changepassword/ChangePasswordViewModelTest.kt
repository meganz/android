package test.mega.privacy.android.app.presentation.changepassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.domain.usecase.GetRootFolder
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity.Companion.KEY_ACTION
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity.Companion.KEY_LINK_TO_RESET
import mega.privacy.android.app.presentation.changepassword.ChangePasswordViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.ChangePassword
import mega.privacy.android.domain.usecase.FetchMultiFactorAuthSetting
import mega.privacy.android.domain.usecase.GetPasswordStrength
import mega.privacy.android.domain.usecase.IsCurrentPassword
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.ResetPassword
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChangePasswordViewModelTest {
    private lateinit var underTest: ChangePasswordViewModel
    private val testFlow = MutableStateFlow(false)
    private val monitorConnectivity = mock<MonitorConnectivity> {
        onBlocking { invoke() }.thenReturn(testFlow)
    }
    private val savedStateHandle = SavedStateHandle()
    private val changePassword = mock<ChangePassword>()
    private val getPasswordStrength = mock<GetPasswordStrength>()
    private val isCurrentPassword = mock<IsCurrentPassword>()
    private val resetPassword = mock<ResetPassword>()
    private val multiFactorAuthSetting = mock<FetchMultiFactorAuthSetting>()
    private val getRootFolder = mock<GetRootFolder>()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        underTest = ChangePasswordViewModel(
            savedStateHandle = savedStateHandle,
            monitorConnectivity = monitorConnectivity,
            isCurrentPassword = isCurrentPassword,
            getPasswordStrength = getPasswordStrength,
            changePassword = changePassword,
            resetPassword = resetPassword,
            getRootFolder = getRootFolder,
            multiFactorAuthSetting = multiFactorAuthSetting
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that when user resets password successfully should return no error code`() = runTest {
        whenever(resetPassword(any(), any(), any())).thenReturn(true)

        underTest.onExecuteResetPassword("")

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isPasswordReset).isTrue()
            assertThat(state.errorCode).isEqualTo(null)
        }
    }

    @Test
    fun `test that when user resets password failed should return an error code`() = runTest {
        val fakeErrorCode = Random.nextInt()
        val fakePassword = "Password"
        val fakeLink = "Link"
        val fakeMasterKey = "MasterKey"

        savedStateHandle[KEY_LINK_TO_RESET] = fakeLink
        savedStateHandle[IntentConstants.EXTRA_MASTER_KEY] = fakeMasterKey

        whenever(
            resetPassword(
                link = fakeLink,
                newPassword = fakePassword,
                masterKey = fakeMasterKey
            )
        ).thenAnswer {
            throw MegaException(
                errorCode = fakeErrorCode,
                errorString = "error"
            )
        }

        underTest.onExecuteResetPassword(fakePassword)

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isPasswordReset).isTrue()
            assertThat(state.errorCode).isEqualTo(fakeErrorCode)
        }
    }

    @Test
    fun `test that when multi factor auth enabled ui state should be true and isPasswordChanged should be false`() =
        runTest {
            whenever(multiFactorAuthSetting()).thenReturn(true)

            underTest.onUserClickChangePassword("")

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isPromptedMultiFactorAuth).isTrue()
                assertThat(state.isPasswordChanged).isFalse()
            }
        }

    @Test
    fun `test that loading message should change when user click change password`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().loadingMessage).isNull()
            underTest.onUserClickChangePassword("")
            assertThat(awaitItem().loadingMessage).isEqualTo(R.string.my_account_changing_password)
        }
    }

    @Test
    fun `test that when multi factor auth disabled and change password is successful isPasswordChanged should be true`() {
        verifyChangePasswordUiState(isSuccessChangePassword = true, expected = true)
    }

    @Test
    fun `test that when multi factor auth disabled and change password fails isPasswordChanged should be false`() {
        verifyChangePasswordUiState(isSuccessChangePassword = false, expected = false)
    }

    @Test
    fun `test that network connection status should return correct value when isConnectedToNetwork called`() =
        runTest {
            val isConnected = Random.nextBoolean()
            testFlow.emit(isConnected)

            underTest.uiState.test {
                assertThat(awaitItem().isConnectedToNetwork).isEqualTo(isConnected)
            }
        }

    @Test
    fun `test that when onMultiFactorAuthShown called, should reset state to default`() = runTest {
        underTest.onMultiFactorAuthShown()

        underTest.uiState.test {
            assertThat(awaitItem().isPromptedMultiFactorAuth).isFalse()
        }
    }

    @Test
    fun `test that when onSnackBarShown called, should reset state to default`() = runTest {
        underTest.onSnackBarShown()

        underTest.uiState.test {
            assertThat(awaitItem().snackBarMessage).isNull()
        }
    }

    @Test
    fun `test that when onPasswordChanged called, should reset state to default`() = runTest {
        underTest.onPasswordChanged()

        underTest.uiState.test {
            assertThat(awaitItem().isPasswordChanged).isFalse()
        }
    }

    @Test
    fun `test that when onPasswordReset called, should reset state to default`() = runTest {
        underTest.onPasswordReset()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isPasswordReset).isFalse()
            assertThat(state.errorCode).isNull()
        }
    }

    @Test
    fun `test that when password character length is less than 4 then return very weak password strength`() =
        runTest {
            val fakePassword = "pas"
            whenever(isCurrentPassword(fakePassword)).thenReturn(false)

            underTest.checkPasswordStrength(fakePassword)

            underTest.uiState.test {
                assertThat(awaitItem().passwordStrength).isEqualTo(PasswordStrength.VERY_WEAK)
            }
        }

    @Test
    fun `test that when checking password strength then passwordStrengthLevel state should be updated and isCurrentPassword should be should also be updated`() =
        runTest {
            val fakePassword = "password"
            whenever(getPasswordStrength(fakePassword)).thenReturn(PasswordStrength.MEDIUM)
            whenever(isCurrentPassword(fakePassword)).thenReturn(true)

            underTest.checkPasswordStrength(fakePassword)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.passwordStrength).isEqualTo(PasswordStrength.MEDIUM)
                assertThat(state.isCurrentPassword).isTrue()
            }
        }

    @Test
    fun `test that when action is reset password and master key or reset link null should update isShowAlertMessage ui state`() =
        runTest {
            val fakeLink = "Link"

            savedStateHandle[KEY_LINK_TO_RESET] = fakeLink
            savedStateHandle[KEY_ACTION] = Constants.ACTION_RESET_PASS_FROM_LINK

            underTest.determineIfScreenIsResetPasswordMode()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isResetPasswordMode).isTrue()
                assertThat(state.isShowAlertMessage).isTrue()
            }
        }

    @Test
    fun `test that when action is reset password but link to reset is null should update isResetPasswordLinkInvalid ui state`() =
        runTest {
            savedStateHandle[KEY_ACTION] = Constants.ACTION_RESET_PASS_FROM_LINK

            underTest.determineIfScreenIsResetPasswordMode()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isResetPasswordMode).isTrue()
                assertThat(state.isResetPasswordLinkValid).isFalse()
            }
        }

    @Test
    fun `test that when action is not reset password should not update reset password ui state`() =
        runTest {
            savedStateHandle[KEY_ACTION] = null

            underTest.determineIfScreenIsResetPasswordMode()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isResetPasswordMode).isFalse()
                assertThat(state.isResetPasswordLinkValid).isTrue()
                assertThat(state.isShowAlertMessage).isFalse()
            }
        }

    @Test
    fun `test that when action is reset password from parking account should update reset password ui state`() =
        runTest {
            savedStateHandle[KEY_ACTION] = Constants.ACTION_RESET_PASS_FROM_PARK_ACCOUNT

            underTest.determineIfScreenIsResetPasswordMode()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isResetPasswordMode).isTrue()
            }
        }

    @Test
    fun `test that reset link should return valid value when not null`() =
        runTest {
            val fakeLink = "Link"
            savedStateHandle[KEY_ACTION] = Constants.ACTION_RESET_PASS_FROM_PARK_ACCOUNT
            savedStateHandle[KEY_LINK_TO_RESET] = fakeLink

            underTest.determineIfScreenIsResetPasswordMode()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isResetPasswordLinkValid).isTrue()
            }
        }

    @Test
    fun `test that when onAlertMessageShown called, should reset state to default`() = runTest {
        underTest.onAlertMessageShown()

        underTest.uiState.test {
            assertThat(awaitItem().isShowAlertMessage).isFalse()
        }
    }

    private fun verifyChangePasswordUiState(
        isSuccessChangePassword: Boolean,
        expected: Boolean,
    ) = runTest {
        whenever(changePassword(any())).thenReturn(isSuccessChangePassword)
        whenever(multiFactorAuthSetting()).thenReturn(false)

        underTest.onUserClickChangePassword("")

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isPromptedMultiFactorAuth).isFalse()
            assertThat(state.isPasswordChanged).isEqualTo(expected)
        }
    }
}