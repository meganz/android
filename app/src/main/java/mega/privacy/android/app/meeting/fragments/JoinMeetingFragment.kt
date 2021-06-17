package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_JOIN
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

@AndroidEntryPoint
class JoinMeetingFragment : AbstractMeetingOnBoardingFragment() {

    override fun onMeetingButtonClick() {
        if (chatId == MEGACHAT_INVALID_HANDLE) {
            logError("Chat Id is invalid when join meeting")
            return
        }

        releaseVideoDeviceAndRemoveChatVideoListener()
        val action = JoinMeetingFragmentDirections
            .actionGlobalInMeeting(MEETING_ACTION_JOIN, chatId, meetingName, meetingLink)
        findNavController().navigate(action)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRTCAudioManager()

        btn_start_join_meeting.setText(R.string.btn_join_meeting)
        type_meeting_edit_text.visibility = View.GONE
    }
}
