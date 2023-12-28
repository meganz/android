package test.mega.privacy.android.app.presentation.changepassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity.Companion.KEY_ACTION
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity.Companion.KEY_LINK_TO_RESET
import mega.privacy.android.app.presentation.changepassword.ChangePasswordViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.changepassword.PasswordStrength
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.ChangePasswordUseCase
import mega.privacy.android.domain.usecase.FetchMultiFactorAuthSettingUseCase
import mega.privacy.android.domain.usecase.GetPasswordStrengthUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.IsCurrentPasswordUseCase
import mega.privacy.android.domain.usecase.ResetPasswordUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChangePasswordViewModelTest {
    private lateinit var underTest: ChangePasswordViewModel
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val savedStateHandle = SavedStateHandle()
    private val changePasswordUseCase = mock<ChangePasswordUseCase>()
    private val getPasswordStrengthUseCase = mock<GetPasswordStrengthUseCase>()
    private val isCurrentPasswordUseCase = mock<IsCurrentPasswordUseCase>()
    private val resetPasswordUseCase = mock<ResetPasswordUseCase>()
    private val multiFactorAuthSetting = mock<FetchMultiFactorAuthSettingUseCase>()
    private val getRootNodeUseCase = mock<GetRootNodeUseCase>()
    private val logoutUseCase = mock<LogoutUseCase>()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    private fun initTestClass() {
        underTest = ChangePasswordViewModel(
            savedStateHandle = savedStateHandle,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            isCurrentPasswordUseCase = isCurrentPasswordUseCase,
            getPasswordStrengthUseCase = getPasswordStrengthUseCase,
            changePasswordUseCase = changePasswordUseCase,
            resetPasswordUseCase = resetPasswordUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            multiFactorAuthSetting = multiFactorAuthSetting,
            logoutUseCase = logoutUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorConnectivityUseCase,
            isCurrentPasswordUseCase,
            getPasswordStrengthUseCase,
            changePasswordUseCase,
            resetPasswordUseCase,
            getRootNodeUseCase,
            multiFactorAuthSetting,
            logoutUseCase,
        )
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that when user resets password successfully should return no error code`() = runTest {
        whenever(resetPasswordUseCase(any(), any(), any())).thenReturn(true)
        initTestClass()
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
            resetPasswordUseCase(
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
        initTestClass()
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
            initTestClass()
            underTest.onUserClickChangePassword("")

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isPromptedMultiFactorAuth).isTrue()
                assertThat(state.isPasswordChanged).isFalse()
            }
        }

    @Test
    fun `test that loading message should change when user click change password`() = runTest {
        initTestClass()
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
            whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnected))
            initTestClass()
            underTest.uiState.test {
                assertThat(awaitItem().isConnectedToNetwork).isEqualTo(isConnected)
            }
        }

    @Test
    fun `test that when onMultiFactorAuthShown called, should reset state to default`() = runTest {
        initTestClass()
        underTest.onMultiFactorAuthShown()

        underTest.uiState.test {
            assertThat(awaitItem().isPromptedMultiFactorAuth).isFalse()
        }
    }

    @Test
    fun `test that when onSnackBarShown called, should reset state to default`() = runTest {
        initTestClass()
        underTest.onSnackBarShown()

        underTest.uiState.test {
            assertThat(awaitItem().snackBarMessage).isNull()
        }
    }

    @Test
    fun `test that when onPasswordChanged called, should reset state to default`() = runTest {
        initTestClass()
        underTest.onPasswordChanged()

        underTest.uiState.test {
            assertThat(awaitItem().isPasswordChanged).isFalse()
        }
    }

    @Test
    fun `test that when onPasswordReset called, should reset state to default`() = runTest {
        initTestClass()
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
            whenever(isCurrentPasswordUseCase(fakePassword)).thenReturn(false)
            initTestClass()
            underTest.checkPasswordStrength(fakePassword)

            underTest.uiState.test {
                assertThat(awaitItem().passwordStrength).isEqualTo(PasswordStrength.VERY_WEAK)
            }
        }

    @Test
    fun `test that when checking password strength then passwordStrengthLevel state should be updated and isCurrentPassword should be should also be updated`() =
        runTest {
            val fakePassword = "password"
            whenever(getPasswordStrengthUseCase(fakePassword)).thenReturn(PasswordStrength.MEDIUM)
            whenever(isCurrentPasswordUseCase(fakePassword)).thenReturn(true)
            initTestClass()
            underTest.checkPasswordStrength(fakePassword)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.passwordStrength).isEqualTo(PasswordStrength.MEDIUM)
                assertThat(state.isCurrentPassword).isTrue()
            }
        }

    @Test
    fun `test that when checking password strength and password is blank should return invisible state`() =
        runTest {
            val fakePassword = ""
            whenever(getPasswordStrengthUseCase(fakePassword)).thenReturn(PasswordStrength.MEDIUM)
            whenever(isCurrentPasswordUseCase(fakePassword)).thenReturn(true)
            initTestClass()
            underTest.checkPasswordStrength(fakePassword)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.passwordStrength).isEqualTo(PasswordStrength.INVALID)
            }
        }

    @Test
    fun `test that when invalidate password is set to true when checking password strength should remove password error state`() =
        runTest {
            val fakePassword = "password"
            whenever(getPasswordStrengthUseCase(fakePassword)).thenReturn(PasswordStrength.MEDIUM)
            whenever(isCurrentPasswordUseCase(fakePassword)).thenReturn(false)
            initTestClass()
            underTest.checkPasswordStrength(fakePassword)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.passwordError).isNull()
            }
        }

    @Test
    fun `test that when action is reset password and master key or reset link null should update isShowAlertMessage ui state`() =
        runTest {
            savedStateHandle[KEY_LINK_TO_RESET] = null
            savedStateHandle[KEY_ACTION] = Constants.ACTION_RESET_PASS_FROM_LINK
            initTestClass()

            underTest.determineIfScreenIsResetPasswordMode()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isResetPasswordMode).isTrue()
                assertThat(state.isShowAlertMessage).isTrue()
            }
        }

    @Test
    fun `test that when action is reset password but link to reset is null should update isResetPasswordLinkValid ui state`() =
        runTest {
            savedStateHandle[KEY_LINK_TO_RESET] = null
            savedStateHandle[KEY_ACTION] = Constants.ACTION_RESET_PASS_FROM_LINK
            initTestClass()
            underTest.determineIfScreenIsResetPasswordMode()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isResetPasswordLinkValid).isFalse()
            }
        }

    @Test
    fun `test that when action is not reset password should not update reset password ui state`() =
        runTest {
            savedStateHandle[KEY_ACTION] = null
            initTestClass()
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
            initTestClass()
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
            initTestClass()
            underTest.determineIfScreenIsResetPasswordMode()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isResetPasswordLinkValid).isTrue()
            }
        }

    @Test
    fun `test that when onAlertMessageShown called, should reset state to default`() = runTest {
        initTestClass()
        underTest.onAlertMessageShown()

        underTest.uiState.test {
            assertThat(awaitItem().isShowAlertMessage).isFalse()
        }
    }

    @Test
    fun `test that when validate confirm password to default should reset confirm password error state to null`() =
        runTest {
            initTestClass()
            underTest.validateConfirmPasswordToDefault()

            underTest.uiState.test {
                assertThat(awaitItem().confirmPasswordError).isNull()
            }
        }

    @Test
    fun `test that when validate password returns an error should return an error message to the ui state`() =
        runTest {
            val fakePassword = ""
            initTestClass()
            underTest.validatePassword(fakePassword)

            underTest.uiState.test {
                assertThat(awaitItem().passwordError).isEqualTo(R.string.error_enter_password)
            }
        }

    @Test
    fun `test that when validate password on save and no errors detected should update successful validation to true`() =
        runTest {
            val fakePassword = "password"
            val fakeConfirmPassword = "password"
            whenever(getPasswordStrengthUseCase(fakePassword)).thenReturn(PasswordStrength.MEDIUM)
            whenever(isCurrentPasswordUseCase(fakePassword)).thenReturn(false)
            initTestClass()
            underTest.validateAllPasswordOnSave(fakePassword, fakeConfirmPassword)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isSaveValidationSuccessful).isTrue()
                assertThat(state.passwordError).isNull()
                assertThat(state.confirmPasswordError).isNull()
            }
        }

    @Test
    fun `test that when validate password on save and password is current password should update successful validation to false and return an error message`() =
        runTest {
            val fakePassword = "password"
            val fakeConfirmPassword = "password"
            whenever(getPasswordStrengthUseCase(fakePassword)).thenReturn(PasswordStrength.MEDIUM)
            whenever(isCurrentPasswordUseCase(fakePassword)).thenReturn(true)
            initTestClass()
            underTest.validateAllPasswordOnSave(fakePassword, fakeConfirmPassword)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isSaveValidationSuccessful).isFalse()
                assertThat(state.passwordError).isEqualTo(R.string.error_same_password)
                assertThat(state.confirmPasswordError).isNull()
            }
        }

    @Test
    fun `test that when validate password on save and password is very weak should update successful validation to false and return an error message`() =
        runTest {
            val fakePassword = "password"
            val fakeConfirmPassword = "password"
            whenever(getPasswordStrengthUseCase(fakePassword)).thenReturn(PasswordStrength.VERY_WEAK)
            whenever(isCurrentPasswordUseCase(fakePassword)).thenReturn(false)
            initTestClass()
            underTest.validateAllPasswordOnSave(fakePassword, fakeConfirmPassword)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isSaveValidationSuccessful).isFalse()
                assertThat(state.passwordError).isEqualTo(R.string.error_password)
                assertThat(state.confirmPasswordError).isNull()
            }
        }

    @Test
    fun `test that when validate password on save and confirm password is blank should update successful validation to false and return an error message`() =
        runTest {
            val fakePassword = "password"
            val fakeConfirmPassword = ""
            whenever(getPasswordStrengthUseCase(fakePassword)).thenReturn(PasswordStrength.STRONG)
            whenever(isCurrentPasswordUseCase(fakePassword)).thenReturn(false)
            initTestClass()
            underTest.validateAllPasswordOnSave(fakePassword, fakeConfirmPassword)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isSaveValidationSuccessful).isFalse()
                assertThat(state.passwordError).isNull()
                assertThat(state.confirmPasswordError).isEqualTo(R.string.error_enter_password)
            }
        }

    @Test
    fun `test that when validate password on save and confirm password is different from password should update successful validation to false and return an error message`() =
        runTest {
            val fakePassword = "password"
            val fakeConfirmPassword = "password1237123891"
            whenever(getPasswordStrengthUseCase(fakePassword)).thenReturn(PasswordStrength.STRONG)
            whenever(isCurrentPasswordUseCase(fakePassword)).thenReturn(false)
            initTestClass()
            underTest.validateAllPasswordOnSave(fakePassword, fakeConfirmPassword)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isSaveValidationSuccessful).isFalse()
                assertThat(state.passwordError).isNull()
                assertThat(state.confirmPasswordError).isEqualTo(R.string.error_passwords_dont_match)
            }
        }

    @Test
    fun `test that when onResetPasswordValidation called, should reset state to default`() =
        runTest {
            initTestClass()
            underTest.onResetPasswordValidation()

            underTest.uiState.test {
                assertThat(awaitItem().isSaveValidationSuccessful).isFalse()
            }
        }

    @Test
    fun `test that an exception from change password is not propagated`() = runTest {
        whenever(multiFactorAuthSetting()).thenReturn(false)
        whenever(changePasswordUseCase(any())).thenAnswer { throw MegaException(1, "It's broken") }
        initTestClass()
        with(underTest) {
            onUserClickChangePassword("")
            uiState.test {
                val state = awaitItem()
                assertEquals(state.snackBarMessage, R.string.general_text_error)
                assertNull(state.loadingMessage)
                assertFalse(state.isPasswordChanged)
            }
        }
    }

    private fun verifyChangePasswordUiState(
        isSuccessChangePassword: Boolean,
        expected: Boolean,
    ) = runTest {
        whenever(changePasswordUseCase(any())).thenReturn(isSuccessChangePassword)
        whenever(multiFactorAuthSetting()).thenReturn(false)
        initTestClass()
        underTest.onUserClickChangePassword("")

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isPromptedMultiFactorAuth).isFalse()
            assertThat(state.isPasswordChanged).isEqualTo(expected)
        }
    }
}