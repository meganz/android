package mega.privacy.android.app.fragments.homepage.documents

import android.content.Context
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import java.io.File
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class DocumentsRepository @Inject constructor(
    private val megaApi: MegaApiAndroid,
    private val dbHandler: DatabaseHandler,
    @ApplicationContext private val context: Context
) {
    private var order = MegaApiJava.ORDER_MODIFICATION_DESC

    // LinkedHashMap guarantees that the index order of elements is consistent with
    // the order of putting. Moreover, it has a quick element search[O(1)] (for
    // the callback of megaApi.getThumbnail())
    private val photoNodesMap: MutableMap<Long, NodeItem> = LinkedHashMap()
    private val savedPhotoNodesMap: MutableMap<Long, NodeItem> = LinkedHashMap()

    private var waitingForRefresh = false

    private val _photoNodes = MutableLiveData<List<NodeItem>>()
    val photoNodes: LiveData<List<NodeItem>> = _photoNodes

    suspend fun getPhotos(forceUpdate: Boolean) {
        if (forceUpdate) {
            withContext(Dispatchers.IO) {
                saveAndClearData()
                getPhotoNodes()

                // Update LiveData must in main thread
                withContext(Dispatchers.Main) {
                    _photoNodes.value = ArrayList<NodeItem>(photoNodesMap.values)
                }
            }
        } else {
            _photoNodes.value = ArrayList<NodeItem>(photoNodesMap.values)
        }
    }

    /**
     * Save some field values (e.g. "selected") which do not exist in the raw MegaNode data.
     * Restore these values in event of querying the raw data again
     */
    private fun saveAndClearData() {
        savedPhotoNodesMap.clear()
        photoNodesMap.toMap(savedPhotoNodesMap)
        photoNodesMap.clear()
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
                        // Must generate a new PhotoNode object, or the oldItem and newItem in
                        // PhotosGridAdapter's areContentsTheSame will be an identical object,
                        // then the item wouldn't be refreshed
                        photoNodesMap[it.nodeHandle]?.apply {
//                            photoNodesMap[it.nodeHandle] = copy(thumbnail = thumbFile.absoluteFile)
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
                _photoNodes.value = ArrayList<NodeItem>(photoNodesMap.values)
            }, UPDATE_DATA_THROTTLE_TIME
        )
    }

    private fun getPhotoNodes() {
        for (node in getMegaNodesOfPhotos()) {
            val thumbnail = getThumbnail(node)
            val modifyDate = Util.fromEpoch(node.modificationTime)
            val dateString = DateTimeFormatter.ofPattern("MMM uuuu").format(modifyDate)

            val selected = savedPhotoNodesMap[node.handle]?.selected ?: false

            photoNodesMap[node.handle] = NodeItem(
                node,
                -1,
                dateString,
                thumbnail,
                selected,
                true
            )
        }
    }

    private fun getMegaNodesOfPhotos(): List<MegaNode> {
        // TODO: use constants
        return megaApi.searchByType(null, null, null, true, order, 4, 3)
    }

    companion object {
        private const val UPDATE_DATA_THROTTLE_TIME =
            500L   // 500ms, user can see the update of photos instantly
    }
}
