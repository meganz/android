package mega.privacy.android.app.gallery.repository

import android.content.Context
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import java.util.*

class GalleryPhotosFetcher(
    context: Context,
    private val megaApi: MegaApiAndroid,
    selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
    zoom: Int,
    private val dbHandler: DatabaseHandler
) : GalleryNodeFetcher(
    context = context,
    megaApi = megaApi,
    selectedNodesMap = selectedNodesMap,
    zoom = zoom
) {


    override fun getMegaNodes(order: Int, type: Int): List<MegaNode> =
        getFilteredCuChildren(order)

    private fun getCuChildren(orderBy: Int): List<MegaNode> {
        var cuNode: MegaNode? = null
        var muNode: MegaNode? = null
        val pref = dbHandler.preferences

        if (pref?.camSyncHandle != null) {
            try {
                val cuHandle = pref.camSyncHandle.toLong()
                cuNode = megaApi.getNodeByHandle(cuHandle)
            } catch (e: NumberFormatException) {
                LogUtil.logError("parse getCamSyncHandle error $e")
            }
        }

        if (pref?.megaHandleSecondaryFolder != null) {
            try {
                val muHandle = pref.megaHandleSecondaryFolder.toLong()
                muNode = megaApi.getNodeByHandle(muHandle)
            } catch (e: NumberFormatException) {
                LogUtil.logError("parse MegaHandleSecondaryFolder error $e")
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

    private fun getFilteredCuChildren(
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

            val mime = MimeTypeThumbnail.typeForName(node.name)
            if (mime.isImage || mime.isVideoReproducible) {
                // when not in search mode, index used by viewer is index in all siblings,
                // including non image/video nodes
                nodes.add(node)
            }
        }
        return nodes
    }
}