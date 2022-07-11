package test.mega.privacy.android.app.presentation.settings.reportissue.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.settings.reportissue.view.ErrorBanner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_that_error_message_is_displayed() {
        val errorMessage = "expectedText"
        composeTestRule.setContent {
            ErrorBanner(errorMessage = errorMessage)
        }

        composeTestRule.onNodeWithText(errorMessage).assertExists("Error message is not displayed")
    }
}