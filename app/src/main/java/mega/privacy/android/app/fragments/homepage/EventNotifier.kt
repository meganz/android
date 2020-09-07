package mega.privacy.android.app.fragments.homepage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import nz.mega.sdk.MegaApiJava

/**
 * This file intends to create a plain publish/subscribe channel for events
 */
private val nodesChange_ = MutableLiveData<Boolean>(false)
val nodesChange: LiveData<Boolean> = nodesChange_

private val orderChange_ = MutableLiveData<Int>(MegaApiJava.ORDER_DEFAULT_ASC)
val orderChange: LiveData<Int> = orderChange_

private val listGridChange_ = MutableLiveData<Boolean>(true)
val listGridChange: LiveData<Boolean> = listGridChange_

fun notifyNodesChange(forceUpdate: Boolean) {
    nodesChange_.value = forceUpdate
}

fun notifyOrderChange(order: Int) {
    orderChange_.value = order
}

fun notifyListGridChange(isList: Boolean) {
    listGridChange_.value = isList
}