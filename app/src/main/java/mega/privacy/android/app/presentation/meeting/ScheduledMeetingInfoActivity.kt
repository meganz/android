package mega.privacy.android.app.presentation.meeting

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.ManageChatHistoryActivity
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.ChatExplorerActivityContract
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_IN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_AUDIO_ENABLE
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_CHAT_ID
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_VIDEO_ENABLE
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ParticipantBottomSheetDialogFragment
import mega.privacy.android.app.presentation.chat.dialog.ManageMeetingLinkBottomSheetDialogFragment
import mega.privacy.android.app.presentation.extensions.changeStatusBarColor
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction
import mega.privacy.android.app.presentation.meeting.view.ScheduledMeetingInfoView
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.ChatUtil.createMuteNotificationsAlertDialogOfAChat
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE
import mega.privacy.android.app.utils.Constants.SCHEDULED_MEETING_ID
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.presentation.theme.AndroidTheme
import android.text.format.DateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Activity which shows scheduled meeting info screen.
 *
 * @property passCodeFacade [PasscodeCheck]
 * @property getThemeMode   [GetThemeMode]
 * @property addContactLauncher
 * @property sendToChatLauncher
 */
@AndroidEntryPoint
class ScheduledMeetingInfoActivity : PasscodeActivity(), SnackbarShower {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<ScheduledMeetingInfoViewModel>()

    private lateinit var addContactLauncher: ActivityResultLauncher<Intent?>
    private lateinit var sendToChatLauncher: ActivityResultLauncher<Unit?>

    private var bottomSheetDialogFragment: BaseBottomSheetDialogFragment? = null

