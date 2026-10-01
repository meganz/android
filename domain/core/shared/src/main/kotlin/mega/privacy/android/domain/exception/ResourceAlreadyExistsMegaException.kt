package mega.privacy.android.domain.exception

/**
 * Resource Already Exist Exception
 *
 *
 * @param errorCode
 * @param errorString
 */
class ResourceAlreadyExistsMegaException(
    errorCode: Int,
    errorString: String? = null,
    value: Long = 0L,
    methodName: String? = null,
) : MegaException(errorCode, errorString, value, methodName)
