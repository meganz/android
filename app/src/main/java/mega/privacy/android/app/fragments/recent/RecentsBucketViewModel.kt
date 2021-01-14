package mega.privacy.android.app.fragments.recent

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import androidx.lifecycle.Observer

import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.homepage.nodesChange
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRecentActionBucket

class RecentsBucketViewModel @ViewModelInject constructor(
    private val megaApi: MegaApiAndroid,
    private val recentsBucketRepository: RecentsBucketRepository
) : ViewModel() {

    var bucket: MutableLiveData<MegaRecentActionBucket> = MutableLiveData()

    var cachedActionList = MutableLiveData<List<MegaRecentActionBucket>>()

    var shouldCloseFragment: MutableLiveData<Boolean> = MutableLiveData(false)

    var items: LiveData<List<MegaNode>> = bucket.switchMap {
        viewModelScope.launch {
            recentsBucketRepository.getNodes(it)
        }

        recentsBucketRepository.nodes
    }

    fun getItemPositionByHandle(handle: Long): Int {
        var index = INVALID_POSITION

        items.value?.forEachIndexed { i, megaNode ->
            if (megaNode.handle == handle) {
                index = i
                return@forEachIndexed
            }
        }

        return index
    }

    private fun isSameBucket(
        selected: MegaRecentActionBucket,
        other: MegaRecentActionBucket
    ): Boolean {
        return selected.isMedia == other.isMedia &&
                selected.isUpdate == other.isUpdate &&
                selected.timestamp == other.timestamp &&
                selected.parentHandle == other.parentHandle &&
                selected.userEmail == other.userEmail
    }

    private val nodesChangeObserver = Observer<Boolean> {
        if (it) {
            if (bucket.value == null) {
                return@Observer
            }

            val recentActions = megaApi.recentActions

            recentActions.forEach { b ->
                bucket.value?.let { current ->
                    if (isSameBucket(current, b)) {
                        bucket.value = b
                        return@Observer
                    }
                }
            }

            cachedActionList.value?.forEach { b ->
                val iterator = recentActions.iterator()
                while (iterator.hasNext()) {
                    if (isSameBucket(iterator.next(), b)) {
                        iterator.remove()
                    }
                }
            }

            // The last one is the changed one.
            if (recentActions.size == 1) {
                bucket.value = recentActions[0]
                return@Observer
            }

            // No nodes contained in the bucket or the action bucket is no loner exists.
            shouldCloseFragment.value = true
        }
    }

    init {
        nodesChange.observeForever(nodesChangeObserver)
    }

    override fun onCleared() {
        nodesChange.removeObserver(nodesChangeObserver)
    }
}

