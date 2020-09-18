package mega.privacy.android.app.fragments.recent

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import nz.mega.sdk.MegaRecentActionBucket

class SelectedBucketViewModel: ViewModel() {

    val selected = MutableLiveData<MegaRecentActionBucket>()

    fun select(bucket: MegaRecentActionBucket) {
        selected.value = bucket
    }

}

