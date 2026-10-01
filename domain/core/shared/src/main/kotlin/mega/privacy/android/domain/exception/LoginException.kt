package mega.privacy.android.domain.exception

/**
 * Login exception.
 */
sealed class LoginException : RuntimeException("LoginException")

/**
 * Logged out from other location.
 */
class LoginLoggedOutFromOtherLocation : LoginException()

/**
 * Multi-factor authentication required.
 */
class LoginMultiFactorAuthRequired : LoginException()

/**
 * Wrong value of Multi-factor authentication.
 */
class LoginWrongMultiFactorAuth : LoginException()

/**
 * Wrong email or password.
 */
class LoginWrongEmailOrPassword : LoginException()

/**
 * Too many login attempts.
 */
class LoginTooManyAttempts : LoginException()

/**
 * Account not validated.
 */
class LoginRequireValidation : LoginException()

/**
 * Blocked account.
 */
class LoginBlockedAccount : LoginException()

/**
 * Other error.
 *
 * @property megaException Exception required for getting the translated error string.
 */
class LoginUnknownStatus(val megaException: MegaException) : LoginException()
