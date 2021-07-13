package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_JOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_REJOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_START
import mega.privacy.android.app.utils.ChatUtil.amIParticipatingInAChat
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRoom

@AndroidEntryPoint
class JoinMeetingFragment : AbstractMeetingOnBoardingFragment() {

    override fun onMeetingButtonClick() {
        if (chatId == MEGACHAT_INVALID_HANDLE) {
            logError("Chat Id is invalid when join meeting")
            return
        }

        releaseVideoDeviceAndRemoveChatVideoListener()
        if (amIParticipatingInAChat(chatId)) {
            logDebug("I am a member of the chat, just answer the call")
            findNavController().navigate(
                JoinMeetingFragmentDirections
                    .actionGlobalInMeeting(MEETING_ACTION_START)
            )
        } else {
            val chatRoom = sharedModel.getSpecificChat(chatId)
            if (chatRoom != null && chatRoom.ownPrivilege == MegaChatRoom.PRIV_RM) {
                logDebug("I was a member of the chat but was removed, I have to re-join")
                findNavController().navigate(
                    JoinMeetingFragmentDirections
                        .actionGlobalInMeeting(
                            MEETING_ACTION_REJOIN,
                            chatId,
                            publicChatHandle = publicChatHandle,
                            meetingName,
                            meetingLink
                        )
                )
            } else {
                logDebug("I am not a member of the chat. I have to auto-join")
                findNavController().navigate(
                    JoinMeetingFragmentDirections
                        .actionGlobalInMeeting(
                            MEETING_ACTION_JOIN,
                            chatId,
                            publicChatHandle = publicChatHandle,
                            meetingName,
                            meetingLink
                        )
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRTCAudioManager()

        btn_start_join_meeting.text = StringResourcesUtils.getString(R.string.join_meeting)
        type_meeting_edit_text.visibility = View.GONE
    }
}
