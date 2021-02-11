package mega.privacy.android.app.utils

import nz.mega.sdk.MegaError

object ErrorUtils {

    /**
     * Converts MegaError to Throwable
     *
     * @return MegaError wrapped into a Throwable
     */
    fun MegaError.toThrowable(): Throwable =
        RuntimeException("$errorCode: $errorString")
}
