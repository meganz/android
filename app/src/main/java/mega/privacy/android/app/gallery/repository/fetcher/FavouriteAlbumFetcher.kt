package mega.privacy.android.app.gallery.repository.fetcher

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.MediaCardType
import mega.privacy.android.app.gallery.extension.formatDateTitle
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ZoomUtil
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.model.MegaPreferences
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaNode
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
import kotlin.coroutines.suspendCoroutine

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
) {

    suspend fun getNodes(): List<MegaNode> {
        return getNodesAfterSort()
    }

    private fun filterFavAlbumImagesByDesc(favouriteNodes: List<MegaNode>): List<MegaNode> {
        return favouriteNodes.filter {
            MimeTypeList.typeForName(it.name).isImage
        }.sortedByDescending { it.modificationTime }
    }

    private fun filterFavAlbumVideosByDesc(favouriteNodes: List<MegaNode>): List<MegaNode> {
        return favouriteNodes.filter {
            MimeTypeList.typeForName(it.name).isVideoReproducible
                    && inSyncFolder(it.parentHandle)
        }.sortedByDescending { it.modificationTime }
    }

    private fun filterFavAlbumNodes(favouriteNodes: List<MegaNode>): List<MegaNode> {
        return favouriteNodes.filter {
            MimeTypeList.typeForName(it.name).isImage
                    || (MimeTypeList.typeForName(it.name).isVideoReproducible
                    && inSyncFolder(it.parentHandle))
        }
    }

    /**
     * Check the node in CU or MU folder or not
     */
    private fun inSyncFolder(parentId: Long): Boolean =
        parentId == getDbPreferences()?.camSyncHandle?.toLong() || parentId == getDbPreferences()?.megaHandleSecondaryFolder?.toLong()

    /**
     * Get All Favourite Nodes
     */
    suspend fun getFavouriteNodes() =
        withContext(Dispatchers.IO) {
            val handleList = suspendCoroutine<MegaHandleList> { continuation ->
                megaApi.getFavourites(
                    null,
                    0,
                    OptionalMegaRequestListenerInterface(
                        onRequestFinish = { request, error ->
                            if (error.errorCode == MegaError.API_OK) {
                                continuation.resumeWith(Result.success(request.megaHandleList))
                            } else {
                                continuation.failWithError(error)
                            }
                        }
                    )
                )
            }
            handleList.getNodesByHandles()
        }

    /**
     * Get node from MegaHandleList
     * @return List<MegaNode>
     */
    private fun MegaHandleList.getNodesByHandles() = (0..size())
        .map { this[it] }
        .mapNotNull { megaApi.getNodeByHandle(it) }

    /**
     * Get megaNodes after sort
     *
     * @return MegaNodes after sort
     */
    private suspend fun getNodesAfterSort(): List<MegaNode> {
        return when (order) {
            MegaApiJava.ORDER_PHOTO_DESC -> sortByPhotos()
            MegaApiJava.ORDER_VIDEO_DESC -> sortByVideos()
            else -> sortByModifiedTime(orderStrategy = order)
        }
    }

    /**
     * Sort by modified time
     *
     * @return MegaNodes after sort
     */
    private suspend fun sortByModifiedTime(
        orderStrategy: Int,
    ): List<MegaNode> {
        val favourites = getFavouriteNodes()
        val favAlbumNodes = filterFavAlbumNodes(favourites)
        return when (orderStrategy) {
            MegaApiJava.ORDER_MODIFICATION_DESC -> favAlbumNodes.sortedByDescending { it.modificationTime }
            MegaApiJava.ORDER_MODIFICATION_ASC -> favAlbumNodes.sortedBy { it.modificationTime }
            else -> favAlbumNodes
        }
    }

    /**
     * Sort by photos
     * Images will be in front of videos
     *
     * @return MegaNodes after sort
     */
    private suspend fun sortByPhotos(): List<MegaNode> {
        val favourites = getFavouriteNodes()
        val images = filterFavAlbumImagesByDesc(favourites)
        val videos = filterFavAlbumVideosByDesc(favourites)
        return images + videos
    }

    /**
     * Sort by videos
     * Videos will be in front of images
     *
     * @return MegaNodes after sort
     */
    private suspend fun sortByVideos(): List<MegaNode> {
        val favourites = getFavouriteNodes()
        val images = filterFavAlbumImagesByDesc(favourites)
        val videos = filterFavAlbumVideosByDesc(favourites)
        return videos + images
    }

    /**
     * get DB Preferences
     */
    private fun getDbPreferences(): MegaPreferences? = dbHandler.preferences

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
     */
    suspend fun getGalleryItems() {
        var lastYearDate: LocalDate? = null
        var lastMonthDate: LocalDate? = null
        var lastDayDate: LocalDate? = null

        for (node in getNodes()) {
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
                        if (error.errorCode == MegaError.API_OK) {
                            request.let {
                                fileNodesMap[it.nodeHandle]?.apply {
                                    thumbnail = getPreviewFile(key).absoluteFile
                                    uiDirty = true
                                }
                            }
                            refreshCallback.invoke()
                        }
                    }
                ))

            // Throttle the getThumbnail call, or the UI would be non-responsive
            delay(GET_THUMBNAIL_THROTTLE)
        }
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

    suspend fun getThumbnailsFromServer() {
        for ((key, value) in getThumbnailNodes) {
            megaApi.getThumbnail(
                key,
                value,
                OptionalMegaRequestListenerInterface(onRequestFinish = { megaRequest, megaError ->
                    if (megaError.errorCode == MegaError.API_OK) {
                        megaRequest.let {
                            fileNodesMap[it.nodeHandle]?.apply {
                                thumbnail = getThumbnailFile(key)
                                uiDirty = true
                            }
                        }
                        refreshLiveData()
                    }
                }))

            // Throttle the getThumbnail call, or the UI would be non-responsive
            delay(GET_THUMBNAIL_THROTTLE)
        }
    }

    companion object {
        // 500ms, user can see the update of photos instantly
        private const val UPDATE_DATA_THROTTLE_TIME = 500L

        // 10ms
        private const val GET_THUMBNAIL_THROTTLE = 10L
    }
}