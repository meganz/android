package mega.privacy.android.app.meeting.activity

import android.content.Context
import android.graphics.Bitmap
import android.util.Pair
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.IndividualCallVideoListener
import mega.privacy.android.app.meeting.listeners.MeetingAvatarListener
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingActivityRepository @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    @ApplicationContext private val context: Context,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
) {

    /**
     * Retrieve the color determined for an avatar.
     *
     * @return The default avatar color.
     */
    suspend fun getDefaultAvatar(): Bitmap = withContext(Dispatchers.IO) {
        AvatarUtil.getDefaultAvatar(
            AvatarUtil.getColorAvatar(megaApi.myUser),
            megaChatApi.myFullname,
            Constants.AVATAR_SIZE,
            true
        )
    }

    /**
     * Get the round actual avatar
     *
     * @return Pair<Boolean, Bitmap> <true, bitmap> if succeed, or <false, null>
     */
    suspend fun loadAvatar(): Pair<Boolean, Bitmap>? = withContext(Dispatchers.IO) {
        AvatarUtil.getCircleAvatar(context, megaApi.myEmail)
    }

    /**
     * Get the actual avatar from the server and save it to the cache folder
     */
    suspend fun createAvatar(listener: BaseListener) = withContext(Dispatchers.IO) {
        megaApi.getUserAvatar(
            megaApi.myUser,
            CacheFolderManager.buildAvatarFile(context,
                megaApi.myEmail + JPG_EXTENSION)?.absolutePath,
            listener
        )
    }

    /**
     * Determine if I am a guest
     *
     * @return True, if my account is an ephemeral account. False otherwise
     */
    fun amIAGuest(): Boolean = megaApi.isEphemeralPlusPlus

    /**
     * Enable or disable Mic
     *
     * @param chatId Chat ID
     * @param shouldAudioBeEnabled True, If the audio is to be enabled. False, otherwise
     * @param listener receive information about requests
     */
    fun switchMic(
        chatId: Long,
        shouldAudioBeEnabled: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) {
        if (shouldAudioBeEnabled) {
            Timber.d("Enable local audio")
            megaChatApi.enableAudio(chatId, listener)
        } else {
            Timber.d("Disable local audio")
            megaChatApi.disableAudio(chatId, listener)
        }
    }

    /**
     * Enable or disable Camera before starting a meeting.
     *
     * @param shouldVideoBeEnabled True, If the video is to be enabled. False, otherwise
     * @param listener receive information about requests
     */
    fun switchCameraBeforeStartMeeting(
        shouldVideoBeEnabled: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) {
        if (shouldVideoBeEnabled) {
            Timber.d("Open video device")
            megaChatApi.openVideoDevice(listener)
        } else {
            Timber.d("Release video device")
            megaChatApi.releaseVideoDevice(listener)
        }
    }

    /**
     * Enable or disable Camera during a meeting.
     *
     * @param chatId Chat ID
     * @param shouldVideoBeEnabled True, If the video is to be enabled. False, otherwise
     * @param listener receive information about requests
     */
    fun switchCamera(
        chatId: Long,
        shouldVideoBeEnabled: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) {
        megaChatApi.getChatCall(chatId)?.let {
            if (shouldVideoBeEnabled && !it.hasLocalVideo()) {
                Timber.d("Enable local video")
                megaChatApi.enableVideo(chatId, listener)
            } else if (!shouldVideoBeEnabled && it.hasLocalVideo()) {
                Timber.d("Disable local video")
                megaChatApi.disableVideo(chatId, listener)
            }
        }
    }

    /**
     * Method of obtaining the local video
     *
     * @param chatId Chat ID
     * @param listener IndividualCallVideoListener
     */
    fun addLocalVideo(chatId: Long, listener: IndividualCallVideoListener) {
        Timber.d("Add Chat video listener")
        megaChatApi.addChatLocalVideoListener(chatId, listener)
    }

    /**
     * Method of remove the local video
     *
     * @param chatId Chat ID
     * @param listener IndividualCallVideoListener
     */
    fun removeLocalVideo(chatId: Long, listener: IndividualCallVideoListener) {
        Timber.d("Removed Chat video listener")
        megaChatApi.removeChatVideoListener(chatId, MEGACHAT_INVALID_HANDLE, true, listener)
    }

    /**
     *  Select the video device to be used in calls
     */
    fun setChatVideoInDevice(cameraDevice: String, listener: MegaChatRequestListenerInterface?) {
        Timber.d("Set chat video in device")
        megaChatApi.setChatVideoInDevice(cameraDevice, listener)
    }

    /**
     * Select new output audio
     *
     * @param device AudioDevice
     */
    fun switchSpeaker(device: AppRTCAudioManager.AudioDevice) {
        Timber.d("Switch the speaker")
        rtcAudioManagerGateway.audioManager?.selectAudioDevice(
            device,
            false
        )
    }

    /**
     * Method for creating a chat link
     *
     * @param chatId chat ID
     * @param listener MegaChatRequestListenerInterface
     */
    fun createChatLink(chatId: Long, listener: MegaChatRequestListenerInterface) {
        megaChatApi.createChatLink(chatId, listener)
    }

    /**
     * Method for creating a chat link
     *
     * @param chatId chat ID
     * @param listener MegaChatRequestListenerInterface
     */
    fun queryChatLink(chatId: Long, listener: MegaChatRequestListenerInterface) {
        megaChatApi.queryChatLink(chatId, listener)
    }

    /**
     * Get a specific MegaChatRoom
     *
     * @param chatId chat ID
     * @return MegaChatRoom
     */
    fun getChatRoom(chatId: Long): MegaChatRoom? {
        return when (chatId) {
            MEGACHAT_INVALID_HANDLE -> null
            else -> megaChatApi.getChatRoom(chatId)
        }
    }

    /**
     * Method for getting a participant's avatar
     *
     * @param peerId user handle of participant
     */
    fun getAvatarBitmapByPeerId(peerId: Long): Bitmap? {
        val mail = ChatController(context).getParticipantEmail(peerId)
        val userHandleString = MegaApiAndroid.userHandleToBase64(peerId)
        val myUserHandleEncoded = MegaApiAndroid.userHandleToBase64(megaApi.myUserHandleBinary)
        var bitmap = when {
            userHandleString == myUserHandleEncoded -> {
                AvatarUtil.getAvatarBitmap(mail)
            }
            TextUtil.isTextEmpty(mail) -> AvatarUtil.getAvatarBitmap(userHandleString)
            else -> AvatarUtil.getUserAvatar(
                userHandleString,
                mail
            )
        }

        if (bitmap == null) {
            megaApi.getUserAvatar(
                mail,
                CacheFolderManager.buildAvatarFile(
                    context,
                    mail + JPG_EXTENSION
                )?.absolutePath, MeetingAvatarListener(context, peerId)
            )
            bitmap = CallUtil.getDefaultAvatarCall(
                MegaApplication.getInstance().applicationContext,
                peerId
            )
        }

        return bitmap
    }

    /**
     * Change permissions to a call participant.
     *
     * @param chatId Chat ID of the call
     * @param userHandle user handle of a participant
     * @param permission type of permit to be assigned to the participant
     * @param listener MegaChatRequestListenerInterface
     */
    fun changeParticipantPermissions(
        chatId: Long,
        userHandle: Long,
        permission: Int,
        listener: MegaChatRequestListenerInterface?,
    ) {
        megaChatApi.updateChatPermissions(
            chatId,
            userHandle,
            permission,
            listener
        )
    }
}