    private var nodeAttacher: MegaAttacher? = null

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { (chatId, scheduledMeeting, finish, openAddContact, dndSecond, _, meetingLink, _, openSendToChat, openRemoveParticipantDialog, selected, openChatRoom, showChangePermissionsDialog, openChatCall) ->
                    if (finish) {
                        Timber.d("Finish activity")
                        finish()
                    }

                    if (chatRoomId != chatId) {
                        chatRoomId = chatId
                    }

                    scheduledMeeting?.let { sched ->
                        sched.startDate?.let { start ->
                            sched.endDate?.let { end ->
                                getFormattedDate(start, end)
                            }
                        }
                    }

                    enabledChatNotification = dndSecond == null

                    if (link != meetingLink) {
                        link = meetingLink
                    }

                    if (openSendToChat) {
                        viewModel.openSendToChat(false)
                        sendToChatLauncher.launch(Unit)
                    }

                    showChangePermissionsDialog?.let {
                        viewModel.showChangePermissionsDialog(null)
                        showChangePermissionsDialog(it)
                    }

                    openChatRoom?.let {
                        viewModel.openChatRoom(null)
                        openChatRoom(it)
                    }

                    openChatCall?.let {
                        viewModel.openChatCall(null)
                        openChatCall(it)
                    }

                    selected?.let {
                        if (openRemoveParticipantDialog) {
                            viewModel.onRemoveParticipantTap(false)
                            showRemoveParticipantDialog(it)
                        }
                    }

                    openAddContact?.let { shouldOpen ->
                        if (shouldOpen) {
                            viewModel.removeAddContact()
                            Timber.d("Open Invite participants screen")
                            addContactLauncher.launch(Intent(this@ScheduledMeetingInfoActivity,
                                AddContactActivity::class.java)
                                .putExtra(INTENT_EXTRA_KEY_CONTACT_TYPE,
                                    Constants.CONTACT_TYPE_MEGA)
                                .putExtra(INTENT_EXTRA_KEY_CHAT, true)
                                .putExtra(INTENT_EXTRA_KEY_CHAT_ID, chatId)
                                .putExtra(INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                                    getString(R.string.add_participants_menu_item))
                            )
                        }
                    }
                }
            }
        }

        viewModel.setChatId(newChatId = intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE),
            newScheduledMeetingId = intent.getLongExtra(SCHEDULED_MEETING_ID,
                MEGACHAT_INVALID_HANDLE))

        setContent { ScheduledMeetingInfoView() }

        addContactLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                        ?.let { contactsData ->
                            viewModel.inviteToChat(contactsData)
                        }
                } else {
                    Timber.e("Error adding participants")
                }
            }

        sendToChatLauncher = registerForActivityResult(ChatExplorerActivityContract()) { data ->
            if (data != null) {
                viewModel.sendToChat(data) { intent ->
                    handleActivityResult(intent)
                }
            }
        }
    }

    /**
     * Format ZonedDateTime to a readable date
     */
    private fun getFormattedDate(startDateTime: ZonedDateTime, endDateTime: ZonedDateTime) {
        val is24Format = DateFormat.is24HourFormat(this)
        val dateFormatter = if (is24Format)
            DateTimeFormatter.ofPattern("d MMM yyyy '·' HH:mm").withZone(ZoneId.systemDefault())
        else
            DateTimeFormatter.ofPattern("d MMM yyyy '·' hh:mm").withZone(ZoneId.systemDefault())

        val hourFormatter = if (is24Format)
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        else
            DateTimeFormatter.ofPattern("hh:mm").withZone(ZoneId.systemDefault())

        viewModel.setScheduledMeetingDate("${dateFormatter.format(startDateTime)} - ${
            hourFormatter.format(endDateTime)
        }")
    }

    /**
     * Finishes the send to chat action.
     *
     * @param data Intent containing the info to send.
     */
    private fun handleActivityResult(data: Intent?) {
        nodeAttacher = MegaAttacher(this as ActivityLauncher)
        nodeAttacher?.handleActivityResult(
            Constants.REQUEST_CODE_SELECT_CHAT,
            RESULT_OK,
            data,
            this@ScheduledMeetingInfoActivity as SnackbarShower
        )
    }

    /**
     * Shows panel to get the chat link
     */
    private fun showGetChatLinkPanel() {
        if (link.isNullOrEmpty() || bottomSheetDialogFragment.isBottomSheetDialogShown()) {
            return
        }
        bottomSheetDialogFragment = ManageMeetingLinkBottomSheetDialogFragment()
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    /**
     * Shows panel to show participant's options
     */
    private fun showParticipantOptionsPanel(participant: ChatParticipant) {
        if (bottomSheetDialogFragment.isBottomSheetDialogShown()) {
            return
        }
        viewModel.onParticipantTap(participant)

        bottomSheetDialogFragment =
            ParticipantBottomSheetDialogFragment.newInstance(chatRoomId, participant.handle)
        bottomSheetDialogFragment?.show(supportFragmentManager, bottomSheetDialogFragment?.tag)
    }

    /**
     * Shows an alert dialog to confirm the deletion of a participant.
     *
     * @param participant [ChatParticipant]
     */
    private fun showRemoveParticipantDialog(participant: ChatParticipant) {
        val dialogBuilder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)

        val name = participant.data.fullName
        dialogBuilder.setMessage(getString(R.string.confirmation_remove_chat_contact, name))
            .setPositiveButton(R.string.general_remove) { _: DialogInterface?, _: Int -> viewModel.removeSelectedParticipant() }
            .setNegativeButton(R.string.general_cancel, null)
            .show()
    }

    /**
     * Shows change permissions dialog
     *
     * @param currentPermission [ChatRoomPermission] current permission
     */
    fun showChangePermissionsDialog(currentPermission: ChatRoomPermission) {
        var dialog: AlertDialog? = null

        val dialogView = this.layoutInflater.inflate(R.layout.change_permissions_dialog, null)

        val layoutHost =
            dialogView.findViewById<LinearLayout>(R.id.change_permissions_dialog_administrator_layout)
        val checkHost =
            dialogView.findViewById<CheckedTextView>(R.id.change_permissions_dialog_administrator)
        val layoutStandard =
            dialogView.findViewById<LinearLayout>(R.id.change_permissions_dialog_member_layout)
        val checkStandard =
            dialogView.findViewById<CheckedTextView>(R.id.change_permissions_dialog_member)
        val layoutReadOnly =
            dialogView.findViewById<LinearLayout>(R.id.change_permissions_dialog_observer_layout)
        val checkReadOnly =
            dialogView.findViewById<CheckedTextView>(R.id.change_permissions_dialog_observer)

        checkHost.isChecked = currentPermission == ChatRoomPermission.Moderator
        checkStandard.isChecked = currentPermission == ChatRoomPermission.Standard
        checkReadOnly.isChecked = currentPermission == ChatRoomPermission.ReadOnly

        layoutHost.setOnClickListener {
            dialog?.dismiss()
            viewModel.updateParticipantPermissions(ChatRoomPermission.Moderator)
        }
        layoutStandard.setOnClickListener {
            dialog?.dismiss()
            viewModel.updateParticipantPermissions(ChatRoomPermission.Standard)
        }
        layoutReadOnly.setOnClickListener {
            dialog?.dismiss()
            viewModel.updateParticipantPermissions(ChatRoomPermission.ReadOnly)
        }

        val builder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
                .setView(dialogView)
                .setTitle(getString(R.string.file_properties_shared_folder_permissions))

        dialog = builder.create()
        dialog.show()
    }

    /**
     * Open chat room
     *
     * @param chatId Chat id.
     */
    private fun openChatRoom(chatId: Long) {
        val intentOpenChat = Intent(this@ScheduledMeetingInfoActivity, ChatActivity::class.java)
        intentOpenChat.action = Constants.ACTION_CHAT_SHOW_MESSAGES
        intentOpenChat.putExtra(CHAT_ID, chatId)
        this.startActivity(intentOpenChat)
    }

    /**
     * Open chat call
     *
     * @param chatId Chat id.
     */
    private fun openChatCall(chatId: Long) {
        val intentOpenCall = Intent(this@ScheduledMeetingInfoActivity, MeetingActivity::class.java)
        intentOpenCall.action = MEETING_ACTION_IN
        intentOpenCall.putExtra(MEETING_CHAT_ID, chatId)
        intentOpenCall.putExtra(MEETING_AUDIO_ENABLE, true)
        intentOpenCall.putExtra(MEETING_VIDEO_ENABLE, false)
        intentOpenCall.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        this.startActivity(intentOpenCall)
    }

    /**
     * Open shared files
     */
    private fun openSharedFiles() {
        val intent =
            Intent(this@ScheduledMeetingInfoActivity, NodeAttachmentHistoryActivity::class.java)
        intent.putExtra("chatId", chatRoomId)
        startActivity(intent)
    }

    /**
     * Enable or disable chat notifications if there is internet connection, shows an error if not.
     */
    private fun onChatNotificationsTap() {
        if (enabledChatNotification) {
            createMuteNotificationsAlertDialogOfAChat(this@ScheduledMeetingInfoActivity,
                chatRoomId)
        } else {
            MegaApplication.getPushNotificationSettingManagement().controlMuteNotificationsOfAChat(
                this@ScheduledMeetingInfoActivity,
                Constants.NOTIFICATIONS_ENABLED,
                chatRoomId)
        }
    }

    /**
     * Shows dialog to confirm making the chat private
     */
    private fun showConfirmationPrivateChatDialog() {
        Timber.d("Show Enable encryption key rotation dialog")
        var dialog: AlertDialog? = null
        val dialogBuilder =
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)

        val dialogView = this.layoutInflater.inflate(R.layout.dialog_chat_link_options, null)
        dialogBuilder.setView(dialogView)

        val actionButton = dialogView.findViewById<Button>(R.id.chat_link_button_action)
        actionButton.text = getString(R.string.general_enable)
        actionButton.setOnClickListener {
            dialog?.dismiss()
            viewModel.enableEncryptedKeyRotation()
        }

        val title = dialogView.findViewById<TextView>(R.id.chat_link_title)
        title.text = getString(R.string.make_chat_private_option)

        val text = dialogView.findViewById<TextView>(R.id.text_chat_link)
        text.text = getString(R.string.context_make_private_chat_warning_text)

        val secondText = dialogView.findViewById<TextView>(R.id.second_text_chat_link)
        secondText.visibility = View.GONE
        dialog = dialogBuilder.create()
        dialog.show()
    }

    /**
     * Open manage chat history
     */
    private fun openManageChatHistory() {
        val intentManageChat = Intent(this@ScheduledMeetingInfoActivity,
            ManageChatHistoryActivity::class.java)
        intentManageChat.putExtra(CHAT_ID, chatRoomId)
        intentManageChat.putExtra(Constants.IS_FROM_CONTACTS, true)
        startActivity(intentManageChat)
    }

    /**
     * Open invite contacts
     */
    private fun openInviteContact() {
        startActivity(Intent(this@ScheduledMeetingInfoActivity, InviteContactActivity::class.java))
    }

    @Composable
    private fun ScheduledMeetingInfoView() {
        val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
        val isDark = themeMode.isDarkMode()
        val uiState by viewModel.state.collectAsState()
        AndroidTheme(isDark = isDark) {
            ScheduledMeetingInfoView(state = uiState,
                onButtonClicked = ::onActionTap,
                onEditClicked = { viewModel::onEditTap },
                onAddParticipantsClicked = { viewModel.onInviteParticipantsTap() },
                onSeeMoreOrLessClicked = { viewModel.onSeeMoreOrLessTap() },
                onLeaveGroupClicked = { viewModel.onLeaveGroupTap() },
                onParticipantClicked = { showParticipantOptionsPanel(it) },
                onScrollChange = { scrolled -> this.changeStatusBarColor(scrolled, isDark) },
                onBackPressed = { finish() },
                onDismiss = { viewModel.dismissDialog() },
                onLeaveGroupDialog = { viewModel.leaveChat() },
                onInviteParticipantsDialog = {
                    openInviteContact()
                    viewModel.dismissDialog()
                },
                onSnackbarShown = viewModel::snackbarShown)
        }
    }

    /**
     * Tap in a button action
     */
    private fun onActionTap(action: ScheduledMeetingInfoAction) {
        when (action) {
            ScheduledMeetingInfoAction.MeetingLink -> viewModel.onMeetingLinkTap()
            ScheduledMeetingInfoAction.ShareMeetingLink,
            ScheduledMeetingInfoAction.ShareMeetingLinkNonHosts,
            -> showGetChatLinkPanel()
            ScheduledMeetingInfoAction.ChatNotifications -> onChatNotificationsTap()
            ScheduledMeetingInfoAction.AllowNonHostAddParticipants -> viewModel.onAllowAddParticipantsTap()
            ScheduledMeetingInfoAction.ShareFiles -> openSharedFiles()
            ScheduledMeetingInfoAction.ManageChatHistory -> openManageChatHistory()
            ScheduledMeetingInfoAction.EnableEncryptedKeyRotation -> showConfirmationPrivateChatDialog()
            ScheduledMeetingInfoAction.EnabledEncryptedKeyRotation -> {}
        }
    }

    /**
     * onDestroy
     */
    override fun onDestroy() {
        nodeAttacher = null
        super.onDestroy()
    }

    companion object {
        private var chatRoomId: Long = MEGACHAT_INVALID_HANDLE
        private var enabledChatNotification: Boolean = false
        private var link: String? = null
    }
}
