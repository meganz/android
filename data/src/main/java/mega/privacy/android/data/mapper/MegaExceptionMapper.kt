package mega.privacy.android.data.mapper

import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.BlockedMegaException
import nz.mega.sdk.MegaError
import javax.inject.Inject

/**
 * [MegaError] to [MegaException] mapper
 */
internal class MegaExceptionMapper @Inject constructor() {
    operator fun invoke(error: MegaError, methodName: String? = null) = when (error.errorCode) {
        MegaError.API_EOVERQUOTA -> {
            QuotaExceededMegaException(
                error.errorCode,
                error.errorString,
                error.value,
                methodName
            )
        }

        MegaError.API_EGOINGOVERQUOTA -> NotEnoughQuotaMegaException(
            error.errorCode,
            error.errorString,
            error.value,
            methodName
        )

        MegaError.API_EBLOCKED -> BlockedMegaException(
            error.errorCode,
            error.errorString,
            error.value,
            methodName
        )

        else -> MegaException(error.errorCode, error.errorString, error.value, methodName)
    }
}
