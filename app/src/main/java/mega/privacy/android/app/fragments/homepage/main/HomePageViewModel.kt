package mega.privacy.android.app.fragments.homepage.main

import android.content.Context
import android.graphics.Bitmap
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.fragments.homepage.Scrollable
import mega.privacy.android.app.fragments.homepage.avatarChange
import mega.privacy.android.app.fragments.homepage.scrolling
import mega.privacy.android.app.listeners.DefaultMegaChatListener
import mega.privacy.android.app.listeners.DefaultMegaGlobalListener
import mega.privacy.android.app.listeners.DefaultMegaRequestListener
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtils
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.Util.getCircleAvatar
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApi.*
import java.util.*

class HomePageViewModel @ViewModelInject constructor(
    private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    @ApplicationContext private val context: Context
) : BaseRxViewModel(), DefaultMegaGlobalListener, DefaultMegaChatListener {

    private val _notification = MutableLiveData<Int>()
    private val _avatar = MutableLiveData<Bitmap>()
    private val _chatStatus = MutableLiveData<Int>()
    private val _isScrolling = MutableLiveData<Pair<Scrollable, Boolean>>()

    val notification: LiveData<Int> = _notification
    val avatar: LiveData<Bitmap> = _avatar
    val chatStatus: LiveData<Int> = _chatStatus
    val isScrolling: LiveData<Pair<Scrollable, Boolean>> = _isScrolling

    private val avatarChangeObserver = androidx.lifecycle.Observer<Boolean> {
        loadAvatar()
    }

    private val scrollingObserver = androidx.lifecycle.Observer<Pair<Scrollable, Boolean>> {
        _isScrolling.value = it
    }

    init {
        updateNotification()
        updateChatStatus(megaChatApi.onlineStatus)

        megaApi.addGlobalListener(this)
        megaChatApi.addChatListener(this)

        showDefaultAvatar()
        loadAvatar(true)
        avatarChange.observeForever(avatarChangeObserver)
        scrolling.observeForever(scrollingObserver)
    }

    override fun onCleared() {
        super.onCleared()

        megaApi.removeGlobalListener(this)
        megaChatApi.removeChatListener(this)
        avatarChange.removeObserver(avatarChangeObserver)
        scrolling.removeObserver(scrollingObserver)
    }

    private fun loadAvatar(retry: Boolean = false) {
        add(Single.fromCallable { getCircleAvatar(context, megaApi.myEmail) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Consumer {
                // actually it won't be null
                it?.apply {
                    when {
                        it.first -> _avatar.value = it.second
                        retry -> createAvatar()
                        else -> showDefaultAvatar()
                    }
                }
            }, logErr("loadAvatar"))
        )
    }

    private fun createAvatar() {
        megaApi.getUserAvatar(
            megaApi.myUser,
            buildAvatarFile(context, megaApi.myEmail + FileUtils.JPG_EXTENSION).absolutePath,
            object : DefaultMegaRequestListener {
                override fun onRequestFinish(
                    api: MegaApiJava,
                    request: MegaRequest,
                    e: MegaError
                ) {
                    if (request.type == MegaRequest.TYPE_GET_ATTR_USER
                        && request.paramType == MegaApiJava.USER_ATTR_AVATAR
                        && e.errorCode == MegaError.API_OK
                    ) {
                        loadAvatar()
                    } else {
                        showDefaultAvatar()
                    }
                }
            })
    }

    private fun showDefaultAvatar() {
        _avatar.value = getDefaultAvatar(
            getColorAvatar(megaApi.myUser), megaChatApi.myFullname, Constants.AVATAR_SIZE, true
        )
    }

    override fun onUserAlertsUpdate(
        api: MegaApiJava,
        userAlerts: ArrayList<MegaUserAlert>?
    ) {
        updateNotification()
    }

    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>?
    ) {
        updateNotification()
    }

    private fun updateNotification() {
        _notification.value =
            megaApi.numUnreadUserAlerts + (megaApi.incomingContactRequests?.size ?: 0)
    }

    override fun onChatOnlineStatusUpdate(
        api: MegaChatApiJava,
        userhandle: Long,
        status: Int,
        inProgress: Boolean
    ) {
        if (userhandle == megaChatApi.myUserHandle) {
            updateChatStatus(status)
        }
    }

    private fun updateChatStatus(status: Int) {
        _chatStatus.value = when (status) {
            STATUS_ONLINE -> R.drawable.ic_online
            STATUS_AWAY -> R.drawable.ic_away
            STATUS_BUSY -> R.drawable.ic_busy
            STATUS_OFFLINE -> R.drawable.ic_offline
            else -> 0
        }
    }

    fun isRootNodeNull() = (megaApi.rootNode == null)
}
