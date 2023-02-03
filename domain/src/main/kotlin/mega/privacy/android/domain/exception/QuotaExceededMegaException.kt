package mega.privacy.android.domain.exception

/**
 * Quota Exceeded Exception
 *
 *
 * @param errorCode
 * @param errorString
 */
class QuotaExceededMegaException(errorCode: Int, errorString: String? = null) :
    MegaException(errorCode, errorString)
