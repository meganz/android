package mega.privacy.android.domain.exception

/**
 * Enable multi factor authentication exception
 *
 *
 * @param errorCode
 * @param errorString
 */
class EnableMultiFactorAuthException(errorCode: Int, errorString: String? = null) :
    MegaException(errorCode, errorString)
