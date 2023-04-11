package mega.privacy.android.data.extensions

import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaError

/**
 * Convenience method to map a [MegaError] to a [MegaException]
 *
 * @return
 */
fun MegaError.toException(methodName: String) =
    MegaException(errorCode, errorString, methodName)