package mega.privacy.android.domain.exception

/**
 * Too Many Requests Exception
 *
 * @param errorCode
 * @param errorString
 * @param value
 * @param methodName
 */
class TooManyRequestsMegaException(
    errorCode: Int,
    errorString: String? = null,
    value: Long = 0L,
    methodName: String? = null,
) : MegaException(errorCode, errorString, value, methodName)
