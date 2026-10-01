package mega.privacy.android.domain.exception

class NotEnoughQuotaMegaException(
    errorCode: Int,
    errorString: String?,
    value: Long = 0L,
    methodName: String? = null,
) : MegaException(errorCode, errorString, value, methodName)