package mega.privacy.android.data.mapper

import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaError

/**
 * [MegaError] to [MegaException] mapper
 */
typealias MegaExceptionMapper = (@JvmSuppressWildcards MegaError) -> @JvmSuppressWildcards MegaException

internal fun toMegaExceptionModel(error: MegaError) =
    MegaException(error.errorCode, error.errorString)