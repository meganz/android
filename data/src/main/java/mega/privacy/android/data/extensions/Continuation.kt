package mega.privacy.android.data.extensions

import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import kotlin.coroutines.Continuation

/**
 * Fail with exception
 *
 * Ends a continuation with a specific Mega exception
 *
 * @param T the type of the continuation
 * @param exception the exception
 */
fun <T> Continuation<T>.failWithException(
    exception: MegaException,
) {
    this.resumeWith(
        Result.failure(
            exception
        )
    )
}

/**
 * Fail with error
 *
 * Ends a continuation with a generic Mega exception derived from a [MegaError]
 *
 * @param T the type of the continuation
 * @param error the Mega error
 */
fun <T> Continuation<T>.failWithError(
    error: MegaError,
) {
    this.failWithException(
        error.toException()
    )
}

fun <T> Continuation<T>.failWithError(
    error: MegaChatError,
) {
    this.failWithException(
        MegaException(
            error.errorCode,
            error.errorString
        )
    )
}

/**
 * get a request Listener
 */
fun <T> Continuation<T>.getRequestListener(
    block: (request: MegaRequest) -> T,
): MegaRequestListenerInterface {
    val listener = OptionalMegaRequestListenerInterface(
        onRequestFinish = { request, error ->
            if (error.errorCode == MegaError.API_OK) {
                this.resumeWith(Result.success(block(request)))
            } else {
                this.failWithError(error)
            }
        }
    )
    return listener
}
