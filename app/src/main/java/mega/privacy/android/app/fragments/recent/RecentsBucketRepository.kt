package mega.privacy.android.app.fragments.recent

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRecentActionBucket
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentsBucketRepository @Inject constructor(
    @ApplicationContext val context: Context,
) {

    private val _nodes = MutableLiveData<List<NodeItem>>()
    val nodes: LiveData<List<NodeItem>> = _nodes

    val getThumbnailNodes = mutableMapOf<MegaNode, String>()

    suspend fun getNodes(bucket: MegaRecentActionBucket?) {
        if (bucket == null) {
            return
        }

        withContext(Dispatchers.IO) {
            val size = bucket.nodes.size()
            val nodesList = ArrayList<NodeItem>(size)
            for (i in 0 until size) {
                val node = bucket.nodes[i]
                nodesList.add(NodeItem(
                    node = node,
                    thumbnail = getThumbnail(node),
                    index = -1,
                    isVideo = node.isVideo(),
                    modifiedDate = node.modificationTime.toString(),
                ))
            }

            withContext(Dispatchers.Main) {
                _nodes.value = nodesList
            }
        }
    }

    /**
     * Get the thumbnail of the file.
     */
    private fun getThumbnail(node: MegaNode): File? {
        val thumbFile = File(
            File(context.cacheDir, CacheFolderManager.THUMBNAIL_FOLDER),
            node.base64Handle.plus(FileUtil.JPG_EXTENSION)
        )

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
}

