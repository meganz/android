package mega.privacy.android.data.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.model.GlobalUpdate


/**
 * Global states repository
 *
 * This is a temporary repository to handle global states. Individual global states should be
 * handled through their respective repositories instead.
 *
 * Current replacements:
 * [nz.mega.sdk.MegaGlobalListenerInterface.onUsersUpdate] onUserUpdate - [AccountRepository.monitorUserUpdates]
 */
interface GlobalStatesRepository {

    /**
     * Monitor global updates
     *
     */
    @Deprecated("See documentation for individual replacements to use instead.")
    fun monitorGlobalUpdates(): Flow<GlobalUpdate>
}