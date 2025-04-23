package test.mega.privacy.android.app.presentation.permissions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.triggered
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.presentation.permissions.NEW_PERMISSIONS_SCREEN_CAMERA_BACKUP_PERMISSION
import mega.privacy.android.app.presentation.permissions.NEW_PERMISSIONS_SCREEN_LOADING
import mega.privacy.android.app.presentation.permissions.NEW_PERMISSIONS_SCREEN_NOTIFICATION_PERMISSION
import mega.privacy.android.app.presentation.permissions.NewPermissionScreen
import mega.privacy.android.app.presentation.permissions.NewPermissionsComposableScreen
import mega.privacy.android.app.presentation.permissions.PermissionsUIState
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class NewPermissionsComposableScreenViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that notification permission screen should be displayed when type is notification`() {
        composeTestRule.setContent {
            NewPermissionsComposableScreen(
                uiState = PermissionsUIState(
                    visiblePermission = NewPermissionScreen.Notification
                )
            )
        }

        composeTestRule.onNodeWithTag(NEW_PERMISSIONS_SCREEN_NOTIFICATION_PERMISSION)
            .assertIsDisplayed()
    }

    @Test
    fun `test that notification permission callback invokes on enable permission event`() {
        val mockCallback = mock<() -> Unit>()

        composeTestRule.setContent {
            NewPermissionsComposableScreen(
                uiState = PermissionsUIState(
                    visiblePermission = NewPermissionScreen.Notification
                ),
                askNotificationPermission = mockCallback
            )
        }

        composeTestRule.onNodeWithText(R.string.notification_permission_enable_button_text)
            .performClick()

        verify(mockCallback).invoke()
    }

    @Test
    fun `test that notification permission callback invokes on skip permission event`() {
        val mockCallback = mock<() -> Unit>()

        composeTestRule.setContent {
            NewPermissionsComposableScreen(
                uiState = PermissionsUIState(
                    visiblePermission = NewPermissionScreen.Notification
                ),
                setNextPermission = mockCallback
            )
        }

        composeTestRule.onNodeWithText(R.string.permission_screen_skip_permission_request_button_text)
            .performClick()

        verify(mockCallback).invoke()
    }

    @Test
    fun `test that camera backup permission screen should be displayed when type is camera backup`() {
        composeTestRule.setContent {
            NewPermissionsComposableScreen(
                uiState = PermissionsUIState(
                    visiblePermission = NewPermissionScreen.CameraBackup
                )
            )
        }

        composeTestRule.onNodeWithTag(NEW_PERMISSIONS_SCREEN_CAMERA_BACKUP_PERMISSION)
            .assertIsDisplayed()
    }

    @Test
    fun `test that camera backup permission callback invokes on enable permission event`() {
        val mockCallback = mock<() -> Unit>()

        composeTestRule.setContent {
            NewPermissionsComposableScreen(
                uiState = PermissionsUIState(
                    visiblePermission = NewPermissionScreen.CameraBackup
                ),
                askCameraBackupPermission = mockCallback
            )
        }

        composeTestRule.onNodeWithText(R.string.camera_backup_permission_enable_button_text)
            .performClick()

        verify(mockCallback).invoke()
    }

    @Test
    fun `test that camera backup permission callback invokes on skip permission event`() {
        val mockCallback = mock<() -> Unit>()

        composeTestRule.setContent {
            NewPermissionsComposableScreen(
                uiState = PermissionsUIState(
                    visiblePermission = NewPermissionScreen.CameraBackup
                ),
                setNextPermission = mockCallback
            )
        }

        composeTestRule.onNodeWithText(R.string.permission_screen_skip_permission_request_button_text)
            .performClick()

        verify(mockCallback).invoke()
    }

    @Test
    fun `test that loading screen should be displayed when type is loading`() {
        composeTestRule.setContent {
            NewPermissionsComposableScreen(
                uiState = PermissionsUIState(
                    visiblePermission = NewPermissionScreen.Loading
                )
            )
        }

        composeTestRule.onNodeWithTag(NEW_PERMISSIONS_SCREEN_LOADING)
            .assertIsDisplayed()
    }

    @Test
    fun `test that callback should invoke on finish event`() {
        val mockCallback = mock<() -> Unit>()

        composeTestRule.setContent {
            NewPermissionsComposableScreen(
                uiState = PermissionsUIState(
                    finishEvent = triggered,
                ),
                closePermissionScreen = mockCallback
            )
        }

        verify(mockCallback).invoke()
    }
}