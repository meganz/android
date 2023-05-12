package mega.privacy.android.app.meeting.listeners

import android.content.Context
import android.text.TextUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

class MeetingAvatarListener(private val context: Context, private val peerId: Long) : MegaRequestListenerInterface {
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (e.errorCode == MegaError.API_OK) {
            if (TextUtils.isEmpty(request.email)) {
                val avatar = CacheFolderManager.buildAvatarFile(context, request.email + ".jpg")
                if (FileUtil.isFileAvailable(avatar)) {
                    LiveEventBus.get(
                        EventConstants.EVENT_MEETING_GET_AVATAR,
                        Long::class.java
                    ).post(peerId)
                }
            }
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
    }
}