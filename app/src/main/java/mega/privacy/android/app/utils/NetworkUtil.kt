package mega.privacy.android.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission

object NetworkUtil {

    /**
     * Indicates whether network connectivity exists and it is possible to establish connections and pass data.
     *
     * @return  true if network connectivity exists or is in the process of being established, false otherwise.
     */
    @Suppress("deprecation")
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    fun Context.isOnline(): Boolean {
        val connectivityManager = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }

    /**
     * Returns if the currently active data network is metered. A network is classified as
     * metered when the user is sensitive to heavy data usage on that connection due to
     * monetary costs, data limitations or battery/performance issues.
     *
     * @return  true if large transfers should be avoided, otherwise false.
     */
    fun Context.isMeteredConnection(): Boolean =
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .isActiveNetworkMetered
}
