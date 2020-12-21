package mega.privacy.android.app.middlelayer

import mega.privacy.android.app.BuildConfig

/**
 * Helper class used to check build flavor in runtime.
 */
object BuildFlavorHelper {

    private const val GMS = "gms"
    private const val HMS = "hms"

    fun isGMS() = GMS == BuildConfig.FLAVOR

    fun isHMS() = HMS == BuildConfig.FLAVOR
}