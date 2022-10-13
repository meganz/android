package mega.privacy.android.domain.exception

/**
 * Mega exception - For unexpected exceptions
 *
 *
 * @param errorCode
 * @param errorString
 */
open class MegaException(val errorCode: Int, val errorString: String?) :
    Throwable(message = "ErrorCode: $errorCode ___ ErrorString: $errorString")
