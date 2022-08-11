package mega.privacy.android.app.gallery.repository.fetcher

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.delay
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.MediaCardType
import mega.privacy.android.app.gallery.extension.formatDateTitle
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ZoomUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    private val order: Int,
    private val dbHandler: DatabaseHandler,
    private val context: Context,
    private val megaApi: MegaApiAndroid,
    private val selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
    private val zoom: Int,
    private val type: Int = MegaApiJava.FILE_TYPE_DEFAULT,
) {

    fun getNodes(cancelToken: MegaCancelToken?): List<MegaNode> {
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
            Timber.e(e, errorMsg)
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

    private val getPreviewNodes = mutableMapOf<MegaNode, String>()

    /**
     * Throttle for updating the LiveData
     */
    private fun refreshLiveData() {
        if (waitingForRefresh) return
        waitingForRefresh = true

        Handler(Looper.getMainLooper()).postDelayed(
            {
                waitingForRefresh = false
                result.postValue(ArrayList(fileNodesMap.values))
            }, UPDATE_DATA_THROTTLE_TIME
        )
    }

    private fun getPreviewFile(node: MegaNode) = File(
        previewFolder,
        node.base64Handle.plus(FileUtil.JPG_EXTENSION)
    )

    /**
     * Get the preview of the file.
     */
    private fun getPreview(node: MegaNode): File? {
        val previewFile = getPreviewFile(node)

        return if (previewFile.exists()) {
            previewFile
        } else {
            // Note down the nodes and going to get their previews from the server
            // as soon as the getGalleryItems finished. (Don't start the getting operation here
            // for avoiding potential ConcurrentModification issue)
            if (node.hasPreview()) {
                getPreviewNodes[node] = previewFile.absolutePath
            }

            null
        }
    }

    /**
     * Get all nodes items.
     *
     * @param cancelToken   MegaCancelToken to cancel the search at any time.
     */
    suspend fun getGalleryItems(cancelToken: MegaCancelToken) {
        var lastYearDate: LocalDate? = null
        var lastMonthDate: LocalDate? = null
        var lastDayDate: LocalDate? = null

        for (node in getNodes(cancelToken)) {
            val thumbnail = if (zoom == ZoomUtil.ZOOM_IN_1X) {
                getPreview(node)
            } else {
                getThumbnail(node)
            }

            val modifyDate = Util.fromEpoch(node.modificationTime)
            val dateString = SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(
                Date.from(modifyDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
            )
            val sameYear = Year.from(LocalDate.now()) == Year.from(modifyDate)

            // Photo "Month-Year" section headers
            when (zoom) {
                ZoomUtil.ZOOM_OUT_2X -> {
                    if (lastYearDate == null || Year.from(lastYearDate) != Year.from(modifyDate)) {
                        lastYearDate = modifyDate
                        addPhotoDateTitle(
                            dateString,
                            Pair(DateTimeFormatter.ofPattern("uuuu").format(modifyDate), "")
                        )
                    }
                }
                ZoomUtil.ZOOM_IN_1X -> {
                    if (lastDayDate == null || lastDayDate.dayOfYear != modifyDate.dayOfYear) {
                        lastDayDate = modifyDate

                        addPhotoDateTitle(
                            dateString, Pair(
                                DateTimeFormatter.ofPattern("dd MMMM").format(modifyDate),
                                if (sameYear) "" else DateTimeFormatter.ofPattern("uuuu")
                                    .format(modifyDate)
                            )
                        )
                    }
                }
                else -> {
                    if (lastMonthDate == null || YearMonth.from(lastMonthDate) != YearMonth.from(
                            modifyDate
                        )
                    ) {
                        lastMonthDate = modifyDate
                        addPhotoDateTitle(
                            dateString, Pair(
                                SimpleDateFormat("LLLL", Locale.getDefault()).format(
                                    Date.from(modifyDate.atStartOfDay()
                                        .atZone(ZoneId.systemDefault()).toInstant())
                                ),
                                if (sameYear) "" else DateTimeFormatter.ofPattern("uuuu")
                                    .format(modifyDate)
                            )
                        )
                    }
                }
            }

            val selected = selectedNodesMap[node.handle]?.selected ?: false
            val galleryItem = GalleryItem(
                node,
                Constants.INVALID_POSITION,
                Constants.INVALID_POSITION,
                thumbnail,
                if (node.duration == -1) MediaCardType.Image else MediaCardType.Video,
                dateString,
                null,
                null,
                selected,
                true
            )
            fileNodesMap[node.handle] = galleryItem
        }

        result.postValue(ArrayList(fileNodesMap.values))

        getThumbnailsFromServer()

        if (zoom == ZoomUtil.ZOOM_IN_1X) {
            getPreviewsFromServer(getPreviewNodes, ::refreshLiveData)
        }
    }

    private fun addPhotoDateTitle(dateString: String, date: Pair<String, String>) {
        // RandomUUID() can ensure non-repetitive values in practical purpose
        fileNodesMap[UUID.randomUUID()] = GalleryItem(
            null,
            Constants.INVALID_POSITION,
            Constants.INVALID_POSITION,
            null,
            MediaCardType.Header,
            dateString,
            date.formatDateTitle(context),
            null,
            false,
            uiDirty = true
        )
    }

    suspend fun getPreviewsFromServer(
        map: Map<MegaNode, String>,
        refreshCallback: () -> Unit,
    ) {
        for ((key, value) in map) {
            megaApi.getPreview(
                key,
                value,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode != MegaError.API_OK) return@OptionalMegaRequestListenerInterface

                        request.let {
                            fileNodesMap[it.nodeHandle]?.apply {
                                thumbnail = getPreviewFile(key).absoluteFile
                                uiDirty = true
                            }
                        }

                        refreshCallback.invoke()
                    }
                ))

            // Throttle the getThumbnail call, or the UI would be non-responsive
            delay(GET_THUMBNAIL_THROTTLE)
        }
    }

    /**
     * Function to filter out node items that are either in rubbish, or is a folder.
     */
    private fun getFilteredChildren(nodes: List<MegaNode>): List<MegaNode> {
        val filteredNodes = ArrayList<MegaNode>()

        for (node in nodes) {
            if (megaApi.isInRubbish(node))
                continue

            if (node.isFolder) {
                continue
            }

            if (shouldAdd(MimeTypeList.typeForName(node.name))) {
                // when not in search mode, index used by viewer is index in all siblings,
                // including non image/video nodes
                filteredNodes.add(node)
            }
        }
        return filteredNodes
    }

    private fun shouldAdd(mime: MimeTypeList) = mime.isImage || mime.isVideoReproducible

    val result = MutableLiveData<List<NodeItem>>()

    val thumbnailFolder = File(context.cacheDir, CacheFolderManager.THUMBNAIL_FOLDER)
    val previewFolder = File(context.cacheDir, CacheFolderManager.PREVIEW_FOLDER)

    /**
     * LinkedHashMap guarantees that the index order of elements is consistent with
     * the order of putting. Moreover, it has a quick element search[O(1)] (for
     * the callback of megaApi.getThumbnail())
     */
    val fileNodesMap: LinkedHashMap<Any, NodeItem> = LinkedHashMap()

    /** Refresh rate limit */
    var waitingForRefresh = false

    val getThumbnailNodes = mutableMapOf<MegaNode, String>()

    fun getThumbnailFile(node: MegaNode) = File(
        thumbnailFolder,
        node.base64Handle.plus(FileUtil.JPG_EXTENSION)
    )

    /**
     * Get the thumbnail of the file.
     */
    private fun getThumbnail(node: MegaNode): File? {
        val thumbFile = getThumbnailFile(node)

        return if (thumbFile.exists()) {
            thumbFile
        } else {
            // Note down the nodes and going to get their thumbnails from the server
            // as soon as the getNodeItems finished. (Don't start the getting operation here
            // for avoiding potential ConcurrentModification issue)
            if (node.hasThumbnail()) {
                getThumbnailNodes[node] = thumbFile.absolutePath
            }

            null
        }
    }

    /**
     * Get all nodes items.
     *
     * @param cancelToken   MegaCancelToken to cancel the fetch at any time.
     */
    suspend fun getNodeItems(cancelToken: MegaCancelToken) {
        for (node in getMegaNodes(cancelToken, order, type)) {
            val thumbnail = getThumbnail(node)
            val dateString = DateTimeFormatter.ofPattern("MMMM uuuu").format(Util.fromEpoch(node.modificationTime))
            val selected = selectedNodesMap[node.handle]?.selected ?: false

            fileNodesMap[node.handle] = NodeItem(
                node,
                -1,
                type == MegaApiJava.FILE_TYPE_VIDEO,
                dateString,
                thumbnail,
                selected
            )
        }

        result.postValue(java.util.ArrayList(fileNodesMap.values))

        getThumbnailsFromServer()
    }

    suspend fun getThumbnailsFromServer() {
        for (item in getThumbnailNodes) {
            megaApi.getThumbnail(
                item.key,
                item.value,
                object : BaseListener(context) {
                    override fun onRequestFinish(
                        api: MegaApiJava,
                        request: MegaRequest,
                        e: MegaError
                    ) {
                        if (e.errorCode != MegaError.API_OK) return

                        request.let {
                            fileNodesMap[it.nodeHandle]?.apply {
                                thumbnail = getThumbnailFile(item.key)
                                uiDirty = true
                            }
                        }

                        refreshLiveData()
                    }
                })

            // Throttle the getThumbnail call, or the UI would be non-responsive
            delay(GET_THUMBNAIL_THROTTLE)
        }
    }

    /**
     * Gets a list of nodes given an order and type.
     *
     * @param cancelToken   MegaCancelToken to cancel the search at any time.
     * @param order         Order to get nodes.
     * @param type          Type of nodes.
     */
    fun getMegaNodes(cancelToken: MegaCancelToken, order: Int, type: Int): List<MegaNode> =
        megaApi.searchByType(cancelToken, order, type, MegaApiJava.SEARCH_TARGET_ROOTNODE)

    companion object {
        // 500ms, user can see the update of photos instantly
        private const val UPDATE_DATA_THROTTLE_TIME = 500L

        // 10ms
        private const val GET_THUMBNAIL_THROTTLE = 10L

        private const val ERROR_MSG_PARSE_CU_HANDLE = "parse getCamSyncHandle error "
        private const val ERROR_MSG_PARSE_MU_HANDLE = "parse MegaHandleSecondaryFolder error "
    }
}