package mega.privacy.android.navigation

import android.content.Context

/**
 * Deeplink processor
 */
interface DeeplinkProcessor {
    /**
     * Check if the deeplink matches with the set rules
     *
     * @param deeplink The deeplink to check
     * @return True if the deeplink matches or false otherwise
     */
    fun matches(deeplink: String): Boolean


    /**
     * Execute the launching of activity after matching
     *
     * @param context [Context]
     * @param deeplink The deeplink
     */
    fun execute(context: Context, deeplink: String)
}