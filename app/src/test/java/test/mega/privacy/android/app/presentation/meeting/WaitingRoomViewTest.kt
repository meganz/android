package test.mega.privacy.android.app.presentation.meeting

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomState
import mega.privacy.android.app.presentation.meeting.view.WaitingRoomView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

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
            )
        }

        composeTestRule.onNodeWithTag("waiting_room:button_info").performClick()

        verify(onInfoClicked).invoke()
    }

    @Test
    fun `test that leave dialog is shown when close button is clicked`() {
        composeTestRule.setContent {
            WaitingRoomView(
                state = WaitingRoomState(),
                onInfoClicked = {},
                onCloseClicked = {},
                onMicToggleChange = {},
                onCameraToggleChange = {},
                onSpeakerToggleChange = {},
            )
        }

        composeTestRule.onNodeWithTag("waiting_room:button_close").performClick()

        composeTestRule.onNodeWithTag("waiting_room:dialog_leave").assertIsDisplayed()
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
            )
        }

        composeTestRule.onAllNodesWithTag("waiting_room:button_mic")
            .filter(hasClickAction())
            .apply {
                fetchSemanticsNodes().forEachIndexed { i, _ ->
                    get(i).performSemanticsAction(SemanticsActions.OnClick)
                }
            }

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
            )
        }

        composeTestRule.onAllNodesWithTag("waiting_room:button_camera")
            .filter(hasClickAction())
            .apply {
                fetchSemanticsNodes().forEachIndexed { i, _ ->
                    get(i).performSemanticsAction(SemanticsActions.OnClick)
                }
            }

        verify(onCameraToggleChange).invoke(true)
    }

    @Test
    fun `test that onSpeakerToggleChange is called when speaker button is toggled`() {
        val onSpeakerToggleChange: (Boolean) -> Unit = mock()

        composeTestRule.setContent {
            WaitingRoomView(
                state = WaitingRoomState(speakerEnabled = false),
                onInfoClicked = {},
                onCloseClicked = {},
                onMicToggleChange = {},
                onCameraToggleChange = {},
                onSpeakerToggleChange = onSpeakerToggleChange,
            )
        }

        composeTestRule.onAllNodesWithTag("waiting_room:button_speaker")
            .filter(hasClickAction())
            .apply {
                fetchSemanticsNodes().forEachIndexed { i, _ ->
                    get(i).performSemanticsAction(SemanticsActions.OnClick)
                }
            }

        verify(onSpeakerToggleChange).invoke(true)
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
            )
        }

        composeTestRule.onNodeWithTag("waiting_room:preview_camera").assertIsDisplayed()
    }

    @Test
    fun `test that camera preview is not visible when camera is disabled`() {
        composeTestRule.setContent {
            WaitingRoomView(
                state = WaitingRoomState(cameraEnabled = false),
                onInfoClicked = {},
                onCloseClicked = {},
                onMicToggleChange = {},
                onCameraToggleChange = {},
                onSpeakerToggleChange = {},
            )
        }

        composeTestRule.onNodeWithTag("waiting_room:preview_camera").assertDoesNotExist()
    }
}
