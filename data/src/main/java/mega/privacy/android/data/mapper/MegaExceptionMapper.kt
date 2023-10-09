package mega.privacy.android.data.mapper

import mega.privacy.android.domain.exception.BlockedMegaException
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.ErrorContexts
import javax.inject.Inject

/**
 * [MegaError] to [MegaException] mapper
 */
internal class MegaExceptionMapper @Inject constructor() {

    operator fun invoke(
        error: MegaError,
        methodName: String? = null,
        errorContext: ErrorContexts? = null,
    ) = when (error.errorCode) {
        MegaError.API_EOVERQUOTA -> {
            QuotaExceededMegaException(
                error.errorCode,
                errorContext?.let { MegaError.getErrorString(error.errorCode, it) }
                    ?: error.errorString,
                error.value,
                methodName
            )
        }

        MegaError.API_EGOINGOVERQUOTA -> NotEnoughQuotaMegaException(
            error.errorCode,
            errorContext?.let { MegaError.getErrorString(error.errorCode, it) }
                ?: error.errorString,
            error.value,
            methodName
        )

        MegaError.API_EBLOCKED -> BlockedMegaException(
            error.errorCode,
            errorContext?.let { MegaError.getErrorString(error.errorCode, it) }
                ?: error.errorString,
            error.value,
            methodName
        )

        MegaError.API_EBUSINESSPASTDUE -> BusinessAccountExpiredMegaException(
            error.errorCode,
            errorContext?.let { MegaError.getErrorString(error.errorCode, it) }
                ?: error.errorString,
            error.value,
            methodName
        )

        else -> MegaException(
            error.errorCode,
            errorContext?.let { MegaError.getErrorString(error.errorCode, it) }
                ?: error.errorString,
            error.value,
            methodName)
    }
}
