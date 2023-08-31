package mega.privacy.android.domain.entity

/**
 * Connectivity state
 *
 * @property connected
 */
sealed class ConnectivityState(val connected: Boolean) {
    object Disconnected : ConnectivityState(false)

    /**
     * Connected
     *
     */
    object Connected : ConnectivityState(true)
}
