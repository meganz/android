package mega.privacy.android.app.presentation.meeting.view

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.presentation.meeting.WaitingRoomManagementViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.getRetentionTimeString
import mega.privacy.android.app.presentation.meeting.model.ChatInfoAction
import mega.privacy.android.app.presentation.meeting.model.ChatInfoUiState
import mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementUiState
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ChatInfoViewTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that meeting link button is shown`() {
        initComposeRuleContent(
            ChatInfoUiState(
                isHost = true,
                isPublic = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(),
            noteToSelfChatState = NoteToSelfChatUIState(),
        )
        composeRule.onNodeWithText(R.string.meeting_link).assertExists()
    }

    @Test
    fun `test that verify meeting link button performs action`() {
        val onButtonClicked = mock<(ChatInfoAction) -> Unit>()
        initComposeRuleContent(
            ChatInfoUiState(
                isHost = true,
                isPublic = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(),
            noteToSelfChatState = NoteToSelfChatUIState(),
            onButtonClicked = onButtonClicked
        )

        composeRule.onNodeWithText(R.string.meeting_link).performClick()

        verify(onButtonClicked).invoke(ChatInfoAction.MeetingLink)
    }

    @Test
    fun `test that share meeting link button is shown`() {
        initComposeRuleContent(
            ChatInfoUiState(
                isHost = true,
                isPublic = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(enabledMeetingLinkOption = true),
            noteToSelfChatState = NoteToSelfChatUIState(),
        )
        composeRule.onNodeWithText(R.string.meetings_scheduled_meeting_info_share_meeting_link_label)
            .assertExists()
    }

    @Test
    fun `test that verify share meeting link button performs action`() {
        val onButtonClicked = mock<(ChatInfoAction) -> Unit>()
        initComposeRuleContent(
            ChatInfoUiState(
                isHost = true,
                isPublic = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(enabledMeetingLinkOption = true),
            noteToSelfChatState = NoteToSelfChatUIState(),
            onButtonClicked = onButtonClicked
        )

        composeRule.onNodeWithText(R.string.meetings_scheduled_meeting_info_share_meeting_link_label)
            .performClick()

        verify(onButtonClicked).invoke(ChatInfoAction.ShareMeetingLink)
    }

    @Test
    fun `test that chat notifications button is shown`() {
        initComposeRuleContent(
            ChatInfoUiState(
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(),
            noteToSelfChatState = NoteToSelfChatUIState(),
        )
        composeRule.onNodeWithText(R.string.meetings_info_notifications_option)
            .assertExists()
    }

    @Test
    fun `test that verify chat notifications button performs action`() {
        val onButtonClicked = mock<(ChatInfoAction) -> Unit>()
        initComposeRuleContent(
            ChatInfoUiState(
                isHost = false,
                isOpenInvite = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(),
            noteToSelfChatState = NoteToSelfChatUIState(),
            onButtonClicked = onButtonClicked
        )

        composeRule.onNodeWithText(R.string.meetings_info_notifications_option)
            .performClick()

        verify(onButtonClicked).invoke(ChatInfoAction.ChatNotifications)
    }

    @Test
    fun `test that allow non-hosts to add participants button is shown`() {
        initComposeRuleContent(
            ChatInfoUiState(
                isHost = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(),
            noteToSelfChatState = NoteToSelfChatUIState(),
        )
        composeRule.onNodeWithText(R.string.chat_group_chat_info_allow_non_host_participants_option)
            .assertExists()
    }

    @Test
    fun `test that verify allow non-hosts to add participants button performs action`() {
        val onButtonClicked = mock<(ChatInfoAction) -> Unit>()
        initComposeRuleContent(
            ChatInfoUiState(
                isHost = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(),
            noteToSelfChatState = NoteToSelfChatUIState(),
            onButtonClicked = onButtonClicked
        )

        composeRule.onNodeWithText(R.string.chat_group_chat_info_allow_non_host_participants_option)
            .performClick()

        verify(onButtonClicked).invoke(ChatInfoAction.AllowNonHostAddParticipants)
    }

    @Test
    fun `test that shared files button is shown`() {
        initComposeRuleContent(
            ChatInfoUiState(
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(),
            noteToSelfChatState = NoteToSelfChatUIState(),
        )
        composeRule.onNodeWithText(R.string.title_chat_shared_files_info)
            .assertExists()
    }

    @Test
    fun `test that verify shared files button performs action`() {
        val onButtonClicked = mock<(ChatInfoAction) -> Unit>()
        initComposeRuleContent(
            ChatInfoUiState(
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(),
            noteToSelfChatState = NoteToSelfChatUIState(),
            onButtonClicked = onButtonClicked
        )

        composeRule.onNodeWithText(R.string.title_chat_shared_files_info)
            .performClick()

        verify(onButtonClicked).invoke(ChatInfoAction.ShareFiles)
    }

    @Test
    fun `test that share meeting link non hosts button is shown`() {
        initComposeRuleContent(
            ChatInfoUiState(
                isHost = false,
                isPublic = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(
                enabledMeetingLinkOption = true,
            ),
            noteToSelfChatState = NoteToSelfChatUIState(),
        )
        composeRule.onNodeWithText(R.string.meetings_scheduled_meeting_info_share_meeting_link_label)
            .assertExists()
    }

    @Test
    fun `test that verify share meeting link non hosts button performs action`() {
        val onButtonClicked = mock<(ChatInfoAction) -> Unit>()
        initComposeRuleContent(
            ChatInfoUiState(
                isHost = false,
                isPublic = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(
                enabledMeetingLinkOption = true,
            ),
            noteToSelfChatState = NoteToSelfChatUIState(),
            onButtonClicked = onButtonClicked
        )

        composeRule.onNodeWithText(R.string.meetings_scheduled_meeting_info_share_meeting_link_label)
            .performClick()

        verify(onButtonClicked).invoke(ChatInfoAction.ShareMeetingLinkNonHosts)
    }

    @Test
    fun `test that enabled encrypted key rotation label is shown`() {
        initComposeRuleContent(
            ChatInfoUiState(
                isHost = true,
                isPublic = false,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(), noteToSelfChatState = NoteToSelfChatUIState(),

            )
        composeRule.onNodeWithText(R.string.meetings_info_notifications_option)
            .assertExists()
    }

    @Test
    fun `test that add participants option is shown when is open invite`() {
        initComposeRuleContent(
            ChatInfoUiState(
                isHost = false,
                isOpenInvite = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState(
                enabledMeetingLinkOption = true,
                waitingRoomReminder = WaitingRoomReminders.Enabled
            ),
            noteToSelfChatState = NoteToSelfChatUIState(),
        )
        composeRule.onNodeWithText(R.string.add_participants_menu_item)
            .assertExists()
    }

    @Test
    fun `test that format retention time 0 seconds`() {
        val seconds: Long = 0
        val expectedResult = ""
        composeRule.setContent {
            getRetentionTime(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 60 seconds`() {
        val seconds: Long = 60
        val expectedResult = ""
        composeRule.setContent {
            getRetentionTime(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 3600 seconds`() {
        val seconds: Long = 3600
        val expectedResult = "1 hour"
        composeRule.setContent {
            getRetentionTime(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 86400 seconds`() {
        val seconds: Long = 86400
        val expectedResult = "1 day"
        composeRule.setContent {
            getRetentionTime(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 604800 seconds`() {
        val seconds: Long = 604800
        val expectedResult = "1 week"
        composeRule.setContent {
            getRetentionTime(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 1209600 seconds`() {
        val seconds: Long = 1209600
        val expectedResult = "2 weeks"
        composeRule.setContent {
            getRetentionTime(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 2592000 seconds`() {
        val seconds: Long = 2592000
        val expectedResult = "1 month"
        composeRule.setContent {
            getRetentionTime(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 31536000 seconds`() {
        val seconds: Long = 31536000
        val expectedResult = "1 year"
        composeRule.setContent {
            getRetentionTime(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that participants warning is shown when user limit is reached`() {
        initComposeRuleContent(
            ChatInfoUiState(
                myPermission = ChatRoomPermission.Moderator,
                shouldShowParticipantsLimitWarning = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState().copy(
                isCallUnlimitedProPlanFeatureFlagEnabled = true,
                subscriptionPlan = AccountType.FREE,
            ),
            noteToSelfChatState = NoteToSelfChatUIState(),
        )
        composeRule.onNodeWithTag(SCHEDULE_MEETING_INFO_PARTICIPANTS_WARNING_TAG).assertExists()
    }

    private val waitingRoomManagementViewModel = mock<WaitingRoomManagementViewModel> {
        on { state } doReturn MutableStateFlow(WaitingRoomManagementState())
    }

    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(WaitingRoomManagementViewModel::class.java.canonicalName.orEmpty()) }) } doReturn waitingRoomManagementViewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }

    @Test
    fun `test that participants warning is not shown when user limit is not reached`() {
        initComposeRuleContent(
            ChatInfoUiState(
                myPermission = ChatRoomPermission.Moderator,
                shouldShowParticipantsLimitWarning = false,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementUiState().copy(
                isCallUnlimitedProPlanFeatureFlagEnabled = true,
                subscriptionPlan = AccountType.FREE,
            ),
            noteToSelfChatState = NoteToSelfChatUIState(),
        )
        composeRule.onNodeWithTag(SCHEDULE_MEETING_INFO_PARTICIPANTS_WARNING_TAG)
            .assertDoesNotExist()
    }

    private fun getRetentionTime(seconds: Long) =
        getRetentionTimeString(composeRule.activity, timeInSeconds = seconds) ?: ""

    private fun getChatScheduledMeeting(): ChatScheduledMeeting =
        ChatScheduledMeeting(
            chatId = -1,
            schedId = -1,
            parentSchedId = null,
            organizerUserId = null,
            timezone = null,
            startDateTime = -1,
            endDateTime = -1,
            title = "Scheduled title",
            description = "Scheduled description",
            attributes = null,
            overrides = null,
            flags = null,
            rules = null,
            changes = null,
            isCanceled = false,
        )

    private fun initComposeRuleContent(
        state: ChatInfoUiState,
        managementState: ScheduledMeetingManagementUiState,
        noteToSelfChatState: NoteToSelfChatUIState
    ) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                ChatInfoView(
                    state = state,
                    managementState = managementState,
                    noteToSelfChatState = noteToSelfChatState,
                    onButtonClicked = {},
                    onEditClicked = {},
                    onAddParticipantsClicked = {},
                    onSeeMoreOrLessClicked = {},
                    onLeaveGroupClicked = {},
                    onParticipantClicked = {},
                    onBackPressed = {},
                    onDismiss = {},
                    onLeaveGroupDialog = {},
                    onInviteParticipantsDialog = {},
                    onResetStateSnackbarMessage = {},
                    onCloseWarningClicked = {},
                )
            }
        }
    }

    private fun initComposeRuleContent(
        state: ChatInfoUiState,
        managementState: ScheduledMeetingManagementUiState,
        noteToSelfChatState: NoteToSelfChatUIState,
        onButtonClicked: (ChatInfoAction) -> Unit = {},
    ) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                ChatInfoView(
                    state = state,
                    managementState = managementState,
                    noteToSelfChatState = noteToSelfChatState,
                    onButtonClicked = onButtonClicked,
                    onEditClicked = {},
                    onAddParticipantsClicked = {},
                    onSeeMoreOrLessClicked = {},
                    onLeaveGroupClicked = {},
                    onParticipantClicked = {},
                    onBackPressed = {},
                    onDismiss = {},
                    onLeaveGroupDialog = {},
                    onInviteParticipantsDialog = {},
                    onResetStateSnackbarMessage = {},
                    onCloseWarningClicked = {},
                )
            }
        }
    }
}
