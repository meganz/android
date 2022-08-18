package mega.privacy.android.app.fragments.recent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

class RecentsBucketRepository @Inject constructor() {

    private val _nodes = MutableLiveData<List<NodeItem>>()
    val nodes: LiveData<List<NodeItem>> = _nodes

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
                    index = i,
                    isVideo = node.isVideo(),
                    modifiedDate = node.modificationTime.toString(),
                ))
            }

            withContext(Dispatchers.Main) {
                _nodes.value = nodesList
            }
        }
    }
}

