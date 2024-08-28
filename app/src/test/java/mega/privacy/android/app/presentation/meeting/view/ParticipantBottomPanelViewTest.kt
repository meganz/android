package mega.privacy.android.app.presentation.meeting.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.app.presentation.meeting.view.BottomPanelView
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_MUTE_ALL_ITEM_VIEW
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_PARTICIPANTS_WARNING
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.meeting.ParticipantsSection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParticipantBottomPanelViewTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private val users: List<Long> = listOf(12L, 34L, 56L)

    @Test
    fun `test that mute all button is shown`() {
        initComposeRuleContent(
            MeetingState(
                myPermission = ChatRoomPermission.Moderator,
                participantsSection = ParticipantsSection.InCallSection
            ),
        )
        composeRule.onNodeWithTag(TEST_TAG_MUTE_ALL_ITEM_VIEW).assertIsDisplayed()
    }

    @Test
    fun `test that mute all button is hidden`() {
        initComposeRuleContent(
            MeetingState(
                myPermission = ChatRoomPermission.ReadOnly,
                participantsSection = ParticipantsSection.InCallSection
            ),
        )
        composeRule.onNodeWithTag(TEST_TAG_MUTE_ALL_ITEM_VIEW).assertDoesNotExist()
    }

    @Test
    fun `test that warning banner is shown when is waiting room section`() {
        initComposeRuleContent(
            uiState = MeetingState(
                myPermission = ChatRoomPermission.Moderator,
                participantsSection = ParticipantsSection.WaitingRoomSection
            ),
            waitingRoomManagementState = WaitingRoomManagementState(
                isCallUnlimitedProPlanFeatureFlagEnabled = true,
                usersInWaitingRoomIDs = users,
                callUsersLimit = users.size,
                numUsersInCall = users.size
            ),
        )
        composeRule.onNodeWithTag(TEST_TAG_PARTICIPANTS_WARNING).assertIsDisplayed()
    }

    @Test
    fun `test that warning banner is not shown when in call tab is not selected`() {
        initComposeRuleContent(
            MeetingState(
                myPermission = ChatRoomPermission.Moderator,
                participantsSection = ParticipantsSection.NotInCallSection
            ),
        )
        composeRule.onNodeWithTag(TEST_TAG_PARTICIPANTS_WARNING).assertDoesNotExist()
    }

    private fun initComposeRuleContent(
        uiState: MeetingState,
    ) {
        composeRule.setContent {
            BottomPanelView(
                uiState = uiState,
                waitingRoomManagementState = WaitingRoomManagementState(),
                onWaitingRoomClick = { },
                onInCallClick = { },
                onNotInCallClick = { },
                onAdmitAllClick = { },
                onSeeAllClick = { },
                onInviteParticipantsClick = { },
                onShareMeetingLinkClick = { },
                onAllowAddParticipantsClick = { },
                onAdmitParticipantClicked = { },
                onDenyParticipantClicked = { },
                onParticipantMoreOptionsClicked = { },
                onRingParticipantClicked = { },
                onRingAllParticipantsClicked = { },
                onMuteAllParticipantsClick = { },
            )
        }
    }

    private fun initComposeRuleContent(
        uiState: MeetingState,
        waitingRoomManagementState: WaitingRoomManagementState
    ) {
        composeRule.setContent {
            BottomPanelView(
                uiState = uiState,
                waitingRoomManagementState = waitingRoomManagementState,
                onWaitingRoomClick = { },
                onInCallClick = { },
                onNotInCallClick = { },
                onAdmitAllClick = { },
                onSeeAllClick = { },
                onInviteParticipantsClick = { },
                onShareMeetingLinkClick = { },
                onAllowAddParticipantsClick = { },
                onAdmitParticipantClicked = { },
                onDenyParticipantClicked = { },
                onParticipantMoreOptionsClicked = { },
                onRingParticipantClicked = { },
                onRingAllParticipantsClicked = { },
                onMuteAllParticipantsClick = { },
            )
        }
    }
}
