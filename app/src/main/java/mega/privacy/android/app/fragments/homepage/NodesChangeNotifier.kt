package mega.privacy.android.app.fragments.homepage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import nz.mega.sdk.MegaApiJava

val nodesChange_ = MutableLiveData<Boolean>(false)
val nodesChange: LiveData<Boolean> = nodesChange_

val orderChange_ = MutableLiveData<Int>(MegaApiJava.ORDER_MODIFICATION_DESC)
val orderChange: LiveData<Int> = orderChange_

fun notifyNodesChange(forceUpdate: Boolean) {
    nodesChange_.value = forceUpdate
}

fun notifyOrderChange(order: Int) {
    orderChange_.value = order
}