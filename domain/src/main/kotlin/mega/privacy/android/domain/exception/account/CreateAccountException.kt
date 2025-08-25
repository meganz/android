package mega.privacy.android.domain.exception.account

import mega.privacy.android.domain.exception.MegaException

/**
 * Exception thrown when an error occurs while creating an account.
 */
sealed class CreateAccountException(message: String) : Exception(message) {
    /**
     * The account already exists
     */
    data object AccountAlreadyExists :
        CreateAccountException("Account already exists") {
        private fun readResolve(): Any = AccountAlreadyExists
    }

    /**
     * Too many attempts.
     */
    data object TooManyAttemptsException :
        CreateAccountException("Too many concurrent connections or transfers") {
        private fun readResolve(): Any = TooManyAttemptsException
    }

    /**
     * Other unknown error
     * @property megaException The original MegaException
     */
    class Unknown(val megaException: MegaException) :
        CreateAccountException(megaException.errorString ?: "UnknownException")
}