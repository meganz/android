package mega.privacy.android.app.repo

import mega.privacy.android.app.*
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.SortUtil.*
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class MegaNodeRepo @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val dbHandler: DatabaseHandler
) {

    /**
     * Get children of CU/MU, with the given order. All nodes, including folders.
     *
     * @param orderBy Order.
     * @return List of nodes containing CU/MU children.
     */
    private fun getCuChildren(orderBy: Int): List<MegaNode> {
        var cuNode: MegaNode? = null
        var muNode: MegaNode? = null
        val pref = dbHandler.preferences

        if (pref?.camSyncHandle != null) {
            try {
                val cuHandle = pref.camSyncHandle.toLong()
                cuNode = megaApi.getNodeByHandle(cuHandle)
            } catch (e: NumberFormatException) {
                Timber.e(e, "parse getCamSyncHandle error")
            }
        }

        if (pref?.megaHandleSecondaryFolder != null) {
            try {
                val muHandle = pref.megaHandleSecondaryFolder.toLong()
                muNode = megaApi.getNodeByHandle(muHandle)
            } catch (e: NumberFormatException) {
                Timber.e(e, "parse MegaHandleSecondaryFolder error")
            }
        }

        if (cuNode == null && muNode == null) {
            return emptyList()
        }

        val nodeList = MegaNodeList.createInstance()

        if (cuNode != null) {
            nodeList.addNode(cuNode)
        }

        if (muNode != null) {
            nodeList.addNode(muNode)
        }

        return megaApi.getChildren(nodeList, orderBy)
    }

    /**
     * Get children of CU/MU, with the given order. Only images and reproducible videos.
     *
     * @param orderBy Order.
     * @return List of nodes containing CU/MU children.
     */
    fun getFilteredCuChildren(
        orderBy: Int
    ): List<MegaNode> {
        val children = getCuChildren(orderBy)
        val nodes = ArrayList<MegaNode>()

        for (node in children) {
            if (megaApi.isInRubbish(node))
                continue

            if (node.isFolder) {
                continue
            }

            val mime = MimeTypeList.typeForName(node.name)
            if (mime.isImage || mime.isVideoReproducible) {
                nodes.add(node)
            }
        }

        return nodes
    }

    fun findOfflineNode(handle: String): MegaOffline? {
        return dbHandler.findByHandle(handle)
    }

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

            if (node.name.lowercase(Locale.ROOT).contains(query.lowercase(Locale.ROOT)) &&
                FileUtil.isFileAvailable(getOfflineFile(MegaApplication.getInstance(), node))
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
