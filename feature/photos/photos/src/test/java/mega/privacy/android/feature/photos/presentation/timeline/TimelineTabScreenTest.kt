package mega.privacy.android.feature.photos.presentation.timeline

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.MediaCameraUploadUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TimelineTabScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that by default the sort dialog is not displayed`() {
        composeRuleScope {
            setScreen()

            onNodeWithTag(TIMELINE_TAB_SCREEN_SORT_DIALOG_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that the sort dialog is successfully displayed`() {
        composeRuleScope {
            setScreen(showTimelineSortDialog = true)

            // The internal implementation of BasicRadioDialog uses the parent modifier for two different
            // components. Therefore, in this test, we only need to verify the first node.
            onAllNodesWithTag(TIMELINE_TAB_SCREEN_SORT_DIALOG_TAG)
                .onFirst()
                .assertIsDisplayed()
        }
    }

    private fun composeRuleScope(block: ComposeContentTestRule.() -> Unit) {
        with(composeRule) {
            block()
        }
    }

    private fun ComposeContentTestRule.setScreen(
        uiState: TimelineTabUiState = TimelineTabUiState(),
        mediaCameraUploadUiState: MediaCameraUploadUiState = MediaCameraUploadUiState(),
        timelineFilterUiState: TimelineFilterUiState = TimelineFilterUiState(),
        showTimelineSortDialog: Boolean = false,
        clearCameraUploadsMessage: () -> Unit = {},
        clearCameraUploadsCompletedMessage: () -> Unit = {},
        onChangeCameraUploadsPermissions: () -> Unit = {},
        clearCameraUploadsChangePermissionsMessage: () -> Unit = {},
        loadNextPage: () -> Unit = {},
        onNavigateCameraUploadsSettings: () -> Unit = {},
        setEnableCUPage: (Boolean) -> Unit = {},
        onGridSizeChange: (value: TimelineGridSize) -> Unit = {},
        onSortDialogDismissed: () -> Unit = {},
        onSortOptionChange: (value: TimelineTabSortOptions) -> Unit = {},
        onPhotoClick: (node: PhotoNodeUiState) -> Unit = {},
        onPhotoSelected: (node: PhotoNodeUiState) -> Unit = {},
        onDismissEnableCameraUploadsBanner: () -> Unit = {},
        handleCameraUploadsPermissionsResult: () -> Unit = {},
        updateIsWarningBannerShown: (value: Boolean) -> Unit = {},
    ) {
        setContent {
            TimelineTabScreen(
                uiState = uiState,
                mediaCameraUploadUiState = mediaCameraUploadUiState,
                timelineFilterUiState = timelineFilterUiState,
                showTimelineSortDialog = showTimelineSortDialog,
                clearCameraUploadsMessage = clearCameraUploadsMessage,
                clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
                onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                clearCameraUploadsChangePermissionsMessage = clearCameraUploadsChangePermissionsMessage,
                loadNextPage = loadNextPage,
                onNavigateCameraUploadsSettings = onNavigateCameraUploadsSettings,
                setEnableCUPage = setEnableCUPage,
                onGridSizeChange = onGridSizeChange,
                onSortDialogDismissed = onSortDialogDismissed,
                onSortOptionChange = onSortOptionChange,
                onPhotoClick = onPhotoClick,
                onPhotoSelected = onPhotoSelected,
                onDismissEnableCameraUploadsBanner = onDismissEnableCameraUploadsBanner,
                handleCameraUploadsPermissionsResult = handleCameraUploadsPermissionsResult,
                updateIsWarningBannerShown = updateIsWarningBannerShown
            )
        }
    }
}
