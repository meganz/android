package mega.privacy.android.domain.exception

/**
 * Business account expired Mega Exception
 *
 * @property errorCode
 * @property errorString
 * @property value
 * @property methodName
 */
class BusinessAccountExpiredMegaException(
    errorCode: Int,
    errorString: String? = null,
    value: Long = 0L,
    methodName: String? = null,
) : MegaException(errorCode, errorString, value, methodName)