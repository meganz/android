package mega.privacy.android.app.utils

import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaError

object ErrorUtils {

    /**
     * Converts MegaError to Throwable

     *
     * @return MegaError wrapped into a Throwable
     */
    fun MegaError.toThrowable(): Throwable =
        RuntimeException("$errorCode: $errorString")

    /**
     * Converts MegaChatError to Throwable

     *
     * @return MegaChatError wrapped into a Throwable
     */
    fun MegaChatError.toThrowable(): Throwable =
        RuntimeException("$errorCode: $errorString")
}
