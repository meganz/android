package mega.privacy.android.app.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.TextUtil.getFolderInfo
import nz.mega.sdk.MegaNode
import timber.log.Timber

object MegaApiUtils {

    /**
     * Gets the string to show as content of a folder.
     *
     * @param node The folder to get its string content.
     * @return The string to show as content of the folder.
     */
    @JvmStatic
    fun getMegaNodeFolderInfo(node: MegaNode, context: Context?): String {
        val megaApi = MegaApplication.getInstance().megaApi
        return getFolderInfo(megaApi.getNumChildFolders(node), megaApi.getNumChildFiles(node), context)
    }

    /**
     * Gets the string to show as content of a folder link.
     *
     * @param node The folder to get its string content.
     * @return The string to show as content of the folder.
     */
    @JvmStatic
    fun getMegaNodeFolderLinkInfo(node: MegaNode, context: Context?): String {
        val megaApiFolder = MegaApplication.getInstance().megaApiFolder
        return getFolderInfo(
            megaApiFolder.getNumChildFolders(node),
            megaApiFolder.getNumChildFiles(node),
            context
        )
    }

    /**
     * If there is an application that can manage the Intent, returns true. Otherwise, false.
     */
    @JvmStatic
    fun isIntentAvailable(ctx: Context, intent: Intent): Boolean {
        Timber.d("isIntentAvailable")
        val mgr = ctx.packageManager
        val list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return list.isNotEmpty()
    }
}
