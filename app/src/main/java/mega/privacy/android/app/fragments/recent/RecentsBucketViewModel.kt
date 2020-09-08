package mega.privacy.android.app.fragments.recent

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import ash.TL
import kotlinx.coroutines.launch
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
}

