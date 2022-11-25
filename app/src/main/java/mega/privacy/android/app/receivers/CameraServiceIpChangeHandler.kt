package mega.privacy.android.app.receivers

import android.app.Application
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber
import javax.inject.Inject

/**
 * [CameraServiceIpChangeHandler] to initiate mega api for connection
 */
class CameraServiceIpChangeHandler @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val application: Application,
) {

    /**
     * Initiate mega api if required based on IP
     */
    fun start() {
        val previousIP = (application as MegaApplication).localIpAddress
        val currentIP = Util.getLocalIpAddress(application)
        Timber.d("Previous IP: %s", previousIP)
        Timber.d("Current IP: %s", currentIP)
        application.localIpAddress = currentIP
        if (currentIP != null && currentIP.isNotEmpty() && currentIP.compareTo("127.0.0.1") != 0) {
            if (previousIP == null || currentIP.compareTo(previousIP) != 0) {
                Timber.d("Reconnecting...")
                megaApi.reconnect()
            } else {
                Timber.d("Retrying pending connections...")
                megaApi.retryPendingConnections()
            }
        }
    }
}
