package mega.privacy.android.data.gateway

/**
 * Gateway for Firebase Remote Config
 */
internal interface RemoteConfigGateway {

    /**
     * Set the minimum interval between fetches from the remote server
     *
     * @param intervalInSeconds Minimum interval in seconds
     */
    suspend fun setMinimumFetchInterval(intervalInSeconds: Long)

    /**
     * Fetch the latest values from the remote server and activate them
     *
     * @return true if the fetched values were activated for this app instance
     */
    suspend fun fetchAndActivate(): Boolean

    /**
     * Get a boolean parameter value
     *
     * @return the value, or null if no remotely fetched value exists for [key]
     */
    fun getBoolean(key: String): Boolean?

    /**
     * Get a string parameter value
     *
     * @return the value, or null if no remotely fetched value exists for [key]
     */
    fun getString(key: String): String?

    /**
     * Get a long parameter value
     *
     * @return the value, or null if no remotely fetched value exists for [key]
     */
    fun getLong(key: String): Long?
}
