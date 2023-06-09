package mega.privacy.android.app.presentation.meeting.list

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.facebook.drawee.drawable.ScalingUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.internal.ViewUtils.dpToPx
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.BottomSheetMeetingDetailBinding
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.presentation.meeting.RecurringMeetingInfoActivity
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoActivity
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.permission.PermissionUtils.checkMandatoryCallPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestCallPermissions
import mega.privacy.android.app.utils.setImageRequestFromFilePath
import mega.privacy.android.app.utils.view.TextDrawable
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE

/**
 * Meeting list bottom sheet dialog fragment that displays meeting options
 */
@AndroidEntryPoint
class MeetingListBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

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

    private val defaultAvatarColor by lazy {
        ContextCompat.getColor(requireContext(), R.color.grey_012_white_012)
    }

    private var currentMeeting: MeetingRoomItem? = null

    private lateinit var binding: BottomSheetMeetingDetailBinding

    private val chatId by lazy {
        arguments?.getLong(CHAT_ID, MEGACHAT_INVALID_HANDLE) ?: MEGACHAT_INVALID_HANDLE
    }

    private val viewModel by viewModels<MeetingListViewModel>({ requireParentFragment() })
    private lateinit var permissionsRequest: ActivityResultLauncher<Array<String>>

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

    /**
     * Get call permissions request
     */
    private fun getCallPermissionsRequest() =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (checkMandatoryCallPermissions(requireActivity())) {
                currentMeeting?.let { room ->
                    room.scheduledMeetingStatus?.let { scheduledMeetingStatus ->
                        when (scheduledMeetingStatus) {
                            is ScheduledMeetingStatus.NotStarted -> viewModel.startSchedMeeting(
                                room.chatId,
                                room.schedId
                            )
                            is ScheduledMeetingStatus.NotJoined -> viewModel.joinSchedMeeting(
                                room.chatId
                            )
                            else -> {}
                        }
                    }
                }

            } else {
                viewModel.updateSnackBar(R.string.allow_acces_calls_subtitle_microphone)
            }

            dismissAllowingStateLoss()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.collectFlow(
            viewModel.getMeeting(chatId),
            Lifecycle.State.RESUMED,
            ::showMeeting
        )

        permissionsRequest = getCallPermissionsRequest()
    }

    private fun showMeeting(room: MeetingRoomItem?) {
        requireNotNull(room) { "Meeting not found" }
        currentMeeting = room

        binding.header.txtTitle.text = room.title
        binding.header.txtTimestamp.setText(
            when {
                room.isRecurring() -> R.string.meetings_list_recurring_meeting_label
                room.isPending -> R.string.meetings_list_one_off_meeting_label
                else -> R.string.context_meeting
            }
        )

        if (room.firstUserChar == null && room.secondUserChar == null) {
            binding.header.groupThumbnails.isVisible = false
            binding.header.imgThumbnail.isVisible = false
        } else {
            val firstUserPlaceholder =
                getImagePlaceholder(room.firstUserChar, room.firstUserColor)
            if (room.isSingleMeeting()) {
                binding.header.imgThumbnail.hierarchy.setPlaceholderImage(
                    firstUserPlaceholder,
                    ScalingUtils.ScaleType.FIT_CENTER
                )
                binding.header.imgThumbnail.setImageRequestFromFilePath(room.firstUserAvatar)
                binding.header.groupThumbnails.isVisible = false
                binding.header.imgThumbnail.isVisible = true
            } else {
                val lastUserPlaceholder =
                    getImagePlaceholder(room.secondUserChar, room.secondUserColor)
                binding.header.imgThumbnailGroupFirst.hierarchy.setPlaceholderImage(
                    firstUserPlaceholder,
                    ScalingUtils.ScaleType.FIT_CENTER
                )
                binding.header.imgThumbnailGroupLast.hierarchy.setPlaceholderImage(
                    lastUserPlaceholder,
                    ScalingUtils.ScaleType.FIT_CENTER
                )
                binding.header.imgThumbnailGroupFirst.setImageRequestFromFilePath(room.firstUserAvatar)
                binding.header.imgThumbnailGroupLast.setImageRequestFromFilePath(room.secondUserAvatar)
                binding.header.groupThumbnails.isVisible = true
                binding.header.imgThumbnail.isVisible = false
            }
        }

        binding.btnCancel.isVisible = false // Disabled until feature implementation
        binding.dividerArchive.isVisible = binding.btnCancel.isVisible

        binding.btnRecurringMeeting.isVisible = room.isRecurring()
        binding.dividerRecurringMeeting.isVisible = binding.btnRecurringMeeting.isVisible

        binding.btnRecurringMeeting.setOnClickListener {
            activity?.startActivity(
                Intent(
                    context,
                    RecurringMeetingInfoActivity::class.java
                ).apply {
                    putExtra(CHAT_ID, chatId)
                })
            dismissAllowingStateLoss()
        }

        binding.btnStartOrJoinSchedMeeting.isVisible =
            room.isActive && room.scheduledMeetingStatus != null &&
                    room.scheduledMeetingStatus !is ScheduledMeetingStatus.Joined
        binding.dividerStartOrJoinSchedMeeting.isVisible =
            binding.btnStartOrJoinSchedMeeting.isVisible

        room.scheduledMeetingStatus?.let { scheduledMeetingStatus ->
            when (scheduledMeetingStatus) {
                is ScheduledMeetingStatus.NotStarted -> {
                    binding.btnStartOrJoinSchedMeeting.setText(R.string.meetings_list_start_scheduled_meeting_option)
                    binding.btnStartOrJoinSchedMeeting.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.start_sched_icon,
                        0,
                        0,
                        0
                    )
                }
                is ScheduledMeetingStatus.NotJoined -> {
                    binding.btnStartOrJoinSchedMeeting.setText(R.string.meetings_list_join_scheduled_meeting_option)
                    binding.btnStartOrJoinSchedMeeting.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.join_sched_icon,
                        0,
                        0,
                        0
                    )
                }
                else -> {}
            }
        }

        binding.btnStartOrJoinSchedMeeting.setOnClickListener {
            requestCallPermissions(permissionsRequest)
        }

        binding.btnInfo.setOnClickListener {
            val intent = if (room.isScheduledMeeting() && room.isActive) {
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
            dismissAllowingStateLoss()
        }

        if (room.isMuted) {
            binding.btnMute.setText(R.string.general_unmute)
            binding.btnMute.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_unmute,
                0,
                0,
                0
            )
        } else {
            binding.btnMute.setText(R.string.general_mute)
            binding.btnMute.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_mute,
                0,
                0,
                0
            )
        }
        binding.btnMute.setOnClickListener {
            if (room.isMuted) {
                MegaApplication.getPushNotificationSettingManagement()
                    .controlMuteNotificationsOfAChat(
                        requireContext(),
                        Constants.NOTIFICATIONS_ENABLED,
                        chatId
                    )
            } else {
                ChatUtil.createMuteNotificationsAlertDialogOfAChat(requireActivity(), chatId)
            }
            dismissAllowingStateLoss()
        }

        binding.btnArchive.setOnClickListener {
            viewModel.archiveChat(chatId)
            dismissAllowingStateLoss()
        }

        binding.btnCancel.setOnClickListener {
            showLeaveChatDialog()
            dismissAllowingStateLoss()
        }
    }

    /**
     * Show leave chat dialog
     */
    private fun showLeaveChatDialog() {
        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).setTitle(
            if (megaChatApi.getChatRoom(chatId)?.isMeeting == true) {
                R.string.meetings_leave_meeting_confirmation_dialog_title
            } else {
                R.string.title_confirmation_leave_group_chat
            }
        ).setMessage(R.string.confirmation_leave_group_chat)
            .setPositiveButton(R.string.general_leave) { _, _ ->
                viewModel.leaveChat(chatId)
                dismissAllowingStateLoss()
            }.setNegativeButton(R.string.general_cancel, null).show()
    }

    private fun getImagePlaceholder(placeholder: String?, avatarColor: Int?): Drawable =
        TextDrawable.builder()
            .beginConfig()
            .width(resources.getDimensionPixelSize(R.dimen.image_group_size))
            .height(resources.getDimensionPixelSize(R.dimen.image_group_size))
            .fontSize(resources.getDimensionPixelSize(R.dimen.image_group_text_size))
            .withBorder(resources.getDimensionPixelSize(R.dimen.image_group_border_size))
            .borderColor(ContextCompat.getColor(requireContext(), R.color.white_dark_grey))
            .bold()
            .toUpperCase()
            .endConfig()
            .buildRound(
                if (placeholder.isNullOrBlank()) "U" else AvatarUtil.getFirstLetter(placeholder),
                avatarColor ?: defaultAvatarColor
            )

    /**
     * Custom show method to avoid showing the same dialog multiple times
     */
    fun show(manager: FragmentManager) {
        if (manager.findFragmentByTag(TAG) == null) super.show(manager, TAG)
    }
}
