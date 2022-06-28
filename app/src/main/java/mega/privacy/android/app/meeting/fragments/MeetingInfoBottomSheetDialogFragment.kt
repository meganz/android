package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
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
import mega.privacy.android.app.components.twemoji.EmojiEditText
import mega.privacy.android.app.databinding.FragmentMeetingInfoBinding
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.listenAction
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
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

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        binding = FragmentMeetingInfoBinding.inflate(LayoutInflater.from(context), null, false)
        dialog.setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog ?: return
        BottomSheetBehavior.from(dialog.findViewById(R.id.design_bottom_sheet)).state =
            BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onResume() {
        super.onResume()
        initView()
    }

    /**
     * Init views
     */
    private fun initView() {
        inMeetingViewModel.chatTitle.observe(this) {
            chatTitle = it
            binding.meetingName.text = it
        }

        inMeetingViewModel.participants.observe(this) { participants ->
            binding.participantSize.text =
                StringResourcesUtils.getString(
                    R.string.info_participants_number,
                    participants.size + 1
                )
            binding.moderatorName.text =
                StringResourcesUtils.getString(
                    R.string.info_moderator_name,
                    inMeetingViewModel.getModeratorNames(requireActivity(), participants)
                )
        }

        shareViewModel.meetingLinkLiveData.observe(this) { link ->
            if (link.isNotEmpty()) {
                binding.copyLink.isVisible = true
                binding.copyLink.text = link
                chatLink = link
                binding.copyLink.setOnClickListener { copyLink() }
            }
        }

        shareViewModel.currentChatId.observe(this) {
            updateView()
            if (chatLink.isEmpty()) {
                (parentFragment as InMeetingFragment).onShareLink(false)
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
    fun initAction() {
        listenAction(binding.shareLink) {
            (parentFragment as InMeetingFragment).onShareLink(true)
        }
        listenAction(binding.invite) { (parentFragment as InMeetingFragment).onInviteParticipants() }
        listenAction(binding.copyLink) { copyLink() }
        listenAction(binding.edit) { showRenameGroupDialog() }
    }

    /**
     * Copy link
     */
    fun copyLink() {
        Timber.d("copyLink")
        if (chatLink.isNotEmpty()) {
            val clipboard =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", chatLink)
            clipboard.setPrimaryClip(clip)
            showSnackbar(requireContext(),
                StringResourcesUtils.getString(R.string.copied_meeting_link))
        } else {
            showSnackbar(requireContext(),
                StringResourcesUtils.getString(R.string.general_text_error))
        }
    }

    /**
     * Show dialog for changing the meeting name, only for moderator
     */
    @Suppress("DEPRECATION")
    fun showRenameGroupDialog() {
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
        input.hint = StringResourcesUtils.getString(R.string.new_meeting_name)
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
        input.setImeActionLabel(StringResourcesUtils.getString(R.string.change_meeting_name),
            EditorInfo.IME_ACTION_DONE)
        builder.setTitle(R.string.change_meeting_name)
            .setPositiveButton(StringResourcesUtils.getString(R.string.change_pass), null)
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
                input.error = StringResourcesUtils.getString(R.string.invalid_string)
                input.requestFocus()
            }
            !ChatUtil.isAllowedTitle(title) -> {
                Timber.w("Title is too long")
                input.error = StringResourcesUtils.getString(R.string.title_long)
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