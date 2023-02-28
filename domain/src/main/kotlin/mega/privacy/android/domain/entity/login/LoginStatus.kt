package mega.privacy.android.domain.entity.login

/**
 * Enum class for defining Login possible statuses.
 */
enum class LoginStatus {

    /**
     * Login cannot start.
     */
    LoginCannotStart,

    /**
     * Login started.
     */
    LoginStarted,

    /**
     * Login finished with success.
     */
    LoginSucceed,

    /**
     * Login requires 2FA.
     */
    LoginRequire2FA,
}