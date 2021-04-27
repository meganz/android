package mega.privacy.android.app.meeting.fragments

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.meeting.listeners.HangChatCallListener
import mega.privacy.android.app.meeting.listeners.SetCallOnHoldListener
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMeetingRepository @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    @ApplicationContext private val context: Context
) {

    fun setCallOnHold(chatId: Long, isHold: Boolean) {
        if (chatId != null) {
            megaChatApi.setCallOnHold(chatId, isHold, SetCallOnHoldListener(context))
        }
    }

    fun leaveMeeting(chatId: Long) {
        val call: MegaChatCall = megaChatApi.getChatCall(chatId)
        megaChatApi.hangChatCall(call.callId, HangChatCallListener(context))
    }
}