package mega.privacy.android.app.gallery.repository.fetcher

import android.content.Context
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaApiJava.ORDER_PHOTO_DESC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_ASC
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import java.util.*

class NewPhotosFetcher(
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
    override fun getNodes(cancelToken: MegaCancelToken?): List<MegaNode> {
        // Getting CU/MU videos
        val cuItems = getVideosFromCUMU()

        // Getting Images files, this will include CU/MU
        val allImages: List<MegaNode> = megaApi.searchByType(
            cancelToken!!,
            ORDER_MODIFICATION_DESC,
            MegaApiJava.FILE_TYPE_PHOTO,
            MegaApiJava.SEARCH_TARGET_ROOTNODE
        ).filter { MimeTypeList.typeForName(it.name).isImage }

        val allItems = if (order == ORDER_PHOTO_DESC) {
            allImages + cuItems
        } else {
            cuItems + allImages
        }

        return when (order) {
            ORDER_MODIFICATION_ASC -> allItems.sortedBy {
                it.modificationTime
            }
            ORDER_MODIFICATION_DESC -> allItems.sortedByDescending {
                it.modificationTime
            }
            else -> allItems
        }
    }

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

        return megaApi.getChildren(
            nodeList,
            ORDER_MODIFICATION_DESC
        )
    }

    /**
     * Get Videos that are only from CU/MU
     */
    private fun getVideosFromCUMU(): List<MegaNode> {
        return getCuChildren().filter { MimeTypeList.typeForName(it.name).isVideoReproducible }
    }
}
