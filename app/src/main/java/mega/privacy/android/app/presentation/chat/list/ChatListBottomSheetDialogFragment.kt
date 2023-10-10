package mega.privacy.android.app.presentation.chat.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.presentation.chat.dialog.view.ChatRoomItemBottomSheetView
import mega.privacy.android.app.presentation.data.SnackBarItem
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingActivity
import mega.privacy.android.app.presentation.meeting.RecurringMeetingInfoActivity
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoActivity
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingManagementViewModel
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.permission.PermissionUtils.checkMandatoryCallPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestCallPermissions
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.MeetingChatRoomItem
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.mobile.analytics.event.ScheduledMeetingCancelMenuItemEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingEditMenuItemEvent
import javax.inject.Inject

/**
 * Chat list bottom sheet dialog fragment that displays chat room options
 */
@AndroidEntryPoint
class ChatListBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ChatListBottomSheetDialogFragment"

        fun newInstance(chatId: Long): ChatListBottomSheetDialogFragment =
            ChatListBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(Constants.CHAT_ID, chatId)
                }
            }
    }

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<ChatTabsViewModel>({ requireParentFragment() })
    private val scheduledMeetingManagementViewModel by viewModels<ScheduledMeetingManagementViewModel>(
        { requireParentFragment() })

    private val chatId by lazy {
        arguments?.getLong(Constants.CHAT_ID) ?: error("Invalid Chat Id")
    }

    private lateinit var permissionsRequest: ActivityResultLauncher<Array<String>>
    private lateinit var editSchedMeetLauncher: ActivityResultLauncher<Intent?>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val item: ChatRoomItem? by viewModel.getChatRoom(chatId)
                .collectAsStateWithLifecycle(null, viewLifecycleOwner, Lifecycle.State.STARTED)
            AndroidTheme(isDark = mode.isDarkMode()) {
                ChatRoomItemBottomSheetView(
                    item = item,
                    onStartMeetingClick = ::onStartMeetingClick,
                    onOccurrencesClick = ::onOccurrencesClick,
                    onInfoClick = ::onInfoClick,
                    onEditClick = ::onEditClick,
                    onClearChatClick = ::onClearChatClick,
                    onMuteClick = { onMuteClick(true) },
                    onUnmuteClick = { onMuteClick(false) },
                    onArchiveClick = ::onArchiveClick,
                    onCancelClick = ::onCancelClick,
                    onLeaveClick = ::showLeaveChatDialog,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        permissionsRequest = getCallPermissionsRequest()
        editSchedMeetLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    scheduledMeetingManagementViewModel.scheduledMeetingUpdated()
                }
                dismissAllowingStateLoss()
            }

        scheduledMeetingManagementViewModel.monitorLoadedMessages(chatId)
    }

    private fun onStartMeetingClick() {
        requestCallPermissions(permissionsRequest)
        dismissAllowingStateLoss()
    }

    private fun onOccurrencesClick() {
        val intent = Intent(context, RecurringMeetingInfoActivity::class.java).apply {
            putExtra(Constants.CHAT_ID, chatId)
        }
        startActivity(intent)
        dismissAllowingStateLoss()
    }

    /**
     * Edit Scheduled meeting
     */
    private fun onEditClick() {
        Analytics.tracker.trackEvent(ScheduledMeetingEditMenuItemEvent)
        editSchedMeetLauncher.launch(
            Intent(
                requireContext(),
                CreateScheduledMeetingActivity::class.java
            ).putExtra(Constants.CHAT_ID, chatId)
        )
    }

    private fun onInfoClick() {
        val item = viewModel.getChatItem(chatId) ?: return
        val intent =
            if (item is MeetingChatRoomItem && item.isPending && item.isActive) {
                Intent(context, ScheduledMeetingInfoActivity::class.java).apply {
                    putExtra(Constants.CHAT_ID, chatId)
                    putExtra(Constants.SCHEDULED_MEETING_ID, item.schedId)
                }
            } else {
                Intent(context, GroupChatInfoActivity::class.java).apply {
                    putExtra(Constants.HANDLE, chatId)
                    putExtra(Constants.ACTION_CHAT_OPEN, true)
                }
            }
        startActivity(intent)
        dismissAllowingStateLoss()
    }

    private fun onClearChatClick() {
        viewModel.clearChatHistory(chatId)
        dismissAllowingStateLoss()
    }

    private fun onMuteClick(mute: Boolean) {
        if (mute) {
            ChatUtil.createMuteNotificationsAlertDialogOfAChat(requireActivity(), chatId)
        } else {
            MegaApplication.getPushNotificationSettingManagement()
                .controlMuteNotificationsOfAChat(
                    requireContext(),
                    Constants.NOTIFICATIONS_ENABLED,
                    chatId
                )
        }
        dismissAllowingStateLoss()
    }

    private fun onArchiveClick() {
        viewModel.archiveChats(chatId)
        dismissAllowingStateLoss()
    }

    private fun onCancelClick() {
        Analytics.tracker.trackEvent(ScheduledMeetingCancelMenuItemEvent)
        viewModel.getChatItem(chatId)?.let { chatRoomItem ->
            scheduledMeetingManagementViewModel.setChatRoomItem(chatRoomItem)
        }
        scheduledMeetingManagementViewModel.checkIfIsChatHistoryEmpty(chatId)
        dismissAllowingStateLoss()
    }

    /**
     * Get call permissions request
     */
    private fun getCallPermissionsRequest(): ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (checkMandatoryCallPermissions(requireActivity())) {
                viewModel.startMeetingCall(chatId)
            } else {
                viewModel.updateSnackBar(
                    SnackBarItem(
                        type = Constants.NOT_CALL_PERMISSIONS_SNACKBAR_TYPE,
                        stringRes = R.string.allow_acces_calls_subtitle_microphone,
                    )
                )
            }
            dismissAllowingStateLoss()
        }

    /**
     * Show leave chat dialog
     */
    private fun showLeaveChatDialog() {
        val item = viewModel.getChatItem(chatId) ?: return
        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).setTitle(
            if (item is MeetingChatRoomItem) {
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

    /**
     * Custom show method to avoid showing the same dialog multiple times
     */
    fun show(manager: FragmentManager) {
        if (manager.findFragmentByTag(TAG) == null) super.show(manager, TAG)
    }
}
