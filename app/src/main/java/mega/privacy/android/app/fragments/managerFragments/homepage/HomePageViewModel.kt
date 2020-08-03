package mega.privacy.android.app.fragments.managerFragments.homepage

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader.TileMode.CLAMP
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.listeners.BaseMegaChatListener
import mega.privacy.android.app.listeners.BaseMegaGlobalListener
import mega.privacy.android.app.listeners.BaseMegaRequestListener
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtils.isFileAvailable
import mega.privacy.android.app.utils.PreviewUtils.calculateInSampleSize
import mega.privacy.android.app.utils.RxUtil.logErr
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi.STATUS_AWAY
import nz.mega.sdk.MegaChatApi.STATUS_BUSY
import nz.mega.sdk.MegaChatApi.STATUS_OFFLINE
import nz.mega.sdk.MegaChatApi.STATUS_ONLINE
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUserAlert
import java.util.ArrayList

class HomePageViewModel @ViewModelInject constructor(
    private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid
) : BaseRxViewModel(), BaseMegaGlobalListener, BaseMegaChatListener {
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
        add(Single.just(true)
                .subscribeOn(Schedulers.io())
                .map {
                    val avatar = buildAvatarFile(getApplication(), megaApi.myEmail + ".jpg")
                    if (!isFileAvailable(avatar) || avatar.length() == 0L) {
                        createAvatar()
                        return@map null
                    }

                    val options = Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(avatar.absolutePath, options)

                    options.inSampleSize = calculateInSampleSize(options, 250, 250)

                    options.inJustDecodeBounds = false
                    val bitmap = BitmapFactory.decodeFile(avatar.absolutePath, options)
                    if (bitmap == null) {
                        avatar.delete()
                        createAvatar()
                        return@map null
                    }

                    val circleBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, ARGB_8888)
                    val shader = BitmapShader(bitmap, CLAMP, CLAMP)
                    val paint = Paint()
                    paint.shader = shader
                    val c = Canvas(circleBitmap)
                    val radius =
                        if (bitmap.width < bitmap.height) bitmap.width / 2 else bitmap.height / 2
                    c.drawCircle(
                            bitmap.width / 2F, bitmap.height / 2F, radius.toFloat(), paint
                    )
                    return@map circleBitmap
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer {
                    if (it != null) {
                        _avatar.value = it
                    }
                }, logErr("loadAvatar"))
        )
    }

    private fun createAvatar() {
        megaApi.getUserAvatar(
                megaApi.myUser,
                buildAvatarFile(getApplication(), megaApi.myEmail + ".jpg").absolutePath,
                object : BaseMegaRequestListener {
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
