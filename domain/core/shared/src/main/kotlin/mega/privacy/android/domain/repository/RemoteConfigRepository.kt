package mega.privacy.android.domain.repository

/**
 * Repository for Firebase Remote Config
 */
interface RemoteConfigRepository {

    /**
     * Fetch the latest remote config values and activate them
     *
     * @param useMinimalFetchInterval If true, bypass the default fetch throttling
     * so consecutive fetches hit the server (intended for debug/QA builds only)
     * @return true if the fetched values were activated for this app instance
     */
    suspend fun fetchAndActivate(useMinimalFetchInterval: Boolean = false): Boolean
}
