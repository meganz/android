package mega.privacy.android.app.presentation.chat.dialog

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetManageMeetingLinkBinding
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoViewModel
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaChatApiJava


/**
 * Bottom Sheet Dialog that represents the UI for a dialog with the meeting link options
 */
class ManageMeetingLinkBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    private val viewModel by activityViewModels<ScheduledMeetingInfoViewModel>()

    private lateinit var binding: BottomSheetManageMeetingLinkBinding

    private var chatRoomId: Long = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private var link: String? = null
    private var title: String = ""

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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { (chatId, _, _, _, _, _, meetingLink, chatTitle) ->
                    if (chatRoomId != chatId)
                        chatRoomId = chatId

                    link = meetingLink
                    title = chatTitle
                }
            }
        }

        return contentView
    }

    /**
     * onViewCreated()
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.copyManageMeetingLinkOption.setOnClickListener {
            viewModel.copyMeetingLink(requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            setStateBottomSheetBehaviorHidden()
        }

        binding.shareManageMeetingLinkOption.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = Constants.TYPE_TEXT_PLAIN
            sharingIntent.putExtra(Intent.EXTRA_TEXT, link)
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title)
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.context_share)))

            setStateBottomSheetBehaviorHidden()
        }

        binding.sendManageMeetingLinkOption.setOnClickListener {
            viewModel.openSendToChat(true)
            setStateBottomSheetBehaviorHidden()
        }

        super.onViewCreated(view, savedInstanceState)
    }
}