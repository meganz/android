package mega.privacy.android.domain.exception

import mega.privacy.android.domain.entity.sync.SyncError

/**
 * Mega Exception for sync errors
 *
 * @property syncError SyncError object
 */
class MegaSyncException(
    errorCode: Int,
    errorString: String?,
    val syncError: SyncError? = null,
) : MegaException(errorCode, errorString) {
}