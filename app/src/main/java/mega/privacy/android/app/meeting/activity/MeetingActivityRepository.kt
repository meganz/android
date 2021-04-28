package mega.privacy.android.app.meeting.activity

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingActivityRepository @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    @ApplicationContext private val context: Context
) {
    /**
     * Enable or disable Mic
     *
     * @param chatId Chat ID
     * @param bOn enable / disable
     * @param listener receive information about requests
     */
    fun switchMic(chatId: Long, bOn: Boolean, listener: MegaChatRequestListenerInterface) {
        if (bOn) {
            megaChatApi.enableAudio(chatId, listener)
        } else {
            megaChatApi.disableAudio(chatId, listener)
        }
    }

    /**
     * Enable or disable Camera before starting a meeting.
     *
     * @param bOn enable / disable
     * @param listener receive information about requests
     */
    fun switchCameraBeforeStartMeeting(bOn: Boolean, listener: MegaChatRequestListenerInterface) {
        if (bOn) {
            megaChatApi.openVideoDevice(listener)
        } else {
            megaChatApi.releaseVideoDevice(listener)
        }
    }

    /**
     * Enable or disable Camera during a meeting.
     *
     * @param chatId Chat ID
     * @param bOn enable / disable
     * @param listener receive information about requests
     */
    fun switchCamera(chatId: Long, bOn: Boolean, listener: MegaChatRequestListenerInterface) {
        if (bOn) {
            megaChatApi.enableVideo(chatId, listener)
        } else {
            megaChatApi.disableVideo(chatId, listener)
        }
    }

    /**
     * Select new output audio
     *
     * @param device AudioDevice
     */
    fun switchSpeaker(device: AppRTCAudioManager.AudioDevice) {
        if (MegaApplication.getInstance().audioManager != null) {
            MegaApplication.getInstance().audioManager.selectAudioDevice(
                device,
                false
            )
        }
    }

    fun getChatRoom(chatId: Long): MegaChatRoom? {
        return when (chatId) {
            MEGACHAT_INVALID_HANDLE -> {
                null
            }
            else -> {
                megaChatApi.getChatRoom(chatId)
            }
        }
    }

    fun getMeeting(chatId: Long): MegaChatCall? {
        return when (chatId) {
            MEGACHAT_INVALID_HANDLE -> {
                null
            }
            else -> {
                megaChatApi.getChatCall(chatId)
            }
        }
    }

    fun getInitialMeetingName(): String {
        return StringResourcesUtils.getString(
            R.string.type_meeting_name, megaChatApi.myFullname
        )
    }

    fun setTitleChatRoom(chatId: Long, newTitle: String, listener: MegaChatRequestListenerInterface) {
        megaChatApi.setChatTitle(chatId, newTitle, listener)
    }

    fun startMeeting(chatId: Long, listener: MegaChatRequestListenerInterface) {
        megaChatApi.startChatCall(chatId, true, true, listener)
    }
}