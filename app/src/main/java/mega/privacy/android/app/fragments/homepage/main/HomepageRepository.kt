package mega.privacy.android.app.fragments.homepage.main

import android.content.Context
import android.graphics.Bitmap
import android.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.DefaultMegaChatListener
import mega.privacy.android.app.listeners.DefaultMegaGlobalListener
import mega.privacy.android.app.listeners.DefaultMegaRequestListener
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.*
import java.util.*
import javax.inject.Inject

class HomepageRepository @Inject constructor(
    private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    @ApplicationContext private val context: Context
) : DefaultMegaGlobalListener, DefaultMegaChatListener {

    private val _notification = MutableLiveData<Int>()
    private val _chatStatus = MutableLiveData<Int>()

    fun getNotificationLiveData(): LiveData<Int> {
        updateNotificationCount()
        return _notification
    }

    fun getChatStatusLiveData(): LiveData<Int> {
        updateChatStatus(megaChatApi.onlineStatus)
        return _chatStatus
    }

    private fun updateNotificationCount() {
        _notification.value =
            megaApi.numUnreadUserAlerts + (megaApi.incomingContactRequests?.size ?: 0)
    }

    fun registerDataListeners() {
        megaApi.addGlobalListener(this)
        megaChatApi.addChatListener(this)
    }

    fun unregisterDataListeners() {
        megaApi.removeGlobalListener(this)
        megaChatApi.removeChatListener(this)
    }

    override fun onUserAlertsUpdate(
        api: MegaApiJava,
        userAlerts: ArrayList<MegaUserAlert>?
    ) {
        updateNotificationCount()
    }

    override fun onContactRequestsUpdate(
        api: MegaApiJava,
        requests: ArrayList<MegaContactRequest>?
    ) {
        updateNotificationCount()
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
            MegaChatApi.STATUS_ONLINE -> R.drawable.ic_online
            MegaChatApi.STATUS_AWAY -> R.drawable.ic_away
            MegaChatApi.STATUS_BUSY -> R.drawable.ic_busy
            MegaChatApi.STATUS_OFFLINE -> R.drawable.ic_offline
            else -> 0
        }
    }

    suspend fun getDefaultAvatar(): Bitmap = withContext(Dispatchers.IO) {
        AvatarUtil.getDefaultAvatar(
            getColorAvatar(megaApi.myUser), megaChatApi.myFullname, Constants.AVATAR_SIZE, true
        )
    }

    suspend fun loadAvatar(): Pair<Boolean, Bitmap>? = withContext(Dispatchers.IO) {
        Util.getCircleAvatar(context, megaApi.myEmail)
    }

    suspend fun createAvatar(listener: DefaultMegaRequestListener) = withContext(Dispatchers.IO) {
        megaApi.getUserAvatar(
            megaApi.myUser,
            CacheFolderManager.buildAvatarFile(context, megaApi.myEmail + ".jpg").absolutePath,
            listener
        )
    }
}