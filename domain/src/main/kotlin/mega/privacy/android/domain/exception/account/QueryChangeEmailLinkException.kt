package mega.privacy.android.domain.exception.account

import mega.privacy.android.domain.exception.MegaException

/**
 * Sealed class containing different [MegaException] types pertaining to querying the Change Email
 * Link
 *
 * @param errorCode The Error Code
 * @param errorString The Error String, which may be nullable
 */
sealed class QueryChangeEmailLinkException(errorCode: Int, errorString: String?) :
    MegaException(errorCode, errorString) {

    /**
     * The Change Email Link was not created for the current Account
     *
     * @param errorCode The Error Code
     * @param errorString The Error String, which may be nullable
     */
    class LinkNotGenerated(errorCode: Int, errorString: String? = null) :
        QueryChangeEmailLinkException(errorCode, errorString)

    /**
     * An unknown error occurred
     *
     * @param errorCode The Error Code
     * @param errorString The Error String, which may be nullable
     */
    class Unknown(errorCode: Int, errorString: String? = null) :
        QueryChangeEmailLinkException(errorCode, errorString)
}