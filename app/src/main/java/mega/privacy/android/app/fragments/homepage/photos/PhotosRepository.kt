package mega.privacy.android.app.fragments.homepage.photos

import android.content.Context
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class PhotosRepository @Inject constructor(
    private val megaApi: MegaApiAndroid,
    private val dbHandler: DatabaseHandler,
    @ApplicationContext private val context: Context
) {
    private var order = MegaApiJava.ORDER_MODIFICATION_DESC

    // LinkedHashMap guarantees that the index order of elements is consistent with
    // the order of putting. Moreover, it has a quick element search[O(1)] (for
    // the callback of megaApi.getThumbnail())
    private val photoNodesMapItem: MutableMap<Long, PhotoNodeItem> = LinkedHashMap()
    private val savedPhotoNodesMapItem: MutableMap<Long, PhotoNodeItem> = LinkedHashMap()

    private var waitingForRefresh = false

    private val _photoNodes = MutableLiveData<List<PhotoNodeItem>>()
    val photoNodesItem: LiveData<List<PhotoNodeItem>> = _photoNodes

    suspend fun getPhotos(forceUpdate: Boolean) {
        if (forceUpdate) {
            withContext(Dispatchers.IO) {
                saveAndClearData()
                getPhotoNodes()

                // Update LiveData must in main thread
                withContext(Dispatchers.Main) {
                    _photoNodes.value = ArrayList<PhotoNodeItem>(photoNodesMapItem.values)
                }
            }
        } else {
            _photoNodes.value = ArrayList<PhotoNodeItem>(photoNodesMapItem.values)
        }
    }

    /**
     * Save some field values (e.g. "selected") which do not exist in the raw MegaNode data.
     * Restore these values in event of querying the raw data again
     */
    private fun saveAndClearData() {
        savedPhotoNodesMapItem.clear()
        photoNodesMapItem.toMap(savedPhotoNodesMapItem)
        photoNodesMapItem.clear()
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
                        photoNodesMapItem[it.nodeHandle]?.apply {
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
                _photoNodes.value = ArrayList<PhotoNodeItem>(photoNodesMapItem.values)
            }, UPDATE_DATA_THROTTLE_TIME
        )
    }

    private fun getPhotoNodes() {
        var lastModifyDate: LocalDate? = null
        var mapKeyTitle = Long.MIN_VALUE

        for (node in getMegaNodesOfPhotos()) {
            val thumbnail = getThumbnail(node)
            val modifyDate = Util.fromEpoch(node.modificationTime)
            val dateString = DateTimeFormatter.ofPattern("MMM uuuu").format(modifyDate)

            if (lastModifyDate == null
                || YearMonth.from(lastModifyDate) != YearMonth.from(
                    modifyDate
                )
            ) {
                lastModifyDate = modifyDate
                photoNodesMapItem[mapKeyTitle++] =
                    PhotoNodeItem(PhotoNodeItem.TYPE_TITLE, -1, null, -1, dateString, null, false)
            }

            val selected = savedPhotoNodesMapItem[node.handle]?.selected ?: false

            photoNodesMapItem[node.handle] = PhotoNodeItem(
                PhotoNodeItem.TYPE_PHOTO,
                -1,
                node,
                -1,
                dateString,
                thumbnail,
                selected
            )
        }
    }

    private fun getMegaNodesOfPhotos(): List<MegaNode> {
        // TODO: use constants
        return megaApi.searchByType(null, null, null, true, order, 1, 3)
    }

    companion object {
        private const val UPDATE_DATA_THROTTLE_TIME =
            500L   // 500ms, user can see the update of photos instantly
    }
}
