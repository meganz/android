package mega.privacy.android.domain.exception

/**
 * Thrown when the user supplies an invalid or expired Multi-Factor Authentication PIN.
 *
 * @param errorCode SDK error code returned by the original request.
 */
class WrongMultiFactorAuthPinException(errorCode: Int, errorString: String? = null) :
    MegaException(errorCode, errorString)
