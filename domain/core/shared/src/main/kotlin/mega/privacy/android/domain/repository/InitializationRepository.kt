package mega.privacy.android.domain.repository

interface InitializationRepository {
    /**
     * Initializes the global request listener.
     */
    suspend fun initializeGlobalRequestListener()
}