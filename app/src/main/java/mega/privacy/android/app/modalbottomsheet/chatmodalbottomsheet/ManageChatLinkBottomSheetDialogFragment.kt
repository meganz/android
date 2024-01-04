package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.content.ClipData
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
import mega.privacy.android.app.databinding.BottomSheetManageChatLinkBinding
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoViewModel
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.app.utils.ChatUtil.showConfirmationRemoveChatLink
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.COPIED_TEXT_LABEL
import mega.privacy.android.app.utils.Util.showSnackbar

class ManageChatLinkBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private const val CHAT_LINK = "CHAT_LINK"
        private const val IS_MODERATOR = "IS_MODERATOR"
        private const val IS_MEETING = "IS_MEETING"
        private const val CHAT_TITLE = "CHAT_TITLE"
    }

    private val viewModel by activityViewModels<ScheduledMeetingInfoViewModel>()

    private lateinit var binding: BottomSheetManageChatLinkBinding

    private var chatLink = ""
    private var isModerator = false
    private var chatTitle: String? = null
    private var isMeeting: Boolean = false
    private var myFullName: String? = ""

    fun setValues(chatLink: String, isModerator: Boolean, chatTitle: String?, isMeeting: Boolean) {
        this.chatLink = chatLink
        this.isModerator = isModerator
        this.chatTitle = chatTitle
        this.isMeeting = isMeeting
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = BottomSheetManageChatLinkBinding.inflate(layoutInflater)
        contentView = binding.root.rootView
        itemsLayout = binding.itemsLayout

        if (savedInstanceState != null) {
            chatLink = savedInstanceState.getString(CHAT_LINK, "")
            isModerator = savedInstanceState.getBoolean(IS_MODERATOR, false)
            chatTitle = savedInstanceState.getString(CHAT_TITLE, null)
            isMeeting = savedInstanceState.getBoolean(IS_MEETING, false)
        }

        collectFlows()

        return contentView
    }

    private fun collectFlows() {
        collectFlow(viewModel.state) { state: ScheduledMeetingInfoState ->
            myFullName = state.myFullName
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.copyManageChatLinkOption.setOnClickListener {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(COPIED_TEXT_LABEL, chatLink)
            clipboard.setPrimaryClip(clip)

            showSnackbar(
                requireActivity(),
                getString(R.string.chat_link_copied_clipboard)
            )

            setStateBottomSheetBehaviorHidden()
        }

        binding.shareManageChatLinkOption.setOnClickListener {
            val subject =
                if (isMeeting) getString(R.string.meetings_sharing_meeting_link_meeting_invite_subject) else chatTitle

            val body = StringBuilder()

            if (isMeeting) {
                body.append(getString(R.string.meetings_sharing_meeting_link_title, myFullName))
                    .append("\n\n")
                    .append(
                        getString(
                            R.string.meetings_sharing_meeting_link_meeting_name,
                            chatTitle
                        )
                    )
                    .append("\n")
                    .append(
                        getString(
                            R.string.meetings_sharing_meeting_link_meeting_link,
                            chatLink
                        )
                    )
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = Constants.TYPE_TEXT_PLAIN
                putExtra(Intent.EXTRA_TEXT, if (isMeeting) body.toString() else chatLink)
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }

            startActivity(
                Intent.createChooser(
                    intent,
                    if (isMeeting) " " else getString(R.string.context_share)
                )
            )

            setStateBottomSheetBehaviorHidden()
        }

        if (!isModerator) binding.deleteManageChatLinkOption.visibility = View.GONE
        else binding.deleteManageChatLinkOption.setOnClickListener {
            showConfirmationRemoveChatLink(requireActivity())
            setStateBottomSheetBehaviorHidden()
        }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(CHAT_LINK, chatLink)
        outState.putBoolean(IS_MODERATOR, isModerator)
        outState.putString(CHAT_TITLE, chatTitle)
        outState.putBoolean(IS_MEETING, isMeeting)

        super.onSaveInstanceState(outState)
    }
}