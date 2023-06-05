package mega.privacy.android.domain.exception

/**
 * Mega exception - For unexpected exceptions
 *
 *
 * @param errorCode
 * @param errorString
 * @param value
 * @param methodName
 */
open class MegaException(
    val errorCode: Int,
    val errorString: String?,
    val value: Long = 0L,
    val methodName: String? = null,
) : Throwable(
    message = "Exception thrown in $methodName: ErrorCode: $errorCode " +
            "___ ErrorString: $errorString ___ ErrorValue: $value"
)
