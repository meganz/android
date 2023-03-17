package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.entity.login.LoginStatus

/**
 * Login repository.
 */
interface LoginRepository {

    /**
     * Initializes megaChat API for fast login.
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

    /**
     * Initializes megaChat API for login.
     *
     */
    suspend fun initMegaChat()

    /**
     * Logs in.
     *
     * @param email Account email.
     * @param email Account password.
     * @return [LoginStatus].
     */
    fun login(email: String, password: String): Flow<LoginStatus>

    /**
     * Logs in with 2FA.
     *
     * @param email Account email.
     * @param email Account password.
     * @param pin   2FA pin.
     * @return [LoginStatus].
     */
    fun multiFactorAuthLogin(
        email: String,
        password: String,
        pin: String,
    ): Flow<LoginStatus>

    /**
     * Refreshes URLs and establish fresh connections.
     */
    suspend fun refreshMegaChatUrl()

    /**
     * Performs a fast login given a session.
     *
     * @param session Required account session.
     * @return Flow of [LoginStatus].
     */
    fun fastLoginFlow(session: String): Flow<LoginStatus>

    /**
     * Performs a fetch nodes.
     *
     * @return Flow of [FetchNodesUpdate].
     */
    fun fetchNodesFlow(): Flow<FetchNodesUpdate>

    /**
     * Monitors fetch nodes finish.
     *
     * @return Flow of Boolean.
     */
    fun monitorFetchNodesFinish(): Flow<Boolean>

    /**
     * Broadcast fetch nodes finish.
     *
     */
    suspend fun broadcastFetchNodesFinish()

    /**
     * Monitors account update.
     *
     * @return Flow of Boolean.
     */
    fun monitorAccountUpdate(): Flow<Boolean>

    /**
     * Broadcast account update.
     *
     */
    suspend fun broadcastAccountUpdate()
}