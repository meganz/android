package mega.privacy.android.app.fragments.recent

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import nz.mega.sdk.MegaNode

class RecentsBucketViewModel @ViewModelInject constructor(
    private val recentsBucketRepository: RecentsBucketRepository
) : ViewModel() {

    private val _openNodeEvent = MutableLiveData<MegaNode>()
    val openNodeEvent: LiveData<MegaNode> = _openNodeEvent

    var serializedNodes: MutableLiveData<Array<String>> = MutableLiveData()

    val items: LiveData<List<MegaNode>> = serializedNodes.switchMap {
        viewModelScope.launch {
            recentsBucketRepository.getNodes(it)
        }

        recentsBucketRepository.nodes
    }

    fun getItemPositionByHandle(handle: Long): Int {
        var index = INVALID_POSITION
        items.value?.forEachIndexed { i, megaNode ->
            if(megaNode.handle == handle) {
                index = i
                return@forEachIndexed
            }
        }
        return index
    }
}

