package mega.privacy.android.app.fragments.photos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

val nodesChange_ = MutableLiveData<Boolean>(false)
val nodesChange: LiveData<Boolean> = nodesChange_

fun notifyNodesChange(forceUpdate: Boolean) {
    nodesChange_.value = forceUpdate
}