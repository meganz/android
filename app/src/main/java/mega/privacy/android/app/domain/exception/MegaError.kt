package mega.privacy.android.app.domain.exception

open class MegaError(errorCode: Int? = null, errorString: String? = null) : Throwable(message = "ErrorCode: $errorCode ___ ErrorString: $errorString")
