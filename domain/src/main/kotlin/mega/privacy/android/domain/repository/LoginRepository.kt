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

    /**
     * Checks if there is a login already running.
     *
     * @return True if there is a login already running, false otherwise.
     */
    fun isLoginAlreadyRunning(): Boolean

    /**
     * Sets isLoggingIn flag to true for starting the login process and not allowing a new one
     * while this is in progress.
     */
    fun startLoginProcess()

    /**
     * Sets isLoggingIn flag to false for finishing the login process and allowing a new one
     * when required.
     */
    fun finishLoginProcess()
}