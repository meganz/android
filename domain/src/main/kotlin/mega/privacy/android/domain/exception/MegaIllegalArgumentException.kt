package mega.privacy.android.domain.exception

/**
 * Mega illegal argument exception
 *
 * @param errorCode Error code
 * @param errorString Error string
 * @param value Error value
 * @param methodName Method name
 */
class MegaIllegalArgumentException(
    errorCode: Int,
    errorString: String? = null,
    value: Long = 0L,
    methodName: String? = null,
) : MegaException(errorCode, errorString, value, methodName)