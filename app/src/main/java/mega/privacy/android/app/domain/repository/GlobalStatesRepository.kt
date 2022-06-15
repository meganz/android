package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.data.model.GlobalUpdate


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

    @Deprecated("See documentation for individual replacements to use instead.")
    fun monitorGlobalUpdates(): Flow<GlobalUpdate>

    /**
     * Are transfers paused (downloads and uploads)
     *
     * @return true if downloads and uploads are paused
     */
    suspend fun areTransfersPaused(): Boolean
}