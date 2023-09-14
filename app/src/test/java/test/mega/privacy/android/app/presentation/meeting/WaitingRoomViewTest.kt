package test.mega.privacy.android.app.presentation.meeting

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.flow.emptyFlow
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomState
import mega.privacy.android.app.presentation.meeting.view.WaitingRoomView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.performClickOnAllNodes

@RunWith(AndroidJUnit4::class)
class WaitingRoomViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val permissionTestRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO
    )

    @Test
    fun `test that onInfoClicked is called when info button is clicked`() {
        val onInfoClicked: () -> Unit = mock()

        composeTestRule.setContent {
            WaitingRoomView(
                state = WaitingRoomState(),
                onInfoClicked = onInfoClicked,
                onCloseClicked = {},
                onMicToggleChange = {},
                onCameraToggleChange = {},
                onSpeakerToggleChange = {},
                onGuestNameChange = { _, _ -> },
                videoStream = emptyFlow()
            )
        }

        composeTestRule.onNodeWithTag("waiting_room:button_info").performClick()

        verify(onInfoClicked).invoke()
    }

    @Test
    fun `test that onMicToggleChange is called when mic button is toggled`() {
        val onMicToggleChange: (Boolean) -> Unit = mock()

        composeTestRule.setContent {
            WaitingRoomView(
                state = WaitingRoomState(micEnabled = false),
                onInfoClicked = {},
                onCloseClicked = {},
                onMicToggleChange = onMicToggleChange,
                onCameraToggleChange = {},
                onSpeakerToggleChange = {},
                onGuestNameChange = { _, _ -> },
                videoStream = emptyFlow()
            )
        }

        composeTestRule.performClickOnAllNodes("waiting_room:button_mic")

        verify(onMicToggleChange).invoke(true)
    }

    @Test
    fun `test that onCameraToggleChange is called when camera button is toggled`() {
        val onCameraToggleChange: (Boolean) -> Unit = mock()

        composeTestRule.setContent {
            WaitingRoomView(
                state = WaitingRoomState(cameraEnabled = false),
                onInfoClicked = {},
                onCloseClicked = {},
                onMicToggleChange = {},
                onCameraToggleChange = onCameraToggleChange,
                onSpeakerToggleChange = {},
                onGuestNameChange = { _, _ -> },
                videoStream = emptyFlow()
            )
        }

        composeTestRule.performClickOnAllNodes("waiting_room:button_camera")

        verify(onCameraToggleChange).invoke(true)
    }

    @Test
    fun `test that camera preview is visible when camera is enabled`() {
        composeTestRule.setContent {
            WaitingRoomView(
                state = WaitingRoomState(cameraEnabled = true),
                onInfoClicked = {},
                onCloseClicked = {},
                onMicToggleChange = {},
                onCameraToggleChange = {},
                onSpeakerToggleChange = {},
                onGuestNameChange = { _, _ -> },
                videoStream = emptyFlow()
            )
        }

        composeTestRule.onNodeWithTag("waiting_room:preview_camera").assertIsDisplayed()
    }

    @Test
    fun `test that deny access dialog is shown when denyAccessDialog is true`() {
        composeTestRule.setContent {
            WaitingRoomView(
                state = WaitingRoomState(denyAccessDialog = true),
                onInfoClicked = {},
                onCloseClicked = {},
                onMicToggleChange = {},
                onCameraToggleChange = {},
                onSpeakerToggleChange = {},
                onGuestNameChange = { _, _ -> },
                videoStream = emptyFlow()
            )
        }

        composeTestRule.onNodeWithTag("waiting_room:dialog_deny_access").assertIsDisplayed()
    }

    @Test
    fun `test that inactive host dialog is shown when inactiveHostDialog is true`() {
        composeTestRule.setContent {
            WaitingRoomView(
                state = WaitingRoomState(inactiveHostDialog = true),
                onInfoClicked = {},
                onCloseClicked = {},
                onMicToggleChange = {},
                onCameraToggleChange = {},
                onSpeakerToggleChange = {},
                onGuestNameChange = { _, _ -> },
                videoStream = emptyFlow()
            )
        }

        composeTestRule.onNodeWithTag("waiting_room:dialog_inactive_host").assertIsDisplayed()
    }

    @Test
    fun `test that guest name input fields are not visible when isGuestMode is false`() {
        composeTestRule.setContent {
            WaitingRoomView(
                state = WaitingRoomState(
                    chatLink = null,
                    guestFirstName = "John",
                    guestLastName = "Doe"
                ),
                onInfoClicked = {},
                onCloseClicked = {},
                onMicToggleChange = {},
                onCameraToggleChange = {},
                onSpeakerToggleChange = {},
                onGuestNameChange = { _, _ -> },
                videoStream = emptyFlow()
            )
        }

        composeTestRule.onNodeWithTag("waiting_room:guest_name_inputs").assertDoesNotExist()
    }

    @Test
    fun `test that join button is not visible when both first and last names are provided`() {
        composeTestRule.setContent {
            WaitingRoomView(
                state = WaitingRoomState(
                    chatLink = "mega.co.nz",
                    guestFirstName = "John",
                    guestLastName = "Doe"
                ),
                onInfoClicked = {},
                onCloseClicked = {},
                onMicToggleChange = {},
                onCameraToggleChange = {},
                onSpeakerToggleChange = {},
                onGuestNameChange = { _, _ -> },
                videoStream = emptyFlow()
            )
        }

        composeTestRule.onNodeWithTag("waiting_room:button_join").assertDoesNotExist()
    }
}
