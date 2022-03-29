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
     * Determine the current node whether is available offline
     * @param context Context
     * @param node current node
     * @return true is available offline
     */
    fun availableOffline(context: Context, node: MegaNode): Boolean

    /**
     * Opens an URL node.
     *
     * @param context Current context.
     * @param node    MegaNode which contains an URL to open.
     */
    fun manageURLNode(context: Context, node: MegaNode)
}