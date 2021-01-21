package mega.privacy.android.app.fragments.recent

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import nz.mega.sdk.MegaRecentActionBucket

class SelectedBucketViewModel: ViewModel() {

    val selected = MutableLiveData<MegaRecentActionBucket>()

    val currentActionList = MutableLiveData<List<MegaRecentActionBucket>>()

    fun select(bucket: MegaRecentActionBucket, currentActions: List<MegaRecentActionBucket>) {
        selected.value = bucket
        currentActionList.value = currentActions
    }

}

