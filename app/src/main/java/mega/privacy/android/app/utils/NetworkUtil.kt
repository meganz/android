package mega.privacy.android.app.utils

import android.content.Context
import android.net.ConnectivityManager

object NetworkUtil {

    fun Context.isOnline(): Boolean =
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .activeNetworkInfo?.isConnectedOrConnecting == true

    fun Context.isMeteredConnection(): Boolean =
        (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .isActiveNetworkMetered
}
