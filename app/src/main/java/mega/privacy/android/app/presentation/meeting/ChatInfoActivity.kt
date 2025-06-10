package mega.privacy.android.app.presentation.meeting

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.SendToChatActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbarWithChat
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_IN
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_AUDIO_ENABLE
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_BOTTOM_PANEL_EXPANDED
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_CHAT_ID
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_VIDEO_ENABLE
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ScheduledMeetingParticipantBottomSheetDialogFragment
import mega.privacy.android.app.presentation.chat.dialog.ManageMeetingLinkBottomSheetDialogFragment
import mega.privacy.android.app.presentation.chat.list.view.MeetingLinkBottomSheet
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingActivity.Companion.MEETING_LINK_CREATED_TAG
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingActivity.Companion.MEETING_LINK_TAG
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingActivity.Companion.MEETING_TITLE_TAG
import mega.privacy.android.app.presentation.meeting.model.ChatInfoAction
import mega.privacy.android.app.presentation.meeting.view.ChatInfoView
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.AlertDialogUtil
import mega.privacy.android.app.utils.ChatUtil.createMuteNotificationsAlertDialogOfAChat
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE
import mega.privacy.android.app.utils.Constants.SCHEDULED_MEETING_CREATED
import mega.privacy.android.app.utils.Constants.SCHEDULED_MEETING_ID
import mega.privacy.android.app.utils.ScheduledMeetingDateUtil
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.mobile.analytics.event.MeetingInfoAddParticipantButtonTappedEvent
import mega.privacy.mobile.analytics.event.MeetingInfoLeaveMeetingButtonTappedEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingEditMenuToolbarEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingSettingEnableMeetingLinkButtonEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingSettingEnableOpenInviteButtonEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingShareMeetingLinkButtonEvent
import mega.privacy.mobile.analytics.event.SendMeetingLinkToChatScheduledMeetingEvent
import mega.privacy.mobile.analytics.event.WaitingRoomEnableButtonEvent
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity which shows scheduled meeting info screen.
 *
 * @property passCodeFacade [PasscodeCheck]
 * @property monitorThemeModeUseCase   [MonitorThemeModeUseCase]
 * @property addContactLauncher
 * @property sendToChatLauncher
 * @property editSchedMeetLauncher
 */
@AndroidEntryPoint
class ChatInfoActivity : PasscodeActivity(), SnackbarShower {

    /**
     * The centralized navigator in the :app module
     */
    @Inject
    lateinit var navigator: MegaNavigator

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    private val viewModel by viewModels<ChatInfoViewModel>()
    private val noteToSelfChatViewModel by viewModels<NoteToSelfChatViewModel>()
    private val scheduledMeetingManagementViewModel by viewModels<ScheduledMeetingManagementViewModel>()
    private val waitingRoomManagementViewModel by viewModels<WaitingRoomManagementViewModel>()

