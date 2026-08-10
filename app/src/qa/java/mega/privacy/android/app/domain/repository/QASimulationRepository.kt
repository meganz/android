package mega.privacy.android.app.domain.repository

/**
 * QA-only repository for simulating account data expiration / purge state, used to test the
 * inactivity banner flow. Backed by the dev option attribute (USER_ATTR_DEV_OPT) and the last
 * purge acknowledged attribute (USER_ATTR_LAST_PURGE_ACKNOWLEDGED).
 */
interface QASimulationRepository {
    /**
     * Set the dev option attribute (USER_ATTR_DEV_OPT) used to simulate the account data purge
     * schedule, producing a value like
     * `{"lastpurge":[purgeTimestamp,reason,warningTimestamp,lastActiveTimestamp]}`.
     *
     * @param purgeTimestamp the time (epoch seconds) when the purge script runs.
     * @param reason the purge reason code.
     * @param warningTimestamp the time (epoch seconds) when the warning email is sent.
     * @param lastActiveTimestamp the user's last active time (epoch seconds).
     */
    suspend fun setDevOptForPurge(
        purgeTimestamp: Long,
        reason: Int,
        warningTimestamp: Long,
        lastActiveTimestamp: Long,
    )

    /**
     * Get the last acknowledged purge timestamp (USER_ATTR_LAST_PURGE_ACKNOWLEDGED).
     *
     * @return the acknowledged purge timestamp (seconds), or 0 if it has not been set.
     */
    suspend fun getLastPurgeAcknowledged(): Long
}
