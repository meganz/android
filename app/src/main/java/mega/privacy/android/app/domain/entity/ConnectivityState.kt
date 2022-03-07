package mega.privacy.android.app.domain.entity

/**
 * Connectivity state
 *
 * @property connected
 */
sealed class ConnectivityState(val connected: Boolean){
    object Disconnected : ConnectivityState(false)

    /**
     * Connected
     *
     * @property meteredConnection
     */
    data class Connected(val meteredConnection: Boolean) : ConnectivityState(true)
}
