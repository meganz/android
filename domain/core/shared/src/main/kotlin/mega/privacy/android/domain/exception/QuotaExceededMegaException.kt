package mega.privacy.android.domain.exception

/**
 * Quota Exceeded Exception
 *
 *
 * @param errorCode
 * @param errorString
 * @param value
 */
class QuotaExceededMegaException(
    errorCode: Int,
    errorString: String? = null,
    value: Long = 0L,
    methodName: String? = null,
) : MegaException(errorCode, errorString, value, methodName)
