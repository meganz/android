package mega.privacy.android.app.fragments.photos

import android.content.Context
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.util.LongSparseArray
import androidx.core.util.containsKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getThumbFolder
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.*
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlin.collections.LinkedHashMap
import kotlin.collections.ArrayList


class PhotosRepository @Inject constructor(
    private val megaApi: MegaApiAndroid,
    private val dbHandler: DatabaseHandler,
    @ApplicationContext private val context: Context
) {
    private var order = MegaApiJava.ORDER_MODIFICATION_DESC

    private val selectedNodes: LongSparseArray<MegaNode> = LongSparseArray()
    private val photoNodesMap: MutableMap<Long, PhotoNode> = LinkedHashMap()

    private var waitingForRefresh = false
//
//    val photoNodes: LiveData<List<PhotoNode>> = liveData {
//        getSortOrder()
//        emit(getPhotoNodes())
//    }

    private val _photoNodes = MutableLiveData<List<PhotoNode>>()
    val photoNodes: LiveData<List<PhotoNode>> = _photoNodes

    suspend fun getPhotos(query: PhotoQuery)/*: LiveData<List<PhotoNode>>*/ = withContext(Dispatchers.IO) {
            if (query.order == MegaApiJava.ORDER_NONE) {
                getSortOrder()
            } else {
                order = query.order
            }

            getPhotoNodes()

            withContext(Dispatchers.Main) {

                _photoNodes.value = ArrayList<PhotoNode>(photoNodesMap.values)
                Log.i("Alex", "_photoNodes size= " + photoNodesMap.values.size)
//                    listOf(PhotoNode(null, -1, null, PhotoNode.TYPE_TITLE, "dateString", false))
            }
        }

//        return photoNodes


    private fun getSortOrder(): Int {
        try {
            dbHandler.preferences.preferredSortCameraUpload?.let {
                order = it.toInt()
            }
        } catch (exception: NumberFormatException) {
            logWarning(exception.message)
        }

        return order
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
                        val photoNode = photoNodesMap[it.nodeHandle]
                        photoNode?.thumbnail = thumbFile.absoluteFile
                    }

                    refreshLiveData()
                }

            })

            null
        }
    }

    private fun refreshLiveData() {
        if (waitingForRefresh) return

        Handler().postDelayed(
            {
                _photoNodes.value = ArrayList<PhotoNode>(photoNodesMap.values)
                Log.i("Alex", "refresh data")
                waitingForRefresh = false
            }, 1000
        )

        waitingForRefresh = true
    }

    private fun getPhotoNodes() {
        var lastModifyDate: LocalDate? = null
        var mapKeyTitle = Long.MIN_VALUE

        for ((index, node) in getMegaNodesOfPhotos().withIndex()) {
            val thumbnail = getThumbnail(node)
            val modifyDate = Util.fromEpoch(node.modificationTime)
            val dateString = DateTimeFormatter.ofPattern("MMM uuuu").format(modifyDate)

            if (lastModifyDate == null
                || YearMonth.from(lastModifyDate) != YearMonth.from(
                    modifyDate
                )
            ) {
                lastModifyDate = modifyDate
                photoNodesMap[mapKeyTitle++] =
                    PhotoNode(null, -1, null, PhotoNode.TYPE_TITLE, dateString, false)
            }

            photoNodesMap[node.handle] = PhotoNode(
                node,
                index,
                thumbnail,
                PhotoNode.TYPE_PHOTO,
                dateString,
                selectedNodes.containsKey(node.handle)
            )
        }
    }

    /*
     * TODO: This is a temp mock function for upcoming:
     * MegaNodeList* MegaApi::searchByType(const char *searchString, MegaCancelToken *cancelToken, int order, int type)
     */
    private fun getMegaNodesOfPhotos(): List<MegaNode> {
        var cuHandle: Long = -1
        val pref = dbHandler.preferences

        if (pref != null && pref.camSyncHandle != null) {
            try {
                cuHandle = pref.camSyncHandle.toLong()
            } catch (ignored: java.lang.NumberFormatException) {
            }

            if (megaApi.getNodeByHandle(cuHandle) == null) {
                cuHandle = -1
            }
        }

        if (cuHandle == -1L) {
            for (node in megaApi.getChildren(megaApi.rootNode)) {
                if (node.isFolder && TextUtils.equals(
                        context.getString(R.string.section_photo_sync),
                        node.name
                    )
                ) {
                    cuHandle = node.handle
                    dbHandler.setCamSyncHandle(cuHandle)
                    break
                }
            }
        }

        if (cuHandle == -1L) {
            return Collections.emptyList()
        }

        return megaApi.getChildren(megaApi.getNodeByHandle(cuHandle), order)
    }
}