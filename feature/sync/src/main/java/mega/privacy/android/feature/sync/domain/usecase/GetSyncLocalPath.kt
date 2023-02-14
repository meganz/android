package mega.privacy.android.feature.sync.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Returns the path to local folder that the user has selected
 */
fun interface GetSyncLocalPath {

    operator fun invoke(): Flow<String>
}
