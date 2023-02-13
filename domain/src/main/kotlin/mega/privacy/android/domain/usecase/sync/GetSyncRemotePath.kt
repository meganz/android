package mega.privacy.android.domain.usecase.sync

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.sync.RemoteFolder

/**
 * Returns the path to remote folder that the user has selected
 */
fun interface GetSyncRemotePath {

    operator fun invoke(): Flow<RemoteFolder>
}
