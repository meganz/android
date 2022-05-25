package mega.privacy.android.app.gallery.repository.fetcher

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.delay
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.fragments.homepage.TypedNodesFetcher
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItem.Companion.TYPE_HEADER
import mega.privacy.android.app.gallery.data.GalleryItem.Companion.TYPE_IMAGE
import mega.privacy.android.app.gallery.data.GalleryItem.Companion.TYPE_VIDEO
import mega.privacy.android.app.gallery.extension.formatDateTitle
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ZoomUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ofPattern
import java.util.*

/**
 * Data fetcher for fetching typed files
 */
abstract class GalleryBaseFetcher(
        protected val context: Context,
        protected val megaApi: MegaApiAndroid,
        protected val selectedNodesMap: LinkedHashMap<Any, GalleryItem>,
        protected val zoom: Int
) : TypedNodesFetcher(context, megaApi, selectedNodesMap = selectedNodesMap) {

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
                                Pair(ofPattern("uuuu").format(modifyDate), "")
                        )
                    }
                }
                ZoomUtil.ZOOM_IN_1X -> {
                    if (lastDayDate == null || lastDayDate.dayOfYear != modifyDate.dayOfYear) {
                        lastDayDate = modifyDate

                        addPhotoDateTitle(
                                dateString, Pair(
                                ofPattern("dd MMMM").format(modifyDate),
                                if (sameYear) "" else ofPattern("uuuu").format(modifyDate)
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
                                        Date.from(modifyDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
                                ),
                                if (sameYear) "" else ofPattern("uuuu").format(modifyDate)
                        )
                        )
                    }
                }
            }

            val selected = selectedNodesMap[node.handle]?.selected ?: false
            val galleryItem = GalleryItem(
                    node,
                    INVALID_POSITION,
                    INVALID_POSITION,
                    thumbnail,
                    if (node.duration == -1) TYPE_IMAGE else TYPE_VIDEO,
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
                INVALID_POSITION,
                INVALID_POSITION,
                null,
                TYPE_HEADER,
                dateString,
                date.formatDateTitle(context),
                null,
                false,
                uiDirty = true
        )
    }

    suspend fun getPreviewsFromServer(
            map: Map<MegaNode, String>,
            refreshCallback: () -> Unit
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
    protected fun getFilteredChildren(nodes: List<MegaNode>): List<MegaNode> {
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

    protected fun shouldAdd(mime: MimeTypeList) = mime.isImage || mime.isVideoReproducible

    /**
     * Sub class to implement their own get node method by their own strategy
     *
     * @param cancelToken   MegaCancelToken to cancel the search at any time.
     */
    abstract fun getNodes(cancelToken: MegaCancelToken? = null): List<MegaNode>
}