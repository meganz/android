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
                editableFile = "",
                importUiItems = emptyList(),
                isUrl = false
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
                editableFile = "",
                importUiItems = listOf(
                    ImportUiItem("file1", "path1"),
                    ImportUiItem("file2", "path2"),
                    ImportUiItem("file3", "path3"),
                ),
                isUrl = false
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
                editableFile = "",
                importUiItems = listOf(
                    ImportUiItem(fileName = "file1", filePath = "path1"),
                    ImportUiItem(fileName = "file2", filePath = "path2"),
                    ImportUiItem(fileName = "file3", filePath = "path3"),
                    ImportUiItem(fileName = "file4", filePath = "path4"),
                    ImportUiItem(fileName = "file5", filePath = "path5"),
                    ImportUiItem(fileName = "file6", filePath = "path6"),
                    ImportUiItem(fileName = "file7", filePath = "path7"),
                    ImportUiItem(fileName = "file8", filePath = "path8"),
                    ImportUiItem(fileName = "file9", filePath = "path9"),
                ),
                isUrl = false
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
                editableFile = "",
                importUiItems = listOf(
                    ImportUiItem("file1", "path1"),
                    ImportUiItem("file2", "path2"),
                ),
                isUrl = true
            )
        }
        composeTestRule.onNodeWithText("Link", useUnmergedTree = true).assertExists()
    }
}