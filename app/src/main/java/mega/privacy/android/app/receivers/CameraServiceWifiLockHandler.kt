package mega.privacy.android.app.receivers

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * A Handler class that performs Wifi Lock operations for Camera Uploads
 *
 * @property context
 */
class CameraServiceWifiLockHandler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var wifiLock: WifiLock? = null

    /**
     * Starts the Wifi Lock to ensure that all content uploaded through Camera Uploads will
     * be completed
     */
    fun startWifiLock() {
        val wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF
        val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager

        wifiLock = wifiManager.createWifiLock(wifiLockMode, "MegaDownloadServiceWifiLock")

        if (wifiLock?.isHeld == false) {
            wifiLock?.acquire()
            Timber.d("WifiLock has started")
        }
    }

    /**
     * Stops the Wifi Lock to prevent adversely affecting battery life
     */
    fun stopWifiLock() {
        if (wifiLock?.isHeld == true) {
            try {
                wifiLock?.release()
                Timber.d("WifiLock has stopped")
            } catch (e: Exception) {
                Timber.e("Error stopping WifiLock with Exception: $e")
            }
        }
    }
}