package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_JOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_REJOIN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_START
import mega.privacy.android.app.utils.ChatUtil.amIParticipatingInAChat
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRoom
import timber.log.Timber

@AndroidEntryPoint
class JoinMeetingFragment : AbstractMeetingOnBoardingFragment() {

    override fun onMeetingButtonClick() {
        if (chatId == MEGACHAT_INVALID_HANDLE) {
            Timber.e("Chat Id is invalid when join meeting")
            return
        }

        releaseVideoDeviceAndRemoveChatVideoListener()
        if (amIParticipatingInAChat(chatId)) {
            Timber.d("I am a member of the chat, just answer the call")
            findNavController().navigate(
                JoinMeetingFragmentDirections
                    .actionGlobalInMeeting(MEETING_ACTION_START)
            )
        } else {
            val chatRoom = sharedModel.getSpecificChat(chatId)
            if (chatRoom != null && chatRoom.ownPrivilege == MegaChatRoom.PRIV_RM) {
                Timber.d("I was a member of the chat but was removed, I have to re-join")
                findNavController().navigate(
                    JoinMeetingFragmentDirections
                        .actionGlobalInMeeting(
                            action = MEETING_ACTION_REJOIN,
                            chatId = chatId,
                            publicChatHandle = publicChatHandle,
                            meetingName = meetingName,
                            meetingLink = meetingLink,
                            firstName = guestFisrtName,
                            lastName = guestLastName,
                        )
                )
            } else {
                Timber.d("I am not a member of the chat. I have to auto-join")
                findNavController().navigate(
                    JoinMeetingFragmentDirections
                        .actionGlobalInMeeting(
                            action = MEETING_ACTION_JOIN,
                            chatId = chatId,
                            publicChatHandle = publicChatHandle,
                            meetingName = meetingName,
                            meetingLink = meetingLink,
                            firstName = guestFisrtName,
                            lastName = guestLastName,
                        )
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRTCAudioManager()
        binding.btnStartJoinMeeting.text = getString(R.string.join_meeting)
        binding.typeMeetingEditText.visibility = View.GONE
    }
}
