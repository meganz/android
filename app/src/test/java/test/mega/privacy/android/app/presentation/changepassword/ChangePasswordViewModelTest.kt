package test.mega.privacy.android.app.presentation.changepassword

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.changepassword.ChangePasswordViewModel
import mega.privacy.android.app.presentation.changepassword.model.ActionResult
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChangePasswordViewModelTest {
    private lateinit var underTest: ChangePasswordViewModel
    private val testFlow = MutableStateFlow(false)
    private val monitorConnectivity = mock<MonitorConnectivity> {
        onBlocking { invoke() }.thenReturn(testFlow)
    }
    private val changePassword = mock<ChangePassword>()
    private val getPasswordStrength = mock<GetPasswordStrength>()
    private val isCurrentPassword = mock<IsCurrentPassword>()
    private val resetPassword = mock<ResetPassword>()
    private val multiFactorAuthSetting = mock<FetchMultiFactorAuthSetting>()

    @Before
    fun setup() {
        underTest = ChangePasswordViewModel(
            monitorConnectivity = monitorConnectivity,
            isCurrentPassword = isCurrentPassword,
            getPasswordStrength = getPasswordStrength,
            changePassword = changePassword,
            resetPassword = resetPassword,
            multiFactorAuthSetting = multiFactorAuthSetting
        )
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that isConnected true when MonitorConnectivity emit true`() = runTest {
        testFlow.emit(true)
        assertTrue(underTest.isConnected)
    }

    @Test
    fun `test that isConnected false when MonitorConnectivity emit false`() = runTest {
        testFlow.emit(false)
        assertFalse(underTest.isConnected)
    }

    @Test
    fun `test that when user resets password successfully isResetPassword should be SUCCESS`() {
        verifyUserResetsPassword(true, ActionResult.SUCCESS)
    }

    @Test
    fun `test that when user resets password failed isResetPassword should be FAILED`() {
        verifyUserResetsPassword(false, ActionResult.FAILED)
    }

    @Test
    fun `test that when multi factor auth enabled ui state should be true and change password should be default`() =
        runTest {
            whenever(multiFactorAuthSetting()).thenReturn(true)

            underTest.onUserClickChangePassword("")

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isMultiFactorAuthEnabled).isTrue()
                assertThat(state.isChangePassword).isEqualTo(ActionResult.DEFAULT)
            }
        }

    @Test
    fun `test that when multi factor auth disabled and change password is successful isChangePassword should be SUCCESS`() {
        verifyChangePasswordUiState(true, ActionResult.SUCCESS)
    }

    @Test
    fun `test that when multi factor auth disabled and change password fails isChangePassword should be FAILED`() {
        verifyChangePasswordUiState(false, ActionResult.FAILED)
    }

    private fun verifyChangePasswordUiState(
        isSuccessChangePassword: Boolean,
        expected: ActionResult,
    ) = runTest {
        whenever(changePassword(any())).thenReturn(isSuccessChangePassword)
        whenever(multiFactorAuthSetting()).thenReturn(false)

        underTest.onUserClickChangePassword("")

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isMultiFactorAuthEnabled).isFalse()
            assertThat(state.isChangePassword).isEqualTo(expected)
        }
    }

    private fun verifyUserResetsPassword(isSuccessResetPassword: Boolean, expected: ActionResult) =
        runTest {
            whenever(resetPassword(any(), any(), any())).thenReturn(isSuccessResetPassword)

            underTest.onConfirmResetPassword("", "", "")

            underTest.uiState.test {
                assertThat(awaitItem().isResetPassword).isEqualTo(expected)
            }
        }
}