package mega.privacy.android.app.presentation.chat.dialog

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.BottomSheetManageMeetingLinkBinding
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoViewModel
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingManagementViewModel
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ScheduledMeetingDateUtil
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber

/**
 * Bottom Sheet Dialog that represents the UI for a dialog with the meeting link options
 */
class ManageMeetingLinkBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    private val viewModel by activityViewModels<ScheduledMeetingInfoViewModel>()
    private val scheduledMeetingManagementViewModel by activityViewModels<ScheduledMeetingManagementViewModel>()

    private lateinit var binding: BottomSheetManageMeetingLinkBinding

    private var chatRoomId: Long = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private var link: String? = null
    private var title: String = ""
    private var chatScheduledMeeting: ChatScheduledMeeting? = null
    private var myFullName: String = ""

    /**
     * onCreateView()
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = BottomSheetManageMeetingLinkBinding.inflate(layoutInflater)
        contentView = binding.root.rootView
        itemsLayout = binding.itemsLayout

        collectFlows()

        return contentView
    }

    private fun collectFlows() {
        collectFlow(viewModel.state) { state: ScheduledMeetingInfoState ->
            if (chatRoomId != state.chatId)
                chatRoomId = state.chatId

            title = state.chatTitle
            chatScheduledMeeting = state.scheduledMeeting
            myFullName = state.myFullName
        }

        viewLifecycleOwner.collectFlow(scheduledMeetingManagementViewModel.state) { (_, _, _, _, _, _, _, _, _, meetingLink) ->
            link = meetingLink
        }
    }

    /**
     * onViewCreated()
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.copyManageMeetingLinkOption.setOnClickListener {
            scheduledMeetingManagementViewModel.copyMeetingLink(
                requireContext().getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager
            )
            setStateBottomSheetBehaviorHidden()
        }

        binding.shareManageMeetingLinkOption.setOnClickListener {
            val subject = getString(R.string.meetings_sharing_meeting_link_meeting_invite_subject)
            val message = getString(R.string.meetings_sharing_meeting_link_title, myFullName)
            val meetingName =
                getString(R.string.meetings_sharing_meeting_link_meeting_name, title)
            val meetingLink =
                getString(R.string.meetings_sharing_meeting_link_meeting_link, link)

            val body = StringBuilder()
            body.append(message)
                .append("\n\n")
                .append(meetingName)
                .append("\n")

            chatScheduledMeeting?.let {
                val meetingDateAndTime = getString(
                    R.string.meetings_sharing_meeting_link_meeting_date_and_time,
                    ScheduledMeetingDateUtil.getAppropriateStringForScheduledMeetingDate(
                        requireContext(),
                        viewModel.is24HourFormat,
                        it
                    )
                )
                body.append(meetingDateAndTime)
                    .append("\n")
            }

            body.append(meetingLink)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = Constants.TYPE_TEXT_PLAIN
                putExtra(Intent.EXTRA_TEXT, body.toString())
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }

            startActivity(Intent.createChooser(intent, " "))

            setStateBottomSheetBehaviorHidden()
        }

        binding.sendManageMeetingLinkOption.setOnClickListener {
            viewModel.openSendToChat(true)
            setStateBottomSheetBehaviorHidden()
        }

        super.onViewCreated(view, savedInstanceState)
    }
}