package mega.privacy.android.app.utils

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_CHAT_ID
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_IS_GUEST
import mega.privacy.android.app.presentation.meetings.OpenCallWrapper
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * The helper class for open calls
 */
class OpenCallHelper @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) : OpenCallWrapper {

    private fun isEphemeralPlusPlusAccount(): Boolean {
        return megaApi.isEphemeralPlusPlus
    }

    override fun getIntentForOpenOngoingCall(
        context: Context,
        chatId: Long,
        isAudioEnabled: Boolean,
        isVideoEnabled: Boolean
    ): Intent {
        return Intent(context, MeetingActivity::class.java).apply {
            putExtra(MEETING_CHAT_ID, chatId)
            putExtra(MEETING_IS_GUEST, isEphemeralPlusPlusAccount())
            putExtra(MeetingActivity.MEETING_AUDIO_ENABLE, isAudioEnabled)
            putExtra(MeetingActivity.MEETING_VIDEO_ENABLE, isVideoEnabled)
            action = MeetingActivity.MEETING_ACTION_IN
        }
    }
}