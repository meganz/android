package mega.privacy.android.app.meeting.list

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.facebook.drawee.drawable.ScalingUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.internal.ViewUtils.dpToPx
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetMeetingDetailBinding
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoActivity
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.setImageRequestFromUri
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import javax.inject.Inject

/**
 * Meeting list bottom sheet dialog fragment that displays meeting options
 */
@AndroidEntryPoint
class MeetingListBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    @Inject
    lateinit var getFeatureFlag: GetFeatureFlagValue

    companion object {
        private const val TAG = "MeetingListBottomSheetDialogFragment"
        private const val CHAT_ID = "CHAT_ID"
        private const val SCHEDULED_MEETING_ID = "SCHEDULED_MEETING_ID"

        fun newInstance(chatId: Long): MeetingListBottomSheetDialogFragment =
            MeetingListBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(CHAT_ID, chatId)
                }
            }
    }

    private lateinit var binding: BottomSheetMeetingDetailBinding

    private val chatId by lazy {
        arguments?.getLong(CHAT_ID,
            MEGACHAT_INVALID_HANDLE) ?: MEGACHAT_INVALID_HANDLE
    }

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
        binding.header.txtLastMessage.isVisible = false
        binding.header.root.updateLayoutParams {
            @SuppressLint("RestrictedApi")
            height = dpToPx(requireContext(), 71).toInt()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.txtTimestamp.text = StringResourcesUtils.getString(R.string.context_meeting)

        viewModel.signalChatPresence()
        viewModel.getMeeting(chatId).observe(viewLifecycleOwner, ::showMeeting)
    }

    private fun showMeeting(meeting: MeetingItem.Data?) {
        requireNotNull(meeting) { "Meeting not found" }

        binding.header.txtTitle.text = meeting.title

        val firstUserPlaceholder = meeting.firstUser.getImagePlaceholder(requireContext())
        if (meeting.isSingleMeeting() || meeting.lastUser == null) {
            binding.header.imgThumbnail.hierarchy.setPlaceholderImage(
                firstUserPlaceholder,
                ScalingUtils.ScaleType.FIT_CENTER
            )
            binding.header.imgThumbnail.setImageRequestFromUri(meeting.firstUser.avatar)
            binding.header.groupThumbnails.isVisible = false
            binding.header.imgThumbnail.isVisible = true
        } else {
            val lastUserPlaceholder = meeting.lastUser.getImagePlaceholder(requireContext())
            binding.header.imgThumbnailGroupFirst.hierarchy.setPlaceholderImage(
                firstUserPlaceholder,
                ScalingUtils.ScaleType.FIT_CENTER
            )
            binding.header.imgThumbnailGroupLast.hierarchy.setPlaceholderImage(
                lastUserPlaceholder,
                ScalingUtils.ScaleType.FIT_CENTER
            )
            binding.header.imgThumbnailGroupFirst.setImageRequestFromUri(meeting.firstUser.avatar)
            binding.header.imgThumbnailGroupLast.setImageRequestFromUri(meeting.lastUser.avatar)
            binding.header.groupThumbnails.isVisible = true
            binding.header.imgThumbnail.isVisible = false
        }

        binding.btnLeave.isVisible = meeting.isActive
        binding.dividerArchive.isVisible = meeting.isActive

        binding.btnInfo.setOnClickListener {
            activity?.lifecycleScope?.launch {
                val scheduleMeetingEnabled = getFeatureFlag(AppFeatures.ScheduleMeeting)
                val intent = if (scheduleMeetingEnabled) {
                    Intent(context, ScheduledMeetingInfoActivity::class.java).apply {
                        putExtra(CHAT_ID, chatId)
                        putExtra(SCHEDULED_MEETING_ID, MEGACHAT_INVALID_HANDLE)
                    }
                } else {
                    Intent(context, GroupChatInfoActivity::class.java).apply {
                        putExtra(Constants.HANDLE, chatId)
                        putExtra(Constants.ACTION_CHAT_OPEN, true)
                    }
                }
                activity?.startActivity(intent)
            }

            dismissAllowingStateLoss()
        }

        if (meeting.isMuted) {
            binding.btnMute.text = StringResourcesUtils.getString(R.string.general_unmute)
            binding.btnMute.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_unmute,
                0,
                0,
                0)
        } else {
            binding.btnMute.text = StringResourcesUtils.getString(R.string.general_mute)
            binding.btnMute.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_mute,
                0,
                0,
                0)
        }
        binding.btnMute.setOnClickListener {
            if (meeting.isMuted) {
                MegaApplication.getPushNotificationSettingManagement()
                    .controlMuteNotificationsOfAChat(
                        requireContext(),
                        Constants.NOTIFICATIONS_ENABLED,
                        chatId)
            } else {
                ChatUtil.createMuteNotificationsAlertDialogOfAChat(requireActivity(), chatId)
            }
            dismissAllowingStateLoss()
        }

        binding.btnClearHistory.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_properties_chat_clear)
                .setMessage(StringResourcesUtils.getString(R.string.confirmation_clear_chat_history))
                .setPositiveButton(R.string.general_clear) { _, _ ->
                    viewModel.clearChatHistory(chatId)
                    dismissAllowingStateLoss()
                }
                .setNegativeButton(StringResourcesUtils.getString(R.string.general_cancel), null)
                .show()
        }
        binding.btnClearHistory.isVisible = meeting.hasPermissions
        binding.dividerClear.isVisible = binding.btnClearHistory.isVisible

        binding.btnArchive.setOnClickListener {
            viewModel.archiveChat(chatId)
            dismissAllowingStateLoss()
        }

        binding.btnLeave.setOnClickListener {
            showLeaveChatDialog()
            dismissAllowingStateLoss()
        }
    }

    /**
     * Show leave chat dialog
     */
    private fun showLeaveChatDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .setTitle(StringResourcesUtils.getString(R.string.title_confirmation_leave_group_chat))
            .setMessage(StringResourcesUtils.getString(R.string.confirmation_leave_group_chat))
            .setPositiveButton(StringResourcesUtils.getString(R.string.general_leave)) { _, _ ->
                viewModel.leaveChat(chatId)
                dismissAllowingStateLoss()
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
