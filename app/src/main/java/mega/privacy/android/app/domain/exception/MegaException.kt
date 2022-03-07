package mega.privacy.android.app.domain.exception

open class MegaException(errorCode: Int?, errorString: String?) : Throwable(message = "ErrorCode: $errorCode ___ ErrorString: $errorString")
