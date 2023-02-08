package mega.privacy.android.domain.usecase.sync

import kotlinx.coroutines.flow.Flow

/**
 * Returns the path to local folder that the user has selected
 */
fun interface GetSyncLocalPath {

    operator fun invoke(): Flow<String>
}
