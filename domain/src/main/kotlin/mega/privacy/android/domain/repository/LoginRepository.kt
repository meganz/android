package mega.privacy.android.domain.repository

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
}