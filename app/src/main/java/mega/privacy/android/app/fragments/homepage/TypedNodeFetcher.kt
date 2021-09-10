package mega.privacy.android.app.fragments.homepage

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.delay
import mega.privacy.android.app.fragments.homepage.photos.PhotoNodeItem
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.*
import nz.mega.sdk.*
import java.io.File
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.LinkedHashMap

/**
 * Data fetcher for fetching typed files
 */
class TypedNodesFetcher(
    private val context: Context,
    private val megaApi: MegaApiAndroid,
    private val type: Int = MegaApiJava.FILE_TYPE_DEFAULT,
    private val order: Int = MegaApiJava.ORDER_DEFAULT_ASC,
    private val selectedNodesMap: LinkedHashMap<Any, NodeItem>,
    private val zoom: Int
) {
    val result = MutableLiveData<List<NodeItem>>()

    /**
     * LinkedHashMap guarantees that the index order of elements is consistent with
     * the order of putting. Moreover, it has a quick element search[O(1)] (for
     * the callback of megaApi.getThumbnail())
     */
    private val fileNodesMap: LinkedHashMap<Any, NodeItem> = LinkedHashMap()

    /** Refresh rate limit */
    private var waitingForRefresh = false

    private val getThumbnailNodes = mutableMapOf<MegaNode, String>()

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

    private fun getThumbnailFile(node: MegaNode) = File(
        ThumbnailUtilsLollipop.getThumbFolder(context),
        node.base64Handle.plus(FileUtil.JPG_EXTENSION)
    )

    private fun getPreviewFile(node: MegaNode) = File(
        PreviewUtils.getPreviewFolder(context),
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
     * Get the preview of the file.
     */
    private fun getPreview(node: MegaNode): File? {
        val previewFile = getPreviewFile(node)

        return if (previewFile.exists()) {
            previewFile
        } else {
            // Note down the nodes and going to get their thumbnails from the server
            // as soon as the getNodeItems finished. (Don't start the getting operation here
            // for avoiding potential ConcurrentModification issue)
            if (node.hasPreview()) {
                getThumbnailNodes[node] = previewFile.absolutePath
            }

            null
        }
    }

    /**
     * Get all nodes items
     */
    suspend fun getNodeItems() {
        var lastYearDate: LocalDate? = null
        var lastMonthDate: LocalDate? = null
        var lastDayDate: LocalDate? = null

        for (node in getMegaNodes()) {
            val thumbnail = if(zoom == ZoomUtil.ZOOM_IN_1X) {
                getPreview(node)
            } else {
                getThumbnail(node)
            }

            val modifyDate = Util.fromEpoch(node.modificationTime)
            val dateString = DateTimeFormatter.ofPattern("MMM uuuu").format(modifyDate)
            val sameYear = Year.from(LocalDate.now()) == Year.from(modifyDate)

            // Photo "Month-Year" section headers
            if (type == MegaApiJava.FILE_TYPE_PHOTO) {
                when(zoom) {
                    ZoomUtil.ZOOM_OUT_2X -> {
                        if (lastYearDate == null || Year.from(lastYearDate) != Year.from(modifyDate)) {
                            lastYearDate = modifyDate
                            addPhotoDateTitle(DateTimeFormatter.ofPattern("uuuu").format(modifyDate))
                        }
                    }
                    ZoomUtil.ZOOM_IN_1X -> {
                        if (lastDayDate == null || lastDayDate.dayOfYear != modifyDate.dayOfYear) {
                            lastDayDate = modifyDate
                            addPhotoDateTitle(DateTimeFormatter.ofPattern(if(sameYear) "dd MMMM" else "dd MMMM uuuu").format(modifyDate))
                        }
                    }
                    else -> {
                        if (lastMonthDate == null || YearMonth.from(lastMonthDate) != YearMonth.from(modifyDate)) {
                            lastMonthDate = modifyDate
                            addPhotoDateTitle(DateTimeFormatter.ofPattern(if(sameYear) "MMMM" else "MMMM uuuu").format(modifyDate))
                        }
                    }
                }
            }

            val selected = selectedNodesMap[node.handle]?.selected ?: false
            var nodeItem: NodeItem?

            if (type == MegaApiJava.FILE_TYPE_PHOTO) {
                nodeItem = PhotoNodeItem(
                    PhotoNodeItem.TYPE_PHOTO,
                    -1,
                    node,
                    -1,
                    dateString,
                    thumbnail,
                    selected
                )
            } else {
                nodeItem = NodeItem(
                    node,
                    -1,
                    type == MegaApiJava.FILE_TYPE_VIDEO,
                    dateString,
                    thumbnail,
                    selected
                )
            }

            fileNodesMap[node.handle] = nodeItem
        }

        result.postValue(ArrayList(fileNodesMap.values))

        getThumbnailsFromServer()
    }

    private fun addPhotoDateTitle(dateString: String) {
        // RandomUUID() can ensure non-repetitive values in practical purpose
        fileNodesMap[UUID.randomUUID()] = PhotoNodeItem(
            PhotoNodeItem.TYPE_TITLE,
            -1,
            null,
            -1,
            dateString,
            null,
            false
        )
    }

    private suspend fun getThumbnailsFromServer() {
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
                                thumbnail = getThumbnailFile(item.key).absoluteFile
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

    private fun getMegaNodes(): List<MegaNode> =
        megaApi.searchByType(order, type, MegaApiJava.SEARCH_TARGET_ROOTNODE)

    companion object {
        private const val UPDATE_DATA_THROTTLE_TIME =
            500L   // 500ms, user can see the update of photos instantly
        private const val GET_THUMBNAIL_THROTTLE = 10L // 10ms
    }
}