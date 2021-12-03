package mega.privacy.android.app.fragments.homepage

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.delay
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.*
import nz.mega.sdk.*
import java.io.File
import java.time.format.DateTimeFormatter.ofPattern
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
    private val selectedNodesMap: LinkedHashMap<Any, NodeItem>
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
     * Get all nodes items
     */
    suspend fun getNodeItems() {
        for (node in getMegaNodes()) {
            val thumbnail = getThumbnail(node)
            val dateString = ofPattern("MMMM uuuu").format(Util.fromEpoch(node.modificationTime))
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

    suspend fun getPreviewsFromServer(
        map: MutableMap<MegaNode, String>,
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

    private fun getMegaNodes(): List<MegaNode> =
        megaApi.searchByType(order, type, MegaApiJava.SEARCH_TARGET_ROOTNODE)

    companion object {
        private const val UPDATE_DATA_THROTTLE_TIME =
            500L   // 500ms, user can see the update of photos instantly
        private const val GET_THUMBNAIL_THROTTLE = 10L // 10ms
    }
}