package mega.privacy.android.app.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.utils.FileUtils
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.SortUtil.sortOfflineByModificationDateAscending
import mega.privacy.android.app.utils.SortUtil.sortOfflineByModificationDateDescending
import mega.privacy.android.app.utils.SortUtil.sortOfflineByNameAscending
import mega.privacy.android.app.utils.SortUtil.sortOfflineByNameDescending
import mega.privacy.android.app.utils.SortUtil.sortOfflineBySizeAscending
import mega.privacy.android.app.utils.SortUtil.sortOfflineBySizeDescending
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_DESC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_ASC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import nz.mega.sdk.MegaApiJava.ORDER_SIZE_ASC
import nz.mega.sdk.MegaApiJava.ORDER_SIZE_DESC
import java.io.File
import java.util.Locale
import javax.inject.Inject

class MegaNodeRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dbHandler: DatabaseHandler
) {
    fun loadOfflineNodes(path: String, order: Int, searchQuery: String?): List<MegaOffline> {
        val nodes = if (searchQuery != null && searchQuery.isNotEmpty()) {
            searchOfflineNodes(path, searchQuery)
        } else {
            dbHandler.findByPath(path)
        }

        when (order) {
            ORDER_DEFAULT_DESC -> {
                sortOfflineByNameDescending(nodes)
            }
            ORDER_DEFAULT_ASC -> {
                sortOfflineByNameAscending(nodes)
            }
            ORDER_MODIFICATION_ASC -> {
                sortOfflineByModificationDateAscending(nodes)
            }
            ORDER_MODIFICATION_DESC -> {
                sortOfflineByModificationDateDescending(nodes)
            }
            ORDER_SIZE_ASC -> {
                sortOfflineBySizeAscending(nodes)
            }
            ORDER_SIZE_DESC -> {
                sortOfflineBySizeDescending(nodes)
            }
            else -> {
            }
        }
        return nodes
    }

    private fun searchOfflineNodes(path: String, query: String): ArrayList<MegaOffline> {
        val result = ArrayList<MegaOffline>()

        val nodes = dbHandler.findByPath(path)
        for (node in nodes) {
            if (node.isFolder) {
                result.addAll(searchOfflineNodes(getChildPath(node), query))
            }

            if (node.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)) &&
                FileUtils.isFileAvailable(getOfflineFile(context, node))
            ) {
                result.add(node)
            }
        }

        return result
    }

    private fun getChildPath(offline: MegaOffline): String {
        return if (offline.path.endsWith(File.separator)) {
            offline.path + offline.name + File.separator
        } else {
            offline.path + File.separator + offline.name + File.separator
        }
    }
}
