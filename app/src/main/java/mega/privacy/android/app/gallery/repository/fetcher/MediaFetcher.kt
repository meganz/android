package mega.privacy.android.app.gallery.repository.fetcher

import android.content.Context
import mega.privacy.android.app.gallery.data.GalleryItem
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import java.util.*

class MediaFetcher(
        context: Context,
        megaApi: MegaApiAndroid,
        selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
        private val order: Int,
        zoom: Int,
        private val handle: Long
) : GalleryBaseFetcher(
        context = context,
        megaApi = megaApi,
        selectedNodesMap = selectedNodesMap,
        zoom = zoom
) {

    companion object {
        @JvmStatic
        var cachedResults : List<MegaNode>? = null
    }

    override fun getNodes(cancelToken: MegaCancelToken?): List<MegaNode> {
        cachedResults = getFilteredChildren(getChildren())
        return cachedResults!!
    }

    private fun getChildren(): List<MegaNode> {
        val nodes = megaApi.getNodeByHandle(handle)

        val nodeList = MegaNodeList.createInstance()
        nodeList.addNode(nodes)

        return megaApi.getChildren(nodeList, order)
    }
}