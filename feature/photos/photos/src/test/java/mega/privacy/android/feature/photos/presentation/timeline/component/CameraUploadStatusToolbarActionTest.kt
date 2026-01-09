package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.photos.presentation.CUStatusUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CameraUploadStatusToolbarActionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the warning icon is displayed`() {
        composeRuleScope {
            setAction(isCuWarningStatusVisible = true)

            onNodeWithTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_WARNING_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the warning banner is shown when the warning icon is clicked`() {
        composeRuleScope {
            val updateIsWarningBannerShown = mock<(value: Boolean) -> Unit>()
            setAction(
                isCuWarningStatusVisible = true,
                updateIsWarningBannerShown = updateIsWarningBannerShown
            )

            onNodeWithTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_WARNING_TAG).performClick()

            verify(updateIsWarningBannerShown).invoke(true)
        }
    }

    @Test
    fun `test that the default icon is displayed`() {
        composeRuleScope {
            setAction(
                isCuWarningStatusVisible = false,
                isCuDefaultStatusVisible = true
            )

            onNodeWithTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_DEFAULT_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the user is navigated to the camera uploads settings when the default icon is clicked`() {
        composeRuleScope {
            val onNavigateCameraUploadsSettings = mock<() -> Unit>()
            setAction(
                isCuWarningStatusVisible = false,
                isCuDefaultStatusVisible = true,
                onNavigateToCameraUploadsSettings = onNavigateCameraUploadsSettings
            )

            onNodeWithTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_DEFAULT_TAG).performClick()

            verify(onNavigateCameraUploadsSettings).invoke()
        }
    }

    @Test
    fun `test that the complete icon is displayed`() {
        composeRuleScope {
            setAction(
                isCuWarningStatusVisible = false,
                isCuDefaultStatusVisible = false,
                isCuCompleteStatusVisible = true
            )

            onNodeWithTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_COMPLETE_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the camera upload message is displayed when the complete icon is clicked`() {
        composeRuleScope {
            val setCameraUploadsMessage = mock<(message: String) -> Unit>()
            setAction(
                isCuWarningStatusVisible = false,
                isCuDefaultStatusVisible = false,
                isCuCompleteStatusVisible = true,
                setCameraUploadsMessage = setCameraUploadsMessage
            )

            onNodeWithTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_COMPLETE_TAG).performClick()

            verify(setCameraUploadsMessage).invoke(any())
        }
    }

    @Test
    fun `test that the sync icon is displayed`() {
        composeRuleScope {
            setAction(
                isCuWarningStatusVisible = false,
                isCuDefaultStatusVisible = false,
                isCuCompleteStatusVisible = false,
                cameraUploadsStatus = CUStatusUiState.Sync
            )

            onNodeWithTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_SYNC_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the upload in-progress icon is displayed`() {
        composeRuleScope {
            setAction(
                isCuWarningStatusVisible = false,
                isCuDefaultStatusVisible = false,
                isCuCompleteStatusVisible = false,
                cameraUploadsStatus = CUStatusUiState.UploadInProgress(1F, 1)
            )

            onNodeWithTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_UPLOAD_IN_PROGRESS_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the upload complete icon is displayed`() {
        composeRuleScope {
            setAction(
                isCuWarningStatusVisible = false,
                isCuDefaultStatusVisible = false,
                isCuCompleteStatusVisible = false,
                cameraUploadsStatus = CUStatusUiState.UploadComplete
            )

            onNodeWithTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_UPLOAD_COMPLETE_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the user is successfully navigated to camera uploads progress screen`() {
        composeRuleScope {
            val onNavigateToCameraUploadsProgressScreen = mock<() -> Unit>()
            setAction(
                isCuWarningStatusVisible = false,
                isCuDefaultStatusVisible = false,
                isCuCompleteStatusVisible = false,
                cameraUploadsStatus = CUStatusUiState.UploadInProgress(1F, 1),
                onNavigateToCameraUploadsProgressScreen = onNavigateToCameraUploadsProgressScreen
            )

            onNodeWithTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_UPLOAD_IN_PROGRESS_TAG).performClick()

            verify(onNavigateToCameraUploadsProgressScreen).invoke()
        }
    }

    private fun composeRuleScope(block: ComposeContentTestRule.() -> Unit) {
        with(composeRule) {
            block()
        }
    }

    private fun ComposeContentTestRule.setAction(
        isCuWarningStatusVisible: Boolean = false,
        isCuDefaultStatusVisible: Boolean = false,
        isCuCompleteStatusVisible: Boolean = false,
        cameraUploadsStatus: CUStatusUiState = CUStatusUiState.None,
        setCameraUploadsMessage: (message: String) -> Unit = {},
        updateIsWarningBannerShown: (value: Boolean) -> Unit = {},
        onNavigateToCameraUploadsSettings: () -> Unit = {},
        onNavigateToCameraUploadsProgressScreen: () -> Unit = {},
    ) {
        setContent {
            CameraUploadStatusToolbarAction(
                isCuWarningStatusVisible = isCuWarningStatusVisible,
                isCuDefaultStatusVisible = isCuDefaultStatusVisible,
                isCuCompleteStatusVisible = isCuCompleteStatusVisible,
                cameraUploadsStatus = cameraUploadsStatus,
                setCameraUploadsMessage = setCameraUploadsMessage,
                updateIsWarningBannerShown = updateIsWarningBannerShown,
                onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                onNavigateToCameraUploadsProgressScreen = onNavigateToCameraUploadsProgressScreen
            )
        }
    }
}
