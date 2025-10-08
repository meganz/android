package mega.privacy.android.app.presentation.login.model

import android.os.Parcelable
import androidx.annotation.StringRes
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.parcelize.Parcelize
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.messageId
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.account.AccountBlockedType
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
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
 * @property fetchNodesUpdate           [FetchNodesUpdate]. If not null, a fetch nodes is in progress.
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
 * @property isPendingToShowFragment    [LoginFragmentType] if pending, null otherwise.
 * @property isCheckingSignupLink       True if it is checking a signup link, false otherwise.
 * @property snackbarMessage            Message to show in Snackbar.
 * @property isFastLoginInProgress      True if a fast login is in progress, false otherwise.
 * @property loginTemporaryError        [TemporaryWaitingError] during login
 * @property requestStatusProgress      Progress of the request status, 0 to 1000, hide progress bar if -1
 * @property isFirstTimeLaunch          True if it is the first time the app is launched.
 * @property themeMode                 [ThemeMode] of the app.
 * @property accountBlockedEvent
 * @property resendVerificationEmailEvent
 * @property checkRecoveryKeyEvent
 * @property openRecoveryUrlEvent
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
    val fetchNodesUpdate: FetchNodesUpdate? = null,
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
    val isFastLoginInProgress: Boolean = false,
    val loginException: LoginException? = null,
    val ongoingTransfersExist: Boolean? = null,
    val isPendingToFinishActivity: Boolean = false,
    val isPendingToShowFragment: LoginFragmentType? = null,
    val isCheckingSignupLink: Boolean = false,
    val snackbarMessage: StateEventWithContent<Int> = consumed(),
    val loginTemporaryError: TemporaryWaitingError? = null,
    val requestStatusProgress: Progress? = null,
    val isFirstTimeLaunch: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val accountBlockedEvent: StateEventWithContent<AccountBlockedUiState> = consumed(),
    val resendVerificationEmailEvent: StateEventWithContent<Boolean> = consumed(),
    val checkRecoveryKeyEvent: StateEventWithContent<Result<RkLink>> = consumed(),
    val openRecoveryUrlEvent: StateEventWithContent<String> = consumed(),
    val miscFlagLoaded: Boolean = false,
    val shouldShowUpgradeAccount: Boolean = false,
    val recoveryKeyLink: String? = null,
    val shouldShowNotificationPermission: Boolean = false,
) {

    /**
     * True if the request status progress event is being processed
     */
    val isRequestStatusInProgress = requestStatusProgress != null

    /**
     * Temporary error during login or fetch nodes
     */
    private val temporaryError get() = loginTemporaryError ?: fetchNodesUpdate?.temporaryError

    /**
     * Text to show below progress bar
     */
    @StringRes
    val currentStatusText: Int = when {
        isCheckingSignupLink -> R.string.login_querying_signup_link
        temporaryError != null && !isRequestStatusInProgress -> temporaryError?.messageId
            ?: R.string.login_connecting_to_server

        isFastLoginInProgress -> R.string.login_connecting_to_server

        (fetchNodesUpdate?.progress?.floatValue
            ?: 0f) > 0f && isFirstTime -> R.string.login_preparing_filelist

        fetchNodesUpdate != null -> R.string.download_updating_filelist
        else -> R.string.login_connecting_to_server
    }

    /**
     * Calculate the current progress of the login and fetch nodes
     * Weights:
     * - 30% for login if first time, else 50%
     * - 10% for updating files if first login, else 45%
     * - 60% for preparing files if first login, else 5%
     * - Direct to 90% if checking signup link
     */
    val currentProgress: Float = run {
        val progressAfterLogin = if (isFirstTime) 0.3f else 0.5f
        val progressAfterFetchNode = progressAfterLogin + if (isFirstTime) 0.1f else 0.45f
        when {
            isCheckingSignupLink -> 0.9f
            isFastLoginInProgress -> progressAfterLogin
            fetchNodesUpdate?.progress != null -> {
                val fetchNodeProgress = fetchNodesUpdate.progress?.floatValue ?: 0f
                if (fetchNodeProgress > 0f)
                    progressAfterFetchNode + (fetchNodeProgress * (1.0f - progressAfterFetchNode))
                else
                    progressAfterFetchNode
            }

            fetchNodesUpdate != null -> progressAfterFetchNode
            else -> progressAfterLogin
        }
    }
}

@Parcelize
data class AccountBlockedUiState(
    val type: AccountBlockedType,
    val text: String,
) : Parcelable
