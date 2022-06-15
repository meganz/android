package mega.privacy.android.app.gallery.repository.fetcher

import android.content.Context
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList

/**
 * FavouriteAlbumFetcher helps FavouriteAlbumRepository gets data, like megaNodes.
 *
 * order: (Set in SortByBottomSheetDialogFragment, type detail in MegaApiJava)
 *     public final static int ORDER_MODIFICATION_ASC = MegaApi.ORDER_MODIFICATION_ASC;
 *     public final static int ORDER_MODIFICATION_DESC = MegaApi.ORDER_MODIFICATION_DESC;
 *     public final static int ORDER_PHOTO_DESC = MegaApi.ORDER_PHOTO_DESC;
 *     public final static int ORDER_VIDEO_ASC = MegaApi.ORDER_VIDEO_ASC;
 */
class FavouriteAlbumFetcher(
    context: Context,
    megaApi: MegaApiAndroid,
    selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
    private val order: Int,
    zoom: Int,
    private val dbHandler: DatabaseHandler,
) : GalleryBaseFetcher(
    context = context,
    megaApi = megaApi,
    selectedNodesMap = selectedNodesMap,
    zoom = zoom
) {

    override fun getNodes(cancelToken: MegaCancelToken?): List<MegaNode> {
        return getFilteredChildren(getNodesAfterSort(cancelToken))
    }

    /**
     * Get megaNodes after sort
     *
     * @return MegaNodes after sort
     */
    private fun getNodesAfterSort(cancelToken: MegaCancelToken?): List<MegaNode> {
        return when (order) {
            MegaApiJava.ORDER_PHOTO_DESC -> sortByPhotos(cancelToken = cancelToken)
            MegaApiJava.ORDER_VIDEO_DESC -> sortByVideos(cancelToken = cancelToken)
            else -> sortByModifiedTime(orderStrategy = order, cancelToken = cancelToken)
        }
    }

    /**
     * Sort by modified time
     *
     * @return MegaNodes after sort
     */
    private fun sortByModifiedTime(
        orderStrategy: Int,
        cancelToken: MegaCancelToken?,
    ): List<MegaNode> {
        val favourites = ArrayList<MegaNode>()
        val images = filterFavImagesByTimeOrder(cancelToken)
        val videos = filterFavVideosFromCU()
        //Images will be in front of videos
        favourites.addAll(images)
        favourites.addAll(videos)
        return when (orderStrategy) {
            MegaApiJava.ORDER_MODIFICATION_DESC -> favourites.sortedByDescending { it.modificationTime }
            MegaApiJava.ORDER_MODIFICATION_ASC -> favourites.sortedBy { it.modificationTime }
            else -> favourites
        }
    }

    /**
     * Sort by photos
     *
     * @return MegaNodes after sort
     */
    private fun sortByPhotos(cancelToken: MegaCancelToken?): List<MegaNode> {
        val favourites = ArrayList<MegaNode>()
        val images = filterFavImagesByPhotosOrVideos(cancelToken)
        val videos = filterFavVideosFromCU()
        //Images will be in front of videos
        favourites.addAll(images)
        favourites.addAll(videos)
        return favourites
    }

    /**
     * Sort by videos
     *
     * @return MegaNodes after sort
     */
    private fun sortByVideos(cancelToken: MegaCancelToken?): List<MegaNode> {
        val favourites = ArrayList<MegaNode>()
        val images = filterFavImagesByPhotosOrVideos(cancelToken)
        val videos = filterFavVideosFromCU()
        //Videos will be in front of images
        favourites.addAll(videos)
        favourites.addAll(images)
        return favourites
    }

    /**
     * Images are from all cloud drive
     *
     * @return MegaNodes after filter
     */
    private fun filterFavImagesByTimeOrder(cancelToken: MegaCancelToken?): List<MegaNode> {
        // Get All Image Nodes by rootNode
        val allImageNodes = megaApi.searchByType(
            cancelToken!!,
            order,
            MegaApiJava.FILE_TYPE_PHOTO,
            MegaApiJava.SEARCH_TARGET_ROOTNODE
        )
        // filter Images and favorite only
        return allImageNodes.filter { it.isFavourite }
    }

    /**
     * If parameter type is different of MegaApi::FILE_TYPE_DEFAULT, the following values for parameter
     * order are invalid: MegaApi::ORDER_PHOTO_ASC, MegaApi::ORDER_PHOTO_DESC,
     * MegaApi::ORDER_VIDEO_ASC, MegaApi::ORDER_VIDEO_DESC
     */
    private fun filterFavImagesByPhotosOrVideos(cancelToken: MegaCancelToken?): List<MegaNode> {
        // Get All Image Nodes by rootNode, sort by ORDER_MODIFICATION_DESC
        val allImageNodes = megaApi.searchByType(
            cancelToken!!,
            MegaApiJava.ORDER_MODIFICATION_DESC,
            MegaApiJava.FILE_TYPE_PHOTO,
            MegaApiJava.SEARCH_TARGET_ROOTNODE
        )
        // filter Images and favorite only
        return allImageNodes.filter { it.isFavourite }
    }

    /**
     * Videos are only from CU/MU
     */
    private fun filterFavVideosFromCU(): List<MegaNode> {
        var cuFolderNode: MegaNode? = null
        var muFolderNode: MegaNode? = null
        val pref = getDbPreferences()

        pref?.let { it ->
            // get cuFolderNode if cu handle existed
            it.camSyncHandle?.let { camSyncHandle ->
                val cuHandle = stringHandleToLong(camSyncHandle, ERROR_MSG_PARSE_CU_HANDLE)
                cuFolderNode = getNodeByHandle(cuHandle)
            }
            // get muFolderNode if mu handle existed
            it.megaHandleSecondaryFolder?.let { megaHandleSecondaryFolder ->
                val muHandle =
                    stringHandleToLong(megaHandleSecondaryFolder, ERROR_MSG_PARSE_MU_HANDLE)
                muFolderNode = getNodeByHandle(muHandle)
            }
        }
        val allCuNodes = getAllNodesFromCUAndMU(cuFolderNode, muFolderNode)
        val videos = filterCuVideos(allCuNodes)
        return filterFavVideos(videos)
    }

    /**
     * get DB Preferences
     */
    private fun getDbPreferences(): MegaPreferences? = dbHandler.preferences

    /**
     * Get all the nodes from CU and MU folders
     *
     * @param cuFolderNode this may be null
     * @param muFolderNode this may be null
     */
    private fun getAllNodesFromCUAndMU(
        cuFolderNode: MegaNode?,
        muFolderNode: MegaNode?,
    ): List<MegaNode> {
        if (!hasNode(cuFolderNode) && !hasNode(muFolderNode)) {
            return emptyList()
        }

        val nodeList = MegaNodeList.createInstance()

        if (hasNode(cuFolderNode)) {
            nodeList.addNode(cuFolderNode)
        }

        if (hasNode(muFolderNode)) {
            nodeList.addNode(muFolderNode)
        }

        // Get all nodes from CU and MU
        return megaApi.getChildren(nodeList, order)
    }

    /**
     * Filter videos from cu nodes (CU folder and MU folder)
     */
    private fun filterCuVideos(allCuNodes: List<MegaNode>): List<MegaNode> =
        allCuNodes.filter { MimeTypeList.typeForName(it.name).isVideoReproducible }

    /**
     * Filter favorite videos
     */
    private fun filterFavVideos(videos: List<MegaNode>) =
        videos.filter { it.isFavourite }

    /**
     * Check the node is null
     *
     * @return True Not null. False, null
     */
    private fun hasNode(node: MegaNode?) = node != null

    /**
     * String handle to long handle
     *
     * @return handle with long type
     */
    private fun stringHandleToLong(
        stringHandle: String,
        errorMsg: String = "parse handle error",
    ): Long {
        var longHandle = 0L
        try {
            longHandle = stringHandle.toLong()
        } catch (e: NumberFormatException) {
            LogUtil.logError(errorMsg + e.toString())
        }
        return longHandle
    }

    /**
     * Get the MegaNode that has a specific handle.
     * <p>
     * You can get the handle of a MegaNode using MegaNode.getHandle(). The same handle
     * can be got in a Base64-encoded string using MegaNode.getBase64Handle(). Conversions
     * between these formats can be done using MegaApiJava.base64ToHandle() and MegaApiJava.handleToBase64().
     *
     * @param handle Node handle to check.
     * @return MegaNode object with the handle, otherwise null.
     */
    private fun getNodeByHandle(handle: Long): MegaNode? {
        return megaApi.getNodeByHandle(handle)
    }

    companion object {
        private const val ERROR_MSG_PARSE_CU_HANDLE = "parse getCamSyncHandle error "
        private const val ERROR_MSG_PARSE_MU_HANDLE = "parse MegaHandleSecondaryFolder error "
    }
}