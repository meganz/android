package mega.privacy.android.app.presentation.login.model

import android.os.Parcelable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.parcelize.Parcelize
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.login.TemporaryWaitingError
import mega.privacy.android.domain.exception.LoginException


/**
 * Data class defining the state of [mega.privacy.android.app.presentation.login.LoginFragment].
 *
 * @property intentState                [LoginIntentState]
 * @property accountSession             [AccountSession]
 * @property emailError                 [LoginError].
 * @property password                   Typed password.
 * @property passwordError              [LoginError].
 * @property accountConfirmationLink    Link for confirming a new account.
 * @property isFirstTime                True if account credentials are null for the first time
 * @property isAlreadyLoggedIn          True if account credentials are not null, false otherwise.
 * @property pressedBackWhileLogin      True if pressed back while a login was in progress, false otherwise.
 * @property is2FAEnabled               True if should ask for 2FA, false otherwise.
 * @property is2FARequired              True if 2FA needs to be requested, false otherwise.
 * @property isFirstTime2FA             True if it is the first time the 2FA is requested.
 * @property twoFAPin                   Typed 2FA pin.
 * @property multiFactorAuthState       [MultiFactorAuthState]
 * @property isAccountConfirmed         True if account is confirmed after creation, false otherwise.
 * @property rootNodesExists            True if root node exists, false otherwise.
 * @property temporalEmail              Temporal email used for account creation.
 * @property temporalPassword           Temporal password used for account creation.
 * @property hasPreferences             True if has user preferences, false otherwise.
 * @property hasCUSetting               True if has CU setting, false otherwise.
 * @property isCUSettingEnabled         Ture if CU setting is enabled, false otherwise.
 * @property isLocalLogoutInProgress    True if local logout is in progress, false otherwise.
 * @property isLoginRequired            True if should ask for login, false otherwise.
 * @property isLoginInProgress          True if a login is in progress, false otherwise.
 * @property loginException             [LoginException].
 * @property ongoingTransfersExist      True if exist ongoing transfers, false if not. Null if pending to check.
 * @property isPendingToFinishActivity  True if should finish the activity, false otherwise.
 * @property isPendingToShowFragment    [LoginScreen] if pending, null otherwise.
 * @property isCheckingSignupLink       True if it is checking a signup link, false otherwise.
 * @property snackbarMessage            Message to show in Snackbar.
 * @property loginTemporaryError        [TemporaryWaitingError] during login
 * @property isFirstTimeLaunch          True if it is the first time the app is launched.
 * @property themeMode                 [ThemeMode] of the app.
 * @property accountBlockedEvent
 * @property resendVerificationEmailEvent
 * @property checkRecoveryKeyEvent
 * @property openUrlEvent
 * @property miscFlagLoaded
 * @property shouldShowUpgradeAccount
 * @property recoveryKeyLink
 * @property shouldShowNotificationPermission

 */
data class LoginState(
    val intentState: LoginIntentState? = null,
    val accountSession: AccountSession? = null,
    val emailError: LoginError? = null,
    val password: String? = null,
    val passwordError: LoginError? = null,
    val accountConfirmationLink: String? = null,
    val isFirstTime: Boolean = false,
    val isAlreadyLoggedIn: Boolean = true,
    val pressedBackWhileLogin: Boolean = false,
    val is2FAEnabled: Boolean = false,
    val is2FARequired: Boolean = false,
    val isFirstTime2FA: StateEvent = consumed,
    val twoFAPin: List<String> = listOf("", "", "", "", "", ""),
    val multiFactorAuthState: MultiFactorAuthState? = null,
    val isAccountConfirmed: Boolean = false,
    val rootNodesExists: Boolean = false,
    val temporalEmail: String? = null,
    val hasPreferences: Boolean = false,
    val hasCUSetting: Boolean = false,
    val isCUSettingEnabled: Boolean = false,
    val isLocalLogoutInProgress: Boolean = false,
    val isLoginRequired: Boolean = false,
    val isLoginInProgress: Boolean = false,
    val loginException: LoginException? = null,
    val ongoingTransfersExist: Boolean? = null,
    val isPendingToFinishActivity: Boolean = false,
    val isPendingToShowFragment: StateEventWithContent<LoginScreen> = consumed(),
    val isCheckingSignupLink: Boolean = false,
    val snackbarMessage: StateEventWithContent<Int> = consumed(),
    val loginTemporaryError: TemporaryWaitingError? = null,
    val isFirstTimeLaunch: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val accountBlockedEvent: StateEventWithContent<AccountBlockedUiState> = consumed(),
    val resendVerificationEmailEvent: StateEventWithContent<Boolean> = consumed(),
    val checkRecoveryKeyEvent: StateEventWithContent<Result<RkLink>> = consumed(),
    val openUrlEvent: StateEventWithContent<String> = consumed(),
    val miscFlagLoaded: Boolean = false,
    val shouldShowUpgradeAccount: Boolean = false,
    val recoveryKeyLink: String? = null,
    val shouldShowNotificationPermission: Boolean = false,
    val initialEmail: String? = null,
    val isPendingToGetLinkWithSession: Boolean = false,
)

@Parcelize
data class AccountBlockedUiState(
    val type: AccountBlockedType,
    val text: String,
) : Parcelable
