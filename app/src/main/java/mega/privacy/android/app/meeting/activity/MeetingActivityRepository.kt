package mega.privacy.android.app.meeting.activity

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatRequestListenerInterface
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
     * @param bOn enable / disable
     * @param listener receive information about requests
     */
    fun switchMic(bOn: Boolean, listener: MegaChatRequestListenerInterface) {
        if(bOn) {
            megaChatApi.enableAudio(MegaChatApiJava.MEGACHAT_INVALID_HANDLE, listener)
        } else{
            megaChatApi.disableAudio(MegaChatApiJava.MEGACHAT_INVALID_HANDLE, listener)
        }
    }

    /**
     * Enable or disable Camera
     *
     * @param bOn enable / disable
     * @param listener receive information about requests
     */
    fun switchCamera(bOn: Boolean, listener: MegaChatRequestListenerInterface) {
        if (bOn) {
            megaChatApi.openVideoDevice(listener)
        } else {
            megaChatApi.releaseVideoDevice(listener)
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
}