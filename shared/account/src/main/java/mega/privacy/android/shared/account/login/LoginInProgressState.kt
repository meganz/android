package mega.privacy.android.shared.account.login

/**
 * Login in progress state
 *
 * @property isLoginInProgress Whether login is currently in progress (login mutex is locked)
 */
data class LoginInProgressState(
    val isLoginInProgress: Boolean = false,
)
