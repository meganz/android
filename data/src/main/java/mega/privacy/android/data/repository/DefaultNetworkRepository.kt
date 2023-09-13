package mega.privacy.android.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    @Suppress("DEPRECATION")
    override fun getCurrentConnectivityState(): ConnectivityState =
        if (connectivityManager?.activeNetworkInfo?.isConnected == true) ConnectivityState.Connected(
            isOnWifi()
        ) else ConnectivityState.Disconnected

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
    @OptIn(FlowPreview::class)
    private val monitorConnectivity = callbackFlow {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // emit current network state every time app resumes from background
        val job = ProcessLifecycleOwner.get().lifecycleScope.launch {
            ProcessLifecycleOwner.get().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                trySend(getCurrentConnectivityState())
            }
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                super.onLost(network)
                Timber.d("onLost")
                // we still check current connectivity state to ensure getting latest value
                // I have no idea it's device specific issue
                trySend(getCurrentConnectivityState())
            }

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Timber.d("onAvailable")
                trySend(ConnectivityState.Connected(isOnWifi()))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Timber.d("onCapabilitiesChanged")
                trySend(getCurrentConnectivityState())
            }
        }
        connectivityManager?.apply {
            registerNetworkCallback(networkRequest, callback)
            registerDefaultNetworkCallback(callback)
        }

        awaitClose {
            connectivityManager?.unregisterNetworkCallback(callback)
            job.cancel()
        }
    }.flowOn(ioDispatcher)
        .debounce(150L)
        .catch { Timber.e(it, "MonitorConnectivity Exception") }
        .stateIn(
            applicationScope,
            started = SharingStarted.Lazily,
            initialValue = getCurrentConnectivityState()
        )

    override fun isConnectedToInternet() = monitorConnectivity.value.connected

    override fun setUseHttps(enabled: Boolean) = megaApi.setUseHttpsOnly(enabled)

    override fun isMeteredConnection() = connectivityManager?.isActiveNetworkMetered

    @Suppress("DEPRECATION")
    override fun isOnWifi(): Boolean {
        return connectivityManager?.getActiveNetworkCapabilities()?.let {
            return@let when {
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    true
                }

                /*
                 * On newer devices even though the VPN is connected it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) returns true.
                 * so it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) will be invoked
                 * only when it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) returns false
                 * and then we will look for whether device is disconnected to WiFi
                 * or not on older devices when it's connected to VPN
                 *  otherwise it should return false immediately for newer devices.
                 */

                it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        connectivityManager.allNetworks.any { network ->
                            connectivityManager.getNetworkCapabilities(network)
                                ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                        }
                    } else {
                        false
                    }
                }

                else -> false
            }
        } ?: false
    }

    override fun monitorChatSignalPresence(): Flow<Unit> =
        appEventGateway.monitorChatSignalPresence()

    override suspend fun broadcastChatSignalPresence() =
        appEventGateway.broadcastChatSignalPresence()
}
