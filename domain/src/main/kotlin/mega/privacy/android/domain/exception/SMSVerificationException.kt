package mega.privacy.android.domain.exception

/**
 * SMS Verification Exception
 */
sealed class SMSVerificationException(errorCode: Int, errorString: String?) :
    MegaException(errorCode, errorString) {

    /**
     * Api Limit Reached Exception
     */
    class LimitReached(errorCode: Int, errorString: String? = null) :
        SMSVerificationException(errorCode, errorString)

    /**
     * Already Verified
     */
    class AlreadyVerified(errorCode: Int, errorString: String? = null) :
        SMSVerificationException(errorCode, errorString)

    /**
     * Invalid Phone Number
     */
    class InvalidPhoneNumber(errorCode: Int, errorString: String? = null) :
        SMSVerificationException(errorCode, errorString)

    /**
     * Already Exists
     */
    class AlreadyExists(errorCode: Int, errorString: String? = null) :
        SMSVerificationException(errorCode, errorString)

    /**
     * Unknown
     */
    class Unknown(errorCode: Int, errorString: String? = null) :
        SMSVerificationException(errorCode, errorString)
}
