package mega.privacy.android.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
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
import kotlinx.coroutines.withContext
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
 * Don't migrate deprecated activeNetworkInfo to activeNetwork because it can detect VPN
 * it's reference to
 * https://github.com/androidx/androidx/blob/androidx-main/work/work-runtime/src/main/java/androidx/work/impl/constraints/trackers/NetworkStateTracker.kt
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
    override suspend fun getCurrentConnectivityState(): ConnectivityState =
        withContext(ioDispatcher) {
            getCurrentConnectivityStateInternal()
        }

    @Suppress("DEPRECATION")
    private fun getCurrentConnectivityStateInternal(): ConnectivityState =
        if (connectivityManager?.activeNetworkInfo?.isConnected == true) ConnectivityState.Connected(
            isOnWifiInternal()
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
        // emit current network state every time app resumes from background
        val job = ProcessLifecycleOwner.get().lifecycleScope.launch {
            ProcessLifecycleOwner.get().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                trySend(getCurrentConnectivityStateInternal())
            }
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                super.onLost(network)
                Timber.d("onLost")
                trySend(getCurrentConnectivityStateInternal())
            }

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Timber.d("onAvailable")
                trySend(ConnectivityState.Connected(isOnWifiInternal()))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Timber.d("onCapabilitiesChanged")
                trySend(getCurrentConnectivityStateInternal())
            }
        }
        val handlerThread = HandlerThread("NetworkCallbackThread").apply {
            start()
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, throwable ->
                Timber.e(
                    throwable,
                    "NetworkCallback handler thread crashed due to framework parcel error"
                )
            }
        }
        connectivityManager?.registerDefaultNetworkCallback(
            callback,
            Handler(handlerThread.looper)
        )

        awaitClose {
            connectivityManager?.unregisterNetworkCallback(callback)
            handlerThread.quitSafely()
            job.cancel()
        }
    }.flowOn(ioDispatcher)
        .debounce(150L)
        .catch { Timber.e(it, "MonitorConnectivity Exception") }
        .stateIn(
            applicationScope,
            started = SharingStarted.Lazily,
            initialValue = getCurrentConnectivityStateInternal()
        )

    override fun isConnectedToInternet() = monitorConnectivity.value.connected

    override suspend fun isMeteredConnection() = withContext(ioDispatcher) {
        connectivityManager?.isActiveNetworkMetered
    }

    @Suppress("DEPRECATION")
    override suspend fun isOnWifi(): Boolean = withContext(ioDispatcher) {
        isOnWifiInternal()
    }

    @Suppress("DEPRECATION")
    private fun isOnWifiInternal(): Boolean {
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

    override suspend fun broadcastSslVerificationFailed() {
        appEventGateway.broadcastSslVerificationFailed()
    }

    override fun monitorSslVerificationFailed(): Flow<Unit> =
        appEventGateway.monitorSslVerificationFailed()
}
