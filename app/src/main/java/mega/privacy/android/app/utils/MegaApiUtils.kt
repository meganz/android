package mega.privacy.android.app.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import mega.privacy.android.app.utils.TextUtil.getFolderInfo
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import timber.log.Timber

object MegaApiUtils {

    /**
     * Gets the string to show as content of a folder.
     *
     * @param node    The folder to get its string content.
     * @param megaApi The [MegaApiAndroid] instance to query the folder's children.
     * @param context Context used to format the resulting string.
     * @return The string to show as content of the folder.
     */
    @JvmStatic
    fun getMegaNodeFolderInfo(node: MegaNode, megaApi: MegaApiAndroid, context: Context): String =
        getFolderInfo(megaApi.getNumChildFolders(node), megaApi.getNumChildFiles(node), context)

    /**
     * Gets the string to show as content of a folder link.
     *
     * @param node          The folder to get its string content.
     * @param megaApiFolder The folder-link [MegaApiAndroid] instance to query the folder's children.
     * @param context       Context used to format the resulting string.
     * @return The string to show as content of the folder.
     */
    @JvmStatic
    fun getMegaNodeFolderLinkInfo(
        node: MegaNode,
        megaApiFolder: MegaApiAndroid,
        context: Context,
    ): String = getFolderInfo(
        megaApiFolder.getNumChildFolders(node),
        megaApiFolder.getNumChildFiles(node),
        context
    )

    /**
     * If there is an application that can manage the Intent, returns true. Otherwise, false.
     * @param ctx
     * @param intent
     */
    @JvmStatic
    fun isIntentAvailable(ctx: Context, intent: Intent): Boolean {
        Timber.d("isIntentAvailable")
        val mgr = ctx.packageManager
        val list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return list.isNotEmpty()
    }
}
