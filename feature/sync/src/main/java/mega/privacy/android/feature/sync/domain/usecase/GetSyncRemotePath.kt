package mega.privacy.android.feature.sync.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

/**
 * Returns the path to remote folder that the user has selected
 */
fun interface GetSyncRemotePath {

    operator fun invoke(): Flow<RemoteFolder>
}
