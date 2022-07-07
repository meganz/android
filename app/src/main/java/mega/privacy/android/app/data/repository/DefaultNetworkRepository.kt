package mega.privacy.android.app.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.data.gateway.MonitorNetworkConnectivityChange
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Default network repository implementation
 *
 * @property context
 * @property monitorNetworkConnectivityChange
 * @property megaApi
 */
class DefaultNetworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val monitorNetworkConnectivityChange: MonitorNetworkConnectivityChange,
    private val megaApi: MegaApiGateway,
) : NetworkRepository {

    private val connectivityManager = getSystemService(context, ConnectivityManager::class.java)

    override fun getCurrentConnectivityState() =
        connectivityManager.getActiveNetworkCapabilities()
            ?.takeIf { it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) }
            ?.let {
                ConnectivityState.Connected(
                    meteredConnection = !it.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                    )
                )
            } ?: ConnectivityState.Disconnected

    private fun ConnectivityManager?.getActiveNetworkCapabilities() =
        this?.activeNetwork?.let {
            getNetworkCapabilities(it)
        }

    override fun monitorConnectivityChanges(): Flow<ConnectivityState> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            monitorConnectivitySDK26()
        } else {
            monitorConnectivity()
        }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun monitorConnectivitySDK26(): Flow<ConnectivityState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(ConnectivityState.Disconnected)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                trySend(
                    ConnectivityState.Connected(
                        meteredConnection = !networkCapabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                        )
                    )
                )
            }
        }
        connectivityManager?.registerDefaultNetworkCallback(callback)

        awaitClose { connectivityManager?.unregisterNetworkCallback(callback) }
    }

    private fun monitorConnectivity(): Flow<ConnectivityState> =
        monitorNetworkConnectivityChange.getEvents().map { connected ->
            val metered = connectivityManager?.isActiveNetworkMetered
            if (connected && metered != null) {
                ConnectivityState.Connected(metered)
            } else {
                ConnectivityState.Disconnected
            }
        }

    override fun setUseHttps(enabled: Boolean) = megaApi.setUseHttpsOnly(enabled)
}
