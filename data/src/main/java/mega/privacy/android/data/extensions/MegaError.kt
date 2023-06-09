package mega.privacy.android.data.extensions

import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import nz.mega.sdk.MegaError

/**
 * Convenience method to map a [MegaError] to a [MegaException]
 *
 * @return
 */
fun MegaError.toException(methodName: String) = when (errorCode) {
    MegaError.API_EOVERQUOTA -> QuotaExceededMegaException(
        errorCode,
        errorString,
        value,
        methodName
    )

    MegaError.API_EGOINGOVERQUOTA -> NotEnoughQuotaMegaException(
        errorCode,
        errorString,
        value,
        methodName
    )

    else -> MegaException(errorCode, errorString, value, methodName)
}
