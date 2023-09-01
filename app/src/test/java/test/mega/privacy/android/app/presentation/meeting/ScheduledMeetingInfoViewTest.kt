package test.mega.privacy.android.app.presentation.meeting

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingManagementState
import mega.privacy.android.app.presentation.meeting.view.ScheduledMeetingInfoView
import mega.privacy.android.app.presentation.meeting.view.formatRetentionTimeInSecondsToString
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class ScheduledMeetingInfoViewTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that meeting link button is shown`() {
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                isHost = true,
                isPublic = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState()
        )
        composeRule.onNodeWithText(R.string.meeting_link).assertExists()
    }

    @Test
    fun `test that verify meeting link button performs action`() {
        val onButtonClicked = mock<(ScheduledMeetingInfoAction) -> Unit>()
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                isHost = true,
                isPublic = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState(),
            onButtonClicked = onButtonClicked
        )

        composeRule.onNodeWithText(R.string.meeting_link).performClick()

        verify(onButtonClicked).invoke(ScheduledMeetingInfoAction.MeetingLink)
    }

    @Test
    fun `test that share meeting link button is shown`() {
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                isHost = true,
                isPublic = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState(enabledMeetingLinkOption = true),
        )
        composeRule.onNodeWithText(R.string.meetings_scheduled_meeting_info_share_meeting_link_label)
            .assertExists()
    }

    @Test
    fun `test that verify share meeting link button performs action`() {
        val onButtonClicked = mock<(ScheduledMeetingInfoAction) -> Unit>()
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                isHost = true,
                isPublic = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState(enabledMeetingLinkOption = true),
            onButtonClicked = onButtonClicked
        )

        composeRule.onNodeWithText(R.string.meetings_scheduled_meeting_info_share_meeting_link_label)
            .performClick()

        verify(onButtonClicked).invoke(ScheduledMeetingInfoAction.ShareMeetingLink)
    }

    @Test
    fun `test that chat notifications button is shown`() {
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState()
        )
        composeRule.onNodeWithText(R.string.meetings_info_notifications_option)
            .assertExists()
    }

    @Test
    fun `test that verify chat notifications button performs action`() {
        val onButtonClicked = mock<(ScheduledMeetingInfoAction) -> Unit>()
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                isHost = false,
                isOpenInvite = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState(),
            onButtonClicked = onButtonClicked
        )

        composeRule.onNodeWithText(R.string.meetings_info_notifications_option)
            .performClick()

        verify(onButtonClicked).invoke(ScheduledMeetingInfoAction.ChatNotifications)
    }

    @Test
    fun `test that allow non-hosts to add participants button is shown`() {
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                isHost = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState()
        )
        composeRule.onNodeWithText(R.string.chat_group_chat_info_allow_non_host_participants_option)
            .assertExists()
    }

    @Test
    fun `test that verify allow non-hosts to add participants button performs action`() {
        val onButtonClicked = mock<(ScheduledMeetingInfoAction) -> Unit>()
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                isHost = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState(),
            onButtonClicked = onButtonClicked
        )

        composeRule.onNodeWithText(R.string.chat_group_chat_info_allow_non_host_participants_option)
            .performClick()

        verify(onButtonClicked).invoke(ScheduledMeetingInfoAction.AllowNonHostAddParticipants)
    }

    @Test
    fun `test that shared files button is shown`() {
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState()
        )
        composeRule.onNodeWithText(R.string.title_chat_shared_files_info)
            .assertExists()
    }

    @Test
    fun `test that verify shared files button performs action`() {
        val onButtonClicked = mock<(ScheduledMeetingInfoAction) -> Unit>()
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState(),
            onButtonClicked = onButtonClicked
        )

        composeRule.onNodeWithText(R.string.title_chat_shared_files_info)
            .performClick()

        verify(onButtonClicked).invoke(ScheduledMeetingInfoAction.ShareFiles)
    }

    @Test
    fun `test that share meeting link non hosts button is shown`() {
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                isHost = false,
                isPublic = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState(
                enabledMeetingLinkOption = true,
            )
        )
        composeRule.onNodeWithText(R.string.meetings_scheduled_meeting_info_share_meeting_link_label)
            .assertExists()
    }

    @Test
    fun `test that verify share meeting link non hosts button performs action`() {
        val onButtonClicked = mock<(ScheduledMeetingInfoAction) -> Unit>()
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                isHost = false,
                isPublic = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState(
                enabledMeetingLinkOption = true,
            ),
            onButtonClicked = onButtonClicked
        )

        composeRule.onNodeWithText(R.string.meetings_scheduled_meeting_info_share_meeting_link_label)
            .performClick()

        verify(onButtonClicked).invoke(ScheduledMeetingInfoAction.ShareMeetingLinkNonHosts)
    }

    @Test
    fun `test that enabled encrypted key rotation label is shown`() {
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                isHost = true,
                isPublic = false,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState()

        )
        composeRule.onNodeWithText(R.string.meetings_info_notifications_option)
            .assertExists()
    }

    @Test
    fun `test that add participants option is shown when is open invite`() {
        initComposeRuleContent(
            ScheduledMeetingInfoState(
                isHost = false,
                isOpenInvite = true,
                scheduledMeeting = getChatScheduledMeeting()
            ),
            ScheduledMeetingManagementState(
                enabledMeetingLinkOption = true,
                waitingRoomReminder = WaitingRoomReminders.Enabled
            )
        )
        composeRule.onNodeWithText(R.string.add_participants_menu_item)
            .assertExists()
    }

    @Test
    fun `test that format retention time 0 seconds`() {
        val seconds: Long = 0
        val expectedResult = ""
        composeRule.setContent {
            formatRetentionTimeInSecondsToString(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 60 seconds`() {
        val seconds: Long = 60
        val expectedResult = ""
        composeRule.setContent {
            formatRetentionTimeInSecondsToString(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 3600 seconds`() {
        val seconds: Long = 3600
        val expectedResult = "1 hour"
        composeRule.setContent {
            formatRetentionTimeInSecondsToString(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 86400 seconds`() {
        val seconds: Long = 86400
        val expectedResult = "1 day"
        composeRule.setContent {
            formatRetentionTimeInSecondsToString(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 604800 seconds`() {
        val seconds: Long = 604800
        val expectedResult = "1 week"
        composeRule.setContent {
            formatRetentionTimeInSecondsToString(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 1209600 seconds`() {
        val seconds: Long = 1209600
        val expectedResult = "2 weeks"
        composeRule.setContent {
            formatRetentionTimeInSecondsToString(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 2592000 seconds`() {
        val seconds: Long = 2592000
        val expectedResult = "1 month"
        composeRule.setContent {
            formatRetentionTimeInSecondsToString(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

    @Test
    fun `test that format retention time 31536000 seconds`() {
        val seconds: Long = 31536000
        val expectedResult = "1 year"
        composeRule.setContent {
            formatRetentionTimeInSecondsToString(seconds = seconds).let {
                Truth.assertThat(it).isEqualTo(expectedResult)
            }
        }
    }

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
        state: ScheduledMeetingInfoState,
        managementState: ScheduledMeetingManagementState,
    ) {
        composeRule.setContent {
            ScheduledMeetingInfoView(
                state = state,
                managementState = managementState,
                onButtonClicked = {},
                onEditClicked = {},
                onAddParticipantsClicked = {},
                onSeeMoreOrLessClicked = {},
                onLeaveGroupClicked = {},
                onParticipantClicked = {},
                onScrollChange = {},
                onBackPressed = {},
                onDismiss = {},
                onLeaveGroupDialog = {},
                onInviteParticipantsDialog = {},
                onSnackbarShown = {},
                onCloseWarningClicked = {},
            )
        }
    }

    private fun initComposeRuleContent(
        state: ScheduledMeetingInfoState,
        managementState: ScheduledMeetingManagementState,
        onButtonClicked: (ScheduledMeetingInfoAction) -> Unit = {},
    ) {
        composeRule.setContent {
            ScheduledMeetingInfoView(
                state = state,
                managementState = managementState,
                onButtonClicked = onButtonClicked,
                onEditClicked = {},
                onAddParticipantsClicked = {},
                onSeeMoreOrLessClicked = {},
                onLeaveGroupClicked = {},
                onParticipantClicked = {},
                onScrollChange = {},
                onBackPressed = {},
                onDismiss = {},
                onLeaveGroupDialog = {},
                onInviteParticipantsDialog = {},
                onSnackbarShown = {},
                onCloseWarningClicked = {},
            )
        }
    }
}