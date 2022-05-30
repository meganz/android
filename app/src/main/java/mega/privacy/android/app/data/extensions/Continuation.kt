package mega.privacy.android.app.data.extensions

import mega.privacy.android.app.domain.exception.MegaException
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaError
import kotlin.coroutines.Continuation

fun <T> Continuation<T>.failWithException(
    exception: MegaException
) {
    this.resumeWith(
        Result.failure(
            exception
        )
    )
}

fun <T> Continuation<T>.failWithError(
    error: MegaError
) {
    this.failWithException(
        MegaException(
            error.errorCode,
            error.errorString
        )
    )
}

fun <T> Continuation<T>.failWithError(
    error: MegaChatError
) {
    this.failWithException(
        MegaException(
            error.errorCode,
            error.errorString
        )
    )
}