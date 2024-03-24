package mega.privacy.android.domain.exception.account

import mega.privacy.android.domain.exception.MegaException

/**
 * Sealed class containing different [MegaException] types pertaining to querying the Account
 * Cancellation Link
 *
 * @param errorCode The Error Code
 * @param errorString The Error String, which may be nullable
 */
sealed class QueryCancelLinkException(errorCode: Int, errorString: String?) :
    MegaException(errorCode, errorString) {

    /**
     * The Account Cancellation Link is unrelated to the current Account
     *
     * @param errorCode The Error Code
     * @param errorString The Error String, which may be nullable
     */
    class UnrelatedAccountCancellationLink(errorCode: Int, errorString: String? = null) :
        QueryCancelLinkException(errorCode, errorString)

    /**
     * The Account Cancellation Link has expired
     *
     * @param errorCode The Error Code
     * @param errorString The Error String, which may be nullable
     */
    class ExpiredAccountCancellationLink(errorCode: Int, errorString: String? = null) :
        QueryCancelLinkException(errorCode, errorString)

    /**
     * An unknown error occurred
     *
     * @param errorCode The Error Code
     * @param errorString The Error String, which may be nullable
     */
    class Unknown(errorCode: Int, errorString: String? = null) :
        QueryCancelLinkException(errorCode, errorString)
}