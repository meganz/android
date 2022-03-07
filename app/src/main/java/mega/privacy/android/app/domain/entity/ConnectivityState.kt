package mega.privacy.android.app.domain.entity

sealed class ConnectivityState(val connected: Boolean){
    object Disconnected : ConnectivityState(false)
    data class Connected(val meteredConnection: Boolean) : ConnectivityState(true)
}
