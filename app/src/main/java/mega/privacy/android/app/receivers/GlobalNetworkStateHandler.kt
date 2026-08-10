package mega.privacy.android.app.receivers

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reacts to connectivity changes by reconnecting or retrying pending SDK connections, tracking
 * the device's last known local IP address to tell the two cases apart.
 *
 * @property localIpAddress the last known local IP address, null while disconnected
 */
@Singleton
class GlobalNetworkStateHandler @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val application: Application,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) {
    var localIpAddress: String? = ""

    /**
     * Starts collecting connectivity changes.
     */
    fun start() {
        applicationScope.launch {
            monitorConnectivityUseCase().collectLatest { isConnected ->
                if (isConnected) {
                    Timber.d("Network state: CONNECTED")
                    val previousIP = localIpAddress
                    val currentIP = Util.getLocalIpAddress(application)
                    Timber.d("Previous IP: %s", previousIP)
                    Timber.d("Current IP: %s", currentIP)
                    localIpAddress = currentIP
                    if (currentIP != null && currentIP.isNotEmpty() && currentIP.compareTo("127.0.0.1") != 0) {
                        if (previousIP == null || currentIP.compareTo(previousIP) != 0) {
                            Timber.d("Reconnecting...")
                            megaApi.reconnect()
                            megaChatApi.retryPendingConnections(true)
                        } else {
                            Timber.d("Retrying pending connections...")
                            megaApi.retryPendingConnections()
                            megaChatApi.retryPendingConnections(false)
                        }
                    }
                } else {
                    Timber.d("Network state: DISCONNECTED")
                    localIpAddress = null
                }
            }
        }
    }
}
