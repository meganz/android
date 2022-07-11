package mega.privacy.android.app.meeting.list

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.facebook.drawee.drawable.ScalingUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.internal.ViewUtils.dpToPx
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetMeetingDetailBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ExtraUtils.extraNotNull
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.setImageRequestFromUri

class MeetingListBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private const val TAG = "MeetingListBottomSheetDialogFragment"
        private const val CHAT_ID = "CHAT_ID"

        fun newInstance(chatId: Long): MeetingListBottomSheetDialogFragment =
            MeetingListBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(CHAT_ID, chatId)
                }
            }
    }

    private lateinit var binding: BottomSheetMeetingDetailBinding
    private val chatId by extraNotNull<Long>(CHAT_ID)
    private val viewModel by viewModels<MeetingListViewModel>({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = BottomSheetMeetingDetailBinding.inflate(layoutInflater, container, false)
        contentView = binding.root
        itemsLayout = binding.itemsLayout
        binding.header.btnMore.isVisible = false
        binding.header.txtTimestamp.isVisible = false
        binding.header.root.updateLayoutParams { height = dpToPx(requireContext(), 71).toInt() }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.getMeeting(chatId).observe(viewLifecycleOwner) { meeting ->
            requireNotNull(meeting) { "Meeting not found" }

            binding.header.txtTitle.text = meeting.title
            binding.header.txtLastMessage.text = meeting.formattedDate

            val firstUserPlaceholder = meeting.firstUser.getImagePlaceholder(requireContext())
            if (meeting.isSingleMeeting()) {
                binding.header.imgThumbnail.hierarchy.setPlaceholderImage(
                    firstUserPlaceholder,
                    ScalingUtils.ScaleType.FIT_CENTER)
                binding.header.imgThumbnail.setImageRequestFromUri(meeting.firstUser.avatar)
                binding.header.imgThumbnailGroupFirst.isVisible = false
                binding.header.imgThumbnailGroupLast.isVisible = false
                binding.header.imgThumbnail.isVisible = true
            } else {
                val lastUserPlaceholder = meeting.lastUser!!.getImagePlaceholder(requireContext())
                binding.header.imgThumbnailGroupFirst.hierarchy.setPlaceholderImage(
                    firstUserPlaceholder,
                    ScalingUtils.ScaleType.FIT_CENTER)
                binding.header.imgThumbnailGroupLast.hierarchy.setPlaceholderImage(
                    lastUserPlaceholder,
                    ScalingUtils.ScaleType.FIT_CENTER)
                binding.header.imgThumbnailGroupFirst.setImageRequestFromUri(meeting.firstUser.avatar)
                binding.header.imgThumbnailGroupLast.setImageRequestFromUri(meeting.lastUser.avatar)
                binding.header.imgThumbnailGroupFirst.isVisible = true
                binding.header.imgThumbnailGroupLast.isVisible = true
                binding.header.imgThumbnail.isVisible = false
            }

            binding.btnInfo.setOnClickListener {
                val intent = Intent(context, GroupChatInfoActivity::class.java).apply {
                    putExtra(Constants.HANDLE, chatId)
                    putExtra(Constants.ACTION_CHAT_OPEN, true)
                }
                activity?.startActivity(intent)
                dismissAllowingStateLoss()
            }

            binding.btnMute.setOnClickListener {
                ChatUtil.createMuteNotificationsAlertDialogOfAChat(requireActivity(), chatId)
                dismissAllowingStateLoss()
            }

            binding.btnArchive.setOnClickListener {
                viewModel.archiveChat(chatId)
                dismissAllowingStateLoss()
            }

            binding.btnLeave.setOnClickListener {
                showLeaveChatDialog()
                dismissAllowingStateLoss()
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    private fun showLeaveChatDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .setTitle(StringResourcesUtils.getString(R.string.title_confirmation_leave_group_chat))
            .setMessage(StringResourcesUtils.getString(R.string.confirmation_leave_group_chat))
            .setPositiveButton(StringResourcesUtils.getString(R.string.general_leave)) { _: DialogInterface?, _: Int ->
                viewModel.leaveChat(chatId)
            }
            .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
            .show()
    }

    /**
     * Custom show method to avoid showing the same dialog multiple times
     */
    fun show(manager: FragmentManager) {
        if (manager.findFragmentByTag(TAG) == null) super.show(manager, TAG)
    }
}
