package mega.privacy.android.app.gallery.repository.fetcher

import android.content.Context
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import java.util.*

class PhotosFetcher(
    context: Context,
    megaApi: MegaApiAndroid,
    selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
    private val order: Int,
    zoom: Int,
    private val dbHandler: DatabaseHandler
) : GalleryBaseFetcher(
        context = context,
        megaApi = megaApi,
        selectedNodesMap = selectedNodesMap,
        zoom = zoom
) {
    override fun getNodes(cancelToken: MegaCancelToken?): List<MegaNode> = getFilteredChildren(getCuChildren())

    private fun getCuChildren(): List<MegaNode> {
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

        return megaApi.getChildren(nodeList, order)
    }
}
