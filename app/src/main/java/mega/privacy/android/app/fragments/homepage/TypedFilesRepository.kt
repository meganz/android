package mega.privacy.android.app.fragments.homepage

import android.content.Context
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import mega.privacy.android.app.fragments.homepage.photos.PhotoNodeItem
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.*
import nz.mega.sdk.MegaNode.TYPE_RUBBISH
import okhttp3.Dispatcher
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class TypedFilesRepository @Inject constructor(
    private val megaApi: MegaApiAndroid,
    @ApplicationContext private val context: Context
) {
    private var order = ORDER_DEFAULT_ASC
    private var type = NODE_UNKNOWN

    // LinkedHashMap guarantees that the index order of elements is consistent with
    // the order of putting. Moreover, it has a quick element search[O(1)] (for
    // the callback of megaApi.getThumbnail())
    private val fileNodesMap: MutableMap<Long, NodeItem> = LinkedHashMap()
    private val savedFileNodesMap: MutableMap<Long, NodeItem> = LinkedHashMap()

    private var waitingForRefresh = false

    private val _fileNodeItems = MutableLiveData<List<NodeItem>>()
    val fileNodeItems: LiveData<List<NodeItem>> = _fileNodeItems

    suspend fun getFiles(type: Int, order: Int) {
        this.type = type
        this.order = order

        withContext(Dispatchers.IO) {
            saveAndClearData()
            getNodeItems()

            // Update LiveData must in main thread
            withContext(Dispatchers.Main) {
                _fileNodeItems.value = ArrayList<NodeItem>(fileNodesMap.values)
            }
        }
    }

    fun emitFiles() {
        _fileNodeItems.value = ArrayList<NodeItem>(fileNodesMap.values)
    }

    /**
     * Save some field values (e.g. "selected") which do not exist in the raw MegaNode data.
     * Restore these values in event of querying the raw data again
     */
    private fun saveAndClearData() {
        savedFileNodesMap.clear()
        fileNodesMap.toMap(savedFileNodesMap)
        fileNodesMap.clear()
    }

    private fun getThumbnail(node: MegaNode): File? {
        val thumbFile = File(
            getThumbFolder(context),
            node.base64Handle.plus(".jpg")
        )

        return if (thumbFile.exists()) {
            thumbFile
        } else {
            megaApi.getThumbnail(node, thumbFile.absolutePath, object : BaseListener(context) {
                override fun onRequestFinish(
                    api: MegaApiJava?,
                    request: MegaRequest?,
                    e: MegaError?
                ) {
                    request?.let {
                        fileNodesMap[it.nodeHandle]?.apply {
                            thumbnail = thumbFile.absoluteFile
                            uiDirty = true
                        }
                    }

                    refreshLiveData()
                }
            })

            null
        }
    }

    /**
     * Throttle for updating the Photos LiveData
     */
    private fun refreshLiveData() {
        if (waitingForRefresh) return
        waitingForRefresh = true

        Handler().postDelayed(
            {
                waitingForRefresh = false
                _fileNodeItems.value = ArrayList<NodeItem>(fileNodesMap.values)
            }, UPDATE_DATA_THROTTLE_TIME
        )
    }

    private fun getNodeItems() {
        var lastModifyDate: LocalDate? = null
        var mapKeyTitle = Long.MIN_VALUE

        if (type != NODE_PHOTO) {
            fileNodesMap[INVALID_HANDLE] = NodeItem()   // "Sort by" header
        }

        for (node in getMegaNodes()) {
            val thumbnail = getThumbnail(node)
            val modifyDate = Util.fromEpoch(node.modificationTime)
            val dateString = DateTimeFormatter.ofPattern("MMM uuuu").format(modifyDate)

            // Photo "Month-Year" section headers
            if (type == NODE_PHOTO && (lastModifyDate == null
                        || YearMonth.from(lastModifyDate) != YearMonth.from(
                    modifyDate
                ))
            ) {
                lastModifyDate = modifyDate
                fileNodesMap[mapKeyTitle++] =
                    PhotoNodeItem(PhotoNodeItem.TYPE_TITLE, -1, null, -1, dateString, null, false)
            }

            val selected = savedFileNodesMap[node.handle]?.selected ?: false
            var nodeItem: NodeItem? = null

            if (type == NODE_PHOTO) {
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
                    dateString,
                    thumbnail,
                    selected
                )
            }

            fileNodesMap[node.handle] = nodeItem
        }
    }

    private fun getMegaNodes(): List<MegaNode> {
        return megaApi.searchByType(
            null, null, null,
            true, order, type, TARGET_ROOTNODES
        )
    }

    companion object {
        private const val UPDATE_DATA_THROTTLE_TIME =
            500L   // 500ms, user can see the update of photos instantly
    }
}
