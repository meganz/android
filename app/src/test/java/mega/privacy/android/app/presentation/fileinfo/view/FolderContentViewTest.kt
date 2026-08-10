package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.fromPluralId
import mega.privacy.android.app.onNodeWithPlural
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.shared.resources.R as SharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderContentViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that empty string is shown when number of folders and number of files is 0`() {
        composeTestRule.setContent {
            FolderContentView(numberOfFolders = 0, numberOfFiles = 0)
        }

        composeTestRule.onNodeWithText(SharedR.string.empty_file_browser_folder)
            .assertExists()
    }

    @Test
    fun `test that the files text is shown when the folder has files but no sub-folders`() {
        composeTestRule.setContent {
            FolderContentView(numberOfFolders = 0, numberOfFiles = 1)
        }

        composeTestRule.onNodeWithPlural(SharedR.plurals.num_of_files_with_parameter, 1)
            .assertExists()
    }

    @Test
    fun `test that the folder text is shown when the folder has sub-folders but no files`() {
        composeTestRule.setContent {
            FolderContentView(numberOfFolders = 1, numberOfFiles = 0)
        }

        composeTestRule.onNodeWithPlural(SharedR.plurals.num_of_folders_with_parameter, 1)
            .assertExists()
    }

    @Test
    fun `test that the files text and folder text are shown when the folder has files and sub-folders`() {
        composeTestRule.setContent {
            FolderContentView(numberOfFolders = 1, numberOfFiles = 1)
        }

        composeTestRule.onNodeWithText(
            fromPluralId(SharedR.plurals.num_of_folders_and_num_of_files, 1),
            substring = true
        ).assertExists()

        composeTestRule.onNodeWithText(
            fromPluralId(SharedR.plurals.num_of_files_with_parameter, 1),
            substring = true
        ).assertExists()
    }
}