package mega.privacy.android.data.extensions

import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber
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
    methodName: String,
) {
    resumeWith(Result.failure(error.toException(methodName)))
}

/**
 * Fail with error
 *
 * Ends a continuation with a generic Mega exception derived from a [MegaChatRequest]
 *
 * @param T the type of the continuation
 * @param error the Mega chat error
 */
fun <T> Continuation<T>.failWithError(
    error: MegaChatError,
    methodName: String,
) {
    resumeWith(
        Result.failure(
            MegaException(
                errorCode = error.errorCode,
                errorString = error.errorString,
                methodName = methodName
            )
        )
    )
}

/**
 * get a request Listener
 */
fun <T> Continuation<T>.getRequestListener(
    methodName: String,
    block: (request: MegaRequest) -> T,
): MegaRequestListenerInterface {
    val listener = OptionalMegaRequestListenerInterface(
        onRequestFinish = { request, error ->
            if (error.errorCode == MegaError.API_OK) {
                this.resumeWith(Result.success(block(request)))
            } else {
                // log the error code when calling SDK api, it helps us easy to find the cause
                Timber.e("Calling $methodName failed with error code ${error.errorCode}")
                this.failWithError(error, methodName)
            }
        }
    )
    return listener
}

/**
 * Gets a chat request Listener.
 */
fun <T> Continuation<T>.getChatRequestListener(
    methodName: String,
    block: (request: MegaChatRequest) -> T,
): MegaChatRequestListenerInterface {
    val listener = OptionalMegaChatRequestListenerInterface(
        onRequestFinish = { request, error ->
            if (error.errorCode == MegaChatError.ERROR_OK) {
                this.resumeWith(Result.success(block(request)))
            } else {
                // log the error code when calling SDK api, it helps us easy to find the cause
                Timber.e("Calling $methodName failed with error code ${error.errorCode}")
                this.failWithError(error, methodName)
            }
        }
    )
    return listener
}
