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
     * Verification code does not match
     */
    class VerificationCodeDoesNotMatch(errorCode: Int, errorString: String? = null) :
        SMSVerificationException(errorCode, errorString)

    /**
     * Calling Code Loading Failed
     */
    object CallingCodesLoadingFailed :
        SMSVerificationException(-1, null)

    /**
     * Unknown
     */
    class Unknown(errorCode: Int, errorString: String? = null) :
        SMSVerificationException(errorCode, errorString)
}