    private lateinit var addContactLauncher: ActivityResultLauncher<Intent>
    private val sendToChatLauncher = registerForActivityResult(SendToChatActivityContract()) {
        if (it != null) {
            viewModel.sendToChat(
                data = it,
                link = scheduledMeetingManagementViewModel.state.value.meetingLink
            )
        }
    }
    private val editSchedMeetLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val isLinkCreated = result.data?.getBooleanExtra(
                    MEETING_LINK_CREATED_TAG,
                    false
                ) == true
                if (isLinkCreated) {
                    // show bottom sheet dialog
                    val chatId = result.data?.getLongExtra(
                        CHAT_ID,
                        -1L
                    ) ?: -1L
                    if (chatId != -1L) {
                        val link = result.data?.getStringExtra(
                            MEETING_LINK_TAG
                        )
                        val title = result.data?.getStringExtra(
                            MEETING_TITLE_TAG
                        ) ?: ""
                        scheduledMeetingManagementViewModel.setMeetingLink(
                            chatId,
                            link,
                            title
                        )
                    }
                }
                scheduledMeetingManagementViewModel.scheduledMeetingUpdated()
                scheduledMeetingManagementViewModel.checkWaitingRoomWarning()
            }
        }

    private var bottomSheetDialogFragment: BaseBottomSheetDialogFragment? = null

    private var forceAppUpdateDialog: AlertDialog? = null

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appContainerWrapper.setPasscodeCheck(passcodeFacade)
        collectFlows()

        val chatId = intent.getLongExtra(CHAT_ID, -1L)
        val schedId = intent.getLongExtra(
            SCHEDULED_MEETING_ID,
            -1L
        )

        viewModel.setChatId(
            newChatId = chatId,
            newScheduledMeetingId = schedId,
        )
        scheduledMeetingManagementViewModel.setChatId(newChatId = chatId)

        setContent { MainComposeView() }

        viewModel.checkInitialSnackbar(intent.getBooleanExtra(SCHEDULED_MEETING_CREATED, false))

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
    }

    private fun collectFlows() {
        collectFlow(scheduledMeetingManagementViewModel.state.map { it.finish }
            .distinctUntilChanged()) {
            if (it) {
                Timber.d("Finish activity")
                finish()
            }
        }

        collectFlow(scheduledMeetingManagementViewModel.state.map { it.meetingLink }
            .distinctUntilChanged()) {
            if (it != link) {
                link = it
            }
        }

        collectFlow(waitingRoomManagementViewModel.state.map { it.snackbarString }
            .distinctUntilChanged()) {
            it?.let {
                viewModel.triggerSnackbarMessage(it)
                waitingRoomManagementViewModel.onConsumeSnackBarMessageEvent()
            }
        }

        collectFlow(waitingRoomManagementViewModel.state.map { it.shouldWaitingRoomBeShown }
            .distinctUntilChanged()) {
            if (it) {
                waitingRoomManagementViewModel.onConsumeShouldWaitingRoomBeShownEvent()
                launchCallScreen()
            }
        }

        collectFlow(viewModel.uiState.map { it.finish }
            .distinctUntilChanged()) {
            if (it) {
                Timber.d("Finish activity")
                finish()
            }
        }

        collectFlow(viewModel.sendLinkToChatResult) {
            it?.let { sendLinkToChatResult ->
                showSnackbarWithChat(
                    resources.getQuantityString(
                        R.plurals.links_sent,
                        1
                    ),
                    sendLinkToChatResult.chatId
                )
                viewModel.onShareLinkResultHandled()
            }
        }

        collectFlow(viewModel.uiState) { state ->
            if (chatRoomId != state.chatId) {
                chatRoomId = state.chatId
            }

            enabledChatNotification = state.dndSeconds == null

            if (state.openSendToChat) {
                viewModel.openSendToChat(false)
                sendToChatLauncher.launch(longArrayOf())
            }

            state.showChangePermissionsDialog?.let {
                viewModel.showChangePermissionsDialog(null)
                showChangePermissionsDialog(it)
            }

            state.openChatRoom?.let {
                viewModel.openChatRoom(null)
                openChatRoom(it)
            }

            state.openChatCall?.let {
                viewModel.openChatCall(null)
                openChatCall(it)
            }

            state.selected?.let {
                if (state.openRemoveParticipantDialog) {
                    viewModel.onRemoveParticipantTap(false)
                    showRemoveParticipantDialog(it)
                }
            }

            state.openAddContact?.let { shouldOpen ->
                if (shouldOpen) {
                    viewModel.removeAddContact()
                    Timber.d("Open Invite participants screen")
                    addContactLauncher.launch(
                        Intent(
                            this@ChatInfoActivity,
                            AddContactActivity::class.java
                        )
                            .putExtra(
                                INTENT_EXTRA_KEY_CONTACT_TYPE,
                                Constants.CONTACT_TYPE_MEGA
                            )
                            .putExtra(INTENT_EXTRA_KEY_CHAT, true)
                            .putExtra(INTENT_EXTRA_KEY_CHAT_ID, state.chatId)
                            .putExtra(
                                INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                                getString(R.string.add_participants_menu_item)
                            )
                    )
                }
            }
            if (state.showForceUpdateDialog) {
                showForceUpdateAppDialog()
            }
        }
    }

    /**
     * Show Force App Update Dialog
     */
    private fun showForceUpdateAppDialog() {
        if (forceAppUpdateDialog?.isShowing == true) return
        forceAppUpdateDialog = AlertDialogUtil.createForceAppUpdateDialog(this) {
            viewModel.onForceUpdateDialogDismissed()
        }
        forceAppUpdateDialog?.show()
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
            ScheduledMeetingParticipantBottomSheetDialogFragment.newInstance(
                chatRoomId,
                participant.handle
            )
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
            .setNegativeButton(sharedR.string.general_dialog_cancel_button, null)
            .show()
    }

    /**
     * Shows change permissions dialog
     *
     * @param currentPermission [ChatRoomPermission] current permission
     */
    private fun showChangePermissionsDialog(currentPermission: ChatRoomPermission) {
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
        navigator.openChat(
            context = this,
            chatId = chatId,
            action = Constants.ACTION_CHAT_SHOW_MESSAGES
        )
    }

    /**
     * Open chat call
     *
     * @param chatId Chat id.
     */
    private fun openChatCall(chatId: Long) {
        val intentOpenCall = Intent(this@ChatInfoActivity, MeetingActivity::class.java)
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
            Intent(this@ChatInfoActivity, NodeAttachmentHistoryActivity::class.java)
        intent.putExtra("chatId", chatRoomId)
        startActivity(intent)
    }

    /**
     * Enable or disable chat notifications if there is internet connection, shows an error if not.
     */
    private fun onChatNotificationsTap() {
        if (enabledChatNotification) {
            createMuteNotificationsAlertDialogOfAChat(
                this@ChatInfoActivity,
                chatRoomId
            )
        } else {
            MegaApplication.getPushNotificationSettingManagement().controlMuteNotificationsOfAChat(
                this@ChatInfoActivity,
                Constants.NOTIFICATIONS_ENABLED,
                chatRoomId
            )
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
        navigator.openManageChatHistoryActivity(
            context = this,
            chatId = chatRoomId
        )
    }

    /**
     * Open meeting
     */
    private fun launchCallScreen() {
        val chatId = waitingRoomManagementViewModel.state.value.chatId
        MegaApplication.getInstance().openCallService(chatId)
        passcodeFacade.enablePassCode()

        val intent = Intent(this@ChatInfoActivity, MeetingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = MEETING_ACTION_IN
            putExtra(MEETING_CHAT_ID, chatId)
            putExtra(MEETING_BOTTOM_PANEL_EXPANDED, true)
        }
        startActivity(intent)
    }

    /**
     * Open invite contacts
     */
    private fun openInviteContact() {
        navigator.openInviteContactActivity(
            this,
            false
        )
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun MainComposeView() {
        val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val isDark = themeMode.isDarkMode()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val managementState by scheduledMeetingManagementViewModel.state.collectAsStateWithLifecycle()
        val noteToSelfChatState by noteToSelfChatViewModel.state.collectAsStateWithLifecycle()

        val coroutineScope = rememberCoroutineScope()
        val modalSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true,
        )
        BackHandler(enabled = modalSheetState.isVisible) {
            coroutineScope.launch { modalSheetState.hide() }
        }

        EventEffect(
            managementState.meetingLinkCreated,
            scheduledMeetingManagementViewModel::onMeetingLinkShareShown
        ) {
            coroutineScope.launch {
                Timber.d("Show MeetingLinkBottomSheet")
                modalSheetState.show()
            }
        }

        OriginalTheme(isDark = isDark) {
            MeetingLinkBottomSheet(
                modalSheetState = modalSheetState,
                coroutineScope = coroutineScope,
                onSendLinkToChat = {
                    Analytics.tracker.trackEvent(SendMeetingLinkToChatScheduledMeetingEvent)
                    sendToChatLauncher.launch(
                        longArrayOf()
                    )
                },
                onShareLink = {
                    Analytics.tracker.trackEvent(ScheduledMeetingShareMeetingLinkButtonEvent)
                    showMeetingShareOptions()
                }
            ) {
                ChatInfoView(
                    state = uiState,
                    managementState = managementState,
                    noteToSelfChatState = noteToSelfChatState,
                    onButtonClicked = ::onActionTap,
                    onEditClicked = { onEditTap() },
                    onAddParticipantsClicked = {
                        Analytics.tracker.trackEvent(MeetingInfoAddParticipantButtonTappedEvent)
                        viewModel.onInviteParticipantsTap()
                    },
                    onSeeMoreOrLessClicked = { viewModel.onSeeMoreOrLessTap() },
                    onLeaveGroupClicked = {
                        Analytics.tracker.trackEvent(MeetingInfoLeaveMeetingButtonTappedEvent)
                        viewModel.onLeaveGroupTap()
                    },
                    onParticipantClicked = { showParticipantOptionsPanel(it) },
                    onBackPressed = {
                        Timber.d(
                            "onBackPressed schedule meeting created ${
                                intent.getBooleanExtra(
                                    SCHEDULED_MEETING_CREATED,
                                    false
                                )
                            }"
                        )

                        if (intent.getBooleanExtra(SCHEDULED_MEETING_CREATED, false)) {
                            setResult(
                                RESULT_OK,
                                Intent().apply {
                                    putExtra(CHAT_ID, uiState.scheduledMeeting?.chatId)
                                    putExtra(MEETING_TITLE_TAG, uiState.scheduledMeeting?.title)
                                    putExtra(
                                        MEETING_LINK_CREATED_TAG,
                                        managementState.meetingLink?.isNotBlank() ?: false
                                    )
                                    putExtra(MEETING_LINK_TAG, managementState.meetingLink)
                                }
                            )
                        }
                        finish()
                    },
                    onDismiss = { viewModel.dismissDialog() },
                    onLeaveGroupDialog = { viewModel.leaveChat() },
                    onInviteParticipantsDialog = {
                        openInviteContact()
                        viewModel.dismissDialog()
                    },
                    onCloseWarningClicked = scheduledMeetingManagementViewModel::closeWaitingRoomWarning,
                    onResetStateSnackbarMessage = viewModel::onSnackbarMessageConsumed,
                )
            }
        }
    }

    /**
     * Edit scheduled meeting if there is internet connection, shows an error if not.
     */
    private fun onEditTap() {
        Analytics.tracker.trackEvent(ScheduledMeetingEditMenuToolbarEvent)
        editSchedMeetLauncher.launch(
            Intent(
                this@ChatInfoActivity,
                CreateScheduledMeetingActivity::class.java
            ).putExtra(CHAT_ID, chatRoomId)
        )
    }

    /**
     * Tap in a button action
     */
    private fun onActionTap(action: ChatInfoAction) {
        when (action) {
            ChatInfoAction.MeetingLink -> {
                if (!scheduledMeetingManagementViewModel.state.value.enabledMeetingLinkOption) {
                    Analytics.tracker.trackEvent(ScheduledMeetingSettingEnableMeetingLinkButtonEvent)
                }
                scheduledMeetingManagementViewModel.onMeetingLinkTap()
            }

            ChatInfoAction.ShareMeetingLink,
            ChatInfoAction.ShareMeetingLinkNonHosts,
                -> {
                Analytics.tracker.trackEvent(ScheduledMeetingShareMeetingLinkButtonEvent)
                showGetChatLinkPanel()
            }

            ChatInfoAction.ChatNotifications -> onChatNotificationsTap()
            ChatInfoAction.AllowNonHostAddParticipants -> {
                Analytics.tracker.trackEvent(ScheduledMeetingSettingEnableOpenInviteButtonEvent)
                viewModel.onAllowAddParticipantsTap()
            }

            ChatInfoAction.ShareFiles, ChatInfoAction.Files -> openSharedFiles()
            ChatInfoAction.ManageChatHistory, ChatInfoAction.ManageMeetingHistory -> openManageChatHistory()
            ChatInfoAction.EnableEncryptedKeyRotation -> showConfirmationPrivateChatDialog()
            ChatInfoAction.EnabledEncryptedKeyRotation -> {}
            ChatInfoAction.WaitingRoom -> {
                if (!viewModel.uiState.value.enabledWaitingRoomOption) {
                    Analytics.tracker.trackEvent(WaitingRoomEnableButtonEvent)
                }
                viewModel.setWaitingRoom()
            }

            ChatInfoAction.Archive, ChatInfoAction.Unarchive -> {
                viewModel.archiveChat()
            }
        }
    }

    private fun showMeetingShareOptions() {
        val subject = getString(R.string.meetings_sharing_meeting_link_meeting_invite_subject)
        val message = getString(
            R.string.meetings_sharing_meeting_link_title,
            scheduledMeetingManagementViewModel.state.value.myFullName
        )
        val meetingName =
            getString(
                R.string.meetings_sharing_meeting_link_meeting_name,
                scheduledMeetingManagementViewModel.state.value.title
            )
        val meetingLink =
            getString(
                R.string.meetings_sharing_meeting_link_meeting_link,
                scheduledMeetingManagementViewModel.state.value.meetingLink
            )

        val body = StringBuilder()
        body.append("\n")
            .append(message)
            .append("\n\n")
            .append(meetingName)

        scheduledMeetingManagementViewModel.chatScheduledMeeting?.let {
            val meetingDateAndTime = getString(
                R.string.meetings_sharing_meeting_link_meeting_date_and_time,
                ScheduledMeetingDateUtil.getAppropriateStringForScheduledMeetingDate(
                    this,
                    scheduledMeetingManagementViewModel.is24HourFormat,
                    it
                )
            )
            body.append(meetingDateAndTime)
        }

        body.append("\n")
            .append(meetingLink)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = Constants.TYPE_TEXT_PLAIN
            putExtra(Intent.EXTRA_SUBJECT, "\n${subject}")
            putExtra(Intent.EXTRA_TEXT, body.toString())
        }

        startActivity(Intent.createChooser(intent, " "))
    }

    companion object {
        private var chatRoomId: Long = MEGACHAT_INVALID_HANDLE
        private var enabledChatNotification: Boolean = false
        private var link: String? = null
    }
}
