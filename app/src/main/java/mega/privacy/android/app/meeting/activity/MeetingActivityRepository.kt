package mega.privacy.android.app.meeting.activity

import android.content.Context
import android.graphics.Bitmap
import android.util.Pair
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.AvatarUtil.getCircleAvatar
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
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

    fun switchSpeaker(bOn: Boolean): Boolean {
        //TODO:
        return true
    }
}