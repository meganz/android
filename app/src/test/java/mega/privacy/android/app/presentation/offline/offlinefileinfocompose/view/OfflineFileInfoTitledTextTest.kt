package mega.privacy.android.app.presentation.offline.offlinefileinfocompose.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineFileInfoTitledTextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that label is shown`() {
        val title = "title"
        composeTestRule.setContent {
            OfflineFileInfoTitledText(title, text = "text")
        }
        composeTestRule.onNodeWithText(title).assertExists()
    }

    @Test
    fun `test that text is shown`() {
        val text = "text"
        composeTestRule.setContent {
            OfflineFileInfoTitledText(title = "title", text)
        }
        composeTestRule.onNodeWithText(text).assertExists()
    }
}
