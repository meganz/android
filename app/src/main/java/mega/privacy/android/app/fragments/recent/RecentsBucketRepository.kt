package mega.privacy.android.app.fragments.recent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nz.mega.sdk.MegaNode
import javax.inject.Inject

class RecentsBucketRepository @Inject constructor() {

    private val _nodes = MutableLiveData<List<MegaNode>>()
    val nodes: LiveData<List<MegaNode>> = _nodes

    suspend fun getNodes(serializedNodes: Array<String>?) {
        withContext(Dispatchers.IO) {
            val temp = serializedNodes?.map { MegaNode.unserialize(it) }

            withContext(Dispatchers.Main) {
                _nodes.value = temp
            }
        }
    }
}

