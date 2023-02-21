package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Login repository.
 */
interface LoginRepository {

    /**
     * Initializes megaChat API.
     *
     * @param session Required account session.
     */
    suspend fun initMegaChat(session: String)

    /**
     * Performs a fast login given a session.
     *
     * @param session Required account session.
     */
    suspend fun fastLogin(session: String)

    /**
     * Performs a fetch nodes.
     */
    suspend fun fetchNodes()

    /**
     * Monitors logout.
     *
     * @return Flow of Boolean.
     */
    fun monitorLogout(): Flow<Boolean>

    /**
     * Broadcast logout.
     *
     */
    suspend fun broadcastLogout()

    /**
     * Logouts of the MEGA account without invalidating the session.
     */
    suspend fun localLogout()

    /**
     * Logs out.
     */
    suspend fun logout()

    /**
     * Chat log out.
     */
    suspend fun chatLogout()

    /**
     * Monitor Finish Activity
     *
     * @return Flow of Boolean.
     */
    fun monitorFinishActivity(): Flow<Boolean>

    /**
     * Broadcast Finish Activity
     *
     */
    suspend fun broadcastFinishActivity()
}