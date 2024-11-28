package mega.privacy.android.domain.exception.account

import mega.privacy.android.domain.exception.MegaException

/**
 * Exception thrown when an error occurs while creating an account.
 */
sealed class CreateAccountException(message: String) : Exception(message) {
    /**
     * The account already exists
     */
    object AccountAlreadyExists :
        CreateAccountException("Account already exists")

    /**
     * Other unknown error
     * @property megaException The original MegaException
     */
    class Unknown(val megaException: MegaException) :
        CreateAccountException(megaException.errorString ?: "UnknownException")
}