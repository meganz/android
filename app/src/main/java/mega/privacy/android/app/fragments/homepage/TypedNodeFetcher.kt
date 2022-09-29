package mega.privacy.android.app.fragments.homepage

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.delay
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import java.io.File
import java.time.format.DateTimeFormatter.ofPattern

/**
 * Data fetcher for fetching typed files
 */
open class TypedNodesFetcher(
    private val context: Context,
    private val megaApi: MegaApiAndroid,
    private val type: Int = MegaApiJava.FILE_TYPE_DEFAULT,
    private val order: Int = MegaApiJava.ORDER_DEFAULT_ASC,
    private val selectedNodesMap: LinkedHashMap<Any, out NodeItem>,
) {
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