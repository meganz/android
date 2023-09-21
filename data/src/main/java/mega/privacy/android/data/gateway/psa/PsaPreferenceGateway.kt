package mega.privacy.android.data.gateway.psa

/**
 * Psa preference gateway
 */
interface PsaPreferenceGateway {

    /**
     * Set last requested time
     *
     * @param time
     */
    suspend fun setLastRequestedTime(time: Long?)

    /**
     * Get last requested time
     *
     * @return time or null
     */
    suspend fun getLastRequestedTime(): Long?
}
