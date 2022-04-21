package mega.privacy.android.app.utils

import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaError

object ErrorUtils {

    /**
     * Converts MegaError to Throwable
     * TODO Replace with MegaError.toMegaException() call
     *
     * @return MegaError wrapped into a Throwable
     */
    fun MegaError.toThrowable(): Throwable =
        RuntimeException("$errorCode: $errorString")

    /**
     * Converts MegaChatError to Throwable
     * TODO Replace with MegaChatError.toMegaException() call
     *
     * @return MegaChatError wrapped into a Throwable
     */
    fun MegaChatError.toThrowable(): Throwable =
        RuntimeException("$errorCode: $errorString")
}
