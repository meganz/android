package mega.privacy.android.app.utils

import android.content.Context
import android.net.ConnectivityManager

/**
 * Network util
 *
 */
object NetworkUtil {
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
