package mega.privacy.android.domain.entity

/**
 * Connectivity state
 *
 * @property connected
 */
sealed class ConnectivityState(val connected: Boolean) {
    /**
     *  Disconnected
     */
    object Disconnected : ConnectivityState(false)

    /**
     * Connected
     * @property isOnWifi
     */
    data class Connected(val isOnWifi: Boolean) : ConnectivityState(true)
}
