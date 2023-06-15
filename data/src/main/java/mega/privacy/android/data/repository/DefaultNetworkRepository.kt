package mega.privacy.android.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.NetworkRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default network repository implementation
 *
 * @property context
 * @property megaApi
 */
@Singleton
internal class DefaultNetworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApi: MegaApiGateway,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val appEventGateway: AppEventGateway,
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

    private fun ConnectivityManager?.getActiveNetworkCapabilities(): NetworkCapabilities? =
        this?.activeNetwork?.let { network ->
            try {
                getNetworkCapabilities(network)
            } catch (ignore: SecurityException) {
                Timber.w(ignore)
                null
            }
        }

    override fun monitorConnectivityChanges(): Flow<ConnectivityState> = monitorConnectivity

    // https://developer.android.com/training/basics/network-ops/reading-network-state#listening-events
    // Note: There is a limit to the number of callbacks that can be registered concurrently, so unregister callbacks once they are no longer needed so that your app can register more.
    // we can create single callback and share state in our application
    private val monitorConnectivity = callbackFlow {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                super.onLost(network)
                Timber.d("onLost")
                trySend(ConnectivityState.Disconnected)
            }

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Timber.d("onAvailable")
                trySend(getCurrentConnectivityState())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Timber.d("onCapabilitiesChanged")
                trySend(
                    ConnectivityState.Connected(
                        meteredConnection = !networkCapabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                        )
                    )
                )
            }
        }
        connectivityManager?.registerNetworkCallback(networkRequest, callback)

        awaitClose { connectivityManager?.unregisterNetworkCallback(callback) }
    }.flowOn(ioDispatcher)
        .catch { Timber.e(it, "MonitorConnectivity Exception") }
        .shareIn(applicationScope, SharingStarted.Lazily)

    override fun setUseHttps(enabled: Boolean) = megaApi.setUseHttpsOnly(enabled)

    override fun isMeteredConnection() = connectivityManager?.isActiveNetworkMetered

    override fun isOnWifi(): Boolean {
        connectivityManager ?: return false
        val capabilities = connectivityManager.getActiveNetworkCapabilities()
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun monitorChatSignalPresence(): Flow<Unit> =
        appEventGateway.monitorChatSignalPresence()

    override suspend fun broadcastChatSignalPresence() =
        appEventGateway.broadcastChatSignalPresence()
}
