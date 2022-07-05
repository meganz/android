package mega.privacy.android.domain.exception

/**
 * No logged in user exception
 *
 *
 * @param errorCode
 * @param errorString
 */
class NoLoggedInUserException(errorCode: Int? = null, errorString: String? = null) :
    MegaException(errorCode, errorString)
