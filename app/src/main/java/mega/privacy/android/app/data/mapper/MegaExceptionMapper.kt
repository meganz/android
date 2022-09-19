package mega.privacy.android.app.data.mapper

import mega.privacy.android.app.data.extensions.toException
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaError

/**
 * [MegaError] to [MegaException] mapper
 */
typealias MegaExceptionMapper = (@JvmSuppressWildcards MegaError) -> @JvmSuppressWildcards MegaException

internal fun toMegaExceptionModel(error: MegaError) = error.toException()