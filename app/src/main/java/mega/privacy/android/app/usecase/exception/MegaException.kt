package mega.privacy.android.app.usecase.exception

/**
 * Class to manage Errors on requests.
 * The error can be:
 *  - MegaError: if the request is from megaApi
 *  - MegaChatError: if the request is from megaChatApi
 *
 * @property errorCode  Error code.
 * @property message    Error string.
 */
class MegaException(
    val errorCode: Int,
    message: String
) : RuntimeException(message)