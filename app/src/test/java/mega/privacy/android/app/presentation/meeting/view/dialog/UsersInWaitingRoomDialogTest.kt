package mega.privacy.android.app.presentation.meeting.view.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomManagementState
import mega.privacy.android.app.presentation.meeting.view.dialog.TEST_TAG_USERS_IN_WAITING_ROOM_DIALOG
import mega.privacy.android.app.presentation.meeting.view.dialog.UsersInWaitingRoomDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class UsersInWaitingRoomDialogTest {
    @get:Rule
    var composeRule = createComposeRule()

    private val users: List<Long> = listOf(12L, 34L, 56L)

    @Test
    fun `test that dialog is shown`() {
        initComposeRuleContent(
            uiState = WaitingRoomManagementState(
                showParticipantsInWaitingRoomDialog = true,
                showDenyParticipantDialog = false,
                isWaitingRoomSectionOpened = false,
                usersInWaitingRoomIDs = users
            ),
            onAdmitClick = {},
            onSeeWaitingRoomClick = {},
            onDismiss = {},
            onDenyClick = {}
        )

        composeRule.onNodeWithTag(TEST_TAG_USERS_IN_WAITING_ROOM_DIALOG).assertIsDisplayed()
    }

    @Test
    fun `test that dialog is hidden`() {
        initComposeRuleContent(
            uiState = WaitingRoomManagementState(showParticipantsInWaitingRoomDialog = false),
            onAdmitClick = {},
            onSeeWaitingRoomClick = {},
            onDismiss = {},
            onDenyClick = {}
        )

        composeRule.onNodeWithTag(TEST_TAG_USERS_IN_WAITING_ROOM_DIALOG).assertIsNotDisplayed()
    }

    @Test
    fun `test that admit participant button is clicked`() {
        val onAdmitClick = mock<() -> Unit>()

        initComposeRuleContent(
            uiState = WaitingRoomManagementState(
                showParticipantsInWaitingRoomDialog = true,
                showDenyParticipantDialog = false,
                isWaitingRoomSectionOpened = false,
                usersInWaitingRoomIDs = users
            ),
            onAdmitClick = onAdmitClick,
            onSeeWaitingRoomClick = {},
            onDismiss = {},
            onDenyClick = {}
        )

        composeRule.onNodeWithTag(TEST_TAG_USERS_IN_WAITING_ROOM_DIALOG).performClick()
    }

    @Test
    fun `test that see waiting room button is clicked`() {
        val onSeeWaitingRoomClick = mock<() -> Unit>()

        initComposeRuleContent(
            uiState = WaitingRoomManagementState(
                showParticipantsInWaitingRoomDialog = true,
                showDenyParticipantDialog = false,
                isWaitingRoomSectionOpened = false,
                usersInWaitingRoomIDs = users
            ),
            onAdmitClick = {},
            onSeeWaitingRoomClick = onSeeWaitingRoomClick,
            onDismiss = {},
            onDenyClick = {}
        )

        composeRule.onNodeWithTag(TEST_TAG_USERS_IN_WAITING_ROOM_DIALOG).performClick()
    }

    @Test
    fun `test that dismiss dialog button is clicked`() {
        val onDismiss = mock<() -> Unit>()

        initComposeRuleContent(
            uiState = WaitingRoomManagementState(
                showParticipantsInWaitingRoomDialog = true,
                showDenyParticipantDialog = false,
                isWaitingRoomSectionOpened = false,
                usersInWaitingRoomIDs = users
            ),
            onAdmitClick = {},
            onSeeWaitingRoomClick = {},
            onDismiss = onDismiss,
            onDenyClick = {}
        )

        composeRule.onNodeWithTag(TEST_TAG_USERS_IN_WAITING_ROOM_DIALOG).performClick()
    }

    @Test
    fun `test that deny participant button is clicked`() {
        val onDenyClick = mock<() -> Unit>()

        initComposeRuleContent(
            uiState = WaitingRoomManagementState(
                showParticipantsInWaitingRoomDialog = true,
                showDenyParticipantDialog = false,
                isWaitingRoomSectionOpened = false,
                usersInWaitingRoomIDs = users
            ),
            onAdmitClick = {},
            onSeeWaitingRoomClick = {},
            onDismiss = {},
            onDenyClick = onDenyClick
        )

        composeRule.onNodeWithTag(TEST_TAG_USERS_IN_WAITING_ROOM_DIALOG).performClick()
    }

    private fun initComposeRuleContent(
        uiState: WaitingRoomManagementState,
        onAdmitClick: () -> Unit,
        onSeeWaitingRoomClick: () -> Unit,
        onDismiss: () -> Unit,
        onDenyClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            UsersInWaitingRoomDialog(
                uiState = uiState,
                onAdmitClick = onAdmitClick,
                onSeeWaitingRoomClick = onSeeWaitingRoomClick,
                onDismiss = onDismiss,
                onDenyClick = onDenyClick,
            )
        }
    }

}