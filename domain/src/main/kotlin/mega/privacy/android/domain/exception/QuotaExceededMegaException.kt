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
    val value: Long = 0L,
) : MegaException(errorCode, errorString)
