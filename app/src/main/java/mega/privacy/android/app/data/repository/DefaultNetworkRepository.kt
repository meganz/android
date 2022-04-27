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
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.entity.ConnectivityState
import mega.privacy.android.app.domain.repository.NetworkRepository
import nz.mega.sdk.MegaApiAndroid
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
    @MegaApi private val megaApi: MegaApiAndroid,
) : NetworkRepository {

    private val connectivityManager = getSystemService(context, ConnectivityManager::class.java)

    override fun getCurrentConnectivityState(): ConnectivityState {
        val activeNetwork =
            connectivityManager?.activeNetwork ?: return ConnectivityState.Disconnected
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return ConnectivityState.Disconnected
        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            ConnectivityState.Connected(
                meteredConnection = !capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                )
            )
        } else {
            ConnectivityState.Disconnected
        }
    }

    override fun monitorConnectivityChanges(): Flow<ConnectivityState> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            monitorConnectivitySDK26()
        } else {
            monitorConnectivity()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun monitorConnectivitySDK26(): Flow<ConnectivityState> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    super.onLost(network)
                    trySend(ConnectivityState.Disconnected)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
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
    }

    private fun monitorConnectivity(): Flow<ConnectivityState> {
        return monitorNetworkConnectivityChange.getEvents().map { connected ->
            val metered = connectivityManager?.isActiveNetworkMetered
            if (connected && metered != null) {
                ConnectivityState.Connected(metered)
            } else {
                ConnectivityState.Disconnected
            }
        }
    }

    override fun setUseHttps(enabled: Boolean) {
        megaApi.useHttpsOnly(enabled)
    }
}
