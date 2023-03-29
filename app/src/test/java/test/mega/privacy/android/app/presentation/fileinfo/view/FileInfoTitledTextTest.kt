package test.mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.fileinfo.view.FileInfoTitledText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileInfoTitledTextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that label is shown`() {
        val title = "title"
        composeTestRule.setContent {
            FileInfoTitledText(title, text = "text")
        }
        composeTestRule.onNodeWithText(title).assertExists()
    }

    @Test
    fun `test that text is shown`() {
        val text = "text"
        composeTestRule.setContent {
            FileInfoTitledText(title = "title", text)
        }
        composeTestRule.onNodeWithText(text).assertExists()
    }
}