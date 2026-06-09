package mega.privacy.android.app.presentation.verifytwofactor

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.changepassword.ChangePasswordActivity
import mega.privacy.android.app.presentation.verifytwofactor.model.PasswordChangedAction
import mega.privacy.android.app.presentation.verifytwofactor.model.VerifyTwoFactorResult
import mega.privacy.android.app.utils.Constants.CANCEL_ACCOUNT_2FA
import mega.privacy.android.app.utils.Constants.CHANGE_MAIL_2FA
import mega.privacy.android.app.utils.Constants.CHANGE_PASSWORD_2FA
import mega.privacy.android.app.utils.Constants.DISABLE_2FA
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.exception.WrongMultiFactorAuthPinException
import mega.privacy.android.domain.usecase.account.ChangePasswordWith2FAUseCase
import mega.privacy.android.domain.usecase.account.DisableMultiFactorAuthUseCase
import mega.privacy.android.domain.usecase.account.IsMultiFactorAuthEnabledUseCase
import mega.privacy.android.domain.usecase.account.RequestChangeEmailWith2FAUseCase
import mega.privacy.android.domain.usecase.account.RequestDeleteAccountLinkWith2FAUseCase
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import nz.mega.sdk.MegaError
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerifyTwoFactorViewModelTest {

    private lateinit var underTest: VerifyTwoFactorViewModel

    private val isMultiFactorAuthEnabledUseCase = mock<IsMultiFactorAuthEnabledUseCase>()
    private val requestDeleteAccountLinkWith2FAUseCase =
        mock<RequestDeleteAccountLinkWith2FAUseCase>()
    private val requestChangeEmailWith2FAUseCase = mock<RequestChangeEmailWith2FAUseCase>()
    private val disableMultiFactorAuthUseCase = mock<DisableMultiFactorAuthUseCase>()
    private val changePasswordWith2FAUseCase = mock<ChangePasswordWith2FAUseCase>()
    private val logoutUseCase = mock<LogoutUseCase>()
    private val getDomainNameUseCase = mock<GetDomainNameUseCase>()

    @BeforeEach
    fun setUp() {
        reset(
            isMultiFactorAuthEnabledUseCase,
            requestDeleteAccountLinkWith2FAUseCase,
            requestChangeEmailWith2FAUseCase,
            disableMultiFactorAuthUseCase,
            changePasswordWith2FAUseCase,
            logoutUseCase,
            getDomainNameUseCase,
        )
        whenever(getDomainNameUseCase()).thenReturn("mega.nz")
    }

    private suspend fun initViewModel(
        verifyType: Int,
        newEmail: String? = null,
        newPassword: String? = null,
        isLogout: Boolean = false,
        is2FAEnabled: Boolean = true,
    ) {
        whenever(isMultiFactorAuthEnabledUseCase()).thenReturn(is2FAEnabled)
        val savedStateHandle = SavedStateHandle(
            mapOf(
                VerifyTwoFactorActivity.KEY_VERIFY_TYPE to verifyType,
                VerifyTwoFactorActivity.KEY_NEW_EMAIL to newEmail,
                VerifyTwoFactorActivity.KEY_NEW_PASSWORD to newPassword,
                ChangePasswordActivity.KEY_IS_LOGOUT to isLogout,
            )
        )
        underTest = VerifyTwoFactorViewModel(
            savedStateHandle = savedStateHandle,
            isMultiFactorAuthEnabledUseCase = isMultiFactorAuthEnabledUseCase,
            requestDeleteAccountLinkWith2FAUseCase = requestDeleteAccountLinkWith2FAUseCase,
            requestChangeEmailWith2FAUseCase = requestChangeEmailWith2FAUseCase,
            disableMultiFactorAuthUseCase = disableMultiFactorAuthUseCase,
            changePasswordWith2FAUseCase = changePasswordWith2FAUseCase,
            logoutUseCase = logoutUseCase,
            getDomainNameUseCase = getDomainNameUseCase,
        )
    }

    @Test
    fun `test that init checks the 2FA enabled state and exposes the recovery url`() = runTest {
        initViewModel(verifyType = DISABLE_2FA, is2FAEnabled = true)
        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.is2FAEnabled).isTrue()
            assertThat(state.recoveryUrl).isEqualTo("https://mega.nz/recovery")
            assertThat(state.verifyType).isEqualTo(DISABLE_2FA)
        }
        verify(isMultiFactorAuthEnabledUseCase).invoke()
    }

    @Test
    fun `test that a partial pin does not trigger any use case`() = runTest {
        initViewModel(verifyType = DISABLE_2FA)
        underTest.onPinChanged("12345")
        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.pin).isEqualTo("12345")
            assertThat(state.isLoading).isFalse()
        }
        verifyNoInteractions(disableMultiFactorAuthUseCase)
    }

    @Test
    fun `test that a full pin triggers disableMultiFactorAuth for DISABLE_2FA and emits the result`() =
        runTest {
            initViewModel(verifyType = DISABLE_2FA)
            underTest.onPinChanged("123456")
            underTest.uiState.test {
                val state = expectMostRecentItem()
                assertThat(state.resultEvent)
                    .isInstanceOf(StateEventWithContentTriggered::class.java)
                val triggeredResult = state.resultEvent as StateEventWithContentTriggered
                assertThat(triggeredResult.content)
                    .isEqualTo(VerifyTwoFactorResult.MultiFactorAuthDisabled)
                assertThat(state.disableSuccessEvent).isEqualTo(triggered)
            }
            verify(disableMultiFactorAuthUseCase).invoke(eq("123456"))
        }

    @Test
    fun `test that CANCEL_ACCOUNT_2FA calls requestDeleteAccountLinkWith2FA on success`() = runTest {
        initViewModel(verifyType = CANCEL_ACCOUNT_2FA)
        underTest.onPinChanged("123456")
        underTest.uiState.test {
            val state = expectMostRecentItem()
            val triggeredResult = state.resultEvent as StateEventWithContentTriggered
            assertThat(triggeredResult.content).isEqualTo(VerifyTwoFactorResult.CancelAccountLinkSent)
        }
        verify(requestDeleteAccountLinkWith2FAUseCase).invoke(eq("123456"))
    }

    @Test
    fun `test that CHANGE_MAIL_2FA forwards the new email and emits EmailChangeLinkSent`() =
        runTest {
            initViewModel(verifyType = CHANGE_MAIL_2FA, newEmail = "new@mega.nz")
            underTest.onPinChanged("123456")
            underTest.uiState.test {
                val triggeredResult = expectMostRecentItem().resultEvent
                        as StateEventWithContentTriggered
                assertThat(triggeredResult.content).isEqualTo(VerifyTwoFactorResult.EmailChangeLinkSent)
            }
            verify(requestChangeEmailWith2FAUseCase).invoke(eq("new@mega.nz"), eq("123456"))
        }

    @Test
    fun `test that CHANGE_PASSWORD_2FA with isLogout false navigates to MyAccount`() = runTest {
        initViewModel(
            verifyType = CHANGE_PASSWORD_2FA,
            newPassword = "new-pass",
            isLogout = false,
        )
        whenever(changePasswordWith2FAUseCase(any(), any())).thenReturn(true)
        underTest.onPinChanged("123456")
        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.passwordChangedEvent)
                .isEqualTo(triggered(PasswordChangedAction.NavigateToMyAccount(MegaError.API_OK)))
            assertThat(state.logoutEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that CHANGE_PASSWORD_2FA with isLogout true fires the logout event`() = runTest {
        initViewModel(
            verifyType = CHANGE_PASSWORD_2FA,
            newPassword = "new-pass",
            isLogout = true,
        )
        whenever(changePasswordWith2FAUseCase(any(), any())).thenReturn(true)
        underTest.onPinChanged("123456")
        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.logoutEvent).isEqualTo(triggered)
            assertThat(state.passwordChangedEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that a wrong pin sets isPinError when 2FA is enabled`() = runTest {
        initViewModel(verifyType = DISABLE_2FA, is2FAEnabled = true)
        whenever(disableMultiFactorAuthUseCase(any())).thenAnswer {
            throw WrongMultiFactorAuthPinException(MegaError.API_EFAILED, "bad pin")
        }
        underTest.onPinChanged("123456")
        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isPinError).isTrue()
            assertThat(state.resultEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that a wrong pin is suppressed when 2FA is reported as disabled`() = runTest {
        initViewModel(verifyType = DISABLE_2FA, is2FAEnabled = false)
        whenever(disableMultiFactorAuthUseCase(any())).thenAnswer {
            throw WrongMultiFactorAuthPinException(MegaError.API_EFAILED, "bad pin")
        }
        underTest.onPinChanged("123456")
        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isPinError).isFalse()
        }
    }

    @Test
    fun `test that editing the pin after an error clears isPinError`() = runTest {
        initViewModel(verifyType = DISABLE_2FA)
        whenever(disableMultiFactorAuthUseCase(any())).thenAnswer {
            throw WrongMultiFactorAuthPinException(MegaError.API_EFAILED, "bad pin")
        }
        underTest.onPinChanged("123456")
        underTest.onPinChanged("12345")
        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isPinError).isFalse()
            assertThat(state.pin).isEqualTo("12345")
        }
    }

    @Test
    fun `test that consuming the result event resets it`() = runTest {
        initViewModel(verifyType = DISABLE_2FA)
        underTest.onPinChanged("123456")
        underTest.onResultEventConsumed()
        underTest.uiState.test {
            assertThat(expectMostRecentItem().resultEvent).isEqualTo(consumed())
        }
    }
}
