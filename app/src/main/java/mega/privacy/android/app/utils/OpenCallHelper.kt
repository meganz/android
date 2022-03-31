package mega.privacy.android.app.utils

import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_CHAT_ID
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_IS_GUEST
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_NAME
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_PUBLIC_CHAT_HANDLE
import mega.privacy.android.app.presentation.calls.facade.OpenCallWrapper
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * The helper class for open calls
 */
class OpenCallHelper @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) : OpenCallWrapper {

    fun isEphemeralPlusPlusAccount(): Boolean {
        return megaApi.isEphemeralPlusPlus
    }

    override fun getIntentForOpenCreateMeeting(context: Context, actionForCall: String): Intent {
        return Intent(context, MeetingActivity::class.java).apply {
            action = actionForCall
            flags = FLAG_ACTIVITY_NEW_TASK
        }
    }

    override fun getIntentForOpenRingingCall(
        context: Context,
        actionForCall: String,
        chatId: Long
    ): Intent {
        return Intent(context, MeetingActivity::class.java).apply {
            putExtra(MEETING_CHAT_ID, chatId)
            action = actionForCall
            flags = FLAG_ACTIVITY_NEW_TASK
        }
    }

    override fun getIntentForOpenOngoingCall(
        context: Context,
        actionForCall: String,
        chatId: Long,
        isAudioEnabled: Boolean?,
        isVideoEnabled: Boolean?,
    ): Intent {
        return Intent(context, MeetingActivity::class.java).apply {
            putExtra(MEETING_CHAT_ID, chatId)
            action = actionForCall
            putExtra(MEETING_IS_GUEST, isEphemeralPlusPlusAccount())
            isAudioEnabled?.let {
                putExtra(MeetingActivity.MEETING_AUDIO_ENABLE, it)
            }

            isVideoEnabled?.let {
                putExtra(MeetingActivity.MEETING_VIDEO_ENABLE, it)
            }
            flags = FLAG_ACTIVITY_NEW_TASK
        }
    }

    override fun getIntentForOpenJoinMeeting(
        context: Context,
        actionForCall: String,
        chatId: Long,
        meetingName: String,
        meetingLink: String,
        publicChatHandle: Long?
    ): Intent {
        return Intent(context, MeetingActivity::class.java).apply {
            putExtra(MEETING_CHAT_ID, chatId)
            action = actionForCall
            putExtra(MEETING_NAME, meetingName)
            putExtra(MEETING_IS_GUEST, isEphemeralPlusPlusAccount())
            data = Uri.parse(meetingLink)

            publicChatHandle?.let {
                putExtra(MEETING_PUBLIC_CHAT_HANDLE, publicChatHandle)
            }
            flags = FLAG_ACTIVITY_NEW_TASK
        }
    }
}