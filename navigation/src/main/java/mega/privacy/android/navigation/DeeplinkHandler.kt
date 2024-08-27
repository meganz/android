package mega.privacy.android.navigation

import android.content.Context

/**
 * Handler for deeplink processor
 */
interface DeeplinkHandler {

    /**
     * Check if the deeplink matches with any of the deeplink processors
     *
     * @param deeplink The deeplink to check
     * @return True if the deeplink matches or false otherwise
     */
    fun matches(deeplink: String): Boolean

    /**
     * Execute the deeplink processor
     *
     * @param context [Context]
     * @param deeplink The deeplink to process
     * @return True if the deeplink has been processed or False otherwise
     */
    fun process(context: Context, deeplink: String): Boolean
}