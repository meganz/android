package mega.privacy.android.domain.repository

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
     * Change the API URL
     *
     * This function allows to change the API URL.
     * It's only useful for testing or debugging purposes.
     *
     * @param apiURL     New API URL
     * @param disablePkp True to disable public key pinning for this URL
     */
    suspend fun changeApiUrl(apiURL: String, disablePkp: Boolean)

}