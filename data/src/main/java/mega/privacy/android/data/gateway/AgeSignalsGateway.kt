package mega.privacy.android.data.gateway

/**
 * Gateway interface for Google Play Age Signals SDK
 *
 * This abstraction allows for easy testing and decouples the repository
 * from the external SDK implementation.
 * Returns the user status directly to avoid issues with AgeSignalsResult in tests.
 */
internal interface AgeSignalsGateway {
    /**
     * Check age signals and return the user status
     *
     * @return The user status (Int? where null means unknown/unverified)
     */
    suspend fun checkAgeSignals(): Int?
}

