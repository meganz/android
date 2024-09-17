package mega.privacy.android.app.presentation.favourites.facade

import android.content.Context
import nz.mega.sdk.MegaNode

/**
 * The interface for wrapping the static method regarding Mega Utils
 */
interface MegaUtilWrapper {
    /**
     * Determine whether is online
     * @param context Context
     * @return true is online
     */
    fun isOnline(context: Context): Boolean

    /**
     * Opens an URL node.
     *
     * @param context Current context.
     * @param node    MegaNode which contains an URL to open.
     */
    fun manageURLNode(context: Context, node: MegaNode)
}