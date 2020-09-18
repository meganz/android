package mega.privacy.android.app.fragments.recent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

class RecentsBucketRepository @Inject constructor() {

    private val _nodes = MutableLiveData<List<MegaNode>>()
    val nodes: LiveData<List<MegaNode>> = _nodes

    suspend fun getNodes(bucket: MegaRecentActionBucket) {

        withContext(Dispatchers.IO) {
            val size = bucket.nodes.size()
            val nodesList = ArrayList<MegaNode>(size)
            for (i in 0 until size) {
                nodesList.add(bucket.nodes[i])
            }

            withContext(Dispatchers.Main) {
                _nodes.value = nodesList
            }
        }
    }
}

