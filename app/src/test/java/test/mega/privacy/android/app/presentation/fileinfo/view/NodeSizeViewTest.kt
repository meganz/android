package test.mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.fileinfo.view.NodeSizeView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class NodeSizeViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that a label with total size is shown when is folder is true`() {
        composeTestRule.setContent {
            NodeSizeView(forFolder = true, sizeString = "100 Bytes")
        }
        composeTestRule.onNodeWithText(R.string.file_properties_info_size).assertExists()
        composeTestRule.onNodeWithText(R.string.file_properties_info_size_file).assertDoesNotExist()
    }

    @Test
    fun `test that label with size is shown when is folder is false`() {
        composeTestRule.setContent {
            NodeSizeView(forFolder = false, sizeString = "100 Bytes")
        }
        composeTestRule.onNodeWithText(R.string.file_properties_info_size_file).assertExists()
        composeTestRule.onNodeWithText(R.string.file_properties_info_size).assertDoesNotExist()
    }
}