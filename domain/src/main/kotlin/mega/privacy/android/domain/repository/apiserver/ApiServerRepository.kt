package mega.privacy.android.domain.repository.apiserver

import mega.privacy.android.domain.entity.apiserver.ApiServer

/**
 * Api server repository
 */
interface ApiServerRepository {

    /**
     * Enable / disable the public key pinning
     *
     * Public key pinning is enabled by default for all sensible communications.
     * It is strongly discouraged to disable this feature.
     *
     * @param enable True to keep public key pinning enabled, false to disable it
     */
    suspend fun setPublicKeyPinning(enable: Boolean)

    /**
     * Change the API
     *
     * This function allows to change the API URL.
     * It's only useful for testing or debugging purposes.
     *
     * @param apiURL     New API URL
     * @param disablePkp True to disable public key pinning for this URL
     */
    suspend fun changeApi(apiURL: String, disablePkp: Boolean)

    /**
     * Get current api
     *
     * @return [ApiServer]
     */
    suspend fun getCurrentApi(): ApiServer

    /**
     * Set new api
     *
     * @param apiServer [ApiServer]
     */
    suspend fun setNewApi(apiServer: ApiServer)
}