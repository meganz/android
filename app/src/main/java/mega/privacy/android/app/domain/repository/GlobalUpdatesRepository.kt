package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.data.model.GlobalUpdate


/**
 * Global updates repository
 *
 * This is a temporary repository to handle global updates. Individual global updates should be
 * handled through their respective repositories instead.
 *
 * Current replacements:
 * [nz.mega.sdk.MegaGlobalListenerInterface.onUsersUpdate] onUserUpdate - [AccountRepository.monitorUserUpdates]
 */
interface GlobalUpdatesRepository {

    @Deprecated("See documentation for individual replacements to use instead.")
    fun monitorGlobalUpdates(): Flow<GlobalUpdate>
}