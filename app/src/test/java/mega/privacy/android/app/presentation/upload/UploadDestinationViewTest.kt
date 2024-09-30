package mega.privacy.android.app.presentation.upload

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadDestinationViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that empty screen is displayed to the user when there is no files`() {
        composeTestRule.setContent {
            UploadDestinationView(
                editFileName = {},
                isValidNameForUpload = {true},
                consumeNameValidationError = {},
                updateFileName = {},
                uiState = UploadDestinationUiState(),
                navigateToChats = {},
                navigateToCloudDrive = {},
            )
        }
        composeTestRule.onNodeWithText("Upload to MEGA", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(
            UPLOAD_DESTINATION_VIEW_CHOOSE_DESTINATION_TEXT,
            useUnmergedTree = true
        ).assertExists()
        composeTestRule.onNodeWithTag(UPLOAD_DESTINATION_VIEW_CLOUD_DRIVE, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(UPLOAD_DESTINATION_VIEW_CHAT, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(
            UPLOAD_DESTINATION_VIEW_SHOW_MORE_TEXT,
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    @Test
    fun `test that show more options are not shown to user when less than 4 number of files selected`() {
        composeTestRule.setContent {
            UploadDestinationView(
                editFileName = {},
                isValidNameForUpload = {true},
                consumeNameValidationError = {},
                updateFileName = {},
                navigateToChats = {},
                navigateToCloudDrive = {},
                uiState = UploadDestinationUiState(
                    importUiItems = listOf(
                        ImportUiItem("file1", "path1", fileName = "file1"),
                        ImportUiItem("file2", "path2", fileName = "file2"),
                        ImportUiItem("file3", "path3", fileName = "file3"),
                    ),
                ),
            )
        }
        composeTestRule.onNodeWithText("Upload to MEGA", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(
            UPLOAD_DESTINATION_VIEW_CHOOSE_DESTINATION_TEXT,
            useUnmergedTree = true
        ).assertExists()
        composeTestRule.onNodeWithTag(UPLOAD_DESTINATION_VIEW_CLOUD_DRIVE, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(UPLOAD_DESTINATION_VIEW_CHAT, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(
            UPLOAD_DESTINATION_VIEW_SHOW_MORE_TEXT,
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    @Test
    fun `test that show more options is shown to user when more than 4 files are selected`() {
        composeTestRule.setContent {
            UploadDestinationView(
                editFileName = {},
                isValidNameForUpload = {true},
                consumeNameValidationError = {},
                updateFileName = {},
                navigateToChats = {},
                navigateToCloudDrive = {},
                uiState = UploadDestinationUiState(
                    importUiItems = listOf(
                        ImportUiItem(originalFileName = "file1", filePath = "path1", fileName = "file1"),
                        ImportUiItem(originalFileName = "file2", filePath = "path2", fileName = "file2"),
                        ImportUiItem(originalFileName = "file3", filePath = "path3", fileName = "file3"),
                        ImportUiItem(originalFileName = "file4", filePath = "path4", fileName = "file4"),
                        ImportUiItem(originalFileName = "file5", filePath = "path5", fileName = "file5"),
                        ImportUiItem(originalFileName = "file6", filePath = "path6", fileName = "file6"),
                        ImportUiItem(originalFileName = "file7", filePath = "path7", fileName = "file7"),
                        ImportUiItem(originalFileName = "file8", filePath = "path8", fileName = "file8"),
                        ImportUiItem(originalFileName = "file9", filePath = "path9", fileName = "file9"),
                    ),
                ),
            )
        }
        composeTestRule.onNodeWithText("Upload to MEGA", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(
            UPLOAD_DESTINATION_VIEW_CHOOSE_DESTINATION_TEXT,
            useUnmergedTree = true
        ).assertExists()
        composeTestRule.onNodeWithTag(UPLOAD_DESTINATION_VIEW_CLOUD_DRIVE, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(UPLOAD_DESTINATION_VIEW_CHAT, useUnmergedTree = true)
            .assertExists()
        composeTestRule.onNodeWithTag(
            UPLOAD_DESTINATION_VIEW_SHOW_MORE_TEXT,
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun `test that header shows Link test when selected content is url`() {
        composeTestRule.setContent {
            UploadDestinationView(
                editFileName = {},
                isValidNameForUpload = {true},
                consumeNameValidationError = {},
                updateFileName = {},
                navigateToChats = {},
                navigateToCloudDrive = {},
                uiState = UploadDestinationUiState(
                    importUiItems = listOf(
                        ImportUiItem("file1", "path1", isUrl = true, fileName = "file1"),
                    ),
                ),
            )
        }
        composeTestRule.onNodeWithText("Link", useUnmergedTree = true).assertExists()
    }
}