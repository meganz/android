package mega.privacy.android.app.presentation.meeting

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.ManageChatHistoryActivity
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity
import mega.privacy.android.app.presentation.chat.dialog.AddParticipantsNoContactsDialogFragment
import mega.privacy.android.app.presentation.chat.dialog.AddParticipantsNoContactsLeftToAddDialogFragment
import mega.privacy.android.app.presentation.extensions.changeStatusBarColor
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.model.InviteParticipantsAction
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction
import mega.privacy.android.app.presentation.meeting.view.ScheduledMeetingInfoView
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE
import mega.privacy.android.app.utils.Constants.SCHEDULED_MEETING_ID
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.presentation.theme.AndroidTheme
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity which shows scheduled meeting info screen.
 *
 * @property passCodeFacade [PasscodeCheck]
 * @property getThemeMode   [GetThemeMode]
 * @property resultLauncher
 */
@AndroidEntryPoint
class ScheduledMeetingInfoActivity : PasscodeActivity() {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<ScheduledMeetingInfoViewModel>()

    private lateinit var resultLauncher: ActivityResultLauncher<Intent?>
    private var chatRoomId: Long = MEGACHAT_INVALID_HANDLE

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { (chatId, _, _, finish, _, _, inviteParticipantAction) ->
                    if (finish) {
                        Timber.d("Finish activity")
                        finish()
                    }

                    if (chatRoomId != chatId)
                        chatRoomId = chatId

                    inviteParticipantAction?.let { action ->
                        viewModel.removeInviteParticipantsAction()
                        when (action) {
                            InviteParticipantsAction.ADD_CONTACTS -> {
                                Timber.d("Open Invite participants screen")
                                resultLauncher.launch(Intent(this@ScheduledMeetingInfoActivity,
                                    AddContactActivity::class.java)
                                    .putExtra(INTENT_EXTRA_KEY_CONTACT_TYPE,
                                        Constants.CONTACT_TYPE_MEGA)
                                    .putExtra(INTENT_EXTRA_KEY_CHAT, true)
                                    .putExtra(INTENT_EXTRA_KEY_CHAT_ID, chatId)
                                    .putExtra(INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                                        StringResourcesUtils.getString(R.string.add_participants_menu_item))
                                )
                            }
                            InviteParticipantsAction.NO_CONTACTS_DIALOG -> {
                                Timber.d("Show dialog when all contacts are chat participants.")
                                val dialog = AddParticipantsNoContactsDialogFragment.newInstance()
                                dialog.show(supportFragmentManager, dialog.tag)
                            }
                            InviteParticipantsAction.NO_MORE_CONTACTS_DIALOG -> {
                                Timber.d("Show dialog when there are no contacts added.")
                                val dialog =
                                    AddParticipantsNoContactsLeftToAddDialogFragment.newInstance()
                                dialog.show(supportFragmentManager, dialog.tag)
                            }
                        }
                    }
                }
            }
        }

        viewModel.setChatId(newChatId = intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE),
            newScheduledMeetingId = intent.getLongExtra(SCHEDULED_MEETING_ID,
                MEGACHAT_INVALID_HANDLE))

        setContent { ScheduledMeetingInfoView() }

        resultLauncher =
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
                onSeeMoreClicked = { viewModel::onSeeMoreTap },
                onLeaveGroupClicked = { viewModel.onLeaveGroupTap() },
                onParticipantClicked = { viewModel::onParticipantTap },
                onScrollChange = { scrolled -> this.changeStatusBarColor(scrolled, isDark) },
                onBackPressed = { finish() },
                onDismiss = { viewModel.dismissDialog() },
                onLeaveGroupDialog = { viewModel.leaveChat() })
        }
    }

    /**
     * Tap in a button action
     */
    private fun onActionTap(action: ScheduledMeetingInfoAction) {
        when (action) {
            ScheduledMeetingInfoAction.MeetingLink -> viewModel.onMeetingLinkTap()
            ScheduledMeetingInfoAction.ShareMeetingLink -> viewModel.onShareMeetingLinkTap()
            ScheduledMeetingInfoAction.ChatNotifications -> viewModel.onChatNotificationsTap()
            ScheduledMeetingInfoAction.AllowNonHostAddParticipants -> viewModel.onAllowAddParticipantsTap()
            ScheduledMeetingInfoAction.ShareFiles -> openSharedFiles()
            ScheduledMeetingInfoAction.ManageChatHistory -> openManageChatHistory()
            ScheduledMeetingInfoAction.EnableEncryptedKeyRotation -> showConfirmationPrivateChatDialog()
            ScheduledMeetingInfoAction.EnabledEncryptedKeyRotation -> {}
        }
    }
}