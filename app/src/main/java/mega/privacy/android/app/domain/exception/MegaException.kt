package mega.privacy.android.app.domain.exception

/**
 * Mega exception - For unexpected exceptions
 *
 *
 * @param errorCode
 * @param errorString
 */
open class MegaException(errorCode: Int?, errorString: String?) : Throwable(message = "ErrorCode: $errorCode ___ ErrorString: $errorString")
