package mega.privacy.android.app.presentation.login.model

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.exception.LoginException
import mega.privacy.android.domain.exception.login.FetchNodesException

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.login.LoginFragment].
 *
 * @property intentState                [LoginIntentState]
 * @property accountSession             [AccountSession]
 * @property password                   Typed password.
 * @property accountConfirmationLink    Link for confirming a new account.
 * @property isFirstTime                True if account credentials are null for the first time
 * @property fetchNodesUpdate           [FetchNodesUpdate]. If not null, a fetch nodes is in progress.
 * @property isAlreadyLoggedIn          True if account credentials are not null, false otherwise.
 * @property pressedBackWhileLogin      True if pressed back while a login was in progress, false otherwise.
 * @property is2FAEnabled               True if should ask for 2FA, false otherwise.
 * @property is2FARequired              True if 2FA needs to be requested, false otherwise.
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
 * @property fetchNodesException        [FetchNodesException].
 * @property ongoingTransfersExist      True if exist ongoing transfers, false if not. Null if pending to check.
 * @property querySignupLinkResult      Result of query signup link.
 * @property isPendingToFinishActivity  True if should finish the activity, false otherwise.
 * @property isPendingToShowFragment    [LoginFragmentType] if pending, null otherwise.
 * @property enabledFlags               Enabled Feature Flags
 */
data class LoginState(
    val intentState: LoginIntentState? = null,
    val accountSession: AccountSession? = null,
    val password: String? = null,
    val accountConfirmationLink: String? = null,
    val fetchNodesUpdate: FetchNodesUpdate? = null,
    val isFirstTime: Boolean = false,
    val isAlreadyLoggedIn: Boolean = true,
    val pressedBackWhileLogin: Boolean = false,
    val is2FAEnabled: Boolean = false,
    val is2FARequired: Boolean = false,
    val multiFactorAuthState: MultiFactorAuthState? = null,
    val isAccountConfirmed: Boolean = false,
    val rootNodesExists: Boolean = false,
    val temporalEmail: String? = null,
    val temporalPassword: String? = null,
    val hasPreferences: Boolean = false,
    val hasCUSetting: Boolean = false,
    val isCUSettingEnabled: Boolean = false,
    val isLocalLogoutInProgress: Boolean = false,
    val isLoginRequired: Boolean = false,
    val isLoginInProgress: Boolean = false,
    val loginException: LoginException? = null,
    val fetchNodesException: FetchNodesException? = null,
    val ongoingTransfersExist: Boolean? = null,
    val querySignupLinkResult: Result<String>? = null,
    val isPendingToFinishActivity: Boolean = false,
    val isPendingToShowFragment: LoginFragmentType? = null,
    val enabledFlags: Set<Feature> = emptySet(),
)
