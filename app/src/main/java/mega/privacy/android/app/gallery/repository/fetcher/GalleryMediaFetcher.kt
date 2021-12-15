package mega.privacy.android.app.gallery.repository.fetcher

import android.content.Context
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import java.util.*

class GalleryMediaFetcher(
    context: Context,
    private val megaApi: MegaApiAndroid,
    selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
    zoom: Int,
    private val dbHandler: DatabaseHandler,
    private val handle:Long
) : GalleryNodeFetcher(
    context = context,
    megaApi = megaApi,
    selectedNodesMap = selectedNodesMap,
    zoom = zoom
) {


    override fun getMegaNodes(order: Int, type: Int): List<MegaNode> =
        getFilteredChildren(order)

    private fun getChildren(orderBy: Int): List<MegaNode> {
        val nodes = megaApi.getNodeByHandle(handle)

        val nodeList = MegaNodeList.createInstance()
        nodeList.addNode(nodes)

        return megaApi.getChildren(nodeList, orderBy)
    }

    private fun getFilteredChildren(
        orderBy: Int
    ): List<MegaNode> {
        val children = getChildren(orderBy)
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