package mega.privacy.android.app.gallery.repository.fetcher

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.delay
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.StringUtils.formatDateTitle
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ZoomUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_ASC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import nz.mega.sdk.MegaApiJava.ORDER_PHOTO_DESC
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRequest
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

class PhotosFetcher(
    private val context: Context,
    private val megaApi: MegaApiAndroid,
    private val selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
    private val order: Int,
    private val zoom: Int,
    private val dbHandler: DatabaseHandler,
    private val type: Int = MegaApiJava.FILE_TYPE_DEFAULT,
) {

    private val getPreviewNodes = mutableMapOf<MegaNode, String>()

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
                if (node.duration == -1) GalleryItem.TYPE_IMAGE else GalleryItem.TYPE_VIDEO,
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
            GalleryItem.TYPE_HEADER,
            dateString,
            date.formatDateTitle(),
            null,
            false,
            uiDirty = true
        )
    }

    suspend fun getPreviewsFromServer(
        map: MutableMap<MegaNode, String>,
        refreshCallback: () -> Unit,
    ) {
        for (item in map) {
            megaApi.getPreview(
                item.key,
                item.value,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode != MegaError.API_OK) return@OptionalMegaRequestListenerInterface

                        request.let {
                            fileNodesMap[it.nodeHandle]?.apply {
                                thumbnail = getPreviewFile(item.key).absoluteFile
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
    fun getFilteredChildren(nodes: List<MegaNode>): List<MegaNode> {
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

    private fun getNodes(cancelToken: MegaCancelToken?): List<MegaNode> {
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

    fun getThumbnailFile(node: MegaNode) = File(
        thumbnailFolder,
        node.base64Handle.plus(FileUtil.JPG_EXTENSION)
    )

    /**
     * Get the thumbnail of the file.
     */
    protected fun getThumbnail(node: MegaNode): File? {
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
            val dateString = DateTimeFormatter.ofPattern("MMMM uuuu")
                .format(Util.fromEpoch(node.modificationTime))
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

        result.postValue(ArrayList(fileNodesMap.values))

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
                        e: MegaError,
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
        const val UPDATE_DATA_THROTTLE_TIME =
            500L   // 500ms, user can see the update of photos instantly
        const val GET_THUMBNAIL_THROTTLE = 10L // 10ms
    }
}
