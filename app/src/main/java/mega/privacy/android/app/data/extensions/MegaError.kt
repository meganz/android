package mega.privacy.android.app.data.extensions

import mega.privacy.android.app.domain.exception.MegaException
import nz.mega.sdk.MegaError

/**
 * Convenience method to map a [MegaError] to a [MegaException]
 *
 * @return
 */
fun MegaError.toException(): MegaException = MegaException(errorCode, errorString)