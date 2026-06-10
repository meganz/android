package mega.privacy.android.app.presentation.login.model

import android.os.Parcelable
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.parcelize.Parcelize
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.exception.LoginException


/**
 * Data class defining the state of [mega.privacy.android.app.presentation.login.LoginFragment].
 *
 * @property intentState                [LoginIntentState]
 * @property accountSession             [AccountSession]
 * @property emailError                 [LoginError].
 * @property password                   Typed password.
 * @property passwordError              [LoginError].
 * @property is2FARequired              True if 2FA needs to be requested, false otherwise.
 * @property multiFactorAuthState       [MultiFactorAuthState]
 * @property isAccountConfirmed         True if account is confirmed after creation, false otherwise.
 * @property temporalEmail              Temporal email used for account creation.
 * @property isLoginRequired            True if should ask for login, false otherwise.
 * @property isLoginInProgress          True if a login is in progress, false otherwise.
 * @property loginException             [LoginException].
 * @property ongoingTransfersExist      True if exist ongoing transfers, false if not. Null if pending to check.
 * @property isPendingToShowFragment    [LoginScreen] if pending, null otherwise.
 * @property snackbarMessage            Message to show in Snackbar.
 * @property themeMode                 [ThemeMode] of the app.
 * @property accountBlockedEvent
 * @property resendVerificationEmailEvent
 * @property checkRecoveryKeyEvent
 * @property openUrlEvent
 * @property miscFlagLoaded
 * @property recoveryKeyLink
 */
data class LoginState(
    val intentState: LoginIntentState? = null,
    val accountSession: AccountSession? = null,
    val emailError: LoginError? = null,
    val password: String? = null,
    val passwordError: LoginError? = null,
    val is2FARequired: Boolean = false,
    val multiFactorAuthState: MultiFactorAuthState? = null,
    val isAccountConfirmed: Boolean = false,
    val temporalEmail: String? = null,
    val isLoginRequired: Boolean = false,
    val isLoginInProgress: Boolean = false,
    val loginException: LoginException? = null,
    val ongoingTransfersExist: Boolean? = null,
    val isPendingToShowFragment: StateEventWithContent<LoginScreen> = consumed(),
    val snackbarMessage: StateEventWithContent<Int> = consumed(),
    val themeMode: ThemeMode = ThemeMode.System,
    val accountBlockedEvent: StateEventWithContent<AccountBlockedUiState> = consumed(),
    val resendVerificationEmailEvent: StateEventWithContent<Boolean> = consumed(),
    val checkRecoveryKeyEvent: StateEventWithContent<Result<RkLink>> = consumed(),
    val openUrlEvent: StateEventWithContent<String> = consumed(),
    val miscFlagLoaded: Boolean = false,
    val recoveryKeyLink: String? = null,
)

@Parcelize
data class AccountBlockedUiState(
    val type: AccountBlockedType,
    val text: String,
) : Parcelable
