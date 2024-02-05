package mega.privacy.android.domain.exception.account

import mega.privacy.android.domain.exception.MegaException

/**
 * Sealed class containing different [MegaException] types pertaining to confirming the User's
 * Account cancellation
 *
 * @param errorCode The Error Code
 * @param errorString The Error String, which may be nullable
 */
sealed class ConfirmCancelAccountException(errorCode: Int, errorString: String?) :
    MegaException(errorCode, errorString) {

    /**
     * The Account cannot be cancelled because the Password provided was incorrect
     *
     * @param errorCode The Error Code
     * @param errorString The Error String, which may be nullable
     */
    class IncorrectPassword(errorCode: Int, errorString: String? = null) :
        ConfirmCancelAccountException(errorCode, errorString)

    /**
     * An unknown error occurred
     *
     * @param errorCode The Error Code
     * @param errorString The Error String, which may be nullable
     */
    class Unknown(errorCode: Int, errorString: String? = null) :
        ConfirmCancelAccountException(errorCode, errorString)
}