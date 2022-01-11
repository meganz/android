package mega.privacy.android.app.domain.exception

class ApiError(val errorCode: Int? = null, errorString: String? = null) : Throwable(message = errorString)
