package mega.privacy.android.domain.usecase

import javax.inject.Inject

/**
 * Default implementation of [HasPendingUploads]
 */
class DefaultHasPendingUploads @Inject constructor(
    private val getNumPendingUploads: GetNumPendingUploads,
) : HasPendingUploads {

    override suspend fun invoke(): Boolean = getNumPendingUploads() > 0
}