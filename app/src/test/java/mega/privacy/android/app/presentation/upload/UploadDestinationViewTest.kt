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
                fileList = emptyList(),
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
                fileList = listOf(
                    Pair("file1", "path1"),
                    Pair("file2", "path2"),
                    Pair("file3", "path3")
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
                fileList = listOf(
                    Pair("file1", "path1"),
                    Pair("file2", "path2"),
                    Pair("file3", "path3"),
                    Pair("file4", "path4"),
                    Pair("file5", "path5"),
                    Pair("file6", "path6"),
                    Pair("file7", "path7"),
                    Pair("file8", "path8"),
                    Pair("file9", "path9"),
                    Pair("file10", "path10"),
                    Pair("file11", "path11"),
                    Pair("file12", "path12"),
                    Pair("file13", "path13"),
                    Pair("file14", "path14"),
                    Pair("file15", "path15")
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
                fileList = listOf(
                    Pair("file1", "path1"),
                ),
                isUrl = true
            )
        }
        composeTestRule.onNodeWithText("Link", useUnmergedTree = true).assertExists()
    }
}