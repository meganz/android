package mega.privacy.android.domain.exception.account

import mega.privacy.android.domain.exception.MegaException

/**
 * Sealed class containing different [MegaException] types pertaining to confirming the User's
 * change of Email
 *
 * @param errorCode The Error Code
 * @param errorString The Error String, which may be nullable
 */
sealed class ConfirmChangeEmailException(errorCode: Int, errorString: String?) :
    MegaException(errorCode, errorString) {

    /**
     * The Email Address is currently in use
     *
     * @param errorCode The Error Code
     * @param errorString The Error String, which may be nullable
     */
    class EmailAlreadyInUse(errorCode: Int, errorString: String? = null) :
        ConfirmChangeEmailException(errorCode, errorString)

    /**
     * The User cannot change the Email Address, because the Password provided was incorrect
     *
     * @param errorCode The Error Code
     * @param errorString The Error String, which may be nullable
     */
    class IncorrectPassword(errorCode: Int, errorString: String? = null) :
        ConfirmChangeEmailException(errorCode, errorString)

    /**
     * An unknown error occurred
     *
     * @param errorCode The Error Code
     * @param errorString The Error String, which may be nullable
     */
    class Unknown(errorCode: Int, errorString: String? = null) :
        ConfirmChangeEmailException(errorCode, errorString)
}