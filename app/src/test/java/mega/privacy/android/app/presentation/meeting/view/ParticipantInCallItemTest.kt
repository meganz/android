package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.view.ParticipantInCallItem
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_ADMIT_PARTICIPANT_ICON
import mega.privacy.android.app.presentation.meeting.view.TEST_TAG_DENY_PARTICIPANT_ICON
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.ParticipantsSection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
class ParticipantInCallItemTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that the admit participant icon is shown and enabled`() {
        initComposeRule(
            section = ParticipantsSection.WaitingRoomSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = false,
            isUsersLimitInCallReached = false,
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
        )
        composeRule.onNodeWithTag(TEST_TAG_ADMIT_PARTICIPANT_ICON, true).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_ADMIT_PARTICIPANT_ICON, true).assertIsEnabled()

    }

    fun `test that the admit participant icon is hidden`() {
        initComposeRule(
            section = ParticipantsSection.InCallSection,
            myPermission = ChatRoomPermission.Standard,
            isGuest = true,
            isUsersLimitInCallReached = false,
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
        )
        composeRule.onNodeWithTag(TEST_TAG_ADMIT_PARTICIPANT_ICON, true).assertIsNotDisplayed()
    }

    @Test
    fun `test that the admit participant icon is disabled`() {
        initComposeRule(
            section = ParticipantsSection.WaitingRoomSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = false,
            isUsersLimitInCallReached = true,
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
        )
        composeRule.onNodeWithTag(TEST_TAG_ADMIT_PARTICIPANT_ICON, true).assertIsNotEnabled()
    }

    @Test
    fun `test that on admit participant clicked is invoked when clicked`() {
        val onAdmitParticipantClicked = mock<(ChatParticipant) -> Unit>()
        initComposeRule(
            section = ParticipantsSection.WaitingRoomSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = false,
            isUsersLimitInCallReached = false,
            onAdmitParticipantClicked = onAdmitParticipantClicked,
            onDenyParticipantClicked = {},
        )
        composeRule.onNodeWithTag(TEST_TAG_ADMIT_PARTICIPANT_ICON).apply {
            assertIsDisplayed()
            performClick()
        }
        verify(onAdmitParticipantClicked).invoke(getChatParticipant())
    }

    @Test
    fun `test that the deny participant icon is shown and enabled`() {
        initComposeRule(
            section = ParticipantsSection.WaitingRoomSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = false,
            isUsersLimitInCallReached = false,
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = {},
        )
        composeRule.onNodeWithTag(TEST_TAG_DENY_PARTICIPANT_ICON, true).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_DENY_PARTICIPANT_ICON, true).assertIsEnabled()

    }

    @Test
    fun `test that on deny participant clicked is invoked when clicked`() {
        val onDenyParticipantClicked = mock<(ChatParticipant) -> Unit>()
        initComposeRule(
            section = ParticipantsSection.WaitingRoomSection,
            myPermission = ChatRoomPermission.Moderator,
            isGuest = false,
            isUsersLimitInCallReached = false,
            onAdmitParticipantClicked = {},
            onDenyParticipantClicked = onDenyParticipantClicked,
        )
        composeRule.onNodeWithTag(TEST_TAG_DENY_PARTICIPANT_ICON).apply {
            assertIsDisplayed()
            performClick()
        }
        verify(onDenyParticipantClicked).invoke(getChatParticipant())
    }

    private fun initComposeRule(
        section: ParticipantsSection,
        myPermission: ChatRoomPermission,
        isGuest: Boolean,
        isUsersLimitInCallReached: Boolean = false,
        onAdmitParticipantClicked: (ChatParticipant) -> Unit = {},
        onDenyParticipantClicked: (ChatParticipant) -> Unit = {},
    ) {
        composeRule.setContent {
            ParticipantInCallItem(
                section = section,
                myPermission = myPermission,
                isGuest = isGuest,
                participant = getChatParticipant(),
                isRingingAll = false,
                isUsersLimitInCallReached = isUsersLimitInCallReached,
                onAdmitParticipantClicked = onAdmitParticipantClicked,
                onDenyParticipantClicked = onDenyParticipantClicked,
                onParticipantMoreOptionsClicked = {},
                onRingParticipantClicked = {}
            )
        }
    }

    private fun getChatParticipant(): ChatParticipant = ChatParticipant(
        handle = 555L,
        data = ContactData(fullName = "Name5", alias = null, avatarUri = null),
        email = "name2@mega.nz",
        isMe = false,
        privilege = ChatRoomPermission.Moderator,
        defaultAvatarColor = -30327,
        status = UserChatStatus.Online
    )
}