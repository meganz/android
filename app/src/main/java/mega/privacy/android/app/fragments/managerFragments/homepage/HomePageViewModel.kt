package mega.privacy.android.app.fragments.managerFragments.homepage

import android.graphics.Bitmap
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.listeners.DefaultMegaChatListener
import mega.privacy.android.app.listeners.DefaultMegaGlobalListener
import mega.privacy.android.app.listeners.DefaultMegaRequestListener
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.Util.getCircleAvatar
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApi.*
import java.util.*

class HomePageViewModel @ViewModelInject constructor(
    private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) : BaseRxViewModel(), DefaultMegaGlobalListener, DefaultMegaChatListener {
    private val _notification = MutableLiveData<Boolean>()
    private val _avatar = MutableLiveData<Bitmap>()
    private val _chatStatus = MutableLiveData<Int>()

    val notification: LiveData<Boolean> = _notification
    val avatar: LiveData<Bitmap> = _avatar
    val chatStatus: LiveData<Int> = _chatStatus

    init {
        updateNotification()
        updateChatStatus(megaChatApi.onlineStatus)

        megaApi.addGlobalListener(this)
        megaChatApi.addChatListener(this)

        _avatar.value = getDefaultAvatar(
            getColorAvatar(megaApi.myUser), megaChatApi.myFullname, Constants.AVATAR_SIZE, true
        )
        loadAvatar()
    }

    override fun onCleared() {
        super.onCleared()

        megaApi.removeGlobalListener(this)
        megaChatApi.removeChatListener(this)
    }

    private fun loadAvatar() {
        add(Single.fromCallable { getCircleAvatar(getApplication(), megaApi.myEmail) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Consumer {
                if (it != null) {
                    _avatar.value = it
                } else {
                    createAvatar()
                }
            }, logErr("loadAvatar"))
        )
    }

    private fun createAvatar() {
        megaApi.getUserAvatar(
            megaApi.myUser,
            buildAvatarFile(getApplication(), megaApi.myEmail + ".jpg").absolutePath,
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
                    }
                }
            })
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
            megaApi.numUnreadUserAlerts + (megaApi.incomingContactRequests?.size ?: 0) > 0
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
}
