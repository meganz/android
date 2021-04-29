package mega.privacy.android.app.meeting.fragments

import android.content.Context
import android.graphics.Bitmap
import android.util.Pair
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.AvatarUtil.getCircleAvatar
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AbstractMeetingOnBoardingRepository @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    @ApplicationContext private val context: Context
) {
    /**
     * Retrieve the color determined for an avatar.
     *
     * @return The default avatar color.
     */
    suspend fun getDefaultAvatar(): Bitmap = withContext(Dispatchers.IO) {
        AvatarUtil.getDefaultAvatar(
            getColorAvatar(megaApi.myUser), megaChatApi.myFullname, Constants.AVATAR_SIZE, true
        )
    }

    /**
     * Get the round actual avatar
     *
     * @return Pair<Boolean, Bitmap> <true, bitmap> if succeed, or <false, null>
     */
    suspend fun loadAvatar(): Pair<Boolean, Bitmap>? = withContext(Dispatchers.IO) {
        getCircleAvatar(context, megaApi.myEmail)
    }

    /**
     * Get the actual avatar from the server and save it to the cache folder
     */
    suspend fun createAvatar(listener: BaseListener) = withContext(Dispatchers.IO) {
        megaApi.getUserAvatar(
            megaApi.myUser,
            CacheFolderManager.buildAvatarFile(context, megaApi.myEmail + ".jpg").absolutePath,
            listener
        )
    }

    /**
     *  Select the video device to be used in calls
     */
    fun setChatVideoInDevice(cameraDevice: String, listener: MegaChatRequestListenerInterface?) {
        megaChatApi.setChatVideoInDevice(cameraDevice, listener)
    }

    fun getChatRoom(chatId: Long) : MegaChatRoom {
        return megaChatApi.getChatRoom(chatId)
    }

    fun getChatCall(chatId: Long) : MegaChatCall {
        return megaChatApi.getChatCall(chatId)
    }

    fun isMe(peerId: Long): Boolean {
        return peerId == megaChatApi.myUserHandle
    }

    fun activateLocalVideo(chatId:Long, listener: MeetingVideoListener){
        megaChatApi.addChatLocalVideoListener(chatId, listener)
    }

    fun activateRemoteVideo(chatId:Long, clientId: Long, hiRes: Boolean, listener: MeetingVideoListener){
        megaChatApi.addChatRemoteVideoListener(chatId, clientId, hiRes, listener)
    }

    fun closeLocalVideo(chatId:Long, listener: MeetingVideoListener){
        megaChatApi.removeChatVideoListener(chatId, MEGACHAT_INVALID_HANDLE, false, listener);
    }

    fun closeRemoteVideo(chatId:Long, clientId: Long, hiRes: Boolean, listener: MeetingVideoListener){
        megaChatApi.removeChatVideoListener(chatId, clientId, hiRes, listener);
    }
}