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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingActivityRepository @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    @ApplicationContext private val context: Context
) {
    fun switchMic(bOn: Boolean): Boolean {
        return true
    }

    fun switchCamera(bOn: Boolean): Boolean {
        return true
    }

    fun switchSpeaker(bOn: Boolean): Boolean {
        return true
    }
}