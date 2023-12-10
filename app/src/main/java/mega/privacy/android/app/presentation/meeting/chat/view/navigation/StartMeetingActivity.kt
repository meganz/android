package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.meeting.activity.MeetingActivity

internal fun startMeetingActivity(
    context: Context,
    chatId: Long,
    enableAudio: Boolean? = null,
    enableVideo: Boolean? = null,
) {
    context.startActivity(Intent(context, MeetingActivity::class.java).apply {
        action =
            if (enableAudio != null && !enableAudio) MeetingActivity.MEETING_ACTION_RINGING
            else MeetingActivity.MEETING_ACTION_IN

        putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
        enableAudio?.let { putExtra(MeetingActivity.MEETING_AUDIO_ENABLE, it) }
        enableVideo?.let { putExtra(MeetingActivity.MEETING_VIDEO_ENABLE, it) }
        addFlags(if (enableAudio != null) Intent.FLAG_ACTIVITY_NEW_TASK else Intent.FLAG_ACTIVITY_CLEAR_TOP)
    })
}