package mega.privacy.android.app.presentation.login.model

import mega.privacy.android.domain.entity.account.AccountSession
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.exception.LoginException

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.login.LoginFragment].
 *
 * @property isFirstFetchNodesUpdate True while there was not any fetch nodes update, false otherwise.
 * @property accountSession          [AccountSession]
 * @property password                Typed password.
 * @property accountConfirmationLink Link for confirming a new account.
 * @property isFirstTime             True if account credentials are null for the first time
 * @property fetchNodesUpdate        [FetchNodesUpdate]. If not null, a fetch nodes is in progress.
 * @property isAlreadyLoggedIn       True if account credentials are not null, false otherwise.
 * @property pressedBackWhileLogin   True if pressed back while a login was in progress, false otherwise.
 * @property is2FAEnabled            True if should ask for 2FA, false otherwise.
 * @property is2FARequired           True if 2FA needs to be requested, false otherwise.
 * @property was2FAErrorShown        True if a pin was wrong already , false otherwise.
 * @property is2FAErrorShown         True if 2FA error is shown because of a wrong pin typed.
 * @property isPinLongClick          True if it is trying to paste 2FA pin with a long click on any of the pin fields.
 * @property isAccountConfirmed      True if account is confirmed after creation, false otherwise.
 * @property intentAction            Intent action.
 * @property pendingClicksKarere     Number of pending clicks before enabling/disabling MEGAChat logs.
 * @property pendingClicksSDK        Number of pending clicks before enabling/disabling SDK logs.
 * @property isRefreshApiServer      True if it is refreshing API server, false otherwise.
 * @property temporalEmail           Temporal email used for account creation.
 * @property temporalPassword        Temporal password used for account creation.
 * @property hasPreferences          True if has user preferences, false otherwise.
 * @property hasCUSetting            True if has CU setting, false otherwise.
 * @property isCUSettingEnabled      Ture if CU setting is enabled, false otherwise.
 * @property isLocalLogoutInProgress True if local logout is in progress, false otherwise.
 * @property isLoginRequired         True if should ask for login, false otherwise.
 * @property isLoginInProgress       True if a login is in progress, false otherwise.
 * @property error                   [LoginException].
 */
data class LoginState(
    val isFirstFetchNodesUpdate: Boolean = true,
    val accountSession: AccountSession? = null,
    val password: String? = null,
    val accountConfirmationLink: String? = null,
    val fetchNodesUpdate: FetchNodesUpdate? = null,
    val isFirstTime: Boolean = false,
    val isAlreadyLoggedIn: Boolean = true,
    val pressedBackWhileLogin: Boolean = false,
    val is2FAEnabled: Boolean = false,
    val is2FARequired: Boolean = false,
    val was2FAErrorShown: Boolean = false,
    val is2FAErrorShown: Boolean = false,
    val isPinLongClick: Boolean = false,
    val isAccountConfirmed: Boolean = false,
    val intentAction: String? = null,
    val pendingClicksKarere: Int = CLICKS_TO_ENABLE_LOGS,
    val pendingClicksSDK: Int = CLICKS_TO_ENABLE_LOGS,
    val isRefreshApiServer: Boolean = false,
    val temporalEmail: String? = null,
    val temporalPassword: String? = null,
    val hasPreferences: Boolean = false,
    val hasCUSetting: Boolean = false,
    val isCUSettingEnabled: Boolean = false,
    val isLocalLogoutInProgress: Boolean = false,
    val isLoginRequired: Boolean = false,
    val isLoginInProgress: Boolean = false,
    val error: RuntimeException? = null,
) {
    companion object {

        /**
         * Necessary click in a view to enable or disable logs.
         */
        const val CLICKS_TO_ENABLE_LOGS = 5
    }
}
