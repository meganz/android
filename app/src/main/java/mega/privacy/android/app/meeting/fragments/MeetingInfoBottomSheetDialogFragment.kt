package mega.privacy.android.app.meeting.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.twemoji.EmojiEditText
import mega.privacy.android.app.databinding.FragmentMeetingInfoBinding
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.listenAction
import mega.privacy.android.app.presentation.meeting.model.InMeetingUiState
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.showSnackbar
import timber.log.Timber

/**
 * Fragment shows the basic information of meeting
 */
class MeetingInfoBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentMeetingInfoBinding
    private val inMeetingViewModel by lazy { (parentFragment as InMeetingFragment).inMeetingViewModel }
    private val shareViewModel: MeetingActivityViewModel by activityViewModels()
    private var changeTitleDialog: AlertDialog? = null
    private var chatTitle: String = ""
    private var chatLink: String = ""

    override fun onStart() {
        super.onStart()

        val dialog = dialog ?: return
        BottomSheetBehavior.from(dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)).state =
            BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeetingInfoBinding.inflate(LayoutInflater.from(context), null, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    /**
     * Init views
     */
    private fun initView() {
        viewLifecycleOwner.collectFlow(inMeetingViewModel.state) { state: InMeetingUiState ->
            if (state.chatTitle.isNotEmpty()) {
                chatTitle = state.chatTitle
                binding.meetingName.text = state.chatTitle
            }

            binding.moderatorName.text = state.updateModeratorsName
            binding.moderatorName.isVisible = state.updateModeratorsName.isNotEmpty()

            binding.participantSize.text =
                resources.getQuantityString(
                    R.plurals.meeting_call_screen_meeting_info_bottom_panel_num_of_participants,
                    state.updateNumParticipants,
                    state.updateNumParticipants
                )
        }

        viewLifecycleOwner.collectFlow(shareViewModel.state) { state: MeetingState ->
            if (state.meetingLink.isNotEmpty()) {
                binding.copyLink.isVisible = true
                binding.copyLink.text = state.meetingLink
                chatLink = state.meetingLink
                binding.copyLink.setOnClickListener { copyLink() }
            }
        }
        initAction()
    }

    /**
     * Update views when the meeting is ready
     */
    fun updateView() {
        binding.edit.isVisible = inMeetingViewModel.isModerator()
        binding.shareLink.isVisible = inMeetingViewModel.isChatRoomPublic()
        binding.invite.isVisible = inMeetingViewModel.isModerator()
    }

    /**
     * Init the click listener for buttons
     */
    private fun initAction() {
        listenAction(binding.invite) { (parentFragment as InMeetingFragment).onInviteParticipants() }
        listenAction(binding.copyLink) { copyLink() }
        listenAction(binding.edit) { showRenameGroupDialog() }
    }

    /**
     * Copy link
     */
    private fun copyLink() {
        Timber.d("copyLink")
        if (chatLink.isNotEmpty()) {
            val clipboard =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", chatLink)
            clipboard.setPrimaryClip(clip)
            showSnackbar(
                requireContext(),
                getString(R.string.copied_meeting_link)
            )
        } else {
            showSnackbar(
                requireContext(),
                getString(R.string.general_text_error)
            )
        }
    }

    /**
     * Show dialog for changing the meeting name, only for moderator
     */
    private fun showRenameGroupDialog() {
        val activity = requireActivity()
        val layout = LinearLayout(requireActivity())
        layout.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(
            Util.scaleWidthPx(20, resources.displayMetrics),
            Util.scaleHeightPx(16, resources.displayMetrics),
            Util.scaleWidthPx(17, resources.displayMetrics),
            0
        )

        val input = EmojiEditText(requireActivity())
        layout.addView(input, params)
        input.setOnLongClickListener { false }
        input.setSingleLine()
        input.hint = getString(R.string.new_meeting_name)
        input.setSelectAllOnFocus(true)
        input.setTextColor(getThemeColor(requireActivity(), android.R.attr.textColorSecondary))
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        input.setEmojiSize(Util.dp2px(Constants.EMOJI_SIZE.toFloat()))
        input.imeOptions = EditorInfo.IME_ACTION_DONE
        input.inputType = InputType.TYPE_CLASS_TEXT
        val maxAllowed = ChatUtil.getMaxAllowed(chatTitle)
        input.filters = arrayOf<InputFilter>(LengthFilter(maxAllowed))
        input.setText(chatTitle)
        val builder = MaterialAlertDialogBuilder(requireContext())
        input.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                changeTitle(input)
            } else {
                Timber.d("Other IME$actionId")
            }
            false
        }
        input.setImeActionLabel(
            getString(R.string.change_meeting_name),
            EditorInfo.IME_ACTION_DONE
        )
        builder.setTitle(R.string.change_meeting_name)
            .setPositiveButton(getString(R.string.change_pass), null)
            .setNegativeButton(android.R.string.cancel, null)
            .setView(layout)
            .setOnDismissListener {
                Util.hideKeyboard(
                    activity,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }
        changeTitleDialog = builder.create()
        changeTitleDialog?.apply {
            show()
            getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener { changeTitle(input) }
        }
    }

    /**
     * Show the incorrect information when users change meeting name
     *
     * @param input the edit view
     */
    private fun changeTitle(input: EmojiEditText) {
        val title = input.text.toString()
        when {
            TextUtil.isTextEmpty(title) -> {
                Timber.w("Input is empty")
                input.error = getString(R.string.invalid_string)
                input.requestFocus()
            }

            !ChatUtil.isAllowedTitle(title) -> {
                Timber.w("Title is too long")
                input.error = getString(R.string.title_long)
                input.requestFocus()
            }

            else -> {
                Timber.d("Positive button pressed - change title")
                inMeetingViewModel.setTitleChat(title)
                changeTitleDialog?.dismiss()
            }
        }

    }

    companion object {
        fun newInstance(): MeetingInfoBottomSheetDialogFragment {
            return MeetingInfoBottomSheetDialogFragment()
        }
    }
}