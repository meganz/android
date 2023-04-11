package mega.privacy.android.domain.exception

/**
 * Mega exception - For unexpected exceptions
 *
 *
 * @param errorCode
 * @param errorString
 */
open class MegaException(
    val errorCode: Int,
    val errorString: String?,
    val methodName: String? = null,
) :
    Throwable(message = "Exception thrown in $methodName: ErrorCode: $errorCode ___ ErrorString: $errorString")
