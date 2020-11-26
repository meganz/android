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

private val avatarChange_ = MutableLiveData<Boolean>()
val avatarChange: LiveData<Boolean> = avatarChange_

private val scrolling_ = MutableLiveData<Pair<Scrollable, Boolean>>()
val scrolling: LiveData<Pair<Scrollable, Boolean>> = scrolling_

private val notificationCountChange_ = MutableLiveData<Int>()
val notificationCountChange: LiveData<Int> = notificationCountChange_

private val chatOnlineStatusChange_ = MutableLiveData<Int>()
val chatOnlineStatusChange: LiveData<Int> = chatOnlineStatusChange_

private val homepageVisibilityChange_ = MutableLiveData<Boolean>()
val homepageVisibilityChange: LiveData<Boolean> = homepageVisibilityChange_

fun notifyNodesChange(forceUpdate: Boolean) {
    nodesChange_.value = forceUpdate
}

fun notifyOrderChange(order: Int) {
    orderChange_.value = order
}

fun notifyListGridChange(isList: Boolean) {
    listGridChange_.value = isList
}

fun notifyAvatarChange(isSet: Boolean) {
    avatarChange_.value = isSet
}

fun onScrolling(scrollingFragment: Pair<Scrollable, Boolean>) {
    scrolling_.value = scrollingFragment
}

fun notifyNotificationCountChange(count: Int) {
    notificationCountChange_.value = count
}

fun notifyChatOnlineStatusChange(status: Int) {
    chatOnlineStatusChange_.value = status
}
