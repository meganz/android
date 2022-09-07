package mega.privacy.android.app.receivers

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.JobUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import timber.log.Timber
import javax.inject.Inject

/**
 * Network state receiver
 *
 * @property megaChatApi
 * @property megaApi
 * @property application
 */
@AndroidEntryPoint
class NetworkStateReceiver : BroadcastReceiver() {
    private var connected: Boolean? = null

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var application: Application

    /**
     * On receive
     */
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.extras == null) return
        if (isNetworkAvailable()) {
            Timber.d("Network state: CONNECTED")
            val previousIP = (application as MegaApplication).localIpAddress
            val currentIP = Util.getLocalIpAddress(context)
            Timber.d("Previous IP: %s", previousIP)
            Timber.d("Current IP: %s", currentIP)
            (application as MegaApplication).localIpAddress = currentIP
            if (currentIP != null && currentIP.isNotEmpty() && currentIP.compareTo("127.0.0.1") != 0) {
                if (previousIP == null || currentIP.compareTo(previousIP) != 0) {
                    Timber.d("Reconnecting...")
                    megaApi.reconnect()
                    megaChatApi.retryPendingConnections(true, null)
                } else {
                    Timber.d("Retrying pending connections...")
                    megaApi.retryPendingConnections()
                    megaChatApi.retryPendingConnections(false, null)
                }
            }
            connected = true
            JobUtil.scheduleCameraUploadJob(context)
        } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
            Timber.d("Network state: DISCONNECTED")
            (application as MegaApplication).localIpAddress = null
            connected = false
        }
        handleNetworkStateChanged()
    }

    private fun handleNetworkStateChanged() {
        if (connected == true) {
            networkAvailable()
        } else {
            networkUnavailable()
        }
    }

    @Suppress("DEPRECATION")
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val capabilities: NetworkCapabilities? =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    ) {
                        return true
                    }
                }
            } else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                return activeNetworkInfo != null && activeNetworkInfo.isConnected
            }
        }
        return false
    }

    private fun networkAvailable() {
        Timber.d("Net available: Broadcast to ManagerActivity")
        val intent = Intent(Constants.BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE)
        intent.putExtra(BroadcastConstants.ACTION_TYPE, Constants.GO_ONLINE)
        application.sendBroadcast(intent)
    }

    private fun networkUnavailable() {
        Timber.d("Net unavailable: Broadcast to ManagerActivity")
        val intent = Intent(Constants.BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE)
        intent.putExtra(BroadcastConstants.ACTION_TYPE, Constants.GO_OFFLINE)
        application.sendBroadcast(intent)
    }
}